import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger
import java.util.zip.ZipInputStream

import groovy.util.XmlParser

// directory of the newly created project
projectDirectory = Paths.get(request.outputDirectory, request.artifactId);

// utility to run a shell command
def run(String command) {
    def process = command.execute(null, projectDirectory.toFile());
    process.consumeProcessOutput(System.out, System.err)
    process.waitFor()
    if (process.exitValue() != 0) {
        throw new RuntimeException("'$command' exited with code ${process.exitValue()}")
    }
}

// utility to unzip files
def unzip(InputStream stream, Path destination) {
    def destinationRootFile = destination.toFile()
    final zipInput = new ZipInputStream(stream)
    zipInput.withStream {
        def entry
        while (entry = zipInput.nextEntry) {
            if (!entry.isDirectory()) {
                final file = destination.resolve(entry.name).toFile()
                checkForZipSlip(destinationRootFile, file)
                new FileOutputStream(file).withStream {
                    it << zipInput
                }
            } else {
                final dir = destination.resolve(entry.name).toFile()
                checkForZipSlip(destinationRootFile, dir)
                dir.mkdirs()
            }
        }
    }
}

private static void checkForZipSlip(File destination, File dir) {
    if (!dir.canonicalPath.startsWith(destination.canonicalPath)) {
        throw new IllegalArgumentException("Attempt to unzip ($dir.canonicalPath) outside of destination ($destination.canonicalPath) rejected")
    }
}


// Download the Maven wrapper without directly using Maven to do so
// Less flexible, but beginner friendly!
def mavenWrapperVersions = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/maven-metadata.xml".toURL().text
def mavenWrapperXml = new XmlParser().parseText(mavenWrapperVersions)
def mavenWrapperVersion = mavenWrapperXml.versioning.release.text()

unzip(
    (InputStream) "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/${mavenWrapperVersion}/maven-wrapper-distribution-${mavenWrapperVersion}-bin.zip".toURL().newInputStream(),
    projectDirectory.resolve("scripts")
)

def wrapperDir = projectDirectory.resolve("scripts").resolve(".mvn").resolve("wrapper")
def wrapperDirFile = wrapperDir.toFile()
wrapperDirFile.mkdirs()

def wrapperJarUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/${mavenWrapperVersion}/maven-wrapper-${mavenWrapperVersion}.jar"
wrapperJarUrl.toURL().newInputStream().withStream { wrapperJar ->
    new FileOutputStream(wrapperDir.resolve("maven-wrapper.jar").toFile()).withStream {
        it << wrapperJar
    }
}

Logger log = Logger.getAnonymousLogger()
// determine the Maven version
// We default to the most recent non-alpha, non-snapshot Maven version
def mavenDistributionVersions = "https://repo.maven.apache.org/maven2/org/apache/maven/maven/maven-metadata.xml".toURL().text
def mavenDistributionVersionsXml = new XmlParser().parseText(mavenDistributionVersions)
def mavenVersion = mavenDistributionVersionsXml.versioning.versions.version.findAll({ version ->
    !version.text().contains("-")
}).last().text()
log.info("Using Maven: ${mavenVersion}")


new FileOutputStream(wrapperDir.resolve("maven-wrapper.properties").toFile()).withStream {
    it << "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${mavenVersion}/apache-maven-${mavenVersion}-bin.zip\n"
    it << "wrapperUrl=${wrapperJarUrl}\n"
}

def isWindows = System.properties["os.name"].toLowerCase().contains("windows")
if (!isWindows) {
    run("chmod +x ${projectDirectory.resolve("scripts").resolve("mvnw").toString()}")
    run("chmod +x ${projectDirectory.resolve("scripts").resolve("mvnwDebug").toString()}")
}

// set the version of the parent to the version of the archetype
// NB at this point, we just use the Maven wrapper to do things
def mvnwCommand = isWindows ?
    projectDirectory.resolve("scripts").resolve("mvnw.cmd").toString() :
    projectDirectory.resolve("scripts").resolve("mvnw").toString()

run("${mvnwCommand} versions:update-parent -DskipResolution -DparentVersion=${request.archetypeVersion} -DgenerateBackupPoms=false")

