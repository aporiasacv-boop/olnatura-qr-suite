package com.company.olnaturaqr.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ScanDto {

    public record Response(
            UUID id,
            String lote,
            UUID scannedBy,
            String deviceId,
            OffsetDateTime createdAt
    ) {}
}