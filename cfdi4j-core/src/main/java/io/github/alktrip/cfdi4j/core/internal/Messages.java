package io.github.alktrip.cfdi4j.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Violation messages of one module, read from {@code <base>_<language>.properties} with fallback
 * to {@code <base>.properties} (English).
 *
 * <p>Files are read as UTF-8 through an opener supplied by the owning module, because resource
 * access is checked against the calling module. The lookup is the same on the module path and the
 * class path and does not depend on the JVM default locale (ADR 0002). Not part of the public API.
 */
public final class Messages {

    private final Properties fallback;
    private final Properties localized;
    private final Locale locale;

    private Messages(Properties fallback, Properties localized, Locale locale) {
        this.fallback = fallback;
        this.localized = localized;
        this.locale = locale;
    }

    /** Opens a message file of the owning module, for example {@code Foo.class::getResourceAsStream}. */
    @FunctionalInterface
    public interface Opener {
        /**
         * Opens a file.
         *
         * @param fileName the file name, relative to the owning package
         * @return the content, or {@code null} if the file does not exist
         * @throws IOException if reading fails
         */
        InputStream open(String fileName) throws IOException;
    }

    /**
     * Loads the messages of a module.
     *
     * @param opener opens files of the owning module, for example
     *     {@code name -> MyClass.class.getResourceAsStream(name)}
     * @param baseName base file name without extension, for example {@code messages}
     * @param locale the language wanted
     * @return the messages
     */
    public static Messages load(Opener opener, String baseName, Locale locale) {
        Objects.requireNonNull(locale, "locale");
        Properties fallback = read(opener, baseName + ".properties");
        if (fallback == null) {
            throw new IllegalStateException("Missing " + baseName + ".properties");
        }
        Properties localized = locale.getLanguage().isEmpty() ? null : read(opener, baseName + "_" + locale.getLanguage() + ".properties");
        return new Messages(fallback, localized == null ? fallback : localized, locale);
    }

    /**
     * Formats the message of a code with {@link MessageFormat}.
     *
     * @param code the code
     * @param args the arguments
     * @return the message; the code itself if no message is defined
     */
    public String format(String code, Object... args) {
        String pattern = localized.getProperty(code, fallback.getProperty(code));
        return pattern == null ? code : new MessageFormat(pattern, locale).format(args);
    }

    private static Properties read(Opener opener, String fileName) {
        try (InputStream in = opener.open(fileName)) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
