package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.AdminLotDeleteService;
import com.company.olnaturaqr.support.workflow.AdminLotStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/lotes")
public class LotesScreenController {

    private final QrLabelRepository qrLabelRepository;
    private final UserRepository userRepository;
    private final AdminLotDeleteService lotDeleteService;

    public LotesScreenController(
            QrLabelRepository qrLabelRepository,
            UserRepository userRepository,
            AdminLotDeleteService lotDeleteService
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.userRepository = userRepository;
        this.lotDeleteService = lotDeleteService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LotRow> list(@AuthenticationPrincipal AuthPrincipal principal) {
        requireAccess(principal);
        return qrLabelRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toRow).toList();
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdminLotDeleteService.DeleteResult> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AdminLotDeleteService.DeleteRequest req
    ) {
        requireAccess(principal);
        return ResponseEntity.ok(lotDeleteService.deleteLot(id, principal, req));
    }

    private void requireAccess(AuthPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Sesión requerida");
        }
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Usuario no encontrado"));
        boolean admin = user.getRole() != null
                && user.getRole().getName() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName().trim());
        if (!admin && !user.isCanDeleteLotes()) {
            throw new ResponseStatusException(FORBIDDEN, "No tienes acceso a Lotes");
        }
    }

    private LotRow toRow(QrLabel q) {
        String admin = AdminLotStatus.normalize(q.getAdminStatus());
        return new LotRow(
                q.getId().toString(),
                q.getLote(),
                q.getCodigo(),
                q.getNombre(),
                AdminLotStatus.display(admin),
                q.getCreatedAt() != null ? q.getCreatedAt().toString() : null
        );
    }

    public record LotRow(
            String id,
            String lote,
            String codigo,
            String nombre,
            String adminStatusDisplay,
            String createdAt
    ) {}
}
