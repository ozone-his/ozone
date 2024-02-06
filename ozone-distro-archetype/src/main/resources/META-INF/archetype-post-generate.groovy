import java.nio.file.Paths

projectDirectory = Paths.get(request.outputDirectory, request.artifactId).toFile()

def run(String command) {
    def process = command.execute(null, projectDirectory);
    process.consumeProcessOutput(System.out, System.err)
    process.waitFor()
    if (process.exitValue() != 0) {
        throw new RuntimeException("'$command' exited with code ${process.exitValue()}")
    }
}

run("mvn wrapper:wrapper")
