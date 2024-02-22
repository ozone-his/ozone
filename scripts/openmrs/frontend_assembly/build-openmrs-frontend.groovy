import java.nio.file.Paths

log.info("Checking for frontend customizations in ${Paths.get("${project.build.directory}", "openmrs_frontend", "spa-assemble-config.json").toString()}")

def refAppConfigFile = Paths.get("${project.build.directory}", "openmrs_frontend", "reference-application-spa-assemble-config.json").toFile()
def slurper = new groovy.json.JsonSlurper()
def refAppConfig = slurper.parse(refAppConfigFile)
def openmrsVersion = refAppConfig["coreVersion"] ?: "next"

def assembleCommand = "npx --legacy-peer-deps openmrs@${openmrsVersion} assemble --manifest --mode config --target ${project.build.directory}/${project.artifactId}-${project.version}/binaries/openmrs/frontend --config ${refAppConfigFile.getAbsolutePath()}"
log.info("Project: ${project.groupId}:${project.artifactId}")
// by default, we build the frontend as part of Ozone and only rebuild if there are local customizations
def shouldBuildFrontend = "${project.groupId}" == "com.ozonehis" && "${project.artifactId}" == "ozone-distro"

if (!shouldBuildFrontend) {
    frontendCustomizationsFile = Paths.get("${project.build.directory}", "openmrs_frontend", "spa-assemble-config.json").toFile()
    if (frontendCustomizationsFile.exists()) {
        log.info("Found frontend customizations. Rebuilding frontend...")

        assembleCommand += " --config ${frontendCustomizationsFile.getAbsolutePath()}"
        shouldBuildFrontend = true
    }
}

if (shouldBuildFrontend) {
    log.info("Running assemble command...")

    def assembleProcess = assembleCommand.execute()
    assembleProcess.consumeProcessOutput(System.out, System.err)
    assembleProcess.waitFor()

    if (assembleProcess.exitValue() != 0) {
        throw new RuntimeException("'openmrs assemble' step failed. See previous messages for details.")
    }

    log.info("Running build command...")

    def buildProcess = "npx --legacy-peer-deps openmrs@${openmrsVersion} build --config-url \$SPA_CONFIG_URLS --default-locale \$SPA_DEFAULT_LOCALE --target ${project.build.directory}/${project.artifactId}-${project.version}/binaries/openmrs/frontend".execute()
    buildProcess.consumeProcessOutput(System.out, System.err)
    buildProcess.waitFor()

    if (buildProcess.exitValue() != 0) {
        throw new RuntimeException("'openmrs build' step failed. See previous messages for details.")
    }
} else {
    log.info("No need to re-build the frontend detected")
}
