package com.livel.escudo.scan;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class ScanControllerImageTest {
    @Test void analyzesExtractedTextAndNeverUsesTheFilenameAsEvidence() {
        RecordingScanService service = new RecordingScanService();
        ImageTextExtractor extractor = ignored -> "Notificación jurídica. Acceder al documento.";
        MockMultipartFile file = new MockMultipartFile("file", "premio-urgente.png", "image/png", new byte[]{1, 2, 3});
        ScanController controller = new ScanController(service, extractor);

        controller.image(file, "Recibido por correo", null, null);

        assertThat(service.type).isEqualTo("IMAGE");
        assertThat(service.content).contains("Notificación jurídica", "Recibido por correo")
                .doesNotContain("premio-urgente.png");
    }

    private static final class RecordingScanService extends ScanService {
        private String type;
        private String content;

        private RecordingScanService() { super(null, null, null); }

        @Override
        public ScanResponse analyze(String type, String content, com.livel.escudo.user.UserEntity user,
                                    jakarta.servlet.http.HttpServletRequest request) {
            this.type = type;
            this.content = content;
            return null;
        }
    }
}
