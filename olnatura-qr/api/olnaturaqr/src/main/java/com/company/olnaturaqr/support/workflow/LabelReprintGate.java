package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

public final class LabelReprintGate {

    public static final Set<String> PRINTED_FIELDS = Set.of(
            "nombre",
            "codigo",
            "fechaEntrada",
            "caducidad",
            "reanalisis",
            "tipoMaterial",
            "cantidadPorEnvase",
            "restosEnabled",
            "cantidadResto",
            "restosCantidades",
            "envaseNum",
            "envaseTotal"
    );

    private LabelReprintGate() {}

    public static boolean touchesPrintedFields(Collection<String> fields) {
        if (fields == null || fields.isEmpty()) return false;
        for (String f : fields) {
            if (f != null && PRINTED_FIELDS.contains(f)) return true;
        }
        return false;
    }

    public static void markRequired(QrLabel label) {
        if (label == null) return;
        label.setReprintRequired(true);
        label.setReprintRequiredAt(Instant.now());
    }

    public static void clear(QrLabel label) {
        if (label == null) return;
        label.setReprintRequired(false);
        label.setReprintRequiredAt(null);
    }
}
