package com.livel.escudo;

import com.livel.escudo.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class EscudoApplication {
    public static void main(String[] args) {
        SpringApplication.run(EscudoApplication.class, args);
    }
}

