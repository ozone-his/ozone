import java.nio.file.Paths
import groovy.text.SimpleTemplateEngine

static def sanitizeName(name) {
    StringBuilder ret = new StringBuilder();
    int underscores = 0;
    boolean lastWasADot = false;
    for (char c : name.toCharArray()) {
        if (c == '_') {
            underscores++;
            // Only _ in a row are allowed
            if (underscores <= 2) {
                ret.append(c);
            }
            continue;
        }

        if (c == '.') {
            // Only one dot in a row is allowed
            if (!lastWasADot) {
                ret.append(c);
            }
            lastWasADot = true;
            continue;
        }

        underscores = 0;
        lastWasADot = false;
        if (Character.isLetter(c) || Character.isDigit(c) || c == '-') {
            ret.append(c);
        }
    }

    // All characters must be lowercase
    return ret.toString().toLowerCase();
}

String getPropertyValue(String name) {
    def value = session.userProperties[name]
    if (value != null) return value //property was defined from command line e.g.: -DpropertyName=value
    return project.properties[name]
} 
groupId = project.groupId
while (groupId.endsWith(".")) {
   groupId = groupId.substring(0, groupId.length() - 1);
}
int idx = groupId.lastIndexOf(".");
sanitizedGroupId = sanitizeName(groupId.substring(idx != -1 ? idx + 1 : 0))
sanitizedArtifactId = sanitizeName(project.artifactId)


dockerTag = project.version
if (project.version.endsWith("-SNAPSHOT")) {
    dockerTag = "latest"
}

dockerUserName = getPropertyValue('docker.push.registry.username')
if (dockerUserName == null || dockerUserName.trim().isEmpty()) {
    throw new IllegalArgumentException("Docker username must be provided via 'docker.push.registry.username' property.")
}
// Ensure the bundled docker build directory is provided
def ozoneBundledDockerBuildDirectory = getPropertyValue('ozone.bundled.docker.build.directory')
if (ozoneBundledDockerBuildDirectory == null || ozoneBundledDockerBuildDirectory.trim().isEmpty()) {
    throw new IllegalArgumentException("Ozone bundled docker build directory must be provided via 'ozone.bundled.docker.build.directory' property.")
}

def engine = new SimpleTemplateEngine()
// Returns the override file if it exists, else the default template
File resolveTemplateFile(String overrideFileName, File defaultTemplateFile) {
    def overrideFile = new File("${project.basedir}/scripts", overrideFileName)
    if (overrideFile.exists()) {
        def targetOverrideFile = Paths.get(ozoneBundledDockerBuildDirectory, "bundled-docker", overrideFileName).toFile()
        targetOverrideFile.getParentFile().mkdirs()
        overrideFile.withInputStream { input ->
            targetOverrideFile.withOutputStream { output ->
                output << input
            }
        }
        return targetOverrideFile
    }
    return defaultTemplateFile
}

// Get override filename from properties or use default if not specified
String getOverrideFileName() {
    def overrideFileName = getPropertyValue('bundled.docker.compose.override.filename')
    return (overrideFileName != null && !overrideFileName.trim().isEmpty()) ? overrideFileName : "docker-compose-bundled.yml.template"
}

// Get output filename from properties or use default if not specified
String getOutputFileName() {
    def outputFileName = getPropertyValue('bundled.docker.compose.output.filename')
    return (outputFileName != null && !outputFileName.trim().isEmpty()) ? outputFileName : "docker-compose-bundled.yml"
}

// Check if SSO is enabled from properties
boolean isSsoEnabled() {
    def ssoEnabled = getPropertyValue('bundled.docker.compose.sso.enabled')
    return (ssoEnabled != null && ssoEnabled.trim().toLowerCase() == 'true')
}

def overrideFileName = getOverrideFileName()
def dockerComposeOutputFileName = getOutputFileName()
def binding = ['dockertag' : dockerTag, 'sanitizedGroupId' : sanitizedGroupId, 'sanitizedArtifactId' : sanitizedArtifactId, 'dockerUserName' : dockerUserName]
def dockerComposePath = Paths.get(ozoneBundledDockerBuildDirectory, "bundled-docker", dockerComposeOutputFileName).toAbsolutePath().toString()

// Check if an override file is provided
def overrideFile = new File("${project.basedir}/scripts", overrideFileName)
if (overrideFile.exists()) {
    // If override file exists, use it regardless of SSO setting
    def targetOverrideFile = Paths.get(ozoneBundledDockerBuildDirectory, "bundled-docker", overrideFileName).toFile()
    targetOverrideFile.getParentFile().mkdirs()
    overrideFile.withInputStream { input ->
        targetOverrideFile.withOutputStream { output ->
            output << input
        }
    }

    // Bind the template with the required variables
    def template = engine.createTemplate(targetOverrideFile)
    def writable = template.make(binding)

    // Write to the output file
    def dockerComposeFile = new File(dockerComposePath)
    dockerComposeFile.write(writable.toString())

    // Also create the SSO file for backward compatibility
    def ssoDockerComposePath = Paths.get(ozoneBundledDockerBuildDirectory, "bundled-docker", "docker-compose-bundled-sso.yml").toAbsolutePath().toString()
    def dockerComposeSsoFile = new File(ssoDockerComposePath)
    dockerComposeSsoFile.write(writable.toString())
} else {
    // No override file, use the appropriate template based on SSO setting
    def templateFile
    if (isSsoEnabled()) {
        // Use SSO template
        templateFile = Paths.get(ozoneBundledDockerBuildDirectory, "bundled-docker", "docker-compose-bundled-sso.yml.template").toFile()
    } else {
        // Use regular template
        templateFile = Paths.get(ozoneBundledDockerBuildDirectory,"bundled-docker", "docker-compose-bundled.yml.template").toFile()
    }

    if (!templateFile.exists()) {
        throw new FileNotFoundException("Docker compose template file not found: ${templateFile.absolutePath}")
    }

    // Bind the template with the required variables
    def template = engine.createTemplate(templateFile)
    def writable = template.make(binding)

    // Write to the output file
    def dockerComposeFile = new File(dockerComposePath)
    dockerComposeFile.write(writable.toString())
}
