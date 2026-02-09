package com.company.olnaturaqr.api;

import java.time.LocalDate;

public class QrDto {

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

    public record Response(Label label, Dynamic dynamic) {}
}