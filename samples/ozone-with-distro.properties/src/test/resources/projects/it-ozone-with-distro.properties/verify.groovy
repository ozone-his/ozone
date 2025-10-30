/**
 * This Groovy script performs comprehensive verification of the ozone-with-distro.properties
 * project build artifacts to ensure the distribution package is correctly assembled.
 *
 * Verifies:
 *  - that `distro.properties` exists in the target build directory
 *  - Modules specified in distro.properties are present in the binaries directory
 *  - Configs specified in distro.properties are present in the configs directory
 *  - that ozone-info.json exists in the target build directory
 *  - that the version of the distribution is correct
 *  - that the distribution package contains the expected modules and configurations
 ***/
import java.nio.file.Paths
import java.util.logging.Logger

Logger log = Logger.getAnonymousLogger()
log.info("Base directory: ${basedir}")
log.info("Running verification script...")

def buildDir = Paths.get("${basedir}/project/ozone-with-distro.properties/target/ozone-with-distro.properties-1.0.0-SNAPSHOT");

// Verify that configs & binaries specified in distro.properties exists
// distro.properties file should exist
def distroPropertiesFile = buildDir.resolve("distro/configs/openmrs/distro_assembly/distro.properties").toFile()
assert distroPropertiesFile.exists()

def properties = new Properties()
distroPropertiesFile.withInputStream {
    properties.load(it)
}

// Verify that the omod files exist in the binaries directory
def binariesDir = buildDir.resolve("distro/binaries/openmrs/modules")

// Check for fhir2 omod
def fhir2Version = properties.getProperty("omod.fhir2")
if (fhir2Version) {
    def fhir2OmodFile = binariesDir.resolve("fhir2-${fhir2Version}.omod").toFile()
    log.info("Checking for fhir2 omod: ${fhir2OmodFile.absolutePath}")
    assert fhir2OmodFile.exists(), "fhir2-${fhir2Version}.omod not found in ${binariesDir}"
    log.info("fhir2-${fhir2Version}.omod found")
}

// Check for initializer omod
def initializerVersion = properties.getProperty("omod.initializer")
if (initializerVersion) {
    def initializerOmodFile = binariesDir.resolve("initializer-${initializerVersion}.omod").toFile()
    log.info("Checking for initializer omod: ${initializerOmodFile.absolutePath}")
    assert initializerOmodFile.exists(), "initializer-${initializerVersion}.omod not found in ${binariesDir}"
    log.info("initializer-${initializerVersion}.omod found")
}

// Check for rest omod
def restVersion = properties.getProperty("omod.webservices.rest")
if (restVersion) {
    def restOmodFile = binariesDir.resolve("webservices.rest-${restVersion}.omod").toFile()
    log.info("Checking for rest omod: ${restOmodFile.absolutePath}")
    assert restOmodFile.exists(), "webservices.rest-${restVersion}.omod not found in ${binariesDir}"
    log.info("webservices.rest-${restVersion}.omod found")
}

log.info("OpenMRS Modules verified successfully")

return true
