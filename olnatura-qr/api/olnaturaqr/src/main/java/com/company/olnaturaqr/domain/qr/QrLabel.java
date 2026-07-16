package com.company.olnaturaqr.domain.qr;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "qr_labels")
public class QrLabel {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tipo_material", nullable = false, length = 60)
    private String tipoMaterial;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "codigo", nullable = false, length = 80)
    private String codigo;

    @Column(name = "lote", nullable = false, unique = true, length = 120)
    private String lote;

    @Column(name = "public_token", nullable = false, unique = true, length = 64)
    private String publicToken;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDate fechaEntrada;

    @Column(name = "caducidad")
    private LocalDate caducidad;

    @Column(name = "reanalisis")
    private LocalDate reanalisis;

    @Column(name = "envase_num", nullable = false)
    private int envaseNum;

    @Column(name = "envase_total", nullable = false)
    private int envaseTotal;

    @Column(name = "cantidad_por_envase", length = 120)
    private String cantidadPorEnvase;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    /** Ciclo administrativo: ACTIVE | INACTIVE | BAJA (independiente del workflow de calidad). */
    @Column(name = "admin_status", nullable = false, length = 20)
    private String adminStatus = "ACTIVE";

    @Column(name = "calidad_approved_at")
    private Instant calidadApprovedAt;

    @Column(name = "calidad_approved_by")
    private UUID calidadApprovedBy;

    @Column(name = "inspeccion_approved_at")
    private Instant inspeccionApprovedAt;

    @Column(name = "inspeccion_approved_by")
    private UUID inspeccionApprovedBy;

    @Column(name = "document_code", length = 60)
    private String documentCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }

    public String getTipoMaterial() { return tipoMaterial; }
    public void setTipoMaterial(String tipoMaterial) { this.tipoMaterial = tipoMaterial; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }

    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }

    public LocalDate getCaducidad() { return caducidad; }
    public void setCaducidad(LocalDate caducidad) { this.caducidad = caducidad; }

    public LocalDate getReanalisis() { return reanalisis; }
    public void setReanalisis(LocalDate reanalisis) { this.reanalisis = reanalisis; }

    public int getEnvaseNum() { return envaseNum; }
    public void setEnvaseNum(int envaseNum) { this.envaseNum = envaseNum; }

    public int getEnvaseTotal() { return envaseTotal; }
    public void setEnvaseTotal(int envaseTotal) { this.envaseTotal = envaseTotal; }

    public String getCantidadPorEnvase() { return cantidadPorEnvase; }
    public void setCantidadPorEnvase(String cantidadPorEnvase) { this.cantidadPorEnvase = cantidadPorEnvase; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminStatus() { return adminStatus; }
    public void setAdminStatus(String adminStatus) { this.adminStatus = adminStatus; }

    public Instant getCalidadApprovedAt() { return calidadApprovedAt; }
    public void setCalidadApprovedAt(Instant calidadApprovedAt) { this.calidadApprovedAt = calidadApprovedAt; }

    public UUID getCalidadApprovedBy() { return calidadApprovedBy; }
    public void setCalidadApprovedBy(UUID calidadApprovedBy) { this.calidadApprovedBy = calidadApprovedBy; }

    public Instant getInspeccionApprovedAt() { return inspeccionApprovedAt; }
    public void setInspeccionApprovedAt(Instant inspeccionApprovedAt) { this.inspeccionApprovedAt = inspeccionApprovedAt; }

    public UUID getInspeccionApprovedBy() { return inspeccionApprovedBy; }
    public void setInspeccionApprovedBy(UUID inspeccionApprovedBy) { this.inspeccionApprovedBy = inspeccionApprovedBy; }

    public String getDocumentCode() { return documentCode; }
    public void setDocumentCode(String documentCode) { this.documentCode = documentCode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}