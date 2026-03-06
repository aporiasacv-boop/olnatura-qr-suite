package com.company.olnaturaqr.support.pdf;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import org.springframework.stereotype.Service;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Genera PDF de historial de trazabilidad (audit_events) para un lote.
 */
@Service
public class AuditPdfService {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public byte[] generate(String lote, List<AuditEvent> events, Instant generatedAt) throws DocumentException {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        Paragraph logo = new Paragraph("Olnatura", titleFont);
        logo.setSpacingAfter(4);
        doc.add(logo);

        Paragraph reportTitle = new Paragraph("Reporte de trazabilidad", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        reportTitle.setSpacingAfter(8);
        doc.add(reportTitle);

        doc.add(new Paragraph("Lote: " + (lote != null ? lote : "—"), bodyFont));
        doc.add(new Paragraph("Generado: " + FMT.format(generatedAt), bodyFont));
        doc.add(new Paragraph(" "));

        if (events == null || events.isEmpty()) {
            doc.add(new Paragraph("Sin eventos de auditoría para este lote.", bodyFont));
        } else {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 1.2f, 1.5f, 0.8f, 2f});
            table.setHeaderRows(1);

            addHeaderCell(table, "Fecha / Hora", headerFont);
            addHeaderCell(table, "Acción", headerFont);
            addHeaderCell(table, "Usuario / Actor", headerFont);
            addHeaderCell(table, "Rol", headerFont);
            addHeaderCell(table, "Detalles", headerFont);

            for (AuditEvent e : events) {
                addCell(table, formatInstant(e.getCreatedAt()), bodyFont);
                addCell(table, safe(e.getActionType()), bodyFont);
                addCell(table, safe(e.getActorEmail()), bodyFont);
                addCell(table, safe(e.getActorRol()), bodyFont);
                addCell(table, formatDetails(e), bodyFont);
            }
            doc.add(table);
        }

        doc.close();
        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(c);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new PdfPCell(new Phrase(safe(text), font)));
    }

    private String formatInstant(Instant i) {
        return i != null ? FMT.format(i) : "—";
    }

    private String formatDetails(AuditEvent e) {
        if (e.getDeviceId() != null && !e.getDeviceId().isBlank()) {
            String device = "Dispositivo: " + e.getDeviceId();
            if (e.getMetadata() != null && !e.getMetadata().isEmpty()) {
                return device + " | " + formatMetadata(e.getMetadata());
            }
            return device;
        }
        if (e.getMetadata() != null && !e.getMetadata().isEmpty()) {
            return formatMetadata(e.getMetadata());
        }
        return "—";
    }

    private String formatMetadata(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> en : meta.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(en.getKey()).append("=").append(en.getValue());
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }
}
