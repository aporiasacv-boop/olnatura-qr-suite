package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.user.Role;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.RoleRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.pdf.UsersPdfService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.security.CredentialRules;
import com.lowagie.text.DocumentException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmm")
            .withZone(ZoneId.of("America/Mexico_City"));

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final UsersPdfService usersPdfService;
    private final PasswordEncoder passwordEncoder;

    public AdminUsersController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuditService auditService,
            UsersPdfService usersPdfService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
        this.usersPdfService = usersPdfService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UserAdminDto> list() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal AuthPrincipal principal) {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        Instant generatedAt = Instant.now();
        String generatedBy = principal != null ? principal.username() : null;

        byte[] pdf;
        try {
            pdf = usersPdfService.generate(users, generatedAt, generatedBy);
        } catch (DocumentException e) {
            throw new RuntimeException("Error al generar PDF de usuarios", e);
        }

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("exportType", "PDF");
        md.put("countUsers", users.size());
        md.put("requester", generatedBy != null ? generatedBy : "anonymous");
        auditService.log(principal, "EXPORT_USERS_PDF", null, md, null);

        String filename = "usuarios-olnatura-qr-" + FILE_TS.format(generatedAt) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
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

        if (req.canCreateLoteComments() != null
                && u.isCanCreateLoteComments() != req.canCreateLoteComments()) {
            u.setCanCreateLoteComments(req.canCreateLoteComments());
            changed = true;
        }

        if (changed) {
            userRepository.save(u);
            Map<String, Object> md = new LinkedHashMap<>();
            md.put("targetUserId", id.toString());
            md.put("targetUsername", u.getUsername());
            md.put("enabled", u.isEnabled());
            md.put("role", u.getRole() != null ? u.getRole().getName() : "?");
            md.put("canCreateLoteComments", u.isCanCreateLoteComments());
            auditService.log(principal, "UPDATE_USER", null, md, null);
        }

        return ResponseEntity.ok(toDto(u));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody ResetPasswordRequest req
    ) {
        String password = req == null || req.password() == null ? "" : req.password();
        String passwordErr = CredentialRules.passwordError(password);
        if (passwordErr != null) {
            throw new ResponseStatusException(BAD_REQUEST, passwordErr);
        }
        User u = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));
        u.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(u);

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("targetUserId", id.toString());
        md.put("targetUsername", u.getUsername());
        auditService.log(principal, "RESET_USER_PASSWORD", null, md, null);
        return ResponseEntity.noContent().build();
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
                u.isCanCreateLoteComments(),
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
            boolean canCreateLoteComments,
            String createdAt
    ) {}

    public record PatchUserRequest(Boolean enabled, String role, Boolean canCreateLoteComments) {}

    public record ResetPasswordRequest(String password) {}
}
