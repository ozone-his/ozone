import java.nio.file.Paths
import java.io.File
import java.text.SimpleDateFormat
import groovy.text.* 
import groovy.json.JsonBuilder

def sanitizeName(name) {
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


myver  = project.version
mydockerUserName = getPropertyValue('docker.push.registry.username')

if (project.version.endsWith("-SNAPSHOT")) {
    myver = "latest"
}

def dockerComposeTemplate = Paths.get("${project.build.directory}", "/bundled-docker-build-tmp", "bundled-docker", "docker-compose-bundled.yml.template").toFile()
def binding = ['dockertag' : myver, 'sanitizedGroupId' : sanitizedGroupId, 'sanitizedArtifactId' : sanitizedArtifactId, 'dockerUserName' : mydockerUserName]			  
def engine = new SimpleTemplateEngine() 
def template = engine.createTemplate(dockerComposeTemplate) 
def writable = template.make(binding) 

def dockerComposePath = Paths.get("${project.build.directory}", "/bundled-docker-build-tmp", "bundled-docker", "docker-compose-bundled.yml").toAbsolutePath().toString()
def myFile = new File(dockerComposePath)
myFile.write(writable.toString())