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

    @Column(name = "status_dinamico", nullable = false, length = 40)
    private String statusDinamico;

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

    public String getStatusDinamico() { return statusDinamico; }
    public void setStatusDinamico(String statusDinamico) { this.statusDinamico = statusDinamico; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}