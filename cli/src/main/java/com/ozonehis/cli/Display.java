package com.ozonehis.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The presentation layer of the ozone CLI: everything the tool prints. This is
 * the "frontend" — isolated from the assembly logic (Model, Resolver, EnvGen,
 * and the command orchestration in OzoneCli) so the console output can evolve
 * independently.
 *
 * The glyphs carry fixed meanings: ✓ a confirmation, ● a row of the
 * deployment, 🔑 the credentials section, ℹ an informational note. The
 * printed command itself is never decorated: it is fenced between rules so
 * it stands out and stays byte-exact for copy/paste and extraction.
 */
final class Display {

    /** All rules and note wrapping align to this fixed width. */
    private static final int RULE_WIDTH = 72;

    private final boolean printCredentials;

    Display(boolean printCredentials) {
        this.printCredentials = printCredentials;
    }

    void validated(int chainSize) {
        System.out.println("✓ Selection is valid — " + chainSize + " compose files on the chain.");
    }

    void command(String command) {
        System.out.println();
        System.out.println(rule("Command"));
        System.out.println();
        System.out.println(command);
        System.out.println();
        System.out.println(rule(null));
        System.out.println();
    }

    void artifactsWritten(String manifestPath, String envPath) {
        System.out.println("✓ Manifest written: " + manifestPath);
        System.out.println("✓ Env file written: " + envPath);
    }

    void resolvedWritten(List<File> resolvedFiles) {
        for (File file : resolvedFiles) {
            System.out.println("✓ Resolved file written: " + file);
        }
    }

    void summary(Model model, Plan plan, Map<String, String> env) {
        List<String[]> services = new ArrayList<String[]>();
        for (String appName : plan.apps) {
            Model.Entry app = model.apps.get(appName);
            if (app != null && app.url != null) {
                services.add(new String[] { appName, resolveTemplate(app.url, env) });
            }
        }
        if (plan.ssoEnabled()) {
            Model.Entry sso = model.modes.get("sso");
            if (sso != null && sso.url != null) {
                services.add(new String[] { "keycloak", resolveTemplate(sso.url, env) });
            }
        }

        System.out.println("● Services");
        int width = nameWidth(services);
        for (String[] service : services) {
            System.out.println("  " + column(service[0], service[1], width));
        }
        System.out.println();

        if (!printCredentials) {
            return;
        }
        if (plan.ssoEnabled()) {
            ssoCredentials(model, plan, env);
        } else {
            defaultCredentials(model, plan, env);
        }
    }

    /**
     * With SSO enabled the apps' declared credentials do not apply: logins
     * are Keycloak users. Only the Keycloak admin console credentials hold;
     * the rest must be created there, or is provisioned by the demo-data
     * service from its users file.
     */
    private void ssoCredentials(Model model, Plan plan, Map<String, String> env) {
        System.out.println("🔑 Credentials — app logins are managed by Keycloak (SSO enabled)");
        Model.Entry sso = model.modes.get("sso");
        if (sso != null && !sso.credentials.isEmpty()) {
            System.out.println("  ● " + column("keycloak", credentialValue(sso, env), "keycloak".length()));
        }
        if (Boolean.TRUE.equals(plan.modes.get("demo-data"))) {
            Model.Entry demoData = model.modes.get("demo-data");
            if (demoData != null && demoData.ssoUsers != null) {
                note("Demo users are provisioned by the demo-data service from: "
                        + resolveTemplate(demoData.ssoUsers, env));
            }
        } else {
            note("No users are provisioned yet: create a user for each activated client in the"
                    + " Keycloak admin console.");
        }
        System.out.println();
    }

    private void defaultCredentials(Model model, Plan plan, Map<String, String> env) {
        List<String[]> credentials = new ArrayList<String[]>();
        for (String appName : plan.apps) {
            Model.Entry app = model.apps.get(appName);
            if (app != null && !app.credentials.isEmpty()) {
                credentials.add(new String[] { appName, credentialValue(app, env) });
            }
        }
        if (credentials.isEmpty()) {
            return;
        }
        System.out.println("🔑 Default credentials");
        int width = nameWidth(credentials);
        for (String[] credential : credentials) {
            System.out.println("  ● " + column(credential[0], credential[1], width));
        }
        System.out.println();
    }

    private String credentialValue(Model.Entry entry, Map<String, String> env) {
        StringBuilder sb = new StringBuilder();
        for (String varName : entry.credentials) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(env.containsKey(varName) ? env.get(varName) : "${" + varName + "}");
        }
        return sb.toString();
    }

    private String resolveTemplate(String template, Map<String, String> env) {
        String resolved = template;
        for (int pass = 0; pass < 10 && resolved.contains("${"); pass++) {
            int start = resolved.indexOf("${");
            int end = resolved.indexOf('}', start);
            if (end < 0) {
                break;
            }
            String name = resolved.substring(start + 2, end);
            String value = env.get(name);
            String next = resolved.substring(0, start) + (value == null ? "${" + name + "}" : value)
                    + resolved.substring(end + 1);
            if (next.equals(resolved)) {
                break;
            }
            resolved = next;
        }
        return resolved;
    }

    /** A rule: plain, or opening the command block with a label. */
    private static String rule(String label) {
        StringBuilder sb = new StringBuilder();
        if (label != null) {
            sb.append("── ").append(label).append(' ');
        }
        while (sb.length() < RULE_WIDTH) {
            sb.append('─');
        }
        return sb.toString();
    }

    /** A note: wrapped to the rule width with a hanging indent. */
    private static void note(String text) {
        String prefix = "  ℹ ";
        StringBuilder line = new StringBuilder();
        int width = RULE_WIDTH - prefix.length();
        for (String word : text.split(" ")) {
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() > width) {
                System.out.println(prefix + line);
                prefix = "    ";
                line = new StringBuilder(word);
            } else {
                line.append(' ').append(word);
            }
        }
        System.out.println(prefix + line);
    }

    private static int nameWidth(List<String[]> rows) {
        int width = 0;
        for (String[] row : rows) {
            width = Math.max(width, row[0].length());
        }
        return width;
    }

    /** A row: the name padded to a column aligned across the section, then the value. */
    private static String column(String name, String value, int nameWidth) {
        StringBuilder sb = new StringBuilder(name);
        while (sb.length() < nameWidth + 4) {
            sb.append(' ');
        }
        return sb.append(value).toString();
    }
}
