package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Bloquea operación estándar (consulta, impresión, cambio de status, escaneo)
 * cuando el lote no está ACTIVE administrativamente.
 */
public final class LotOperationalGate {

    private LotOperationalGate() {}

    public static void requireActive(QrLabel label) {
        if (label == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado");
        }
        String admin = AdminLotStatus.normalize(label.getAdminStatus());
        if (AdminLotStatus.isOperational(admin)) {
            return;
        }
        String msg = AdminLotStatus.BAJA.equals(admin)
                ? "Lote dado de baja: no disponible en consulta operativa"
                : "Lote inactivo: no disponible en consulta operativa";
        throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
}
