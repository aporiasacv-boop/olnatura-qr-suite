package com.company.olnaturaqr.infra.dynamics;

import java.util.Optional;

public interface DynamicsClient {

    record DynamicCard(
            String status,
            double cantidad,
            String uom,
            String ubicacion,
            String fuente
    ) {}

    Optional<DynamicCard> fetchByLote(String lote);
}
