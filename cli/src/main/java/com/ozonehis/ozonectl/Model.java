package com.ozonehis.ozonectl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The compose project's model (compose/configs/model.yml): everything that
 * CAN run and the rules of combination.
 */
final class Model {

    final List<String> baseCompose = new ArrayList<String>();

    /** database engine -> compose file */
    final Map<String, String> databaseServices = new LinkedHashMap<String, String>();

    final Map<String, Entry> apps = new LinkedHashMap<String, Entry>();
    final Map<String, Entry> integrations = new LinkedHashMap<String, Entry>();
    final Map<String, Entry> modes = new LinkedHashMap<String, Entry>();

    /** pairs of app names that cannot be selected together */
    final List<String[]> incompatibilities = new ArrayList<String[]>();

    /**
     * The model's `deployment:` section — deployment-provided value
     * declarations (hostname templates, datadirs, extra globals). The
     * values themselves are deployment-time; this declares the knobs.
     */
    final Map<String, Object> deployment = new LinkedHashMap<String, Object>();

    /** One model entry: an app, an integration or a mode. */
    static final class Entry {

        String name;
        final List<String> requires = new ArrayList<String>();
        final List<String> compose = new ArrayList<String>();
        final List<String> sso = new ArrayList<String>();
        final List<String> restoreProfiles = new ArrayList<String>();
        final List<String> restoreCompose = new ArrayList<String>();
        final List<String> dbInit = new ArrayList<String>();
        final List<String> db = new ArrayList<String>();
        String url;
        final List<String> credentials = new ArrayList<String>();
        /** e.g. demo-data's `sso-users:` — the Keycloak users a mode provisions */
        String ssoUsers;
    }

    static Model load(File modelYml) throws IOException {
        Model model = new Model();
        Map<String, Object> root = Yamls.load(modelYml);

        model.baseCompose.addAll(Yamls.stringList(Yamls.map(root.get("base")).get("compose")));

        for (Map.Entry<String, Object> e : Yamls.map(root.get("database-services")).entrySet()) {
            model.databaseServices.put(e.getKey(), String.valueOf(e.getValue()));
        }

        fill(model.apps, Yamls.map(root.get("apps")));
        fill(model.integrations, Yamls.map(root.get("integrations")));
        fill(model.modes, Yamls.map(root.get("modes")));

        Object incompat = root.get("incompatibilities");
        if (incompat instanceof List) {
            for (Object pair : (List<?>) incompat) {
                List<String> names = Yamls.stringList(pair);
                if (names.size() != 2) {
                    throw new IOException("incompatibilities entries must be pairs, got: " + names);
                }
                model.incompatibilities.add(names.toArray(new String[0]));
            }
        }

        model.deployment.putAll(Yamls.map(root.get("deployment")));

        return model;
    }

    private static void fill(Map<String, Entry> target, Map<String, Object> source) throws IOException {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            Entry entry = new Entry();
            entry.name = e.getKey();
            Map<String, Object> fields = Yamls.map(e.getValue());
            entry.requires.addAll(Yamls.stringList(fields.get("requires")));
            entry.compose.addAll(Yamls.stringList(fields.get("compose")));
            entry.sso.addAll(Yamls.stringList(fields.get("sso")));
            entry.restoreProfiles.addAll(Yamls.stringList(fields.get("restore-profiles")));
            entry.restoreCompose.addAll(Yamls.stringList(fields.get("restore-compose")));
            entry.dbInit.addAll(Yamls.stringList(fields.get("db-init")));
            entry.db.addAll(Yamls.stringList(fields.get("db")));
            entry.url = Yamls.string(fields.get("url"));
            entry.credentials.addAll(Yamls.stringList(fields.get("credentials")));
            entry.ssoUsers = Yamls.string(fields.get("sso-users"));
            target.put(e.getKey(), entry);
        }
    }
}
