package com.ozonehis.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The structural merge of distro files (vars.yml, model-overrides.yml):
 * maps merge recursively per key, lists append, scalars are overridden
 * with the child's value — the declared inheritance rules, with type
 * conflicts rejected loudly.
 */
final class YamlMerge {

    private YamlMerge() {
    }

    static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> override, String file)
            throws IOException {
        Map<String, Object> out = new LinkedHashMap<String, Object>(base);
        for (Map.Entry<String, Object> e : override.entrySet()) {
            out.put(e.getKey(), value(e.getKey(), base.get(e.getKey()), e.getValue(), file));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object value(String path, Object base, Object override, String file) throws IOException {
        if (base == null) {
            return override;
        }
        if (base instanceof Map && override instanceof Map) {
            return merge((Map<String, Object>) base, (Map<String, Object>) override, file);
        }
        if (base instanceof List && override instanceof List) {
            List<Object> out = new ArrayList<Object>((List<Object>) base);
            out.addAll((List<Object>) override);
            return out;
        }
        if (base instanceof Map || base instanceof List || override instanceof Map || override instanceof List) {
            throw new IOException("Merge error in " + file + ": cannot merge the values at '" + path
                    + "': a " + base.getClass().getSimpleName() + " with a " + override.getClass().getSimpleName());
        }
        return override;
    }
}
