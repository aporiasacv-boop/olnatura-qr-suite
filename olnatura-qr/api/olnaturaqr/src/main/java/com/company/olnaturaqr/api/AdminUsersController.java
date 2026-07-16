package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.user.Role;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.RoleRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    public AdminUsersController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<UserAdminDto> list() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserAdminDto> patch(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody PatchUserRequest req
    ) {
        if (req == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Body requerido");
        }
        User u = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));

        boolean changed = false;

        if (req.enabled() != null) {
            if (principal != null && principal.id() != null && principal.id().equals(id) && !req.enabled()) {
                throw new ResponseStatusException(FORBIDDEN, "No puedes deshabilitarte a ti mismo");
            }
            if (u.isEnabled() != req.enabled()) {
                u.setEnabled(req.enabled());
                changed = true;
            }
        }

        if (req.role() != null && !req.role().isBlank()) {
            String roleName = req.role().trim().toUpperCase(Locale.ROOT);
            Role role = roleRepository.findByName(roleName).orElseThrow(() ->
                    new ResponseStatusException(BAD_REQUEST, "Rol inválido: " + roleName));
            if (u.getRole() == null || !roleName.equalsIgnoreCase(u.getRole().getName())) {
                if (principal != null && principal.id() != null && principal.id().equals(id)
                        && !"ADMIN".equals(roleName)) {
                    throw new ResponseStatusException(FORBIDDEN, "No puedes quitarte el rol ADMIN");
                }
                u.setRole(role);
                changed = true;
            }
        }

        if (changed) {
            userRepository.save(u);
            auditService.log(principal, "UPDATE_USER", null,
                    Map.of(
                            "targetUserId", id.toString(),
                            "targetUsername", u.getUsername(),
                            "enabled", u.isEnabled(),
                            "role", u.getRole() != null ? u.getRole().getName() : "?"
                    ), null);
        }

        return ResponseEntity.ok(toDto(u));
    }

    private UserAdminDto toDto(User u) {
        boolean enabled = u.isEnabled();
        return new UserAdminDto(
                u.getId().toString(),
                u.getUsername(),
                u.getEmail(),
                u.getRole() != null ? u.getRole().getName() : "?",
                enabled ? "Activo" : "Deshabilitado",
                enabled,
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        );
    }

    public record UserAdminDto(
            String id,
            String username,
            String email,
            String role,
            String estado,
            boolean enabled,
            String createdAt
    ) {}

    public record PatchUserRequest(Boolean enabled, String role) {}
}
