package com.company.olnaturaqr.support.config;

import com.company.olnaturaqr.infra.dynamics.DynamicsProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-fast en perfil {@code prod}: no arranca sin secretos y configuración crítica
 * provistos por variables de entorno (sin defaults inseguros).
 */
@Configuration
public class ProdConfigGuard {

    private static final int MIN_JWT_SECRET_LENGTH = 32;

    private final Environment env;
    private final CorsProps corsProps;
    private final DynamicsProperties dynamicsProperties;

    public ProdConfigGuard(Environment env, CorsProps corsProps, DynamicsProperties dynamicsProperties) {
        this.env = env;
        this.corsProps = corsProps;
        this.dynamicsProperties = dynamicsProperties;
    }

    @PostConstruct
    void validate() {
        if (!env.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        List<String> errors = new ArrayList<>();

        requireEnvSecret(errors, "JWT_SECRET", env.getProperty("jwt.secret"), MIN_JWT_SECRET_LENGTH);
        requireEnvSecret(errors, "SPRING_DATASOURCE_URL", env.getProperty("spring.datasource.url"), 1);
        requireEnvSecret(errors, "SPRING_DATASOURCE_USERNAME", env.getProperty("spring.datasource.username"), 1);
        requireEnvSecret(errors, "SPRING_DATASOURCE_PASSWORD", env.getProperty("spring.datasource.password"), 1);

        if (corsProps.allowedOriginsList() == null || corsProps.allowedOriginsList().isEmpty()) {
            errors.add("APP_CORS_ALLOWED_ORIGINS debe definir al menos un origen permitido");
        }

        String dynamicsMode = dynamicsProperties.getMode();
        if (isBlank(dynamicsMode)) {
            errors.add("APP_DYNAMICS_MODE es obligatorio en prod (p. ej. real)");
        } else if ("real".equalsIgnoreCase(dynamicsMode.trim())) {
            requireEnvSecret(errors, "APP_DYNAMICS_BASE_URL", dynamicsProperties.getBaseUrl(), 1);
            requireEnvSecret(errors, "APP_DYNAMICS_TENANT_ID", dynamicsProperties.getTenantId(), 1);
            requireEnvSecret(errors, "APP_DYNAMICS_CLIENT_ID", dynamicsProperties.getClientId(), 1);
            requireEnvSecret(errors, "APP_DYNAMICS_CLIENT_SECRET", dynamicsProperties.getClientSecret(), 1);
            String resource = dynamicsProperties.getResource();
            if (isBlank(resource) && isBlank(dynamicsProperties.getBaseUrl())) {
                errors.add("APP_DYNAMICS_RESOURCE o APP_DYNAMICS_BASE_URL es obligatorio con mode=real");
            }
        } else if (!"mock".equalsIgnoreCase(dynamicsMode.trim())) {
            errors.add("APP_DYNAMICS_MODE inválido: '" + dynamicsMode + "' (use real o mock)");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Configuración de producción incompleta o insegura. Corrige:\n - "
                            + String.join("\n - ", errors));
        }
    }

    private static void requireEnvSecret(List<String> errors, String name, String value, int minLength) {
        if (isBlank(value)) {
            errors.add(name + " es obligatorio (sin valor por defecto; definir variable de entorno)");
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() < minLength) {
            errors.add(name + " debe tener al menos " + minLength + " caracteres");
        }
        if (looksLikePlaceholder(trimmed)) {
            errors.add(name + " parece un placeholder inseguro; define un secreto real vía entorno");
        }
    }

    private static boolean looksLikePlaceholder(String value) {
        String v = value.toLowerCase();
        return v.contains("change-me")
                || v.contains("changeme")
                || v.contains("dev-secret")
                || v.equals("secret")
                || v.equals("password")
                || v.equals("olnatura123");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
