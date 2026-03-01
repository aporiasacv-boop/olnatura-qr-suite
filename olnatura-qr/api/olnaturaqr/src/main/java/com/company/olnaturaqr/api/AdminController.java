package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminController(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/access-requests")
    public List<AccessRequestDto> listPending() {
        return userRepository.findTop50ByEnabledFalseOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/access-requests/{id}/approve")
    public ResponseEntity<Void> approve(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id
    ) {
        User u = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));
        u.setEnabled(true);
        userRepository.save(u);

        auditService.log(principal, "APPROVE_USER", null,
                java.util.Map.of("targetUserId", id.toString(), "targetUsername", u.getUsername()), null);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/access-requests/{id}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id
    ) {
        User u = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));

        auditService.log(principal, "REJECT_USER", null,
                java.util.Map.of("targetUserId", id.toString(), "targetUsername", u.getUsername()), null);

        userRepository.delete(u);
        return ResponseEntity.noContent().build();
    }

    private AccessRequestDto toDto(User u) {
        return new AccessRequestDto(
                u.getId().toString(),
                u.getUsername(),
                u.getEmail(),
                u.getRole() != null ? u.getRole().getName() : "?",
                u.isEnabled(),
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
        );
    }

    public record AccessRequestDto(String id, String username, String email, String role, boolean enabled, String createdAt) {}
}
