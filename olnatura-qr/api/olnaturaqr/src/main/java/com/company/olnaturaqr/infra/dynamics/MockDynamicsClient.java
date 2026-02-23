package com.company.olnaturaqr.infra.dynamics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            Map.entry("260112-MES003456",
                    new DynamicCard("CUARENTENA", 50.0, "kg",
                            "Almacén principal", "MOCK_DYNAMICS")),

            Map.entry("LOTE-TEST-001",
                    new DynamicCard("LIBERADO", 100.0, "kg",
                            "Almacén A", "MOCK_DYNAMICS")),

            Map.entry("251201-MEM0005643",
                    new DynamicCard("LIBERADO", 20.0, "kg",
                            "Almacén secundario", "MOCK_DYNAMICS"))
    );

    public Optional<DynamicCard> fetchByLote(String raw) {

        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String lote = raw.trim();

        // 🔍 Si viene JSON, extraer campo "lote"
        if (lote.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(lote);
                if (node.has("lote")) {
                    lote = node.get("lote").asText();
                }
            } catch (Exception ignored) {
                // Si falla el parseo, se intentará buscar el raw tal cual
            }
        }

        return Optional.ofNullable(fake.get(lote.trim()));
    }
}