package com.ozonehis.ozonectl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvGenTest {

    private File composeRoot;
    private File distroDir;

    @BeforeEach
    void setUp() throws URISyntaxException {
        composeRoot = new File(getClass().getResource("/fixture/compose").toURI());
        distroDir = new File(getClass().getResource("/fixture/distro").toURI());
    }

    private Map<String, String> generate(boolean ssoEnabled) throws IOException {
        Map<String, String> runtime = new LinkedHashMap<String, String>();
        runtime.put("HOST_NAME", "demo.example.org");
        runtime.put("SERVER_SCHEME", "https");
        runtime.put("ENABLE_SSO", Boolean.toString(ssoEnabled));
        runtime.put("DB_INIT_LIST", "alpha beta");
        Model model = Model.load(new File(composeRoot, "configs/model.yml"));
        return new EnvGen(ssoEnabled).generate(
                new File(composeRoot, "configs/vars.yml"),
                new File(distroDir, "vars.yml"),
                model.deployment,
                runtime);
    }

    @Test
    void keepsVarReferencesRawForDeploymentTimeOverrides() throws IOException {
        // The .env keeps ${HOST_NAME} references raw: compose resolves them
        // at runtime, so a deployment can override HOST_NAME without
        // regenerating.
        Map<String, String> env = generate(true);
        assertEquals("${HOST_NAME}:8080", env.get("ALPHA_HOSTNAME"));
        assertEquals("demo.example.org", env.get("HOST_NAME")); // the runtime value
    }

    @Test
    void substituteResolvesChainedReferencesForDisplay() throws IOException {
        Map<String, String> resolved = EnvGen.substitute(generate(true));
        assertEquals("demo.example.org:8080", resolved.get("ALPHA_HOSTNAME"));
    }

    @Test
    void mergesCodeDistroAndDeploymentVarsWithTheRightPrecedence() throws IOException {
        Map<String, String> env = generate(true);
        assertEquals("alphauser", env.get("ALPHA_USER"));   // code vars
        assertEquals("distro-pass", env.get("ALPHA_PASSWORD")); // distro overrides code
        assertEquals("alphadb", env.get("ALPHA_DB"));
        assertEquals("UTC", env.get("TIMEZONE"));            // deployment extra
    }

    @Test
    void mergesSsoBlocksOnlyWhenSsoIsEnabled() throws IOException {
        assertTrue(generate(true).containsKey("ALPHA_CLIENT_SECRET"));
        assertFalse(generate(false).containsKey("ALPHA_CLIENT_SECRET"));
    }

    @Test
    void runtimeRootsAreWrittenFirstSoReferencesResolveInFileOrder() throws IOException {
        // Compose interpolates env-file values in file order, single pass:
        // HOST_NAME must precede the values that reference it.
        List<String> lines = EnvGen.toFileLines(generate(true));
        int hostNameIndex = lines.indexOf("HOST_NAME=demo.example.org");
        int alphaHostnameIndex = lines.indexOf("ALPHA_HOSTNAME=${HOST_NAME}:8080");
        assertTrue(hostNameIndex >= 0 && alphaHostnameIndex >= 0, lines.toString());
        assertTrue(hostNameIndex < alphaHostnameIndex, lines.toString());
    }

    @Test
    void runtimeValuesWin() throws IOException {
        assertEquals("alpha beta", generate(true).get("DB_INIT_LIST"));
        assertEquals("true", generate(true).get("ENABLE_SSO"));
    }

    @Test
    void doesNotFlattenNamedBlocksCollidingWithSubBlockNames() throws IOException {
        // The section child named 'sso' (modes.sso) is the sso mode block,
        // not a section-level sso sub-block: its own keys must never leak
        // into the .env as flattened 'configs'/'paths' values.
        Map<String, String> env = generate(true);
        assertFalse(env.containsKey("configs"));
        assertFalse(env.containsKey("paths"));
        assertEquals("kcadmin", env.get("KEYCLOAK_USER")); // merged as a named block
    }

    @Test
    void distroRelativePathsStayAsDistroPathReferences() throws IOException {
        // The .env must be portable: paths are emitted as ${DISTRO_PATH}/...
        // references (DISTRO_PATH is a runtime root at the top of the file),
        // so a deployment overrides one line and everything follows.
        Map<String, String> env = generate(true);
        assertEquals("${DISTRO_PATH}/data", env.get("SQL_SCRIPTS_PATH"));
        assertEquals("${DISTRO_PATH}/data", env.get("ALPHA_DATA_PATH"));
        // The invocation provides DISTRO_PATH (command prefix / runner export):
        Map<String, String> provided = new LinkedHashMap<String, String>(env);
        provided.put("DISTRO_PATH", distroDir.getAbsolutePath());
        assertEquals(new File(distroDir, "data").getPath(), EnvGen.substitute(provided).get("SQL_SCRIPTS_PATH"));
    }
}
