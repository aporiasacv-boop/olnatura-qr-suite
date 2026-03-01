package com.company.olnaturaqr.infra.dynamics;

import com.company.olnaturaqr.support.qr.LoteExtractor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class MockDynamicsClient {

    public record DynamicCard(
            String status,
            double cantidad,
            String uom,
            String ubicacion,
            String fuente
    ) {}

    private final Map<String, DynamicCard> fake = Map.ofEntries(
            Map.entry("260112-MES003456",
                    new DynamicCard("CUARENTENA", 50.0, "kg", "Almacén principal", "MOCK_DYNAMICS")),
            Map.entry("LOTE-TEST-001",
                    new DynamicCard("LIBERADO", 100.0, "kg", "Almacén A", "MOCK_DYNAMICS")),
            Map.entry("251201-MEM0003454",
                    new DynamicCard("LIBERADO", 20.0, "kg", "Almacén secundario", "MOCK_DYNAMICS"))
    );

    public Optional<DynamicCard> fetchByLote(String raw) {
        return LoteExtractor.extract(raw)
                .flatMap(l -> Optional.ofNullable(fake.get(l)));
    }
}