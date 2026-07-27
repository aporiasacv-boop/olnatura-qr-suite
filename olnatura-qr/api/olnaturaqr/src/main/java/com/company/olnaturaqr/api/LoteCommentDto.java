package com.company.olnaturaqr.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class LoteCommentDto {

    private LoteCommentDto() {}

    public record CreateRequest(String comment) {}

    public record Response(
            UUID id,
            String lote,
            UUID userId,
            String username,
            String displayName,
            String role,
            OffsetDateTime createdAt,
            String comment
    ) {}
}
