package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.api.DynamicsPreviewResponse;

public interface DynamicsPreviewService {

    DynamicsPreviewResponse fetchPreview(String itemNumber, String lote);
}
