package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.comment.LoteComment;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.LoteCommentRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.qr.LoteExtractor;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.workflow.LotOperationalGate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Bitácora operativa de comentarios por lote.
 * Inmutable: no hay endpoints de edición ni borrado.
 */
@RestController
@RequestMapping("/api/v1/comments")
@PreAuthorize("hasAnyRole('ADMIN','ALMACEN','CALIDAD','INSPECCION')")
public class LoteCommentController {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "ALMACEN", "CALIDAD", "INSPECCION");
    private static final int MAX_BODY = 200;

    private final LoteCommentRepository commentRepository;
    private final QrLabelRepository qrLabelRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public LoteCommentController(
            LoteCommentRepository commentRepository,
            QrLabelRepository qrLabelRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.commentRepository = commentRepository;
        this.qrLabelRepository = qrLabelRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/{lote}")
    public ResponseEntity<List<LoteCommentDto.Response>> list(@PathVariable String lote) {
        String actualLote = resolveToLote(lote);
        List<LoteCommentDto.Response> items = commentRepository.findByLoteOrderByCreatedAtAsc(actualLote)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{lote}")
    public ResponseEntity<LoteCommentDto.Response> create(
            @PathVariable String lote,
            @RequestBody LoteCommentDto.CreateRequest req,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (principal == null || principal.id() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Sesión requerida");
        }

        String body = req == null || req.comment() == null ? "" : req.comment().trim();
        if (body.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "El comentario no puede estar vacío");
        }
        if (body.length() > MAX_BODY) {
            throw new ResponseStatusException(BAD_REQUEST, "El comentario supera el máximo de " + MAX_BODY + " caracteres");
        }

        String actualLote = resolveToLote(lote);
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Usuario no encontrado"));

        String role = resolveAuthorRole(principal, user);
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(FORBIDDEN, "Tu rol no puede agregar comentarios");
        }

        String displayName = user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername().trim()
                : principal.username();

        LoteComment c = new LoteComment();
        c.setLote(actualLote);
        c.setAuthorUserId(user.getId());
        c.setAuthorUsername(user.getUsername());
        c.setAuthorDisplayName(displayName);
        c.setAuthorRole(role);
        c.setBody(body);

        LoteComment saved = commentRepository.save(c);

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("commentId", saved.getId().toString());
        md.put("rol", role);
        md.put("preview", body.length() > 120 ? body.substring(0, 117) + "…" : body);
        auditService.log(principal, "ADD_LOTE_COMMENT", actualLote, md, null);

        return ResponseEntity.ok(toResponse(saved));
    }

    private LoteCommentDto.Response toResponse(LoteComment c) {
        return new LoteCommentDto.Response(
                c.getId(),
                c.getLote(),
                c.getAuthorUserId(),
                c.getAuthorUsername(),
                c.getAuthorDisplayName(),
                c.getAuthorRole(),
                c.getCreatedAt(),
                c.getBody()
        );
    }

    private static String resolveAuthorRole(AuthPrincipal principal, User user) {
        if (user.getRole() != null && user.getRole().getName() != null) {
            String fromDb = user.getRole().getName().trim().toUpperCase(Locale.ROOT);
            if (ALLOWED_ROLES.contains(fromDb)) {
                return fromDb;
            }
        }
        if (principal.roles() != null) {
            for (String r : principal.roles()) {
                if (r == null) continue;
                String n = r.trim().toUpperCase(Locale.ROOT);
                if (ALLOWED_ROLES.contains(n)) {
                    return n;
                }
            }
        }
        return "";
    }

    private String resolveToLote(String raw) {
        String identifier = LoteExtractor.extract(raw).orElse(raw != null ? raw.trim() : "");
        if (identifier.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Identificador vacío");
        }
        QrLabel label = qrLabelRepository.findByPublicToken(identifier)
                .or(() -> qrLabelRepository.findByLote(identifier))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + identifier));
        LotOperationalGate.requireActive(label);
        return label.getLote();
    }
}
