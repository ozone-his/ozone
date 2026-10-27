package com.ozonehis.cli;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Resolves model-declared compose files to their file system location. The
 * declaring model decides the root — never the state of the file system:
 * files declared by the base model resolve against the compose root, files
 * declared by a distro's model-overrides.yml against the distro dir.
 */
final class ComposePaths {

    private ComposePaths() {
    }

    static File resolve(File composeRoot, File distroDir, String path,
            Set<String> composeRootFiles, Set<String> distroDirFiles) throws IOException {
        File absolute = new File(path);
        if (absolute.isAbsolute()) {
            return absolute;
        }
        if (distroDirFiles.contains(path)) {
            return new File(distroDir, path);
        }
        if (composeRootFiles.contains(path)) {
            return new File(composeRoot, path);
        }
        throw new IOException("Model error: compose file is not declared by any model: " + path);
    }
}
