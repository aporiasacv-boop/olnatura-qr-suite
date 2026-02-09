package com.company.olnaturaqr.domain.scan;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scan_events")
public class ScanEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String lote;

    @Column(name = "scanned_by")
    private UUID scannedBy;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public UUID getScannedBy() { return scannedBy; }
    public void setScannedBy(UUID scannedBy) { this.scannedBy = scannedBy; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}