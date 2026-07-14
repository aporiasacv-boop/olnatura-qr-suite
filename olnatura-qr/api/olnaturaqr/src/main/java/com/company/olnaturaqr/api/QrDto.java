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
            String publicToken,
            LocalDate fechaEntrada,
            LocalDate caducidad,
            LocalDate reanalisis,
            int envaseNum,
            int envaseTotal,
            String cantidadPorEnvase
    ) {}

    public record Dynamic(
            String codigo,
            String nombre,
            String lote,
            String caducidad,
            Double cantidadAlmacen,
            String status,
            String statusDynamics,
            String almacen,
            String ubicacion,
            String fuente
    ) {}

 
    public record Response(
            Label label,
            Dynamic dynamic,
            List<String> availableTransitions,
            Permissions permissions
    ) {}
}