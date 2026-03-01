package com.company.olnaturaqr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class OlnaturaQrApplication {

    public static void main(String[] args) {
        SpringApplication.run(OlnaturaQrApplication.class, args);
    }
}