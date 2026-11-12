package com.ozonehis.cli;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
 * The Ozone CLI (command 'ozone') - assembles the Docker Compose command of an Ozone distribution
 * from the compose project's model and the distro's selection. Prints the
 * commands; never runs them. Generates the manifest and the .env files.
 * The 'resolve' command is the distro build's inheritance step: it merges
 * the parent distro's resolved chain with this distro's own meta/ sources
 * and records the distro's identity as meta/resolved/info.yml.
 */
public final class OzoneCli {

    private static final String USAGE = "Usage: ozone resolve [--from <parent-distro-dir>] --to <distro-dir>\n"
            + "                    [--project-name <n>] [--project-version <v>] [--project-description <d>]\n"
            + "       ozone --compose-root <dir> --distro <dir> [--hostname <h>]\n"
            + "                    [--scheme <http|https>] [--env-file <path>] [--manifest <path>]\n"
            + "                    [--no-credentials] <run | up [mode] | down | restore | validate | manifest>\n"
            + "\n"
            + "  --compose-root  required (except for resolve): the Docker Compose project location.\n"
            + "  --distro        required (except for resolve): the distro location.\n"
            + "  --hostname      required for run/up/restore/down: the deployment's hostname,\n"
            + "                  or the machine's IP for local runs (localhost breaks SSO,\n"
            + "                  e.g. on macOS). Not required for validate/manifest.\n\n"
            + "  resolve        build step: resolve the parent distro's resolved files with\n"
            + "                 this distro's own meta/ sources into meta/resolved/,\n"
            + "                 and record the --project-* identity as resolved/info.yml\n"
            + "  run            print the 'docker compose ... up -d' command for the selection\n"
            + "  up [mode]      same as run; with a mode name, print the ad-hoc chain of that mode\n"
            + "                (e.g. 'up backup', 'up demo' on a running stack)\n"
            + "  down           print the 'docker compose ... down' command\n"
            + "  restore        print the pre-computed restore command\n"
            + "  validate       run all validation rules, print nothing on success\n"
            + "  manifest       generate manifest.yml and .env, print the summary\n";

    private static final String EXAMPLES = "\n"
            + "Examples:\n"
            + "  # build time - resolve this distro's meta/ sources against its parent\n"
            + "  # (run by the Maven build; run by hand only to regenerate):\n"
            + "  ozone resolve --from target/ozone/distro --to target/my-distro-1.0.0-SNAPSHOT/distro \\\n"
            + "                --project-name my-distro --project-version 1.0.0-SNAPSHOT \\\n"
            + "                --project-description 'My distro'\n"
            + "\n"
            + "  # deployment - from an unpacked Ozone artifact:\n"
            + "  ozone --compose-root run/docker --distro distro --hostname demo.local run\n"
            + "  ozone --compose-root run/docker --distro distro --hostname demo.local up backup\n"
            + "  ozone --compose-root run/docker --distro distro --hostname demo.local down\n";

    private static final String HELP = USAGE + EXAMPLES;

    /** Appended to error messages instead of the full help text. */
    private static final String HINT = " Type 'ozone --help' for usage and examples.";

    private File composeRoot;
    private File distroDir = new File(".");
    private String hostname;
    private String scheme = "https";
    private File envFile;
    private File manifestFile;
    private boolean printCredentials = true;
    private String command;
    private String commandArg;
    private File fromDir;
    private File toDir;
    private String projectName;
    private String projectVersion;
    private String projectDescription;

    public static void main(String[] args) {
        try {
            // Always emit UTF-8: the display's glyphs and dashes render as
            // correct bytes regardless of the platform's default charset.
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            new OzoneCli().run(args);
        } catch (Exception e) {
            System.err.println("ozone: " + e.getMessage());
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                System.out.println(HELP);
                return;
            }
        }
        parse(args);
        if ("resolve".equals(command)) {
            if (commandArg != null) {
                throw new IOException("Unexpected argument: " + commandArg);
            }
            if (toDir == null) {
                throw new IOException("Missing --to." + HINT);
            }
            // The build-declared distro identity, recorded as resolved/info.yml
            Map<String, String> info = new LinkedHashMap<String, String>();
            if (projectName != null) {
                info.put("name", projectName);
            }
            if (projectVersion != null) {
                info.put("version", projectVersion);
            }
            if (projectDescription != null) {
                info.put("description", projectDescription);
            }
            List<File> resolvedFiles = DistroMerge.merge(
                    fromDir == null ? null : fromDir.getCanonicalFile(), toDir.getCanonicalFile(), info);
            new Display(printCredentials).resolvedWritten(resolvedFiles);
            return;
        }
        if (command == null || composeRoot == null) {
            throw new IOException("Missing command or --compose-root." + HINT);
        }
        distroDir = distroDir.getCanonicalFile();
        composeRoot = composeRoot.getCanonicalFile();
        // Everything the Ozone CLI reads and writes lives in the distro's
        // meta/ dir: the own sources, the build-generated resolved/ files
        // (the only vars and model source read at run time) and the generated
        // .env and manifest.yml.
        File metaDir = new File(distroDir, "meta");
        if (envFile == null) {
            envFile = new File(metaDir, ".env");
        }
        if (manifestFile == null) {
            manifestFile = new File(metaDir, "manifest.yml");
        }

        File selectionFile = new File(metaDir, "selection.yml");
        if (!selectionFile.isFile()) {
            throw new IOException("No meta/selection.yml in the distro directory: " + distroDir
                    + ". Pass --distro <dir> pointing at the distro.");
        }
        // resolved/ is generated by the distro build (or by hand:
        // ozone resolve) and is the only vars and model source read here;
        // the own sources at the meta/ root are never read at run time.
        File resolvedVars = new File(metaDir, "resolved/vars.yml");
        if (!resolvedVars.isFile()) {
            throw new IOException("No resolved vars file: " + resolvedVars
                    + ". Run the distro build, or: ozone resolve --to <distro-dir>");
        }
        File resolvedModel = new File(metaDir, "resolved/model-overrides.yml");
        // The build-declared distro identity (resolved/info.yml); the source
        // of the compose project name when the distro carries no pom.xml.
        File resolvedInfo = new File(metaDir, "resolved/info.yml");
        Map<String, Object> distroInfo = resolvedInfo.isFile()
                ? Yamls.load(resolvedInfo) : new LinkedHashMap<String, Object>();
        Model model = Model.load(new File(composeRoot, "configs/model.yml"));
        if (resolvedModel.isFile()) {
            // The distro's additive extension of the model: new entries and
            // appended files - never removals (see Model.mergeOverlay).
            model.mergeOverlay(Yamls.load(resolvedModel));
        }
        Selection selection = Selection.load(selectionFile);
        Resolver resolver = new Resolver(model, selection, composeRoot, distroDir);
        Plan plan = resolver.resolve();
        Display display = new Display(printCredentials);

        if ("validate".equals(command)) {
            display.validated(plan.chain.size());
            return;
        }

        // HOST_NAME and DISTRO_PATH are provided by the invocation only -
        // never stored in the portable .env. Refuse the commands that
        // actually start the stack unless both are known.
        if (hostname == null && ("run".equals(command) || "up".equals(command)
                || "restore".equals(command) || "down".equals(command))) {
            throw new IOException("--hostname is required for '" + command + "'. Use the deployment's\n"
                    + "hostname, or your machine's IP for local runs; 'localhost' does not work for\n"
                    + "SSO in some environments (e.g. macOS).");
        }

        Map<String, String> env = buildEnv(model, plan, resolvedVars);
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
        writeManifest(model, plan, distroInfo);

        if ("manifest".equals(command)) {
            display.artifactsWritten(manifestFile.getCanonicalPath(), envFile.getCanonicalPath());
            return;
        }

        // The printed command is ephemeral and necessarily machine-specific
        // (it points at this machine's compose tree); generated artifacts
        // (.env, manifest.yml) must stay portable, so no paths are baked
        // into them.
        if ("run".equals(command) || "up".equals(command)) {
            String name = composeProjectName(distroInfo);
            List<String> chain = plan.chain;
            if (commandArg != null) {
                chain = plan.modeChains.get(commandArg);
                if (chain == null) {
                    throw new IOException("Unknown mode '" + commandArg + "'. Valid modes: " + plan.modeChains.keySet());
                }
            }
            display.command(buildCommand(model, name, chain, new ArrayList<String>(), "up -d"));
            display.summary(model, plan, displayEnv);
            return;
        }

        if ("down".equals(command)) {
            display.command(buildCommand(model, composeProjectName(distroInfo), plan.chain, new ArrayList<String>(), "down"));
            return;
        }

        if ("restore".equals(command)) {
            List<String> chain = new ArrayList<String>(plan.chain);
            chain.addAll(plan.restoreCompose);
            display.command(buildCommand(model, composeProjectName(distroInfo), chain, plan.restoreProfiles, "up -d"));
            return;
        }

        throw new IOException("Unknown command '" + command + "'." + HINT);
    }

    /**
     * The compose project name: the build's declaration (resolved/info.yml)
     * wins; the distro's pom.xml is the source-checkout fallback.
     */
    private String composeProjectName(Map<String, Object> distroInfo) throws Exception {
        Object name = distroInfo.get("name");
        if (name != null && !String.valueOf(name).trim().isEmpty()) {
            return String.valueOf(name).trim();
        }
        return artifactId(distroDir);
    }

    private Map<String, String> buildEnv(Model model, Plan plan, File resolvedVars) throws IOException {
        StringBuilder dbInitList = new StringBuilder();
        for (String dir : plan.dbInit) {
            if (dbInitList.length() > 0) {
                dbInitList.append(' ');
            }
            dbInitList.append(dir);
        }
        // HOST_NAME and DISTRO_PATH are NOT written to the .env: the .env is
        // a committed, portable artifact and both are machine/deployment
        // specific. They are provided by the invocation instead - the
        // printed command carries them as prefix assignments (dev) and the
        // runner exports them (deployment) - exactly like every ${HOST_NAME}
        // and ${DISTRO_PATH} reference expects.
        Map<String, String> runtime = new LinkedHashMap<String, String>();
        runtime.put("SERVER_SCHEME", scheme);
        runtime.put("ENABLE_SSO", Boolean.toString(plan.ssoEnabled()));
        runtime.put("DB_INIT_LIST", dbInitList.toString());

        EnvGen envGen = new EnvGen(plan.ssoEnabled());
        return envGen.generate(new File(composeRoot, "configs/vars.yml"),
                resolvedVars,
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
            } else if ("--from".equals(arg)) {
                fromDir = new File(optionValue(args, ++i, arg));
            } else if ("--to".equals(arg)) {
                toDir = new File(optionValue(args, ++i, arg));
            } else if ("--project-name".equals(arg)) {
                projectName = optionValue(args, ++i, arg);
            } else if ("--project-version".equals(arg)) {
                projectVersion = optionValue(args, ++i, arg);
            } else if ("--project-description".equals(arg)) {
                projectDescription = optionValue(args, ++i, arg);
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
                throw new IOException("Unknown option: " + arg + "." + HINT);
            }
        }
    }

    private static String optionValue(String[] args, int index, String option) throws IOException {
        if (index >= args.length) {
            throw new IOException("Missing value for " + option + "." + HINT);
        }
        return args[index];
    }

    /**
     * Builds the full compose command as a string, one flag per line with
     * shell backslash continuations - copy-paste valid; Display renders it.
     */
    private String buildCommand(Model model, String name, List<String> chain, List<String> profiles, String action)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        // The distro location and the hostname are machine/deployment
        // specific and must not be stored in the portable .env: the command
        // provides them as prefix assignments (the runner exports them at
        // deployment).
        sb.append("DISTRO_PATH=").append(shellQuote(distroDir.getCanonicalPath()));

        sb.append(" \\\n HOST_NAME=").append(shellQuote(hostname));
        sb.append(" \\\n docker compose");
        sb.append(" \\\n  --project-directory ").append(shellQuote(composeRoot.getCanonicalPath()));
        sb.append(" \\\n  -p ").append(shellQuote(name));
        sb.append(" \\\n  --env-file ").append(shellQuote(envFile.getCanonicalPath()));
        for (String file : chain) {
            // Absolute paths: '-f' arguments resolve against the caller's
            // working directory, not --project-directory. The declaring
            // model decides the root - never the state of the file system.
            sb.append(" \\\n  -f ").append(shellQuote(ComposePaths.resolve(composeRoot, distroDir, file,
                    model.composeRootFiles, model.distroDirFiles).getPath()));
        }
        for (String profile : profiles) {
            sb.append(" \\\n  --profile ").append(shellQuote(profile));
        }
        sb.append(" \\\n  ").append(action);
        return sb.toString();
    }

    /**
     * Quotes a value for the printed, copy-paste shell command - only when
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
        lines.add("# Generated by the Ozone CLI. This file is portable: it carries no machine");
        lines.add("# or deployment specific values. HOST_NAME and DISTRO_PATH are NOT stored");
        lines.add("# here: they are provided by the invocation (the printed command prefixes");
        lines.add("# them; the runner exports them at deployment). Derived values referencing");
        lines.add("# ${HOST_NAME} and ${DISTRO_PATH} follow automatically.");
        lines.addAll(EnvGen.toFileLines(env));
        Files.write(envFile.toPath(), lines, StandardCharsets.UTF_8);
    }

    private void writeManifest(Model model, Plan plan, Map<String, Object> distroInfo) throws IOException {
        List<String> lines = new ArrayList<String>();
        lines.add("# Generated by the Ozone CLI - do not edit. Regenerate with: 'ozone manifest'");
        lines.add("# Compose files are relative to the compose root, except files declared by");
        lines.add("# the distro's model-overrides.yml, which are relative to the distro dir.");
        if (!distroInfo.isEmpty()) {
            // The build-declared distro identity, echoed here for consumers
            lines.add("info:");
            for (String line : Yamls.dump(distroInfo).split("\n")) {
                lines.add("  " + line);
            }
            lines.add("");
        }
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
            throw new IOException("No project name: none was declared at build time (ozone resolve"
                    + " --project-name <name>, recorded in resolved/info.yml) and no pom.xml is present"
                    + " in the distro dir");
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
