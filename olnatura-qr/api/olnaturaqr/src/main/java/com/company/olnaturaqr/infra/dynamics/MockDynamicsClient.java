package com.company.olnaturaqr.infra.dynamics;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class MockDynamicsClient {

    public record DynamicCard(
            String status,     // LIBERADO | CUARENTENA | PENDIENTE | RECHAZADO
            double cantidad,
            String uom,
            String ubicacion,
            String fuente
    ) {}

    private final Map<String, DynamicCard> fake = Map.ofEntries(
            Map.entry("260112-MES003456", new DynamicCard("CUARENTENA", 50.0, "kg", "Almacén principal", "MOCK_DYNAMICS")),
            Map.entry("LOTE-TEST-001", new DynamicCard("LIBERADO", 100.0, "kg", "Almacén A", "MOCK_DYNAMICS"))
    );

    public Optional<DynamicCard> fetchByLote(String lote) {
        return Optional.ofNullable(fake.get(lote));
    }
}