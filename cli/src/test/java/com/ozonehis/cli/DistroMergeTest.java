package com.ozonehis.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistroMergeTest {

    @TempDir
    File tempDir;

    private File write(String path, String content) throws IOException {
        File file = new File(tempDir, path);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private Map<String, Object> loadResolved(File distroDir, String name) throws IOException {
        return Yamls.load(new File(distroDir, "meta/resolved/" + name));
    }

    @Test
    void mergesParentResolvedWithOwnSources() throws IOException {
        write("parent/meta/resolved/vars.yml",
                "apps:\n  alpha:\n    configs:\n      ALPHA_PASSWORD: parent-pass\n      ALPHA_DB: alphadb\n");
        write("child/meta/vars.yml",
                "apps:\n  alpha:\n    configs:\n      ALPHA_PASSWORD: child-pass\n");

        DistroMerge.merge(new File(tempDir, "parent"), new File(tempDir, "child"));

        Map<String, Object> vars = loadResolved(new File(tempDir, "child"), "vars.yml");
        Map<String, Object> alpha = Yamls.map(Yamls.map(Yamls.map(vars.get("apps")).get("alpha")).get("configs"));
        assertEquals("child-pass", alpha.get("ALPHA_PASSWORD")); // own wins
        assertEquals("alphadb", alpha.get("ALPHA_DB"));           // parent value inherited
    }

    @Test
    void inheritsParentFilesWhenNoOwnSources() throws IOException {
        write("parent/meta/resolved/vars.yml",
                "apps:\n  alpha:\n    configs:\n      ALPHA_PASSWORD: parent-pass\n");

        DistroMerge.merge(new File(tempDir, "parent"), new File(tempDir, "child"));

        Map<String, Object> vars = loadResolved(new File(tempDir, "child"), "vars.yml");
        assertEquals("parent-pass",
                Yamls.map(Yamls.map(Yamls.map(vars.get("apps")).get("alpha")).get("configs")).get("ALPHA_PASSWORD"));
    }

    @Test
    void publishesOwnSourcesWhenNoParent() throws IOException {
        write("root/meta/vars.yml",
                "apps:\n  alpha:\n    configs:\n      ALPHA_PASSWORD: root-pass\n");

        DistroMerge.merge(null, new File(tempDir, "root"));

        Map<String, Object> vars = loadResolved(new File(tempDir, "root"), "vars.yml");
        assertEquals("root-pass",
                Yamls.map(Yamls.map(Yamls.map(vars.get("apps")).get("alpha")).get("configs")).get("ALPHA_PASSWORD"));
    }

    @Test
    void modelOverlaysAppendListsAndOverrideScalars() throws IOException {
        write("parent/meta/resolved/model-overrides.yml",
                "apps:\n  alpha:\n    compose: [c2c/base.yml]\n    url: parent://host\n");
        write("child/meta/model-overrides.yml",
                "apps:\n  alpha:\n    compose: [c2c/tuning.yml]\n    url: child://host\n");

        DistroMerge.merge(new File(tempDir, "parent"), new File(tempDir, "child"));

        Map<String, Object> overlay = loadResolved(new File(tempDir, "child"), "model-overrides.yml");
        Map<String, Object> alpha = Yamls.map(Yamls.map(overlay.get("apps")).get("alpha"));
        assertEquals(java.util.Arrays.asList("c2c/base.yml", "c2c/tuning.yml"), alpha.get("compose"));
        assertEquals("child://host", alpha.get("url"));
    }

    @Test
    void errorsWhenParentIsNotABuiltArtifact() throws IOException {
        write("parent/meta/vars.yml", "apps: {}\n"); // source, no resolved/
        IOException e = assertThrows(IOException.class,
                () -> DistroMerge.merge(new File(tempDir, "parent"), new File(tempDir, "child")));
        assertTrue(e.getMessage().contains("not a built distro artifact"), e.getMessage());
    }

    @Test
    void errorsWhenNothingToMerge() throws IOException {
        new File(tempDir, "empty/ozone").mkdirs();
        IOException e = assertThrows(IOException.class,
                () -> DistroMerge.merge(null, new File(tempDir, "empty")));
        assertTrue(e.getMessage().contains("Nothing to merge"), e.getMessage());
    }

    @Test
    void mergeIsDeterministic() throws IOException {
        write("parent/meta/resolved/vars.yml",
                "apps:\n  alpha:\n    configs:\n      ALPHA_PASSWORD: parent-pass\n");
        write("child/meta/vars.yml",
                "apps:\n  alpha:\n    configs:\n      ALPHA_PASSWORD: child-pass\n");

        DistroMerge.merge(new File(tempDir, "parent"), new File(tempDir, "child"));
        byte[] first = Files.readAllBytes(new File(tempDir, "child/meta/resolved/vars.yml").toPath());
        DistroMerge.merge(new File(tempDir, "parent"), new File(tempDir, "child"));
        byte[] second = Files.readAllBytes(new File(tempDir, "child/meta/resolved/vars.yml").toPath());
        assertTrue(new String(first, StandardCharsets.UTF_8).equals(new String(second, StandardCharsets.UTF_8)));
    }

    @Test
    void mergedModelOverlaysEqualSequentialApplication() throws IOException {
        write("parent/meta/resolved/model-overrides.yml",
                "apps:\n  alpha:\n    compose: [c2c/base.yml]\n    db: [mysql]\n    url: parent://host\n");
        write("child/meta/model-overrides.yml",
                "apps:\n  alpha:\n    compose: [c2c/tuning.yml]\n    db: [redis]\n    url: child://host\n"
                        + "  c2c-connector:\n    compose: [c2c/connector.yml]\n");

        DistroMerge.merge(new File(tempDir, "parent"), new File(tempDir, "child"));

        // Deployment applies the merged file in one shot; it must be
        // equivalent to applying the parent's and the own overlays in order.
        Model viaMergedFile = new Model();
        viaMergedFile.mergeOverlay(Yamls.load(new File(tempDir, "child/meta/resolved/model-overrides.yml")));

        Model viaSequentialApplication = new Model();
        viaSequentialApplication.mergeOverlay(Yamls.load(new File(tempDir, "parent/meta/resolved/model-overrides.yml")));
        viaSequentialApplication.mergeOverlay(Yamls.load(new File(tempDir, "child/meta/model-overrides.yml")));

        Model.Entry mergedAlpha = viaMergedFile.apps.get("alpha");
        Model.Entry sequentialAlpha = viaSequentialApplication.apps.get("alpha");
        assertEquals(sequentialAlpha.compose, mergedAlpha.compose);
        assertEquals(sequentialAlpha.db, mergedAlpha.db);
        assertEquals(sequentialAlpha.url, mergedAlpha.url);
        assertEquals(viaSequentialApplication.apps.get("c2c-connector").compose,
                viaMergedFile.apps.get("c2c-connector").compose);
    }

    @Test
    void yamlMergeRejectsTypeConflicts() throws IOException {
        Map<String, Object> base = Yamls.load(write("base.yml", "apps:\n  alpha:\n    configs:\n      A: x\n"));
        Map<String, Object> override = Yamls.load(write("override.yml", "apps:\n  alpha: scalar\n"));
        IOException e = assertThrows(IOException.class,
                () -> YamlMerge.merge(base, override, "model-overrides.yml"));
        assertTrue(e.getMessage().contains("cannot merge"), e.getMessage());
    }

    @Test
    void recordsTheDistroIdentityWhenProvided() throws IOException {
        write("root/meta/vars.yml", "apps:\n  alpha:\n    configs:\n      A: x\n");
        Map<String, String> info = new java.util.LinkedHashMap<String, String>();
        info.put("name", "test-distro");
        info.put("version", "1.2.3");
        info.put("description", "A test distro");

        DistroMerge.merge(null, new File(tempDir, "root"), info);

        Map<String, Object> recorded = Yamls.load(new File(tempDir, "root/meta/resolved/info.yml"));
        assertEquals("test-distro", recorded.get("name"));
        assertEquals("1.2.3", recorded.get("version"));
        assertEquals("A test distro", recorded.get("description"));
    }

    @Test
    void writesNoInfoFileWithoutIdentity() throws IOException {
        write("root/meta/vars.yml", "apps:\n  alpha:\n    configs:\n      A: x\n");
        DistroMerge.merge(null, new File(tempDir, "root"));
        assertTrue(!new File(tempDir, "root/meta/resolved/info.yml").isFile());
    }

    @Test
    void identityAloneIsSomethingToMerge() throws IOException {
        new File(tempDir, "empty/ozone").mkdirs();
        Map<String, String> info = new java.util.LinkedHashMap<String, String>();
        info.put("name", "identity-only");
        DistroMerge.merge(null, new File(tempDir, "empty"), info);
        Map<String, Object> recorded = Yamls.load(new File(tempDir, "empty/meta/resolved/info.yml"));
        assertEquals("identity-only", recorded.get("name"));
    }
}
