package com.company.olnaturaqr.support.bootstrap;

import com.company.olnaturaqr.domain.user.Role;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.RoleRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.config.BootstrapAdminProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin inicial idempotente:
 * - Si no existe: lo crea.
 * - Si existe, enabled=true y password coincide: no modifica nada.
 * - Si existe y (enabled=false o password no coincide con bootstrap): recuperación.
 * No toca otros usuarios ni crea duplicados.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String ADMIN_ROLE_NAME = "ADMIN";
    private static final String DEFAULT_BOOTSTRAP_PASSWORD = "Admin123!";

    private final BootstrapAdminProperties props;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminBootstrapRunner(
            BootstrapAdminProperties props,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.props = props;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    void diagnoseBeanCreated() {
        System.out.println("=== ADMIN BOOTSTRAP BEAN CREATED ===");
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("=== ADMIN BOOTSTRAP RUN EXECUTED ===");

        String cfgUsername = props.username();
        String cfgEmail = props.email();
        String effectivePassword = resolveBootstrapPassword();
        int passwordLength = effectivePassword == null ? 0 : effectivePassword.length();

        System.out.println("=== ADMIN BOOTSTRAP CONFIG ===");
        System.out.println("enabled=" + props.enabled());
        System.out.println("username=" + cfgUsername);
        System.out.println("email=" + cfgEmail);
        System.out.println("passwordLength=" + passwordLength);

        log.info("Bootstrap admin: inicio (enabled={})", props.enabled());

        if (!props.enabled()) {
            System.out.println("BOOTSTRAP DECISION = OMITIDO");
            log.info("Bootstrap admin: decisión=OMITIDO (app.bootstrap.admin.enabled=false)");
            return;
        }

        String username = props.username();
        if (username == null || username.isBlank()) {
            System.out.println("BOOTSTRAP DECISION = OMITIDO");
            log.warn("Bootstrap admin: decisión=OMITIDO (username vacío)");
            return;
        }

        username = username.trim();
        String password = effectivePassword;
        String email = resolveBootstrapEmail();
        if (email == null) {
            System.out.println("BOOTSTRAP DECISION = OMITIDO");
            log.warn("Bootstrap admin: decisión=OMITIDO (email vacío)");
            return;
        }

        var existingOpt = userRepository.findByUsernameIgnoreCase(username);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            boolean passwordOk = passwordEncoder.matches(password, existing.getPasswordHash());
            String roleName = existing.getRole() != null ? existing.getRole().getName() : "null";

            System.out.println("=== ADMIN BOOTSTRAP EXISTING USER ===");
            System.out.println("username=" + existing.getUsername());
            System.out.println("enabled=" + existing.isEnabled());
            System.out.println("roles=" + roleName);
            System.out.println("passwordMatchesBootstrap=" + passwordOk);

            if (existing.isEnabled() && passwordOk) {
                System.out.println("BOOTSTRAP DECISION = REUTILIZAR");
                log.info(
                        "Bootstrap admin: decisión=REUTILIZAR sin cambios (usuario='{}' enabled=true password=ok)",
                        existing.getUsername()
                );
                return;
            }

            String reason = !existing.isEnabled()
                    ? "enabled=false"
                    : "password mismatch vs bootstrap";
            System.out.println("BOOTSTRAP DECISION = RECUPERAR");
            log.info(
                    "Bootstrap admin: decisión=RECUPERAR (usuario='{}' motivo={})",
                    existing.getUsername(),
                    reason
            );
            recoverAdmin(existing, password, email, reason);
            return;
        }

        System.out.println("BOOTSTRAP DECISION = CREAR");
        createAdmin(username, password, email);
    }

    /**
     * Azure a menudo define APP_BOOTSTRAP_ADMIN_PASSWORD="" y eso anula el default
     * del YAML. Tratar blank como "usar default seguro de arranque".
     */
    private String resolveBootstrapPassword() {
        String password = props.password();
        if (password == null || password.isBlank()) {
            log.warn(
                    "Bootstrap admin: password vacío en config/env; usando default de arranque (no se imprime el valor)"
            );
            return DEFAULT_BOOTSTRAP_PASSWORD;
        }
        return password;
    }

    private String resolveBootstrapEmail() {
        String email = props.email();
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim();
    }

    private void recoverAdmin(User existing, String password, String email, String reason) {
        Role adminRole = resolveAdminRole();

        existing.setEnabled(true);
        existing.setPasswordHash(passwordEncoder.encode(password));
        existing.setRole(adminRole);

        if (!email.equalsIgnoreCase(existing.getEmail())) {
            var emailOwner = userRepository.findByEmailIgnoreCase(email);
            if (emailOwner.isPresent() && !emailOwner.get().getId().equals(existing.getId())) {
                log.warn(
                        "Bootstrap admin: email '{}' ya está en uso por otro usuario; se mantiene email actual '{}'",
                        email,
                        existing.getEmail()
                );
            } else {
                log.info(
                        "Bootstrap admin: actualización de email '{}' -> '{}'",
                        existing.getEmail(),
                        email
                );
                existing.setEmail(email);
            }
        } else {
            log.info("Bootstrap admin: email sin cambios ('{}')", existing.getEmail());
        }

        userRepository.save(existing);

        log.info(
                "Bootstrap admin: recuperación aplicada (usuario='{}' enabled={} rol={} motivo={} password=actualizado-BCrypt)",
                existing.getUsername(),
                existing.isEnabled(),
                ADMIN_ROLE_NAME,
                reason
        );
    }

    private void createAdmin(String username, String password, String email) {
        log.info("Bootstrap admin: decisión=CREAR (usuario '{}' no existe)", username);

        Role adminRole = resolveAdminRole();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setRole(adminRole);

        userRepository.save(user);

        log.info(
                "Bootstrap admin: creado (usuario='{}' email='{}' enabled={} rol={})",
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                ADMIN_ROLE_NAME
        );
    }

    private Role resolveAdminRole() {
        return roleRepository.findByName(ADMIN_ROLE_NAME).map(role -> {
            log.info("Bootstrap admin: reutilizado rol '{}' (id={})", role.getName(), role.getId());
            return role;
        }).orElseGet(() -> {
            Role created = roleRepository.save(new Role(ADMIN_ROLE_NAME));
            log.info("Bootstrap admin: creado rol '{}' (id={})", created.getName(), created.getId());
            return created;
        });
    }
}
