package com.company.olnaturaqr.support.bootstrap;

import com.company.olnaturaqr.support.config.BootstrapAdminProperties;
import com.company.olnaturaqr.repository.RoleRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.domain.user.User;

import org.springframework.boot.CommandLineRunner;
import com.company.olnaturaqr.domain.user.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

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

    @Override
    @Transactional
    public void run(String... args) {
        if (!props.enabled()) {
            return;
        }

        String username = props.username();
        if (username == null || username.isBlank()) {
            return;
        }

        // ===== id =====
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        // ===== rol ADMIN =====
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException("Rol ADMIN no existe. Revisa migraciones.")
                );

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(props.email());
        user.setPasswordHash(passwordEncoder.encode(props.password()));
        user.setEnabled(true); // si se crea es porque debe estar activo
        user.setRole(adminRole);

        userRepository.save(user);

        System.out.println("✅ Bootstrap admin creado: " + username);
    }
}