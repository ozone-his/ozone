package com.ozonehis.ozonectl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResolverTest {

    private File composeRoot;
    private File distroDir;
    private Model model;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        composeRoot = new File(getClass().getResource("/fixture/compose").toURI());
        distroDir = new File(getClass().getResource("/fixture/distro").toURI());
        model = Model.load(new File(composeRoot, "configs/model.yml"));
    }

    private Plan resolve(Selection selection) throws IOException {
        return new Resolver(model, selection, composeRoot, distroDir).resolve();
    }

    private Selection selection(String appsYaml, String modesYaml) throws IOException {
        String yaml = "apps:\n" + appsYaml + "\nmodes:\n" + modesYaml + "\n";
        File file = File.createTempFile("selection", ".yml");
        java.nio.file.Files.write(file.toPath(), yaml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        file.deleteOnExit();
        return Selection.load(file);
    }

    @Test
    void resolvesTheExpectedChainForASelection() throws IOException {
        Plan plan = resolve(selection("  - alpha\n  - beta", "  sso: true\n  backup: true"));

        assertEquals(Arrays.asList("alpha", "beta"), plan.apps);
        assertEquals(Arrays.asList("alpha-beta"), plan.integrations);
        assertEquals(Arrays.asList(
                "other/init.yml",
                "other/mysql.yml",
                "other/postgresql.yml",
                "apps/alpha.yml",
                "apps/beta.yml",
                "integrations/alpha-beta.yml",
                "sso/keycloak.yml",
                "support/backup.yml",
                "sso/alpha.yml"), plan.chain);
        assertEquals(Arrays.asList("alpha", "beta", "keycloak"), plan.dbInit);
        assertTrue(plan.ssoEnabled());
    }

    @Test
    void omitsDatabasesNoSelectedEntryRequires() throws IOException {
        Plan plan = resolve(selection("  - beta", "  backup: true"));
        assertFalse(plan.chain.contains("other/mysql.yml"));
        assertTrue(plan.chain.contains("other/postgresql.yml"));
        assertEquals(Arrays.asList(), plan.integrations); // alpha not selected: not activated
    }

    @Test
    void rejectsUnknownApps() {
        IOException e = assertThrows(IOException.class, () ->
                resolve(selection("  - alpha\n  - nope", "  backup: true")));
        assertTrue(e.getMessage().contains("unknown app 'nope'"), e.getMessage());
    }

    @Test
    void rejectsIntegrationsListedInSelection() {
        IOException e = assertThrows(IOException.class, () ->
                resolve(selection("  - alpha\n  - alpha-beta", "  backup: true")));
        assertTrue(e.getMessage().contains("is an integration"), e.getMessage());
    }

    @Test
    void rejectsIncompatiblePairs() {
        IOException e = assertThrows(IOException.class, () ->
                resolve(selection("  - alpha\n  - gamma", "  backup: true")));
        assertTrue(e.getMessage().contains("incompatible"), e.getMessage());
    }

    @Test
    void rejectsMissingDbInitDirs() {
        IOException e = assertThrows(IOException.class, () ->
                resolve(selection("  - gamma", "  backup: true")));
        assertTrue(e.getMessage().contains("db-init"), e.getMessage());
    }

    @Test
    void nonTrueModeValuesAreNotEnabled() throws IOException {
        // 'true0' is a typo, not a boolean: the mode must stay disabled.
        Plan plan = resolve(selection("  - alpha", "  backup: true0"));
        assertFalse(plan.chain.contains("support/backup.yml"), plan.chain.toString());
    }

    @Test
    void enabledModesContributeTheirSsoOverridesToTheRunChain() throws IOException {
        Plan plan = resolve(selection("  - alpha", "  sso: true\n  extra: true"));
        assertTrue(plan.chain.contains("support/extra.yml"), plan.chain.toString());
        assertTrue(plan.chain.contains("sso/extra-sso.yml"), plan.chain.toString());
    }

    @Test
    void computesTheRestorePlan() throws IOException {
        Plan plan = resolve(selection("  - alpha", "  sso: true"));
        assertEquals(Arrays.asList("support/restore.yml", "support/backup.yml", "sso/restore.yml"),
                plan.restoreCompose);
        assertEquals(Arrays.asList("keycloak-restore", "alpha-restore"), plan.restoreProfiles);
    }

    @Test
    void computesAdHocModeChains() throws IOException {
        Plan plan = resolve(selection("  - alpha", "  sso: true"));
        assertEquals(Arrays.asList("other/init.yml", "support/backup.yml"), plan.modeChains.get("backup"));
        assertEquals(Arrays.asList("other/init.yml", "other/postgresql.yml", "sso/keycloak.yml"),
                plan.modeChains.get("sso"));
        assertFalse(plan.modeChains.containsKey("restore"));
    }
}
