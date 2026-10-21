package com.ozonehis.ozonectl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a selection against the model and resolves the plan: the
 * ordered compose chain, the db-init list, the restore invocation and the
 * per-mode ad-hoc chains. Every rule fails loudly; nothing is guessed.
 */
final class Resolver {

    private final Model model;
    private final Selection selection;
    private final File composeRoot;
    private final File distroDir;

    Resolver(Model model, Selection selection, File composeRoot, File distroDir) {
        this.model = model;
        this.selection = selection;
        this.composeRoot = composeRoot;
        this.distroDir = distroDir;
    }

    Plan resolve() throws IOException {
        validateSelection();
        Plan plan = new Plan();

        plan.apps.addAll(selection.apps);
        plan.modes.putAll(selection.modes);

        // Integrations activate automatically; model order keeps the chain
        // deterministic.
        for (Model.Entry integration : model.integrations.values()) {
            if (selection.apps.containsAll(integration.requires)) {
                plan.integrations.add(integration.name);
            }
        }

        LinkedHashSet<String> engines = new LinkedHashSet<String>();
        for (String appName : selection.apps) {
            engines.addAll(model.apps.get(appName).db);
        }
        for (String integrationName : plan.integrations) {
            engines.addAll(model.integrations.get(integrationName).db);
        }
        for (Model.Entry mode : model.modes.values()) {
            if (enabledModesOnly().contains(mode.name)) {
                engines.addAll(mode.db);
            }
        }

        List<String> chain = plan.chain;
        chain.addAll(model.baseCompose);
        for (String engine : model.databaseServices.keySet()) {
            if (engines.contains(engine)) {
                chain.add(model.databaseServices.get(engine));
            }
        }
        for (String appName : selection.apps) {
            chain.addAll(model.apps.get(appName).compose);
        }
        for (String integrationName : plan.integrations) {
            chain.addAll(model.integrations.get(integrationName).compose);
        }
        for (Model.Entry mode : model.modes.values()) {
            if (enabledModesOnly().contains(mode.name)) {
                chain.addAll(mode.compose);
            }
        }
        if (plan.ssoEnabled()) {
            for (String appName : selection.apps) {
                chain.addAll(model.apps.get(appName).sso);
            }
            for (String integrationName : plan.integrations) {
                chain.addAll(model.integrations.get(integrationName).sso);
            }
            for (Model.Entry mode : model.modes.values()) {
                if (enabledModesOnly().contains(mode.name)) {
                    chain.addAll(mode.sso);
                }
            }
        }
        dedupe(chain);
        checkFilesExist(chain);

        for (String appName : selection.apps) {
            addAll(plan.dbInit, model.apps.get(appName).dbInit);
        }
        for (String integrationName : plan.integrations) {
            addAll(plan.dbInit, model.integrations.get(integrationName).dbInit);
        }
        for (Model.Entry mode : model.modes.values()) {
            if (enabledModesOnly().contains(mode.name)) {
                addAll(plan.dbInit, mode.dbInit);
            }
        }
        checkDbInitDirs(plan.dbInit);

        // The pre-computed restore invocation.
        Model.Entry restoreMode = model.modes.get("restore");
        if (restoreMode != null) {
            plan.restoreCompose.addAll(restoreMode.compose);
        }
        if (plan.ssoEnabled()) {
            Model.Entry ssoMode = model.modes.get("sso");
            if (ssoMode != null) {
                plan.restoreCompose.addAll(ssoMode.restoreCompose);
                addAll(plan.restoreProfiles, ssoMode.restoreProfiles);
            }
        }
        for (String appName : selection.apps) {
            addAll(plan.restoreProfiles, model.apps.get(appName).restoreProfiles);
        }
        dedupe(plan.restoreCompose);
        checkFilesExist(plan.restoreCompose);

        // Pre-computed ad-hoc chains for every invocable mode (`up <mode>`):
        // base + the mode's files + its databases + its sso override when sso
        // is enabled in the selection.
        for (Model.Entry mode : model.modes.values()) {
            if ("restore".equals(mode.name)) {
                continue;
            }
            List<String> modeChain = new ArrayList<String>();
            modeChain.addAll(model.baseCompose);
            for (String engine : model.databaseServices.keySet()) {
                if (mode.db.contains(engine)) {
                    modeChain.add(model.databaseServices.get(engine));
                }
            }
            modeChain.addAll(mode.compose);
            if (plan.ssoEnabled()) {
                modeChain.addAll(mode.sso);
            }
            dedupe(modeChain);
            checkFilesExist(modeChain);
            plan.modeChains.put(mode.name, modeChain);
        }

        return plan;
    }

    private Set<String> enabledModesOnly() {
        Set<String> enabled = new LinkedHashSet<String>();
        for (Model.Entry mode : model.modes.values()) {
            if (selection.modeEnabled(mode.name)) {
                enabled.add(mode.name);
            }
        }
        return enabled;
    }

    private void validateSelection() throws IOException {
        for (String appName : selection.apps) {
            if (model.integrations.containsKey(appName)) {
                throw new IOException("Selection error: '" + appName + "' is an integration; integrations "
                        + "activate automatically when their required apps are selected and are never listed "
                        + "in selection.yml");
            }
            if (!model.apps.containsKey(appName)) {
                throw new IOException("Selection error: unknown app '" + appName + "'. Valid apps: "
                        + model.apps.keySet());
            }
        }
        for (String modeName : selection.modes.keySet()) {
            if (!model.modes.containsKey(modeName)) {
                throw new IOException("Selection error: unknown mode '" + modeName + "'. Valid modes: "
                        + model.modes.keySet());
            }
        }
        for (String[] pair : model.incompatibilities) {
            if (selection.apps.contains(pair[0]) && selection.apps.contains(pair[1])) {
                throw new IOException("Selection error: incompatible apps '" + pair[0] + "' and '" + pair[1]
                        + "' cannot be selected together");
            }
        }
    }

    private void checkFilesExist(List<String> files) throws IOException {
        for (String file : files) {
            if (!new File(composeRoot, file).isFile()) {
                throw new IOException("Model error: compose file does not exist: " + file);
            }
        }
    }

    private void checkDbInitDirs(List<String> dirs) throws IOException {
        for (String dir : dirs) {
            boolean inMysql = new File(distroDir, "data/mysql/" + dir).isDirectory();
            boolean inPostgresql = new File(distroDir, "data/postgresql/" + dir).isDirectory();
            if (!inMysql && !inPostgresql) {
                throw new IOException("Selection error: db-init directory '" + dir
                        + "' not found in the distro's data tree (data/mysql/ or data/postgresql/)");
            }
        }
    }

    private static void addAll(List<String> target, List<String> source) {
        for (String item : source) {
            if (!target.contains(item)) {
                target.add(item);
            }
        }
    }

    private static void dedupe(List<String> list) {
        List<String> seen = new ArrayList<String>();
        for (String item : list) {
            if (!seen.contains(item)) {
                seen.add(item);
            }
        }
        list.clear();
        list.addAll(seen);
    }
}
