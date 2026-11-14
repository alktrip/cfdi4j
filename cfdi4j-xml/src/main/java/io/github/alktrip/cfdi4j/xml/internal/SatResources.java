package io.github.alktrip.cfdi4j.xml.internal;

import io.github.alktrip.cfdi4j.core.Cfdi4jException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Official SAT files bundled in this module (ADR 0007). An absolute SAT URL maps to the resource
 * {@code sat/xml/<host>/<path>}, the layout of {@code specs/sat/xml}. Nothing is ever downloaded.
 */
public final class SatResources {

    private static final String HOST = "www.sat.gob.mx/";

    private SatResources() {}

    /**
     * Returns the bundled resource path for a SAT URL.
     *
     * @param url an {@code http} or {@code https} URL on {@code www.sat.gob.mx}
     * @return the resource path, or empty if the URL is not a SAT URL
     */
    public static Optional<String> resourcePath(String url) {
        if (url == null) {
            return Optional.empty();
        }
        String rest;
        if (url.startsWith("http://")) {
            rest = url.substring("http://".length());
        } else if (url.startsWith("https://")) {
            rest = url.substring("https://".length());
        } else {
            return Optional.empty();
        }
        return rest.startsWith(HOST) && !rest.contains("..") ? Optional.of("sat/xml/" + rest) : Optional.empty();
    }

    /**
     * Opens a bundled SAT file.
     *
     * @param url absolute SAT URL, as written in an {@code xs:import} or {@code xsl:include}
     * @return the content; the caller closes it
     * @throws Cfdi4jException if the URL is not one of the bundled files (it is never fetched)
     */
    public static InputStream open(String url) {
        String path = resourcePath(url).orElseThrow(() -> notBundled(url));
        try {
            InputStream in = SatResources.class.getModule().getResourceAsStream(path);
            if (in == null) {
                throw notBundled(url);
            }
            return in;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Cfdi4jException notBundled(String url) {
        return new Cfdi4jException("Not a bundled SAT file, and cfdi4j never downloads: " + url);
    }
}
