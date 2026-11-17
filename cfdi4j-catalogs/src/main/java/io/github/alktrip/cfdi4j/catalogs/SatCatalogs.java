package io.github.alktrip.cfdi4j.catalogs;

import io.github.alktrip.cfdi4j.core.catalog.CatalogProvider;
import io.github.alktrip.cfdi4j.core.catalog.CatalogVersion;
import io.github.alktrip.cfdi4j.core.catalog.ClaveProdServInfo;
import io.github.alktrip.cfdi4j.core.catalog.ClaveUnidadInfo;
import io.github.alktrip.cfdi4j.core.catalog.CodigoPostalInfo;
import io.github.alktrip.cfdi4j.core.catalog.Requisito;
import io.github.alktrip.cfdi4j.core.catalog.Vigencia;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The large SAT catalogs c_ClaveProdServ, c_ClaveUnidad and c_CodigoPostal, generated from the
 * official catalog file named by {@link #SOURCE_FILE} (ADR 0003).
 *
 * <p>Adding this module to the class or module path is enough: {@code Cfdi40Validator} and the
 * {@code cfdi4j} facade find it with {@link java.util.ServiceLoader}. Each catalog is loaded the
 * first time it is queried and kept in memory (about 50,000 product keys and 95,000 postal
 * codes). Instances are thread-safe.
 */
public final class SatCatalogs implements CatalogProvider {

    /** File name of the SAT catalog the data comes from. */
    public static final String SOURCE_FILE = CatalogVersion.SOURCE_FILE;

    private static final SatCatalogs INSTANCE = new SatCatalogs();

    /**
     * Creates a provider. All instances share the same data; the constructor exists for
     * {@link java.util.ServiceLoader} on the class path, prefer {@link #provider()}.
     */
    public SatCatalogs() {
        // The data lives in static holders, loaded on first use.
    }

    /**
     * Returns the provider; also the {@link java.util.ServiceLoader} provider method.
     *
     * @return the shared instance
     */
    public static SatCatalogs provider() {
        return INSTANCE;
    }

    @Override
    public Optional<ClaveProdServInfo> claveProdServ(String clave) {
        return Optional.ofNullable(ProdServ.MAP.get(Objects.requireNonNull(clave, "clave")));
    }

    @Override
    public Optional<ClaveUnidadInfo> claveUnidad(String clave) {
        return Optional.ofNullable(Unidad.MAP.get(Objects.requireNonNull(clave, "clave")));
    }

    @Override
    public Optional<CodigoPostalInfo> codigoPostal(String codigoPostal) {
        return Optional.ofNullable(Postal.MAP.get(Objects.requireNonNull(codigoPostal, "codigoPostal")));
    }

    /**
     * Number of keys of each catalog, loading all of them.
     *
     * @return the counts by catalog name
     */
    public Map<String, Integer> sizes() {
        return Map.of("c_ClaveProdServ", ProdServ.MAP.size(), "c_ClaveUnidad", Unidad.MAP.size(), "c_CodigoPostal", Postal.MAP.size());
    }

    // One holder per catalog: the JVM loads each one lazily and safely.

    private static final class ProdServ {
        static final Map<String, ClaveProdServInfo> MAP = load("c_ClaveProdServ.tsv", 7, f -> new ClaveProdServInfo(f[0], f[1],
                Requisito.valueOf(f[2]), Requisito.valueOf(f[3]), f[4].isEmpty() ? Optional.empty() : Optional.of(f[4]), vigencia(f[5], f[6])));
    }

    private static final class Unidad {
        static final Map<String, ClaveUnidadInfo> MAP = load("c_ClaveUnidad.tsv", 4, f -> new ClaveUnidadInfo(f[0], f[1], vigencia(f[2], f[3])));
    }

    private static final class Postal {
        static final Map<String, CodigoPostalInfo> MAP = load("c_CodigoPostal.tsv", 6, f -> new CodigoPostalInfo(f[0], f[1].intern(),
                vigencia(f[2], f[3]), ZoneOffset.of(f[4]), ZoneOffset.of(f[5])));
    }

    static Vigencia vigencia(String inicio, String fin) {
        return fin.isEmpty() ? Vigencia.desde(LocalDate.parse(inicio)) : Vigencia.entre(LocalDate.parse(inicio), LocalDate.parse(fin));
    }

    static <T> Map<String, T> load(String resource, int fields, Function<String[], T> parse) {
        Map<String, T> map = new HashMap<>();
        try (InputStream in = SatCatalogs.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource + " of cfdi4j-catalogs");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                if (line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split("\t", -1);
                if (f.length != fields) {
                    throw new IllegalStateException(resource + " line " + number + ": expected " + fields + " fields but found " + f.length);
                }
                map.put(f[0], parse.apply(f));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Map.copyOf(map);
    }
}
