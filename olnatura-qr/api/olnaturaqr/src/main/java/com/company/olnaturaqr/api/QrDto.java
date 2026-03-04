package com.company.olnaturaqr.api;

import java.time.LocalDate;
import java.util.List;

public class QrDto {

    public record Permissions(
            boolean canChangeStatus,
            boolean canRegisterScan,
            boolean canCreateLabel
    ) {}

    public record Label(
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

    public record Dynamic(
            String status,
            Double cantidad,
            String uom,
            String ubicacion,
            String fuente
    ) {}

    /** label and dynamic unchanged; optional fields for web client (Android ignores). */
    public record Response(
            Label label,
            Dynamic dynamic,
            List<String> availableTransitions,
            Permissions permissions
    ) {}
}