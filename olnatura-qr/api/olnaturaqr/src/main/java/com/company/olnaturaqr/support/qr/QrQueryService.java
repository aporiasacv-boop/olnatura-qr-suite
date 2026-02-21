package com.company.olnaturaqr.support.qr;

import com.company.olnaturaqr.api.QrDto;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.infra.dynamics.MockDynamicsClient;
import com.company.olnaturaqr.repository.QrLabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class QrQueryService {

    private final QrLabelRepository qrLabelRepository;
    private final MockDynamicsClient dynamics;

    public QrQueryService(QrLabelRepository qrLabelRepository, MockDynamicsClient dynamics) {
        this.qrLabelRepository = qrLabelRepository;
        this.dynamics = dynamics;
    }

    @Transactional(readOnly = true)
    public QrDto.Response getByLote(String loteRaw) {
       String lote = (loteRaw == null) ? "" : loteRaw.trim();

        if (lote.isBlank()) {
        throw new ResponseStatusException(BAD_REQUEST, "Lote requerido");
        }

        if (lote.length() > 120) {
            throw new ResponseStatusException(BAD_REQUEST, "Lote demasiado largo");
        }

        QrLabel label = qrLabelRepository.findByLote(lote)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lote no encontrado: " + lote));

        var dtoLabel = new QrDto.Label(
                label.getTipoMaterial(),
                label.getNombre(),
                label.getCodigo(),
                label.getLote(),
                label.getFechaEntrada(),
                label.getCaducidad(),
                label.getReanalisis(),
                label.getEnvaseNum(),
                label.getEnvaseTotal()
        );

        var dyn = dynamics.fetchByLote(lote)
                .map(d -> new QrDto.Dynamic(d.status(), d.cantidad(), d.uom(), d.ubicacion(), d.fuente()))
                .orElseGet(() -> new QrDto.Dynamic(
                        normalizeStatus(label.getStatusDinamico()),
                        null,
                        null,
                        null,
                        "DB_ONLY"
                ));

        return new QrDto.Response(dtoLabel, dyn);
    }

    private String normalizeStatus(String s) {
        if (s == null) return "DESCONOCIDO";
        var v = s.trim().toUpperCase();
        return v.isBlank() ? "DESCONOCIDO" : v;
    }
}