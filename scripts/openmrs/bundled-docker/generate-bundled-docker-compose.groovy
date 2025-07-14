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

def engine = new SimpleTemplateEngine()
// Returns the override file if it exists, else the default template
File resolveTemplateFile(String overrideFileName, File defaultTemplateFile) {
    def overrideFile = new File("${project.basedir}/scripts", overrideFileName)
    if (overrideFile.exists()) {
        def targetOverrideFile = Paths.get("${project.build.directory}", "/bundled-docker-build-tmp", "bundled-docker", overrideFileName).toFile()
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

def overrideFileName = getOverrideFileName()

def dockerComposeTemplate = resolveTemplateFile(overrideFileName,
        Paths.get("${project.build.directory}", "/bundled-docker-build-tmp", "bundled-docker", "docker-compose-bundled.yml.template").toFile()
)

if (!dockerComposeTemplate.exists()) {
    throw new FileNotFoundException("Docker compose template file not found: ${dockerComposeTemplate.absolutePath}")
}

// Bind the template with the required variables
def binding = ['dockertag' : dockerTag, 'sanitizedGroupId' : sanitizedGroupId, 'sanitizedArtifactId' : sanitizedArtifactId, 'dockerUserName' : dockerUserName]
def template = engine.createTemplate(dockerComposeTemplate) 
def writable = template.make(binding)

def dockerComposeOutputFileName = getOutputFileName()
def dockerComposePath = Paths.get("${project.build.directory}", "/bundled-docker-build-tmp", "bundled-docker", dockerComposeOutputFileName).toAbsolutePath().toString()
def dockerComposeFile = new File(dockerComposePath)
dockerComposeFile.write(writable.toString())

// Bind the SSO template
def ssoDockerComposeTemplate = Paths.get("${project.build.directory}", "/bundled-docker-build-tmp", "bundled-docker", "docker-compose-bundled-sso.yml.template").toFile()
def ssoBinding = ['dockertag' : dockerTag, 'sanitizedGroupId' : sanitizedGroupId, 'sanitizedArtifactId' : sanitizedArtifactId, 'dockerUserName' : dockerUserName]
def ssoTemplate = engine.createTemplate(ssoDockerComposeTemplate)
def ssoWritable = ssoTemplate.make(ssoBinding)

def ssoDockerComposePath = Paths.get("${project.build.directory}", "/bundled-docker-build-tmp", "bundled-docker", "docker-compose-bundled-sso.yml").toAbsolutePath().toString()
def dockerComposeSsoFile = new File(ssoDockerComposePath)
dockerComposeSsoFile.write(ssoWritable.toString())
