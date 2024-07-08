import java.nio.file.Paths

log.info("Checking for frontend customizations in ${Paths.get("${project.build.directory}", "openmrs_frontend", "spa-assemble-config.json").toString()}")

def refAppConfigFile = Paths.get("${project.build.directory}", "openmrs_frontend", "reference-application-spa-assemble-config.json").toFile()
def slurper = new groovy.json.JsonSlurper()
def refAppConfig = slurper.parse(refAppConfigFile)
def openmrsVersion = refAppConfig["coreVersion"] ?: "next"
def outputDirectory = "${project.groupId}" == "com.ozonehis" && "${project.artifactId}" == "ozone-distro" ?
    "${project.build.directory}/${project.artifactId}-${project.version}/binaries/openmrs/frontend" :
    "${project.build.directory}/${project.artifactId}-${project.version}/distro/binaries/openmrs/frontend"

def outputDirectoryFile = new File(outputDirectory)

def assembleCommand = "npx --legacy-peer-deps openmrs@${openmrsVersion} assemble --manifest --mode config --target ${outputDirectory} --config ${refAppConfigFile.getAbsolutePath()}"
log.info("Project: ${project.groupId}:${project.artifactId}")
// by default, we build the frontend as part of Ozone and only rebuild if there are local customizations
def shouldBuildFrontend = "${project.groupId}" == "com.ozonehis" && "${project.artifactId}" == "ozone-distro"

if (!shouldBuildFrontend) {
    frontendCustomizationsFile = Paths.get("${project.build.directory}", "openmrs_frontend", "spa-assemble-config.json").toFile()
    if (frontendCustomizationsFile.exists()) {
        log.info("Found frontend customizations. Rebuilding frontend...")

        assembleCommand += " --config ${frontendCustomizationsFile.getAbsolutePath()}"
        shouldBuildFrontend = true
        // Update the openmrs version to the one specified in the customizations file if it exists.
        openmrsVersion = slurper.parse(frontendCustomizationsFile)["coreVersion"] ?: openmrsVersion
        log.info("Using OpenMRS Frontend Core Version: ${openmrsVersion}")
    }
}

if (shouldBuildFrontend) {
    log.info("Cleaning ${outputDirectory}...")
    if (outputDirectoryFile.exists()) {
        outputDirectoryFile.eachFile(it -> it.delete())
        outputDirectoryFile.eachDir(it -> { if (it.getName() != "ozone") { it.deleteDir() } })
    }

    log.info("Running assemble command...")

    def assembleProcess = assembleCommand.execute()
    assembleProcess.consumeProcessOutput(System.out, System.err)
    assembleProcess.waitFor()

    if (assembleProcess.exitValue() != 0) {
        throw new RuntimeException("'openmrs assemble' step failed. See previous messages for details.")
    }

    log.info("Running build command...")

    def buildProcess = "npx --legacy-peer-deps openmrs@${openmrsVersion} build --config-url \$SPA_CONFIG_URLS --default-locale \$SPA_DEFAULT_LOCALE --target ${outputDirectory}".execute()
    buildProcess.consumeProcessOutput(System.out, System.err)
    buildProcess.waitFor()

    if (buildProcess.exitValue() != 0) {
        throw new RuntimeException("'openmrs build' step failed. See previous messages for details.")
    }
} else {
    log.info("No need to re-build the frontend detected")
}
