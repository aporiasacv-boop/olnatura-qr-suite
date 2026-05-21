package com.company.olnaturaqr.api;

import com.company.olnaturaqr.infra.dynamics.DynamicsPreviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dynamics")
public class DynamicsController {

    private final DynamicsPreviewService previewService;

    public DynamicsController(DynamicsPreviewService previewService) {
        this.previewService = previewService;
    }

    @GetMapping("/preview")
    public DynamicsPreviewResponse preview(
            @RequestParam(required = false) String itemNumber,
            @RequestParam(required = false) String lote
    ) {
        return previewService.fetchPreview(itemNumber, lote);
    }
}
