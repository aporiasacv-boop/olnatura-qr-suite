package com.company.olnaturaqr.support.export;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.domain.scan.ScanEvent;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.ScanEventRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.workflow.WorkflowStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Exportación tabular multi-hoja para Power BI (sin DW ni tablas nuevas).
 * Solo reutiliza datos ya persistidos en PostgreSQL.
 */
@Service
public class ExecutiveDashboardExportService {

    public static final String FILENAME = "Executive_Dashboard_Export.xlsx";
    private static final ZoneId ZONE = AuditService.ZONE;

    private final QrLabelRepository qrLabelRepository;
    private final ScanEventRepository scanEventRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ExecutiveDashboardExportService(
            QrLabelRepository qrLabelRepository,
            ScanEventRepository scanEventRepository,
            AuditEventRepository auditEventRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.scanEventRepository = scanEventRepository;
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public ExportResult buildWorkbook() throws IOException {
        List<QrLabel> labels = qrLabelRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ScanEvent> scans = scanEventRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditEvent> audits = auditEventRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();

        Map<UUID, User> usersById = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, LinkedHashMap::new));
        Map<String, QrLabel> labelsByLote = labels.stream()
                .collect(Collectors.toMap(QrLabel::getLote, l -> l, (a, b) -> a, LinkedHashMap::new));

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = Styles.create(wb);

            writeQrLabels(wb.createSheet("QR_Labels"), labels, styles);
            writeScanEvents(wb.createSheet("Scan_Events"), scans, usersById, labelsByLote, styles);
            writeAuditEvents(wb.createSheet("Audit_Events"), audits, styles);
            writeUsers(wb.createSheet("Users"), users, styles);
            writeResumen(wb.createSheet("Resumen"), labels, scans, audits, users, styles);
            writeDataDictionary(wb.createSheet("DataDictionary"), styles);

            wb.write(out);
            return new ExportResult(
                    out.toByteArray(),
                    labels.size(),
                    scans.size(),
                    audits.size(),
                    users.size()
            );
        }
    }

    public record ExportResult(
            byte[] bytes,
            int labelsExported,
            int scansExported,
            int auditsExported,
            int usersExported
    ) {}

    private void writeQrLabels(Sheet sheet, List<QrLabel> labels, Styles styles) {
        String[] headers = {
                "Id",
                "FechaCreacion",
                "HoraCreacion",
                "Material",
                "TipoMaterial",
                "CodigoInterno",
                "Lote",
                "Estado",
                "AdminStatus",
                "FechaEntrada",
                "FechaCaducidad",
                "FechaReanalisis",
                "EnvaseNum",
                "EnvaseTotal",
                "CantidadPorEnvase",
                "Unidad",
                "DocumentCode",
                "PublicToken",
                "UsuarioCreador"
        };
        writeHeader(sheet, headers, styles.header);
        int r = 1;
        for (QrLabel q : labels) {
            Row row = sheet.createRow(r++);
            int c = 0;
            writeString(row, c++, uuid(q.getId()), styles.text);
            writeDate(row, c++, toLocalDate(q.getCreatedAt()), styles.date);
            writeTime(row, c++, toLocalTime(q.getCreatedAt()), styles.time);
            writeString(row, c++, q.getNombre(), styles.text);
            writeString(row, c++, q.getTipoMaterial(), styles.text);
            writeString(row, c++, q.getCodigo(), styles.text);
            writeString(row, c++, q.getLote(), styles.text);
            writeString(row, c++, WorkflowStatus.normalize(q.getStatus()), styles.text);
            writeString(row, c++, q.getAdminStatus(), styles.text);
            writeDate(row, c++, q.getFechaEntrada(), styles.date);
            writeDate(row, c++, q.getCaducidad(), styles.date);
            writeDate(row, c++, q.getReanalisis(), styles.date);
            writeNumber(row, c++, q.getEnvaseNum(), styles.number);
            writeNumber(row, c++, q.getEnvaseTotal(), styles.number);
            QtyUnit parsed = parseCantidadPorEnvase(q.getCantidadPorEnvase());
            writeDecimal(row, c++, parsed.cantidad(), styles.decimal);
            writeString(row, c++, parsed.unidad(), styles.text);
            writeString(row, c++, q.getDocumentCode(), styles.text);
            writeString(row, c++, q.getPublicToken(), styles.text);
            // Actor de creación no se registra hoy en qr_labels ni audit CREATE.
            writeString(row, c++, null, styles.text);
        }
        autosize(sheet, headers.length);
    }

    private void writeScanEvents(
            Sheet sheet,
            List<ScanEvent> scans,
            Map<UUID, User> usersById,
            Map<String, QrLabel> labelsByLote,
            Styles styles
    ) {
        String[] headers = {
                "Id",
                "Fecha",
                "Hora",
                "Lote",
                "Resultado",
                "EstadoLoteActual",
                "UsuarioId",
                "UsuarioUsername",
                "UsuarioEmail",
                "DeviceId"
        };
        writeHeader(sheet, headers, styles.header);
        int r = 1;
        for (ScanEvent s : scans) {
            Row row = sheet.createRow(r++);
            int c = 0;
            User u = s.getScannedBy() != null ? usersById.get(s.getScannedBy()) : null;
            QrLabel label = labelsByLote.get(s.getLote());
            writeString(row, c++, uuid(s.getId()), styles.text);
            writeDate(row, c++, toLocalDate(s.getCreatedAt()), styles.date);
            writeTime(row, c++, toLocalTime(s.getCreatedAt()), styles.time);
            writeString(row, c++, s.getLote(), styles.text);
            // El sistema solo registra el evento de escaneo exitoso; no hay “resultado” de consulta.
            writeString(row, c++, "SCAN_REGISTERED", styles.text);
            writeString(row, c++,
                    label != null ? WorkflowStatus.normalize(label.getStatus()) : null,
                    styles.text);
            writeString(row, c++, uuid(s.getScannedBy()), styles.text);
            writeString(row, c++, u != null ? u.getUsername() : null, styles.text);
            writeString(row, c++, u != null ? u.getEmail() : null, styles.text);
            writeString(row, c++, s.getDeviceId(), styles.text);
        }
        autosize(sheet, headers.length);
    }

    private void writeAuditEvents(Sheet sheet, List<AuditEvent> audits, Styles styles) {
        String[] headers = {
                "Id",
                "Fecha",
                "Hora",
                "ActorId",
                "ActorEmail",
                "ActorRol",
                "ActionType",
                "Lote",
                "DeviceId",
                "MetadataJson"
        };
        writeHeader(sheet, headers, styles.header);
        int r = 1;
        for (AuditEvent e : audits) {
            Row row = sheet.createRow(r++);
            int c = 0;
            writeString(row, c++, uuid(e.getId()), styles.text);
            writeDate(row, c++, toLocalDate(e.getCreatedAt()), styles.date);
            writeTime(row, c++, toLocalTime(e.getCreatedAt()), styles.time);
            writeString(row, c++, uuid(e.getActorId()), styles.text);
            writeString(row, c++, e.getActorEmail(), styles.text);
            writeString(row, c++, e.getActorRol(), styles.text);
            writeString(row, c++, e.getActionType(), styles.text);
            writeString(row, c++, e.getLote(), styles.text);
            writeString(row, c++, e.getDeviceId(), styles.text);
            writeString(row, c++, metadataJson(e.getMetadata()), styles.text);
        }
        autosize(sheet, headers.length);
    }

    private void writeUsers(Sheet sheet, List<User> users, Styles styles) {
        String[] headers = {
                "Id",
                "Username",
                "Email",
                "Enabled",
                "Role",
                "FechaCreacion",
                "HoraCreacion"
        };
        writeHeader(sheet, headers, styles.header);
        int r = 1;
        for (User u : users) {
            Row row = sheet.createRow(r++);
            int c = 0;
            writeString(row, c++, uuid(u.getId()), styles.text);
            writeString(row, c++, u.getUsername(), styles.text);
            writeString(row, c++, u.getEmail(), styles.text);
            writeBoolean(row, c++, u.isEnabled(), styles.text);
            writeString(row, c++, u.getRole() != null ? u.getRole().getName() : null, styles.text);
            writeDate(row, c++, toLocalDate(u.getCreatedAt()), styles.date);
            writeTime(row, c++, toLocalTime(u.getCreatedAt()), styles.time);
        }
        autosize(sheet, headers.length);
    }

    private void writeResumen(
            Sheet sheet,
            List<QrLabel> labels,
            List<ScanEvent> scans,
            List<AuditEvent> audits,
            List<User> users,
            Styles styles
    ) {
        String[] headers = {"Indicador", "Valor", "Descripcion"};
        writeHeader(sheet, headers, styles.header);

        long aprobados = labels.stream()
                .filter(l -> WorkflowStatus.APROBADO.equals(WorkflowStatus.normalize(l.getStatus())))
                .count();
        long rechazados = labels.stream()
                .filter(l -> WorkflowStatus.RECHAZADO.equals(WorkflowStatus.normalize(l.getStatus())))
                .count();
        long cuarentena = labels.stream()
                .filter(l -> WorkflowStatus.CUARENTENA.equals(WorkflowStatus.normalize(l.getStatus())))
                .count();
        long activos = labels.stream()
                .filter(l -> "ACTIVE".equalsIgnoreCase(l.getAdminStatus()))
                .count();
        long enabledUsers = users.stream().filter(User::isEnabled).count();
        long pendingUsers = users.stream().filter(u -> !u.isEnabled()).count();

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"TotalEtiquetas", String.valueOf(labels.size()), "Filas en qr_labels"});
        rows.add(new String[]{"TotalEscaneos", String.valueOf(scans.size()), "Filas en scan_events"});
        rows.add(new String[]{"MaterialesAprobados", String.valueOf(aprobados), "qr_labels.status = APROBADO"});
        rows.add(new String[]{"MaterialesRechazados", String.valueOf(rechazados), "qr_labels.status = RECHAZADO"});
        rows.add(new String[]{"MaterialesEnCuarentena", String.valueOf(cuarentena), "qr_labels.status = CUARENTENA"});
        rows.add(new String[]{"LotesAdminActivos", String.valueOf(activos), "qr_labels.admin_status = ACTIVE"});
        rows.add(new String[]{"UsuariosRegistrados", String.valueOf(users.size()), "Filas en users"});
        rows.add(new String[]{"UsuariosHabilitados", String.valueOf(enabledUsers), "users.enabled = true"});
        rows.add(new String[]{"UsuariosPendientes", String.valueOf(pendingUsers), "users.enabled = false"});
        rows.add(new String[]{"EventosAuditados", String.valueOf(audits.size()), "Filas en audit_events"});
        rows.add(new String[]{
                "ZonaHorariaExport",
                ZONE.getId(),
                "Zona usada para Fecha/Hora derivadas de timestamps"
        });
        rows.add(new String[]{
                "ExportGeneratedAt",
                Instant.now().toString(),
                "Instant UTC de generación del archivo"
        });

        int r = 1;
        for (String[] line : rows) {
            Row row = sheet.createRow(r++);
            writeString(row, 0, line[0], styles.text);
            if ("ZonaHorariaExport".equals(line[0]) || "ExportGeneratedAt".equals(line[0])) {
                writeString(row, 1, line[1], styles.text);
            } else {
                writeNumber(row, 1, Long.parseLong(line[1]), styles.number);
            }
            writeString(row, 2, line[2], styles.text);
        }
        autosize(sheet, headers.length);
    }

    private void writeDataDictionary(Sheet sheet, Styles styles) {
        String[] headers = {"Hoja", "Columna", "Descripcion", "TablaOrigen", "TipoDato"};
        writeHeader(sheet, headers, styles.header);

        List<String[]> dict = new ArrayList<>();
        // QR_Labels
        dict.add(d("QR_Labels", "Id", "Identificador UUID de la etiqueta", "qr_labels.id", "UUID/Text"));
        dict.add(d("QR_Labels", "FechaCreacion", "Fecha de creación (America/Mexico_City)", "qr_labels.created_at", "Date"));
        dict.add(d("QR_Labels", "HoraCreacion", "Hora de creación (America/Mexico_City)", "qr_labels.created_at", "Time"));
        dict.add(d("QR_Labels", "Material", "Nombre del material", "qr_labels.nombre", "Text"));
        dict.add(d("QR_Labels", "TipoMaterial", "Tipo de material", "qr_labels.tipo_material", "Text"));
        dict.add(d("QR_Labels", "CodigoInterno", "Código de ítem", "qr_labels.codigo", "Text"));
        dict.add(d("QR_Labels", "Lote", "Número de lote (único)", "qr_labels.lote", "Text"));
        dict.add(d("QR_Labels", "Estado", "Estado workflow plataforma", "qr_labels.status", "Text"));
        dict.add(d("QR_Labels", "AdminStatus", "Estado administrativo del lote", "qr_labels.admin_status", "Text"));
        dict.add(d("QR_Labels", "FechaEntrada", "Fecha de entrada capturada al registrar", "qr_labels.fecha_entrada", "Date"));
        dict.add(d("QR_Labels", "FechaCaducidad", "Caducidad", "qr_labels.caducidad", "Date"));
        dict.add(d("QR_Labels", "FechaReanalisis", "Reanálisis", "qr_labels.reanalisis", "Date"));
        dict.add(d("QR_Labels", "EnvaseNum", "Número de envase", "qr_labels.envase_num", "Number"));
        dict.add(d("QR_Labels", "EnvaseTotal", "Total de envases", "qr_labels.envase_total", "Number"));
        dict.add(d("QR_Labels", "CantidadPorEnvase", "Parte numérica parseada de cantidad_por_envase (ej. 5 de \"5 Kg\")", "qr_labels.cantidad_por_envase", "Number"));
        dict.add(d("QR_Labels", "Unidad", "Unidad parseada de cantidad_por_envase (ej. Kg de \"5 Kg\")", "qr_labels.cantidad_por_envase", "Text"));
        dict.add(d("QR_Labels", "DocumentCode", "Código de documento", "qr_labels.document_code", "Text"));
        dict.add(d("QR_Labels", "PublicToken", "Token público del QR", "qr_labels.public_token", "Text"));
        dict.add(d("QR_Labels", "UsuarioCreador", "No disponible en modelo actual", "N/A", "Text/Empty"));
        // Scan
        dict.add(d("Scan_Events", "Id", "UUID del evento de escaneo", "scan_events.id", "UUID/Text"));
        dict.add(d("Scan_Events", "Fecha", "Fecha del escaneo", "scan_events.created_at", "Date"));
        dict.add(d("Scan_Events", "Hora", "Hora del escaneo", "scan_events.created_at", "Time"));
        dict.add(d("Scan_Events", "Lote", "Lote escaneado", "scan_events.lote", "Text"));
        dict.add(d("Scan_Events", "Resultado", "Constante SCAN_REGISTERED (único outcome persistido)", "derivado", "Text"));
        dict.add(d("Scan_Events", "EstadoLoteActual", "status actual del lote al exportar", "qr_labels.status", "Text"));
        dict.add(d("Scan_Events", "UsuarioId", "Usuario que escaneó", "scan_events.scanned_by", "UUID/Text"));
        dict.add(d("Scan_Events", "UsuarioUsername", "Username del escáner", "users.username", "Text"));
        dict.add(d("Scan_Events", "UsuarioEmail", "Email del escáner", "users.email", "Text"));
        dict.add(d("Scan_Events", "DeviceId", "Identificador de dispositivo", "scan_events.device_id", "Text"));
        // Audit
        dict.add(d("Audit_Events", "Id", "UUID evento auditoría", "audit_events.id", "UUID/Text"));
        dict.add(d("Audit_Events", "Fecha", "Fecha del evento", "audit_events.created_at", "Date"));
        dict.add(d("Audit_Events", "Hora", "Hora del evento", "audit_events.created_at", "Time"));
        dict.add(d("Audit_Events", "ActorId", "Usuario actor", "audit_events.actor_id", "UUID/Text"));
        dict.add(d("Audit_Events", "ActorEmail", "Email actor", "audit_events.actor_email", "Text"));
        dict.add(d("Audit_Events", "ActorRol", "Rol actor", "audit_events.actor_rol", "Text"));
        dict.add(d("Audit_Events", "ActionType", "Tipo de acción", "audit_events.action_type", "Text"));
        dict.add(d("Audit_Events", "Lote", "Lote relacionado", "audit_events.lote", "Text"));
        dict.add(d("Audit_Events", "DeviceId", "Dispositivo", "audit_events.device_id", "Text"));
        dict.add(d("Audit_Events", "MetadataJson", "Metadata JSON", "audit_events.metadata", "Text"));
        // Users
        dict.add(d("Users", "Id", "UUID usuario", "users.id", "UUID/Text"));
        dict.add(d("Users", "Username", "Nombre de usuario", "users.username", "Text"));
        dict.add(d("Users", "Email", "Correo", "users.email", "Text"));
        dict.add(d("Users", "Enabled", "Habilitado", "users.enabled", "Boolean"));
        dict.add(d("Users", "Role", "Rol", "roles.name", "Text"));
        dict.add(d("Users", "FechaCreacion", "Fecha alta", "users.created_at", "Date"));
        dict.add(d("Users", "HoraCreacion", "Hora alta", "users.created_at", "Time"));
        // Resumen
        dict.add(d("Resumen", "Indicador", "Nombre del KPI exportado", "agregado", "Text"));
        dict.add(d("Resumen", "Valor", "Valor numérico o texto del KPI", "agregado", "Number/Text"));
        dict.add(d("Resumen", "Descripcion", "Descripción del indicador", "agregado", "Text"));

        int r = 1;
        for (String[] line : dict) {
            Row row = sheet.createRow(r++);
            for (int i = 0; i < line.length; i++) {
                writeString(row, i, line[i], styles.text);
            }
        }
        autosize(sheet, headers.length);
    }

    private static String[] d(String sheet, String col, String desc, String origin, String type) {
        return new String[]{sheet, col, desc, origin, type};
    }

    private String metadataJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return String.valueOf(metadata);
        }
    }

    private static void writeHeader(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static void writeString(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
    }

    private static void writeBoolean(Row row, int col, boolean value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    private static void writeNumber(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        cell.setCellValue(value);
    }

    private static void writeDecimal(Row row, int col, Double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
    }

    /**
     * Separa textos tipo "5 Kg", "1 Bulto", "20 L", "1.5 kg" en número + unidad.
     * Si no hay número inicial, cantidad=null y unidad=texto completo (si existe).
     */
    static QtyUnit parseCantidadPorEnvase(String raw) {
        if (raw == null) {
            return new QtyUnit(null, null);
        }
        String s = raw.trim();
        if (s.isEmpty() || "N/A".equalsIgnoreCase(s) || "-".equals(s)) {
            return new QtyUnit(null, null);
        }
        // número con . o , como decimal; el resto es unidad
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^\\s*([+-]?\\d+(?:[.,]\\d+)?)\\s*(.*)$")
                .matcher(s);
        if (!m.matches()) {
            return new QtyUnit(null, s);
        }
        String numPart = m.group(1).replace(',', '.');
        String unitPart = m.group(2) != null ? m.group(2).trim() : "";
        try {
            double qty = Double.parseDouble(numPart);
            return new QtyUnit(qty, unitPart.isEmpty() ? null : unitPart);
        } catch (NumberFormatException ex) {
            return new QtyUnit(null, s);
        }
    }

    record QtyUnit(Double cantidad, String unidad) {}

    private static void writeDate(Row row, int col, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
    }

    private static void writeTime(Row row, int col, LocalTime value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setBlank();
        } else {
            // Excel time as fraction of day
            double fraction = (value.toSecondOfDay() + value.getNano() / 1_000_000_000d) / 86_400d;
            cell.setCellValue(fraction);
        }
    }

    private static LocalDate toLocalDate(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(ZONE).toLocalDate();
    }

    private static LocalTime toLocalTime(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(ZONE).toLocalTime().withNano(0);
    }

    private static LocalDate toLocalDate(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.atZoneSameInstant(ZONE).toLocalDate();
    }

    private static LocalTime toLocalTime(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.atZoneSameInstant(ZONE).toLocalTime().withNano(0);
    }

    private static String uuid(UUID id) {
        return id == null ? null : id.toString();
    }

    private static void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Exception ignored) {
                // autosize puede fallar en headless; no bloquea el export
            }
        }
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle text;
        final CellStyle date;
        final CellStyle time;
        final CellStyle number;
        final CellStyle decimal;

        private Styles(
                CellStyle header,
                CellStyle text,
                CellStyle date,
                CellStyle time,
                CellStyle number,
                CellStyle decimal
        ) {
            this.header = header;
            this.text = text;
            this.date = date;
            this.time = time;
            this.number = number;
            this.decimal = decimal;
        }

        static Styles create(Workbook wb) {
            CreationHelper helper = wb.getCreationHelper();

            CellStyle header = wb.createCellStyle();
            var font = wb.createFont();
            font.setBold(true);
            header.setFont(font);

            CellStyle text = wb.createCellStyle();

            CellStyle date = wb.createCellStyle();
            date.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));

            CellStyle time = wb.createCellStyle();
            time.setDataFormat(helper.createDataFormat().getFormat("hh:mm:ss"));

            CellStyle number = wb.createCellStyle();
            number.setDataFormat(helper.createDataFormat().getFormat("0"));

            CellStyle decimal = wb.createCellStyle();
            decimal.setDataFormat(helper.createDataFormat().getFormat("0.####"));

            return new Styles(header, text, date, time, number, decimal);
        }
    }
}
