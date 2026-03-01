package com.company.olnaturaqr.domain.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 150)
    private String actorEmail;

    @Column(name = "actor_rol", length = 50)
    private String actorRol;

    @Column(name = "action_type", nullable = false, length = 80)
    private String actionType;

    @Column(name = "lote", length = 120)
    private String lote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    public UUID getId() { return id; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID v) { this.actorId = v; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String v) { this.actorEmail = v; }

    public String getActorRol() { return actorRol; }
    public void setActorRol(String v) { this.actorRol = v; }

    public String getActionType() { return actionType; }
    public void setActionType(String v) { this.actionType = v; }

    public String getLote() { return lote; }
    public void setLote(String v) { this.lote = v; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> v) { this.metadata = v; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String v) { this.deviceId = v; }
}
