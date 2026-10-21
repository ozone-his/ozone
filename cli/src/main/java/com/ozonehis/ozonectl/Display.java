package com.ozonehis.ozonectl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The presentation layer of ozonectl: everything the tool prints. This is
 * the "frontend" — isolated from the assembly logic (Model, Resolver,
 * EnvGen, and the command orchestration in Cli) so the console output can
 * evolve independently: labels, guidance texts and the credentials
 * semantics (SSO vs non-SSO) live here and nowhere else.
 */
final class Display {

    private final boolean printCredentials;

    Display(boolean printCredentials) {
        this.printCredentials = printCredentials;
    }

    void validated(int chainSize) {
        System.out.println("OK: selection is valid, " + chainSize + " compose files on the chain.");
    }

    void command(String command) {
        System.out.println(command);
        System.out.println();
    }

    void artifactsWritten(String manifestPath, String envPath) {
        System.out.println("Manifest written to: " + manifestPath);
        System.out.println("Env file written to: " + envPath);
    }

    void summary(Model model, Plan plan, Map<String, String> env) {
        List<String[]> urls = new ArrayList<String[]>();
        for (String appName : plan.apps) {
            Model.Entry app = model.apps.get(appName);
            if (app != null && app.url != null) {
                urls.add(new String[] { appName, resolveTemplate(app.url, env) });
            }
        }
        if (plan.ssoEnabled()) {
            Model.Entry sso = model.modes.get("sso");
            if (sso != null && sso.url != null) {
                urls.add(new String[] { "keycloak", resolveTemplate(sso.url, env) });
            }
        }

        System.out.println("Services:");
        for (String[] url : urls) {
            System.out.println("  " + pad(url[0]) + " " + url[1]);
        }
        System.out.println();

        if (!printCredentials) {
            return;
        }
        if (plan.ssoEnabled()) {
            ssoCredentials(model, plan, env);
        } else {
            List<String[]> credentials = new ArrayList<String[]>();
            for (String appName : plan.apps) {
                collectCredentials(model.apps.get(appName), appName, env, credentials);
            }
            if (!credentials.isEmpty()) {
                System.out.println("Default credentials:");
                for (String[] credential : credentials) {
                    System.out.println("  " + pad(credential[0]) + " " + credential[1]);
                }
                System.out.println();
            }
        }
    }

    /**
     * With SSO enabled the apps' declared credentials do not apply: logins
     * are Keycloak users. Only the Keycloak admin console credentials hold;
     * the rest must be created there, or is provisioned by the demo-data
     * service from its users file.
     */
    private void ssoCredentials(Model model, Plan plan, Map<String, String> env) {
        System.out.println("Credentials: app logins are managed by Keycloak (SSO enabled).");
        Model.Entry sso = model.modes.get("sso");
        if (sso != null && !sso.credentials.isEmpty()) {
            System.out.println("  " + pad("keycloak") + " " + credentialValue(sso, env));
        }
        if (Boolean.TRUE.equals(plan.modes.get("demo-data"))) {
            Model.Entry demoData = model.modes.get("demo-data");
            if (demoData != null && demoData.ssoUsers != null) {
                System.out.println("  Demo users are provisioned by the demo-data service from:");
                System.out.println("    " + resolveTemplate(demoData.ssoUsers, env));
            }
        } else {
            System.out.println("  No users are provisioned yet: log into the Keycloak admin console and create");
            System.out.println("  a user for each activated client.");
        }
        System.out.println();
    }

    private void collectCredentials(Model.Entry entry, String displayName, Map<String, String> env,
            List<String[]> credentials) {
        if (entry == null || entry.credentials.isEmpty()) {
            return;
        }
        credentials.add(new String[] { displayName, credentialValue(entry, env) });
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

    private static String pad(String value) {
        while (value.length() < 10) {
            value = value + " ";
        }
        return value;
    }
}
