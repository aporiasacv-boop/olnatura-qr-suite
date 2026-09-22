package com.company.olnaturaqr.support.label;

import java.time.LocalDate;

public final class LabelPrintDates {

    private static final String[] MESES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };

    private LabelPrintDates() {}

    public static String formatDdMmmYy(LocalDate d) {
        if (d == null) return "N/A";
        int month = d.getMonthValue();
        String mon = month >= 1 && month <= 12 ? MESES[month - 1] : "???";
        return String.format("%02d/%s/%02d", d.getDayOfMonth(), mon, d.getYear() % 100);
    }
}
