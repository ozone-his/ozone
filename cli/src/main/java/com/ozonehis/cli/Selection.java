package com.ozonehis.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The distro's selection (distro/selection.yml): what WILL run - the
 * selected apps and the enabled modes. Integrations are never listed here;
 * they activate automatically when their required apps are all selected.
 */
final class Selection {

    final List<String> apps = new ArrayList<String>();
    final Map<String, Boolean> modes = new LinkedHashMap<String, Boolean>();

    static Selection load(File selectionYml) throws IOException {
        Selection selection = new Selection();
        Map<String, Object> root = Yamls.load(selectionYml);
        selection.apps.addAll(Yamls.stringList(root.get("apps")));
        for (Map.Entry<String, Object> e : Yamls.map(root.get("modes")).entrySet()) {
            boolean enabled = "true".equalsIgnoreCase(String.valueOf(e.getValue()));
            selection.modes.put(e.getKey(), enabled);
        }
        return selection;
    }

    boolean modeEnabled(String mode) {
        Boolean value = modes.get(mode);
        return value != null && value;
    }
}
