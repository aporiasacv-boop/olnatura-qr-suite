package com.company.olnaturaqr.api;

import com.company.olnaturaqr.support.qr.QrQueryService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class QrController {

    private final QrQueryService qrQueryService;

    public QrController(QrQueryService qrQueryService) {
        this.qrQueryService = qrQueryService;
    }

    @GetMapping("/{lote}")
    public QrDto.Response getByLote(
            @PathVariable String lote,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return qrQueryService.getByLote(lote, principal);
    }
}