package com.company.olnaturaqr.support.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class NoopUserDetailsConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        // Evita que Spring cree el usuario default con password generado
        return username -> { throw new UnsupportedOperationException("No UserDetailsService"); };
    }
}