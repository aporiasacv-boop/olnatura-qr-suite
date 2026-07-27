package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.util.SpanishFlexibleDateParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Corrección administrativa de datos capturados en la etiqueta.
 * Solo ADMIN. No sobrescribe sin auditoría: cada campo cambia con valor anterior/nuevo + motivo.
 */
@Service
public class AdminLabelCorrectionService {

    private final QrLabelRepository qrLabelRepository;
    private final AuditService auditService;

    public AdminLabelCorrectionService(QrLabelRepository qrLabelRepository, AuditService auditService) {
        this.qrLabelRepository = qrLabelRepository;
        this.auditService = auditService;
    }

    public record CorrectionRequest(
            String motivo,
            String tipoMaterial,
            String nombre,
            String codigo,
            String fechaEntrada,
            String caducidad,
            String reanalisis,
            Integer envaseNum,
            Integer envaseTotal,
            String cantidadPorEnvase,
            String documentCode
    ) {}

    public record FieldChange(String field, String fieldLabel, String from, String to) {}

    public record CorrectionResult(QrLabel label, List<FieldChange> changes) {}

    @Transactional
    public CorrectionResult correct(QrLabel label, AuthPrincipal principal, CorrectionRequest req) {
        LotOperationalGate.requireActive(label);
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");
        }
        String motivo = req.motivo() == null ? "" : req.motivo().trim();
        if (motivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de la modificación es obligatorio");
        }
        if (motivo.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo supera 500 caracteres");
        }

        List<FieldChange> changes = new ArrayList<>();

        if (req.tipoMaterial() != null) {
            String next = MaterialType.normalize(req.tipoMaterial());
            if (!MaterialType.isValid(next)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "tipoMaterial inválido. Usa: Materia Prima, Material de Empaque Primario o Material de Empaque Secundario");
            }
            String prev = nullToEmpty(label.getTipoMaterial());
            if (!prev.equals(next)) {
                changes.add(new FieldChange("tipoMaterial", "Tipo de material", prev, next));
                label.setTipoMaterial(next);
            }
        }
        if (req.nombre() != null) {
            String next = req.nombre().trim();
            if (next.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nombre no puede quedar vacío");
            }
            String prev = nullToEmpty(label.getNombre());
            if (!prev.equals(next)) {
                changes.add(new FieldChange("nombre", "Nombre", prev, next));
                label.setNombre(next);
            }
        }
        if (req.codigo() != null) {
            String next = req.codigo().trim();
            if (next.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "codigo no puede quedar vacío");
            }
            String prev = nullToEmpty(label.getCodigo());
            if (!prev.equals(next)) {
                changes.add(new FieldChange("codigo", "Código", prev, next));
                label.setCodigo(next);
            }
        }
        if (req.fechaEntrada() != null) {
            LocalDate next = SpanishFlexibleDateParser.parseRequired(req.fechaEntrada(), "fechaEntrada");
            LocalDate prev = label.getFechaEntrada();
            if (!Objects.equals(prev, next)) {
                changes.add(new FieldChange("fechaEntrada", "Fecha de entrada",
                        dateStr(prev), dateStr(next)));
                label.setFechaEntrada(next);
            }
        }
        if (req.caducidad() != null) {
            LocalDate next = req.caducidad().isBlank()
                    ? null
                    : SpanishFlexibleDateParser.parseRequired(req.caducidad(), "caducidad");
            LocalDate prev = label.getCaducidad();
            if (!Objects.equals(prev, next)) {
                changes.add(new FieldChange("caducidad", "Caducidad", dateStr(prev), dateStr(next)));
                label.setCaducidad(next);
            }
        }
        if (req.reanalisis() != null) {
            LocalDate next = req.reanalisis().isBlank()
                    ? null
                    : SpanishFlexibleDateParser.parseRequired(req.reanalisis(), "reanalisis");
            LocalDate prev = label.getReanalisis();
            if (!Objects.equals(prev, next)) {
                changes.add(new FieldChange("reanalisis", "Reanálisis", dateStr(prev), dateStr(next)));
                label.setReanalisis(next);
            }
        }

        Integer nextEnvaseNum = req.envaseNum();
        Integer nextEnvaseTotal = req.envaseTotal();
        int envaseNum = nextEnvaseNum != null ? nextEnvaseNum : label.getEnvaseNum();
        int envaseTotal = nextEnvaseTotal != null ? nextEnvaseTotal : label.getEnvaseTotal();
        if (nextEnvaseNum != null || nextEnvaseTotal != null) {
            if (envaseNum <= 0 || envaseTotal <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "envaseNum/envaseTotal deben ser > 0");
            }
            if (envaseNum > envaseTotal) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "envaseNum no puede ser mayor que envaseTotal");
            }
            if (label.getEnvaseNum() != envaseNum) {
                changes.add(new FieldChange("envaseNum", "Envase (número)",
                        String.valueOf(label.getEnvaseNum()), String.valueOf(envaseNum)));
                label.setEnvaseNum(envaseNum);
            }
            if (label.getEnvaseTotal() != envaseTotal) {
                changes.add(new FieldChange("envaseTotal", "Envases (total)",
                        String.valueOf(label.getEnvaseTotal()), String.valueOf(envaseTotal)));
                label.setEnvaseTotal(envaseTotal);
            }
        }

        if (req.cantidadPorEnvase() != null) {
            String next = req.cantidadPorEnvase().trim();
            String normalized = next.isEmpty() ? null : next;
            if (normalized != null && normalized.length() > 120) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cantidadPorEnvase supera 120 caracteres");
            }
            String prev = label.getCantidadPorEnvase();
            if (!Objects.equals(nullToEmpty(prev), nullToEmpty(normalized))) {
                changes.add(new FieldChange("cantidadPorEnvase", "Cantidad por envase",
                        nullToDash(prev), nullToDash(normalized)));
                label.setCantidadPorEnvase(normalized);
            }
        }

        if (req.documentCode() != null) {
            String next = req.documentCode().trim();
            String normalized = next.isEmpty() ? null : next;
            String prev = label.getDocumentCode();
            if (!Objects.equals(nullToEmpty(prev), nullToEmpty(normalized))) {
                changes.add(new FieldChange("documentCode", "Código de documento",
                        nullToDash(prev), nullToDash(normalized)));
                label.setDocumentCode(normalized);
            }
        }

        if (changes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay cambios para aplicar");
        }

        QrLabel saved = qrLabelRepository.save(label);

        Map<String, Object> md = new LinkedHashMap<>();
        md.put("labelId", saved.getId().toString());
        md.put("motivo", motivo);
        md.put("rol", "ADMIN");
        List<Map<String, String>> changeMaps = new ArrayList<>();
        for (FieldChange c : changes) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("field", c.field());
            row.put("fieldLabel", c.fieldLabel());
            row.put("from", c.from());
            row.put("to", c.to());
            changeMaps.add(row);
        }
        md.put("changes", changeMaps);
        auditService.log(principal, "ADMIN_CORRECT_LABEL", saved.getLote(), md, null);

        return new CorrectionResult(saved, changes);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String dateStr(LocalDate d) {
        return d == null ? "—" : d.toString();
    }
}
