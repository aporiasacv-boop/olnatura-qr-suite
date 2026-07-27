package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.support.presentation.AuditActionTranslator;
import com.company.olnaturaqr.support.presentation.RoleDisplayTranslator;
import com.company.olnaturaqr.support.presentation.UserDisplayHelper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Vista enriquecida de auditoría para UI y exportaciones legibles. */
public record AuditEventView(
        UUID id,
        Instant createdAt,
        UUID actorId,
        String actorEmail,
        String actorRol,
        String actorDisplay,
        String actorRoleDisplay,
        String actionType,
        String actionTypeDisplay,
        String lote,
        Map<String, Object> metadata
) {
    public static AuditEventView from(AuditEvent event, User actor) {
        String actorDisplay = UserDisplayHelper.displayFromUser(actor);
        if ("—".equals(actorDisplay) && event.getActorEmail() != null && !event.getActorEmail().isBlank()) {
            actorDisplay = UserDisplayHelper.displayFromUsernameOrEmail(null, event.getActorEmail());
        }
        return new AuditEventView(
                event.getId(),
                event.getCreatedAt(),
                event.getActorId(),
                event.getActorEmail(),
                event.getActorRol(),
                actorDisplay,
                RoleDisplayTranslator.translate(event.getActorRol()),
                event.getActionType(),
                AuditActionTranslator.translate(event.getActionType()),
                event.getLote(),
                event.getMetadata()
        );
    }
}
