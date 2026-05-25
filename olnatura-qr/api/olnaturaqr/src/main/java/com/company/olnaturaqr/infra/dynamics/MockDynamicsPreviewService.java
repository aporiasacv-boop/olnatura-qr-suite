package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.api.DynamicsPreviewResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@ConditionalOnProperty(prefix = "app.dynamics", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockDynamicsPreviewService implements DynamicsPreviewService {

    @Override
    public DynamicsPreviewResponse fetchPreview(String itemNumber, String lote) {
        String item = itemNumber != null ? itemNumber.trim() : "";
        String batch = lote != null ? lote.trim() : "";
        if (item.isEmpty() && batch.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Indica itemNumber (código) o lote");
        }
        if (item.isEmpty()) {
            item = "MOCK-ITEM";
        }
        if (batch.isEmpty()) {
            batch = "251201-MEM0003454";
        }
        return new DynamicsPreviewResponse(
                item,
                "Producto demo mock",
                "Item",
                "MP",
                "ALM-01",
                "SITE-01",
                120.0,
                150.0,
                "kg",
                batch,
                "ATTR-DEMO",
                "42",
                "PASS",
                1.0,
                12L
        );
    }
}
