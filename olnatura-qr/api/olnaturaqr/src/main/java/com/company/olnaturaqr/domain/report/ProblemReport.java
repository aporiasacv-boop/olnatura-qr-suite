package com.company.olnaturaqr.domain.report;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "problem_reports")
public class ProblemReport {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(length = 120)
    private String lote;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(name = "comment_body", columnDefinition = "TEXT")
    private String commentBody;

    @Column(name = "reporter_user_id")
    private UUID reporterUserId;

    @Column(name = "reporter_username", length = 100)
    private String reporterUsername;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolved_by_username", length = 100)
    private String resolvedByUsername;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "OPEN";
        }
    }

    public UUID getId() { return id; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCommentBody() { return commentBody; }
    public void setCommentBody(String commentBody) { this.commentBody = commentBody; }

    public UUID getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(UUID reporterUserId) { this.reporterUserId = reporterUserId; }

    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedByUsername() { return resolvedByUsername; }
    public void setResolvedByUsername(String resolvedByUsername) { this.resolvedByUsername = resolvedByUsername; }
}
