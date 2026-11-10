package com.ozonehis.cli;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small, defensive helpers over SnakeYAML's Map&lt;String,Object&gt; graphs. */
final class Yamls {

    private Yamls() {
    }

    static Map<String, Object> load(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            Object loaded = new Yaml().load(in);
            if (loaded == null) {
                return new LinkedHashMap<String, Object>();
            }
            if (!(loaded instanceof Map)) {
                throw new IOException("Expected a mapping at the root of " + file);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) loaded;
            return map;
        }
    }

    static Map<String, Object> map(Object value) {
        if (value == null) {
            return new LinkedHashMap<String, Object>();
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        throw new IllegalStateException("Expected a mapping, got: " + value.getClass().getSimpleName());
    }

    static List<String> stringList(Object value) {
        List<String> out = new ArrayList<String>();
        if (value == null) {
            return out;
        }
        if (value instanceof String) {
            out.add((String) value);
            return out;
        }
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                out.add(String.valueOf(item));
            }
            return out;
        }
        throw new IllegalStateException("Expected a string or a list, got: " + value.getClass().getSimpleName());
    }

    static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Dumps a map as block-style YAML - the same layout as the hand-written
     * sources (SnakeYAML's default would inline compact nested collections).
     */
    static String dump(Map<String, Object> map) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options).dump(map);
    }

    static String fileHeader(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append("# ").append(line).append("\n");
        }
        return sb.toString();
    }
}
