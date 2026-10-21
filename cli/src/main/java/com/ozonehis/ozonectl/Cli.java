package com.ozonehis.ozonectl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ozonectl — assembles the Docker Compose command of an Ozone distribution
 * from the compose project's model and the distro's selection. Prints the
 * commands; never runs them. Generates the manifest and the .env files.
 */
public final class Cli {

    private static final String USAGE = "Usage: ozonectl --compose-root <dir> --distro <dir> [--hostname <h>]\n"
            + "                    [--scheme <http|https>] [--env-file <path>] [--manifest <path>]\n"
            + "                    [--no-credentials] <run | up [mode] | down | restore | validate | manifest>\n"
            + "\n"
            + "  --compose-root  required: the Docker Compose project location.\n"
            + "  --distro        required: the distro location. Defaults to the current\n"
            + "                  directory, as ozonectl runs from the distro.\n"
            + "  --hostname      required for run/up/restore/down: the deployment's hostname,\n"
            + "                  or the machine's IP for local runs (localhost breaks SSO,\n"
            + "                  e.g. on macOS). Not required for validate/manifest.\n\n"
            + "  run            print the 'docker compose ... up -d' command for the selection\n"
            + "  up [mode]      same as run; with a mode name, print the ad-hoc chain of that mode\n"
            + "                (e.g. 'up backup', 'up demo' on a running stack)\n"
            + "  down           print the 'docker compose ... down' command\n"
            + "  restore        print the pre-computed restore command\n"
            + "  validate       run all validation rules, print nothing on success\n"
            + "  manifest       generate manifest.yml and .env, print the summary\n";

    private File composeRoot;
    private File distroDir = new File(".");
    private String hostname;
    private String scheme = "https";
    private File envFile;
    private File manifestFile;
    private boolean printCredentials = true;
    private String command;
    private String commandArg;

    public static void main(String[] args) {
        try {
            new Cli().run(args);
        } catch (Exception e) {
            System.err.println("ozonectl: " + e.getMessage());
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        parse(args);
        if (command == null || composeRoot == null) {
            throw new IOException("Missing command or --compose-root.\n\n" + USAGE);
        }
        distroDir = distroDir.getCanonicalFile();
        composeRoot = composeRoot.getCanonicalFile();
        if (envFile == null) {
            envFile = new File(distroDir, ".env");
        }
        if (manifestFile == null) {
            manifestFile = new File(distroDir, "manifest.yml");
        }

        if (!new File(distroDir, "selection.yml").isFile()) {
            throw new IOException("No selection.yml in the distro directory: " + distroDir
                    + ". Pass --distro <dir> or run ozonectl from the distro.");
        }
        Model model = Model.load(new File(composeRoot, "configs/model.yml"));
        Selection selection = Selection.load(new File(distroDir, "selection.yml"));
        Resolver resolver = new Resolver(model, selection, composeRoot, distroDir);
        Plan plan = resolver.resolve();
        Display display = new Display(printCredentials);

        if ("validate".equals(command)) {
            display.validated(plan.chain.size());
            return;
        }

        // HOST_NAME and DISTRO_PATH are provided by the invocation only —
        // never stored in the portable .env. Refuse the commands that
        // actually start the stack unless both are known.
        if (hostname == null && ("run".equals(command) || "up".equals(command)
                || "restore".equals(command) || "down".equals(command))) {
            throw new IOException("--hostname is required for '" + command + "'. Use the deployment's\n"
                    + "hostname, or your machine's IP for local runs ; 'localhost' does not work for\n"
                    + "SSO in some environments (e.g. macOS).");
        }

        // The project name is needed only by the commands that print a
        // compose command; 'manifest' must work without a pom.xml in the
        // distro dir.
        Map<String, String> env = buildEnv(model, selection, plan);
        // The .env keeps ${VAR} references raw so that invocation-provided
        // roots follow through every derived value; the resolved copy is for
        // display only. The roots are injected into the display copy (never
        // the .env) so ${HOST_NAME}/${DISTRO_PATH} templates resolve for
        // printing.
        Map<String, String> displayEnv = new LinkedHashMap<String, String>(env);
        if (hostname != null) {
            displayEnv.put("HOST_NAME", hostname);
        }
        displayEnv.put("DISTRO_PATH", distroDir.getCanonicalPath());
        displayEnv = EnvGen.substitute(displayEnv);
        writeEnv(env);
        writeManifest(model, plan);

        if ("manifest".equals(command)) {
            display.artifactsWritten(manifestFile.getCanonicalPath(), envFile.getCanonicalPath());
            return;
        }

        // The printed command is ephemeral and necessarily machine-specific
        // (it points at this machine's compose tree); generated artifacts
        // (.env, manifest.yml) must stay portable, so no paths are baked
        // into them.
        if ("run".equals(command) || "up".equals(command)) {
            String projectName = artifactId(distroDir);
            List<String> chain = plan.chain;
            if (commandArg != null) {
                chain = plan.modeChains.get(commandArg);
                if (chain == null) {
                    throw new IOException("Unknown mode '" + commandArg + "'. Valid modes: " + plan.modeChains.keySet());
                }
            }
            display.command(buildCommand(projectName, chain, new ArrayList<String>(), "up -d"));
            display.summary(model, plan, displayEnv);
            return;
        }

        if ("down".equals(command)) {
            display.command(buildCommand(artifactId(distroDir), plan.chain, new ArrayList<String>(), "down"));
            return;
        }

        if ("restore".equals(command)) {
            List<String> chain = new ArrayList<String>(plan.chain);
            chain.addAll(plan.restoreCompose);
            display.command(buildCommand(artifactId(distroDir), chain, plan.restoreProfiles, "up -d"));
            return;
        }

        throw new IOException("Unknown command '" + command + "'.\n\n" + USAGE);
    }

    private Map<String, String> buildEnv(Model model, Selection selection, Plan plan) throws IOException {
        StringBuilder dbInitList = new StringBuilder();
        for (String dir : plan.dbInit) {
            if (dbInitList.length() > 0) {
                dbInitList.append(' ');
            }
            dbInitList.append(dir);
        }
        // HOST_NAME and DISTRO_PATH are NOT written to the .env: the .env is
        // a committed, portable artifact and both are machine/deployment
        // specific. They are provided by the invocation instead — the
        // printed command carries them as prefix assignments (dev) and the
        // runner exports them (deployment) — exactly like every ${HOST_NAME}
        // and ${DISTRO_PATH} reference expects.
        Map<String, String> runtime = new LinkedHashMap<String, String>();
        runtime.put("SERVER_SCHEME", scheme);
        runtime.put("ENABLE_SSO", Boolean.toString(plan.ssoEnabled()));
        runtime.put("DB_INIT_LIST", dbInitList.toString());

        EnvGen envGen = new EnvGen(plan.ssoEnabled());
        return envGen.generate(new File(composeRoot, "configs/vars.yml"),
                new File(distroDir, "vars.yml"),
                model.deployment,
                runtime);
    }

    private void parse(String[] args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--compose-root".equals(arg)) {
                composeRoot = new File(optionValue(args, ++i, arg));
            } else if ("--distro".equals(arg)) {
                distroDir = new File(optionValue(args, ++i, arg));
            } else if ("--hostname".equals(arg)) {
                hostname = optionValue(args, ++i, arg);
            } else if ("--scheme".equals(arg)) {
                scheme = optionValue(args, ++i, arg);
                if (!"http".equals(scheme) && !"https".equals(scheme)) {
                    throw new IOException("Invalid --scheme '" + scheme + "': expected 'http' or 'https'.");
                }
            } else if ("--env-file".equals(arg)) {
                envFile = new File(optionValue(args, ++i, arg));
            } else if ("--manifest".equals(arg)) {
                manifestFile = new File(optionValue(args, ++i, arg));
            } else if ("--no-credentials".equals(arg)) {
                printCredentials = false;
            } else if (!arg.startsWith("--")) {
                if (command == null) {
                    command = arg;
                } else if (commandArg == null) {
                    commandArg = arg;
                } else {
                    throw new IOException("Unexpected argument: " + arg);
                }
            } else {
                throw new IOException("Unknown option: " + arg + "\n\n" + USAGE);
            }
        }
    }

    private static String optionValue(String[] args, int index, String option) throws IOException {
        if (index >= args.length) {
            throw new IOException("Missing value for " + option + ".\n\n" + USAGE);
        }
        return args[index];
    }

    /** Builds the full compose command as a string; Display renders it. */
    private String buildCommand(String projectName, List<String> chain, List<String> profiles, String action)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        // The distro location and the hostname are machine/deployment
        // specific and must not be stored in the portable .env: the command
        // provides them as prefix assignments (the runner exports them at
        // deployment).
        sb.append("DISTRO_PATH=").append(shellQuote(distroDir.getCanonicalPath()));
        sb.append(" HOST_NAME=").append(shellQuote(hostname));
        sb.append(" docker compose");
        sb.append(" --project-directory ").append(shellQuote(composeRoot.getCanonicalPath()));
        sb.append(" -p ").append(shellQuote(projectName));
        sb.append(" --env-file ").append(shellQuote(envFile.getCanonicalPath()));
        for (String file : chain) {
            // Absolute paths: '-f' arguments resolve against the caller's
            // working directory, not --project-directory.
            sb.append(" -f ").append(shellQuote(new File(composeRoot, file).getPath()));
        }
        for (String profile : profiles) {
            sb.append(" --profile ").append(shellQuote(profile));
        }
        sb.append(' ').append(action);
        return sb.toString();
    }

    /**
     * Quotes a value for the printed, copy-paste shell command — only when
     * needed, so the common output stays bare.
     */
    private static String shellQuote(String value) {
        if (value.matches("[A-Za-z0-9._/:=+@%,-]+")) {
            return value;
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private void writeEnv(Map<String, String> env) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("# Generated by ozonectl. This file is portable: it carries no machine");
        lines.add("# or deployment specific values. HOST_NAME and DISTRO_PATH are NOT stored");
        lines.add("# here: they are provided by the invocation (the printed command prefixes");
        lines.add("# them; the runner exports them at deployment). Derived values referencing");
        lines.add("# ${HOST_NAME} and ${DISTRO_PATH} follow automatically.");
        lines.addAll(EnvGen.toFileLines(env));
        Files.write(envFile.toPath(), lines, StandardCharsets.UTF_8);
    }

    private void writeManifest(Model model, Plan plan) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("# Generated by ozonectl — do not edit. Regenerate with: ozonectl manifest");
        lines.add("apps: [" + join(plan.apps) + "]");
        lines.add("integrations: [" + join(plan.integrations) + "]");
        lines.add("modes: {" + joinModes(plan) + "}");
        lines.add("db-init: [" + join(plan.dbInit) + "]");
        lines.add("");
        lines.add("# The ordered -f chain for 'run'");
        lines.add("compose-files:");
        for (String file : plan.chain) {
            lines.add("  - " + file);
        }
        lines.add("");
        lines.add("# The pre-computed restore invocation: 'run' chain + these files,");
        lines.add("# with these restore gate profiles active");
        lines.add("restore:");
        lines.add("  compose-files:");
        for (String file : plan.restoreCompose) {
            lines.add("    - " + file);
        }
        lines.add("  profiles: [" + join(plan.restoreProfiles) + "]");
        lines.add("");
        lines.add("# Ad-hoc chains: 'up <mode>' runs the base + the mode's files");
        lines.add("mode-chains:");
        for (Map.Entry<String, List<String>> modeChain : plan.modeChains.entrySet()) {
            lines.add("  " + modeChain.getKey() + ": [" + join(modeChain.getValue()) + "]");
        }
        Files.write(manifestFile.toPath(), lines, StandardCharsets.UTF_8);
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item);
        }
        return sb.toString();
    }

    private static String joinModes(Plan plan) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> mode : plan.modes.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(mode.getKey()).append(": ").append(mode.getValue());
        }
        return sb.toString();
    }

    private static String artifactId(File distroDir) throws Exception {
        File pom = new File(distroDir, "pom.xml");
        if (!pom.isFile()) {
            throw new IOException("No pom.xml found in the distro dir; the project name is taken from the distro's artifactId");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(pom);
        NodeList children = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && "artifactId".equals(node.getNodeName())) {
                return node.getTextContent().trim();
            }
        }
        throw new IOException("No artifactId found in " + pom);
    }
}
