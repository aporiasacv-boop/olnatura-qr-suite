package com.company.olnaturaqr.support.pdf;

import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.support.presentation.RoleDisplayTranslator;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class UsersPdfService {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Mexico_City"));

    private static final Color HEADER_BG = new Color(232, 235, 220);
    private static final Color ROW_ALT = new Color(246, 246, 240);
    private static final Color BORDER = new Color(180, 180, 170);

    public byte[] generate(List<User> users, Instant generatedAt, String generatedBy)
            throws DocumentException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 48, 42);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new PageFooter(FMT.format(generatedAt)));
        doc.open();

        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(74, 92, 40));
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        Paragraph brand = new Paragraph("Olnatura QR", brandFont);
        brand.setSpacingAfter(2);
        doc.add(brand);

        Paragraph title = new Paragraph("Listado de usuarios y permisos de comentarios", titleFont);
        title.setSpacingAfter(6);
        doc.add(title);

        List<User> rows = users == null ? List.of() : users.stream()
                .sorted(Comparator
                        .comparing((User u) -> safe(u.getUsername()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(u -> safe(u.getEmail()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        long canCreate = rows.stream().filter(User::isCanCreateLoteComments).count();
        long onlyView = rows.size() - canCreate;
        long enabled = rows.stream().filter(User::isEnabled).count();

        Paragraph meta = new Paragraph(
                "Generado: " + FMT.format(generatedAt)
                        + (generatedBy == null || generatedBy.isBlank() ? "" : "  ·  Por: " + generatedBy.trim()),
                metaFont
        );
        meta.setSpacingAfter(2);
        doc.add(meta);
        Paragraph totals = new Paragraph(
                "Total: " + rows.size()
                        + "  ·  Habilitados: " + enabled
                        + "  ·  Pueden crear comentarios: " + canCreate
                        + "  ·  Solo ver comentarios: " + onlyView,
                metaFont
        );
        totals.setSpacingAfter(10);
        doc.add(totals);

        if (rows.isEmpty()) {
            doc.add(new Paragraph("No hay usuarios registrados.", bodyFont));
        } else {
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.4f, 2.2f, 1.4f, 1.1f, 0.9f, 1.5f});
            table.setHeaderRows(1);
            table.setSpacingBefore(2);

            addHeaderCell(table, "Usuario", headerFont);
            addHeaderCell(table, "Correo", headerFont);
            addHeaderCell(table, "Rol", headerFont);
            addHeaderCell(table, "Estado", headerFont);
            addHeaderCell(table, "Habilitado", headerFont);
            addHeaderCell(table, "Comentarios", headerFont);

            int i = 0;
            for (User u : rows) {
                Color bg = (i % 2 == 1) ? ROW_ALT : Color.WHITE;
                addBodyCell(table, safe(u.getUsername()), bodyFont, bg);
                addBodyCell(table, safe(u.getEmail()), bodyFont, bg);
                addBodyCell(table, RoleDisplayTranslator.translate(
                        u.getRole() != null ? u.getRole().getName() : null
                ), bodyFont, bg);
                addBodyCell(table, u.isEnabled() ? "Activo" : "Deshabilitado", bodyFont, bg);
                addBodyCell(table, u.isEnabled() ? "Sí" : "No", bodyFont, bg);
                addBodyCell(table, u.isCanCreateLoteComments() ? "Puede crear" : "Solo ver", bodyFont, bg);
                i++;
            }
            doc.add(table);
        }

        doc.close();
        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(HEADER_BG);
        c.setPadding(6);
        c.setBorderColor(BORDER);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(c);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(safe(text), font));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setBorderColor(BORDER);
        c.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(c);
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "—" : s.trim();
    }

    private static final class PageFooter extends PdfPageEventHelper {
        private final String generatedLabel;
        private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        private PageFooter(String generatedLabel) {
            this.generatedLabel = generatedLabel;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidths(new float[]{3f, 1f});
                footer.setTotalWidth(document.right() - document.left());
                footer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                PdfPCell left = new PdfPCell(new Phrase(
                        "Olnatura QR · Usuarios · " + generatedLabel, footerFont));
                left.setBorder(Rectangle.NO_BORDER);
                left.setHorizontalAlignment(Element.ALIGN_LEFT);

                PdfPCell right = new PdfPCell(new Phrase(
                        "Página " + writer.getPageNumber(), footerFont));
                right.setBorder(Rectangle.NO_BORDER);
                right.setHorizontalAlignment(Element.ALIGN_RIGHT);

                footer.addCell(left);
                footer.addCell(right);
                footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 12, writer.getDirectContent());
            } catch (DocumentException ignored) {
            }
        }
    }
}
