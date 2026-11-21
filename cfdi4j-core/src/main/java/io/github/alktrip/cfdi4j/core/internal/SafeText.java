package io.github.alktrip.cfdi4j.core.internal;

/**
 * Makes text taken from a document safe to put in a message: control characters and line
 * separators are escaped, so a value cannot forge extra lines in a log, and long values are
 * shortened. Not part of the public API.
 */
public final class SafeText {

    private SafeText() {}

    /**
     * Escapes control characters and keeps at most {@code max} characters.
     *
     * @param text the text
     * @param max the maximum number of characters kept, before the length note
     * @return the safe text
     */
    public static String of(String text, int max) {
        StringBuilder out = new StringBuilder(Math.min(text.length(), max) + 16);
        int kept = 0;
        for (int i = 0; i < text.length() && kept < max; i++, kept++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (Character.isISOControl(c) || c == ' ' || c == ' ') {
                        out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        if (text.length() > max) {
            out.append("… (").append(text.length()).append(" characters)");
        }
        return out.toString();
    }
}
