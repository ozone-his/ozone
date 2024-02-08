import java.nio.file.Files
import java.nio.file.Paths

projectDirectory = Paths.get(request.outputDirectory, request.artifactId);

def run(String command) {
    def process = command.execute(null, projectDirectory.toFile());
    process.consumeProcessOutput(System.out, System.err)
    process.waitFor()
    if (process.exitValue() != 0) {
        throw new RuntimeException("'$command' exited with code ${process.exitValue()}")
    }
}

run("mvn wrapper:wrapper")
Files.move(projectDirectory.resolve(".mvn"), projectDirectory.resolve("scripts").resolve(".mvn"))
Files.move(projectDirectory.resolve("mvnw"), projectDirectory.resolve("scripts").resolve("mvnw"))
Files.move(projectDirectory.resolve("mvnw.cmd"), projectDirectory.resolve("scripts").resolve("mvnw.cmd"))
