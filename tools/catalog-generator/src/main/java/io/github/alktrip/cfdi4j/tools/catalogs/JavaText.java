package io.github.alktrip.cfdi4j.tools.catalogs;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Escaping and literal helpers for the generated Java sources. */
final class JavaText {

    private JavaText() {}

    static String string(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    static String javadoc(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("@", "&#64;")
                .replace("*/", "*&#47;");
    }

    static String decimal(BigDecimal value) {
        return value == null ? "null" : "new BigDecimal(\"" + value.toPlainString() + "\")";
    }

    static String date(LocalDate date) {
        return "LocalDate.of(" + date.getYear() + ", " + date.getMonthValue() + ", " + date.getDayOfMonth() + ")";
    }

    static String vigencia(LocalDate inicio, LocalDate fin) {
        if (inicio == null) {
            return "null";
        }
        return fin == null
                ? "Vigencia.desde(" + date(inicio) + ")"
                : "Vigencia.entre(" + date(inicio) + ", " + date(fin) + ")";
    }

    /** Appends a period unless the sentence already ends with one. */
    static String sentence(String text) {
        return text.endsWith(".") ? text : text + ".";
    }
}
