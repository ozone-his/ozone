import java.nio.file.Paths
import java.io.File
import java.text.SimpleDateFormat
import groovy.text.* 
import groovy.json.JsonBuilder

myver  = "${project.groupId}-${project.artifactId}-${project.version}"

def dockerComposeTemplate = Paths.get("${project.build.directory}", "/embedded-build-tmp", "docker-embedded", "docker-compose-embedded.yaml.template").toFile()
def binding = ['dockertag' : myver ]			  
def engine = new SimpleTemplateEngine() 
def template = engine.createTemplate(dockerComposeTemplate) 
def writable = template.make(binding) 

def dockerComposePath = Paths.get("${project.build.directory}", "/embedded-build-tmp", "docker-embedded", "docker-compose-embedded.yaml").toAbsolutePath().toString()
def myFile = new File(dockerComposePath)
myFile.write(writable.toString())