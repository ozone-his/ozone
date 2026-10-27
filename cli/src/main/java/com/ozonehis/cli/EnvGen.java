package com.ozonehis.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the .env: merges the compose project's vars.yml, the distro's
 * vars.yml (overrides), the model's `deployment:` section and the runtime
 * values, then leaves ${VAR} references between values raw.
 *
 * Merge order (later wins): code vars.yml -> distro vars.yml -> deployment
 * section -> runtime values.
 *
 * The `sso:` sub-blocks are merged only when SSO is enabled, per the
 * vars.yml header contract.
 */
final class EnvGen {

    private final boolean ssoEnabled;

    /**
     * `paths:` values of the distro's vars.yml are distro-relative and are
     * emitted as ${DISTRO_PATH}/... references (see mergeSubBlock); nothing
     * is resolved against the distro dir at generation time, keeping the
     * .env portable across machines.
     */
    EnvGen(boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    Map<String, String> generate(File codeVars, File distroVars, Map<String, Object> deployment,
            Map<String, String> runtime) throws IOException {

        Map<String, String> env = new LinkedHashMap<String, String>();

        // Runtime roots (HOST_NAME, SERVER_SCHEME, ...) must come FIRST:
        // compose interpolates env-file values in file order, single pass,
        // so every ${HOST_NAME} reference below them resolves. They are
        // put again at the end so their values also win the merge.
        env.putAll(runtime);
        mergeOwnerBlocks(env, Yamls.load(codeVars), false);
        if (distroVars.isFile()) {
            mergeOwnerBlocks(env, Yamls.load(distroVars), true);
        }
        mergeDeploymentVars(env, deployment);
        env.putAll(runtime);
        // ${VAR} references between values are left as-is on purpose: compose
        // resolves them at runtime, so a deployment can override HOST_NAME
        // (edit the line or export it in the shell) without regenerating,
        // and every derived hostname follows. Resolution for display happens
        // in substitute(), on a copy.
        return env;
    }

    /** Resolves ${VAR} references between values, e.g. hostnames referencing ${HOST_NAME}. Display only. */
    static Map<String, String> substitute(Map<String, String> env) {
        Map<String, String> resolved = new LinkedHashMap<String, String>(env);
        substituteInPlace(resolved);
        return resolved;
    }

    private static void substituteInPlace(Map<String, String> env) {
        for (int pass = 0; pass < 10; pass++) {
            boolean changed = false;
            for (Map.Entry<String, String> var : env.entrySet()) {
                String value = var.getValue();
                if (value == null || !value.contains("${")) {
                    continue;
                }
                String resolved = resolve(value, env);
                if (!resolved.equals(value)) {
                    var.setValue(resolved);
                    changed = true;
                }
            }
            if (!changed) {
                return;
            }
        }
    }

    /**
     * vars.yml owners: apps / integrations / modes / databases / restic.
     * Each section holds named blocks (e.g. apps.openmrs), each block with
     * configs / paths (always merged) and sso (merged only when SSO is
     * enabled). A section may also carry configs / paths directly, e.g. the
     * shared databases paths — but not sso: a section child named 'sso' is
     * the sso mode block, a named block, not an sso sub-block.
     */
    private void mergeOwnerBlocks(Map<String, String> env, Map<String, Object> vars, boolean override) {
        for (Map.Entry<String, Object> section : vars.entrySet()) {
            if (!(section.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> blocks = Yamls.map(section.getValue());
            mergeSubBlock(env, blocks.get("configs"), override, false);
            mergeSubBlock(env, blocks.get("paths"), override, true);
            for (Map.Entry<String, Object> block : blocks.entrySet()) {
                if (block.getValue() instanceof Map) {
                    mergeBlock(env, Yamls.map(block.getValue()), override);
                }
            }
        }
    }

    private void mergeBlock(Map<String, String> env, Map<String, Object> fields, boolean override) {
        mergeSubBlock(env, fields.get("configs"), override, false);
        mergeSubBlock(env, fields.get("paths"), override, true);
        if (ssoEnabled) {
            mergeSubBlock(env, fields.get("sso"), override, false);
        }
    }

    private void mergeSubBlock(Map<String, String> env, Object subBlock, boolean override, boolean resolvePaths) {
        for (Map.Entry<String, Object> var : Yamls.map(subBlock).entrySet()) {
            String value = var.getValue() == null ? "" : String.valueOf(var.getValue());
            if (resolvePaths && !value.isEmpty() && !value.startsWith("/") && !value.startsWith("$")) {
                // Distro-relative paths stay as ${DISTRO_PATH} references: the
                // .env must be portable across machines, and DISTRO_PATH is a
                // runtime root written at the top of the file (and overridable
                // by the deployment through the shell), exactly like HOST_NAME.
                value = "${DISTRO_PATH}/" + value;
            }
            if (override || !env.containsKey(var.getKey())) {
                env.put(var.getKey(), value);
            }
        }
    }

    /**
     * deployment_vars.yml: services.&lt;name&gt;.{hostnames,datadirs,configs}
     * and the extra block — all flattened, always overriding.
     */
    private void mergeDeploymentVars(Map<String, String> env, Map<String, Object> deploymentVars) {
        for (Map.Entry<String, Object> service : Yamls.map(deploymentVars.get("services")).entrySet()) {
            Map<String, Object> block = Yamls.map(service.getValue());
            mergeSubBlock(env, block.get("hostnames"), true, false);
            mergeSubBlock(env, block.get("datadirs"), true, false);
            mergeSubBlock(env, block.get("configs"), true, false);
        }
        mergeSubBlock(env, deploymentVars.get("extra"), true, false);
    }

    private static String resolve(String value, Map<String, String> env) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int start = value.indexOf("${", i);
            if (start < 0) {
                out.append(value, i, value.length());
                break;
            }
            int end = value.indexOf('}', start);
            if (end < 0) {
                out.append(value, i, value.length());
                break;
            }
            out.append(value, i, start);
            String name = value.substring(start + 2, end);
            String replacement = env.get(name);
            out.append(replacement == null ? "${" + name + "}" : replacement);
            i = end + 1;
        }
        return out.toString();
    }

    static List<String> toFileLines(Map<String, String> env) {
        List<String> lines = new ArrayList<String>();
        for (Map.Entry<String, String> var : env.entrySet()) {
            lines.add(var.getKey() + "=" + (var.getValue() == null ? "" : var.getValue()));
        }
        return lines;
    }
}
