package com.livel.escudo.scan;

import com.livel.escudo.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class TesseractImageTextExtractor implements ImageTextExtractor {
    private static final int HEADER_LIMIT = 65_536;
    private static final int MAX_TEXT_CHARS = 18_000;
    private static final int MAX_EDGE = 6_000;

    private final String command;
    private final String language;
    private final Duration timeout;
    private final long maxPixels;
    private final Semaphore slots = new Semaphore(1, true);

    public TesseractImageTextExtractor(@Value("${app.ocr.command:tesseract}") String command,
                                       @Value("${app.ocr.language:spa+eng}") String language,
                                       @Value("${app.ocr.timeout-seconds:25}") long timeoutSeconds,
                                       @Value("${app.ocr.max-pixels:8000000}") long maxPixels) {
        this.command = command;
        this.language = language;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.maxPixels = Math.max(1, maxPixels);
    }

    @Override
    public String extract(MultipartFile file) {
        validateImage(file);
        Path input = null;
        Path output = null;
        Process process = null;
        boolean slotAcquired = false;
        try {
            slotAcquired = slots.tryAcquire(2, TimeUnit.SECONDS);
            if (!slotAcquired) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OCR_BUSY",
                        "El lector de capturas está ocupado. Esperá unos segundos e intentá nuevamente.");
            }
            input = Files.createTempFile("escudo-ocr-", extensionFor(file.getContentType()));
            output = Files.createTempFile("escudo-ocr-output-", ".txt");
            file.transferTo(input);

            process = new ProcessBuilder(command, input.toString(), "stdout", "-l", language, "--psm", "6")
                    .redirectOutput(output.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "OCR_TIMEOUT",
                        "La lectura de la captura tardó demasiado. Probá con una imagen más nítida o más pequeña.");
            }
            if (process.exitValue() != 0) {
                throw unreadableImage();
            }

            String extracted = Files.readString(output, StandardCharsets.UTF_8)
                    .replace('\f', ' ')
                    .replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", " ")
                    .strip();
            if (extracted.isBlank()) throw unreadableImage();
            if (extracted.length() > MAX_TEXT_CHARS) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "OCR_TEXT_TOO_LONG",
                        "La captura contiene demasiado texto. Recortala en partes más pequeñas.");
            }
            return extracted;
        } catch (ApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OCR_INTERRUPTED",
                    "No pudimos completar la lectura de la captura. Intentá nuevamente.");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OCR_UNAVAILABLE",
                    "El lector de capturas no está disponible temporalmente.");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            deleteQuietly(input);
            deleteQuietly(output);
            if (slotAcquired) slots.release();
        }
    }

    private void validateImage(MultipartFile file) {
        byte[] header;
        try (InputStream stream = file.getInputStream()) {
            header = stream.readNBytes(HEADER_LIMIT);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "No pudimos leer la imagen enviada.");
        }

        String detectedType = detectType(header);
        if (detectedType == null || !detectedType.equals(file.getContentType())) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_IMAGE_CONTENT",
                    "El contenido del archivo no coincide con una imagen PNG, JPG o WebP válida.");
        }
        Dimensions dimensions = dimensions(header, detectedType);
        if (dimensions == null || dimensions.width() < 1 || dimensions.height() < 1) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_IMAGE_DIMENSIONS",
                    "No pudimos validar las dimensiones de la imagen.");
        }
        long pixels = (long) dimensions.width() * dimensions.height();
        if (dimensions.width() > MAX_EDGE || dimensions.height() > MAX_EDGE || pixels > maxPixels) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE",
                    "La captura tiene demasiados píxeles. Reducila antes de analizarla.");
        }
    }

    private String detectType(byte[] bytes) {
        if (startsWith(bytes, new int[]{0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return "image/png";
        if (startsWith(bytes, new int[]{0xff, 0xd8, 0xff})) return "image/jpeg";
        if (bytes.length >= 12 && ascii(bytes, 0, 4).equals("RIFF") && ascii(bytes, 8, 4).equals("WEBP")) return "image/webp";
        return null;
    }

    private Dimensions dimensions(byte[] bytes, String type) {
        return switch (type) {
            case "image/png" -> pngDimensions(bytes);
            case "image/jpeg" -> jpegDimensions(bytes);
            case "image/webp" -> webpDimensions(bytes);
            default -> null;
        };
    }

    private Dimensions pngDimensions(byte[] bytes) {
        if (bytes.length < 24) return null;
        return new Dimensions(bigEndianInt(bytes, 16), bigEndianInt(bytes, 20));
    }

    private Dimensions jpegDimensions(byte[] bytes) {
        int offset = 2;
        while (offset + 3 < bytes.length) {
            while (offset < bytes.length && unsigned(bytes[offset]) != 0xff) offset++;
            while (offset < bytes.length && unsigned(bytes[offset]) == 0xff) offset++;
            if (offset >= bytes.length) return null;
            int marker = unsigned(bytes[offset++]);
            if (marker == 0xd8 || marker == 0xd9 || marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) continue;
            if (offset + 1 >= bytes.length) return null;
            int length = unsigned(bytes[offset]) << 8 | unsigned(bytes[offset + 1]);
            if (length < 2 || offset + length > bytes.length) return null;
            if (isStartOfFrame(marker) && length >= 7) {
                int height = unsigned(bytes[offset + 3]) << 8 | unsigned(bytes[offset + 4]);
                int width = unsigned(bytes[offset + 5]) << 8 | unsigned(bytes[offset + 6]);
                return new Dimensions(width, height);
            }
            offset += length;
        }
        return null;
    }

    private boolean isStartOfFrame(int marker) {
        return marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
    }

    private Dimensions webpDimensions(byte[] bytes) {
        if (bytes.length < 30) return null;
        String chunk = ascii(bytes, 12, 4);
        if (chunk.equals("VP8X")) {
            int width = 1 + littleEndian24(bytes, 24);
            int height = 1 + littleEndian24(bytes, 27);
            return new Dimensions(width, height);
        }
        if (chunk.equals("VP8L") && unsigned(bytes[20]) == 0x2f) {
            int b1 = unsigned(bytes[21]), b2 = unsigned(bytes[22]), b3 = unsigned(bytes[23]), b4 = unsigned(bytes[24]);
            int width = 1 + (b1 | (b2 & 0x3f) << 8);
            int height = 1 + ((b2 & 0xc0) >> 6 | b3 << 2 | (b4 & 0x0f) << 10);
            return new Dimensions(width, height);
        }
        if (chunk.equals("VP8 ")) {
            for (int i = 20; i + 6 < Math.min(bytes.length, 64); i++) {
                if (unsigned(bytes[i]) == 0x9d && unsigned(bytes[i + 1]) == 0x01 && unsigned(bytes[i + 2]) == 0x2a) {
                    int width = (unsigned(bytes[i + 3]) | unsigned(bytes[i + 4]) << 8) & 0x3fff;
                    int height = (unsigned(bytes[i + 5]) | unsigned(bytes[i + 6]) << 8) & 0x3fff;
                    return new Dimensions(width, height);
                }
            }
        }
        return null;
    }

    private boolean startsWith(byte[] bytes, int[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (unsigned(bytes[i]) != prefix[i]) return false;
        return true;
    }

    private int bigEndianInt(byte[] bytes, int offset) {
        return unsigned(bytes[offset]) << 24 | unsigned(bytes[offset + 1]) << 16 |
                unsigned(bytes[offset + 2]) << 8 | unsigned(bytes[offset + 3]);
    }

    private int littleEndian24(byte[] bytes, int offset) {
        return unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8 | unsigned(bytes[offset + 2]) << 16;
    }

    private int unsigned(byte value) { return value & 0xff; }
    private String ascii(byte[] bytes, int offset, int length) { return new String(bytes, offset, length, StandardCharsets.US_ASCII); }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }

    private ApiException unreadableImage() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGE_TEXT_UNAVAILABLE",
                "No encontramos texto legible en la captura. Probá con una imagen más nítida o pegá el mensaje como texto.");
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    private record Dimensions(int width, int height) { }
}
