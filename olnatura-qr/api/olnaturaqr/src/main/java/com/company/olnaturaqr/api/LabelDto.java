package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.qr.QrLabel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class LabelDto {

    public record CreateRequest(
            String tipoMaterial,
            String nombre,
            String codigo,
            String lote,
            LocalDate fechaEntrada,
            LocalDate caducidad,
            LocalDate reanalisis,
            int envaseNum,
            int envaseTotal
    ) {}

    public record LabelView(
            UUID id,
            String tipoMaterial,
            String nombre,
            String codigo,
            String lote,
            String publicToken,
            LocalDate fechaEntrada,
            LocalDate caducidad,
            LocalDate reanalisis,
            int envaseNum,
            int envaseTotal,
            String statusDinamico,
            Instant createdAt
    ) {
        public static LabelView from(QrLabel q) {
            return new LabelView(
                    q.getId(),
                    q.getTipoMaterial(),
                    q.getNombre(),
                    q.getCodigo(),
                    q.getLote(),
                    q.getPublicToken(),
                    q.getFechaEntrada(),
                    q.getCaducidad(),
                    q.getReanalisis(),
                    q.getEnvaseNum(),
                    q.getEnvaseTotal(),
                    q.getStatusDinamico(),
                    q.getCreatedAt()
            );
        }
    }

    public record CreateResponse(
            UUID id,
            String status,
            String qrUrl,
            String publicToken,
            LabelView label
    ) {}

    public record StatusRequest(String status) {}

    public record StatusResponse(UUID id, String status) {}

    /** Optional body for POST /label/{id}/zpl - embeds QR image as ^GF when provided */
    public record ZplRequest(Integer total, Integer from, Integer to, String qrImageBase64) {}
}