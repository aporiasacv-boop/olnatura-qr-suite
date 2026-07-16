package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sugerencias de lote desde PostgreSQL (sin Dynamics).
 */
@RestController
@RequestMapping("/api/v1/labels")
public class LabelSuggestController {

    private static final int MAX_RESULTS = 15;
    private static final int MIN_QUERY = 1;

    private final QrLabelRepository qrLabelRepository;

    public LabelSuggestController(QrLabelRepository qrLabelRepository) {
        this.qrLabelRepository = qrLabelRepository;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/suggest")
    public List<SuggestItem> suggest(@RequestParam(name = "q", defaultValue = "") String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < MIN_QUERY) {
            return List.of();
        }
        return qrLabelRepository.searchSuggest(query, MAX_RESULTS).stream()
                .map(SuggestItem::from)
                .toList();
    }

    public record SuggestItem(
            String lote,
            String codigo,
            String nombre,
            String status
    ) {
        static SuggestItem from(QrLabel l) {
            return new SuggestItem(
                    l.getLote(),
                    l.getCodigo(),
                    l.getNombre(),
                    WorkflowStatus.normalize(l.getStatus())
            );
        }
    }
}
