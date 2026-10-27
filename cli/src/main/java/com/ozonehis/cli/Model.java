package com.ozonehis.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The compose project's model (compose/configs/model.yml): everything that
 * CAN run and the rules of combination. A distro may extend it with an
 * optional model-overrides.yml (see mergeOverlay) to add entries of its
 * own or append files to existing ones — never to remove anything.
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

    /** compose files declared by the base model — resolved against the compose root */
    final Set<String> composeRootFiles = new LinkedHashSet<String>();

    /** compose files declared by the distro's model-overrides.yml — resolved against the distro dir */
    final Set<String> distroDirFiles = new LinkedHashSet<String>();

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
        model.composeRootFiles.addAll(model.baseCompose);

        for (Map.Entry<String, Object> e : Yamls.map(root.get("database-services")).entrySet()) {
            String file = String.valueOf(e.getValue());
            model.databaseServices.put(e.getKey(), file);
            model.composeRootFiles.add(file);
        }

        model.fill(model.apps, Yamls.map(root.get("apps")), false);
        model.fill(model.integrations, Yamls.map(root.get("integrations")), false);
        model.fill(model.modes, Yamls.map(root.get("modes")), false);

        addIncompatibilities(model.incompatibilities, root.get("incompatibilities"));

        model.deployment.putAll(Yamls.map(root.get("deployment")));

        return model;
    }

    /**
     * Merges the distro's optional model-overrides.yml into this model.
     * The overlay is strictly additive: new entries (apps, integrations,
     * modes, database engines, incompatibility pairs, deployment knobs)
     * are added; on existing entries list fields are appended and declared
     * scalars (url, sso-users, a database engine's file) are overridden
     * with the child's value. Nothing can be removed — disabling a service
     * is done in selection.yml.
     */
    void mergeOverlay(Map<String, Object> overlay) throws IOException {
        for (String file : Yamls.stringList(Yamls.map(overlay.get("base")).get("compose"))) {
            declareDistroFile(file);
            baseCompose.add(file);
        }

        for (Map.Entry<String, Object> e : Yamls.map(overlay.get("database-services")).entrySet()) {
            String file = String.valueOf(e.getValue());
            declareDistroFile(file);
            databaseServices.put(e.getKey(), file);
        }

        fill(apps, Yamls.map(overlay.get("apps")), true);
        fill(integrations, Yamls.map(overlay.get("integrations")), true);
        fill(modes, Yamls.map(overlay.get("modes")), true);

        addIncompatibilities(incompatibilities, overlay.get("incompatibilities"));

        mergeDeployment(deployment, Yamls.map(overlay.get("deployment")));
    }

    /**
     * Records a compose file declared by the overlay. Overlay-declared files
     * are the distro's own: declaring a base-model file is a model error,
     * not a silent reuse.
     */
    private void declareDistroFile(String file) throws IOException {
        if (composeRootFiles.contains(file)) {
            throw new IOException("Model error: model-overrides.yml declares '" + file + "', which is a file"
                    + " of the base model: overlay-declared paths must be the distro's own files");
        }
        distroDirFiles.add(file);
    }

    private void fill(Map<String, Entry> target, Map<String, Object> source, boolean overlay) throws IOException {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            Entry entry = target.get(e.getKey());
            boolean isNew = entry == null;
            if (isNew) {
                entry = new Entry();
                entry.name = e.getKey();
            }
            applyFields(entry, Yamls.map(e.getValue()), overlay);
            if (isNew) {
                target.put(e.getKey(), entry);
            }
        }
    }

    private void applyFields(Entry entry, Map<String, Object> fields, boolean overlay) throws IOException {
        declareFiles(Yamls.stringList(fields.get("compose")), overlay);
        declareFiles(Yamls.stringList(fields.get("sso")), overlay);
        declareFiles(Yamls.stringList(fields.get("restore-compose")), overlay);
        entry.requires.addAll(Yamls.stringList(fields.get("requires")));
        entry.compose.addAll(Yamls.stringList(fields.get("compose")));
        entry.sso.addAll(Yamls.stringList(fields.get("sso")));
        entry.restoreProfiles.addAll(Yamls.stringList(fields.get("restore-profiles")));
        entry.restoreCompose.addAll(Yamls.stringList(fields.get("restore-compose")));
        entry.dbInit.addAll(Yamls.stringList(fields.get("db-init")));
        entry.db.addAll(Yamls.stringList(fields.get("db")));
        String url = Yamls.string(fields.get("url"));
        if (url != null) {
            entry.url = url;
        }
        entry.credentials.addAll(Yamls.stringList(fields.get("credentials")));
        String ssoUsers = Yamls.string(fields.get("sso-users"));
        if (ssoUsers != null) {
            entry.ssoUsers = ssoUsers;
        }
    }

    /** Records declared compose files under their declaring root: base or overlay. */
    private void declareFiles(List<String> files, boolean overlay) throws IOException {
        if (overlay) {
            for (String file : files) {
                declareDistroFile(file);
            }
        } else {
            composeRootFiles.addAll(files);
        }
    }

    private static void addIncompatibilities(List<String[]> target, Object incompat) throws IOException {
        if (!(incompat instanceof List)) {
            return;
        }
        for (Object pair : (List<?>) incompat) {
            List<String> names = Yamls.stringList(pair);
            if (names.size() != 2) {
                throw new IOException("incompatibilities entries must be pairs, got: " + names);
            }
            target.add(names.toArray(new String[0]));
        }
    }

    /**
     * deployment section merge: services.&lt;name&gt;.{hostnames,datadirs,configs}
     * and the extra block — per var, the child-declared value wins.
     */
    private static void mergeDeployment(Map<String, Object> target, Map<String, Object> overlay) {
        Map<String, Object> services = Yamls.map(target.get("services"));
        for (Map.Entry<String, Object> service : Yamls.map(overlay.get("services")).entrySet()) {
            Map<String, Object> blocks = Yamls.map(services.get(service.getKey()));
            for (Map.Entry<String, Object> block : Yamls.map(service.getValue()).entrySet()) {
                Map<String, Object> vars = Yamls.map(blocks.get(block.getKey()));
                vars.putAll(Yamls.map(block.getValue()));
                blocks.put(block.getKey(), vars);
            }
            services.put(service.getKey(), blocks);
        }
        target.put("services", services);
        Map<String, Object> extra = Yamls.map(target.get("extra"));
        extra.putAll(Yamls.map(overlay.get("extra")));
        target.put("extra", extra);
    }
}
