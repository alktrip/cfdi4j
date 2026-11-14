package io.github.alktrip.cfdi4j.tools.catalogs;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Constant names for catalog keys, read from {@code catalog-names.yaml} (ADR 0008).
 *
 * <p>For each catalog the file holds either {@code key} (the key is the name), {@code key-upper}
 * (the upper-cased key is the name) or a map from key to a curated name. A key without a name, a
 * name without a key, an invalid identifier or a duplicated name stops the generator.
 */
final class NameTable {

    private static final Pattern CONSTANT = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final Map<String, Object> catalogs;
    private final Set<String> used = new HashSet<>();

    private NameTable(Map<String, Object> catalogs) {
        this.catalogs = catalogs;
    }

    static NameTable load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
            if (!(root instanceof Map<?, ?> map)) {
                throw new GeneratorException(file + ": expected a map of catalogs");
            }
            Map<String, Object> catalogs = new LinkedHashMap<>();
            map.forEach((k, v) -> catalogs.put(String.valueOf(k), v));
            return new NameTable(catalogs);
        }
    }

    /**
     * Returns the constant name for every key, in the order of {@code keys}.
     *
     * @param catalog catalog name, for example {@code c_FormaPago}
     * @param keys keys in catalog order, with their descriptions for error messages
     */
    Map<String, String> namesFor(String catalog, Map<String, String> keys) {
        Object spec = catalogs.get(catalog);
        if (spec == null) {
            throw new GeneratorException("catalog-names.yaml has no entry for " + catalog);
        }
        used.add(catalog);
        Map<String, String> names = new LinkedHashMap<>();
        if ("key".equals(spec)) {
            keys.keySet().forEach(k -> names.put(k, k));
        } else if ("key-upper".equals(spec)) {
            keys.keySet().forEach(k -> names.put(k, k.toUpperCase(Locale.ROOT)));
        } else if (spec instanceof Map<?, ?> curated) {
            List<String> problems = new ArrayList<>();
            for (Object k : curated.keySet()) {
                if (!(k instanceof String)) {
                    problems.add("key " + k + " must be quoted in the YAML file");
                } else if (!keys.containsKey(k)) {
                    problems.add("name given for key '" + k + "', which is not in the catalog");
                }
            }
            keys.forEach((key, descripcion) -> {
                Object name = curated.get(key);
                if (name == null) {
                    problems.add("no name for new key '" + key + "' (" + descripcion + ")");
                } else {
                    names.put(key, String.valueOf(name));
                }
            });
            if (!problems.isEmpty()) {
                throw new GeneratorException(catalog + ": " + String.join("; ", problems));
            }
        } else {
            throw new GeneratorException(catalog + ": expected 'key', 'key-upper' or a map of names");
        }
        Set<String> seen = new HashSet<>();
        names.forEach((key, name) -> {
            if (!CONSTANT.matcher(name).matches()) {
                throw new GeneratorException(catalog + ": '" + name + "' (key " + key + ") is not a constant name");
            }
            if (!seen.add(name)) {
                throw new GeneratorException(catalog + ": name '" + name + "' is used twice");
            }
        });
        return names;
    }

    /** Fails if the file names catalogs that the generator does not produce. */
    void checkAllUsed() {
        Set<String> unused = new HashSet<>(catalogs.keySet());
        unused.removeAll(used);
        if (!unused.isEmpty()) {
            throw new GeneratorException("catalog-names.yaml has entries for unknown catalogs: " + unused);
        }
    }
}
