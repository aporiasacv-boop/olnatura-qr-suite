package com.company.olnaturaqr.support.security;

import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.config.AuthCookieProperties;
import com.company.olnaturaqr.support.config.CorsProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableConfigurationProperties({ AuthCookieProperties.class, CorsProps.class })
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            AuthCookieProperties cookieProps,
            CorsProps corsProps
    ) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(l -> l.disable())

            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .cors(cors -> cors.configurationSource(req -> {
                var cfg = new CorsConfiguration();
                cfg.setAllowedOrigins(corsProps.allowedOriginsList());
                cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
                cfg.setAllowedHeaders(List.of(
                        "Content-Type",
                        "Authorization",
                        "X-Device-Id",
                        "Accept",
                        "Origin"
                ));
                cfg.setAllowCredentials(true);
                cfg.setMaxAge(3600L); // cache preflight 1h
                return cfg;
            }))

        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

         // Landing pública del QR (sin login) - HTML para cámara genérica
            .requestMatchers(HttpMethod.GET, "/qr/**").permitAll()

         // Auth
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/request-access").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()

        // QR / Scan - requiere cookie de sesión
            .requestMatchers(HttpMethod.GET, "/api/v1/qr/**").authenticated()
            .requestMatchers(HttpMethod.POST, "/api/v1/scan/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/v1/scan/**").authenticated()

            .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> {
                var entryPoint = new JsonErrorEntryPoint();
                ex.authenticationEntryPoint(entryPoint);
                ex.accessDeniedHandler(entryPoint);
            })

            .addFilterBefore(
                new JwtCookieAuthFilter(jwtTokenProvider, userRepository, cookieProps),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}