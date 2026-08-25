package com.livel.escudo.scan;

import org.springframework.web.multipart.MultipartFile;

public interface ImageTextExtractor {
    String extract(MultipartFile file);
}
