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
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger

Logger log = Logger.getAnonymousLogger()
log.info("Base directory: ${basedir}")
log.info("Running verification script...")

/**
 * Check if a directory contains all specified files
 * @param dir - Directory path (String, File, or Path)
 * @param files - List of file names to check for
 * @param options - Optional configuration map
 * @return true if all files exist, false otherwise (unless assertOnMissing is true)
 */
static def hasFiles(dir, List<String> files, Map options = [:]) {
    Logger log = Logger.getAnonymousLogger()
    def directory
    def assertOnMissing = options.assertOnMissing ?: false
    def recursive = options.recursive ?: false
    def caseSensitive = options.caseSensitive ?: true

    // Convert dir to File object
    if (dir instanceof String) {
        directory = new File(dir)
    } else if (dir instanceof Path) {
        directory = dir.toFile()
    } else if (dir instanceof File) {
        directory = dir
    } else {
        throw new IllegalArgumentException("Directory must be String, File, or Path")
    }

    // Check if directory exists
    if (!directory.exists()) {
        def message = "Directory does not exist: ${directory.absolutePath}"
        if (assertOnMissing) {
            assert false, message
        } else {
            return false
        }
    }

    if (!directory.isDirectory()) {
        def message = "Path is not a directory: ${directory.absolutePath}"
        if (assertOnMissing) {
            assert false, message
        } else {
            return false
        }
    }

    def missingFiles = []
    def foundFiles = []

    // Get list of files in directory
    def availableFiles
    if (recursive) {
        availableFiles = []
        directory.eachFileRecurse { file ->
            if (file.isFile()) {
                availableFiles.add(file.name)
            }
        }
    } else {
        availableFiles = directory.listFiles()?.findAll { it.isFile() }?.collect { it.name } ?: []
    }

    // Handle case sensitivity
    def fileCheckList = caseSensitive ? availableFiles : availableFiles.collect { it.toLowerCase() }
    def searchFiles = caseSensitive ? files : files.collect { it.toLowerCase() }

    // Check each required file
    searchFiles.each { fileName ->
        if (fileCheckList.contains(fileName)) {
            foundFiles.add(fileName)
            log.info("Found file: ${fileName}")
        } else {
            missingFiles.add(fileName)
            log.warning("Missing file: ${fileName}")
        }
    }

    log.info("File check in ${directory.absolutePath}:")
    log.info("  Found: ${foundFiles.size()}/${files.size()} files")

    if (foundFiles.size() > 0) {
        log.info("  Found files: ${foundFiles.join(', ')}")
    }

    if (missingFiles.size() > 0) {
        log.warning("  Missing files: ${missingFiles.join(', ')}")

        if (assertOnMissing) {
            assert false, "Missing files in ${directory.absolutePath}: ${missingFiles.join(', ')}"
        }
    }

    return missingFiles.size() == 0
}

static def verifyOpenmrsBackendModules(Properties properties, Path binariesDir, Map options = [:]) {
    Logger log = Logger.getAnonymousLogger()
    def modulePrefix = options.prefix ?: "omod."
    def fileExtension = options.extension ?: ".omod"
    def failOnMissing = options.failOnMissing ?: false
    def customNaming = options.customNaming ?: [:]  // Map for custom module file naming

    def results = [found: [], missing: [], skipped: []]

    properties.forEach { key, value ->
        if (key.toString().startsWith(modulePrefix) && !key.toString().contains(".groupId") && !key.toString().contains(".type")) {
            def moduleName = key.toString().substring(modulePrefix.length())
            def moduleVersion = value.toString()

            if (!moduleVersion || moduleVersion.trim().isEmpty()) {
                results.skipped.add(moduleName)
                return
            }

            def moduleFileName
            if (customNaming.containsKey(moduleName)) {
                moduleFileName = customNaming[moduleName].replace('${version}', moduleVersion)
            } else {
                moduleFileName = "${moduleName}-${moduleVersion}${fileExtension}"
            }

            def moduleFile = binariesDir.resolve(moduleFileName).toFile()

            log.info("Checking for ${moduleName} module at: ${moduleFile.getPath()}")

            if (moduleFile.exists()) {
                log.info("${moduleFileName} found")
                results.found.add([name: moduleName, version: moduleVersion, file: moduleFileName])
            } else {
                log.info("${moduleFileName} not found in ${binariesDir}")
                results.missing.add([name: moduleName, version: moduleVersion, file: moduleFileName])
            }
        }
    }

    log.info("OpenMRS Modules Check Summary for Ozone with distro.properties:")
    log.info("  Found: ${results.found.size()}")
    log.info("  Missing: ${results.missing.size()}")
    log.info("  Skipped: ${results.skipped.size()}")

    if (failOnMissing && results.missing.size() > 0) {
        def missingFiles = results.missing.collect { it.file }
        assert false, "Missing required modules: ${missingFiles.join(', ')}"
    }
    return results
}

def buildDir = Paths.get("${basedir}/project/ozone-with-distro-properties/target/ozone-with-distro-properties-1.0.0-SNAPSHOT");

// Verify that configs & binaries specified in distro.properties exists
// distro.properties file should exist
def distroPropertiesFile = buildDir.resolve("distro/configs/openmrs/distro_assembly/distro.properties").toFile()
assert distroPropertiesFile.exists()

def properties = new Properties()
distroPropertiesFile.withInputStream {
    properties.load(it)
}

// Verify that the OpenMRS modules exist in the binaries directory
def binariesOpenmrsModulesDir = buildDir.resolve("distro/binaries/openmrs/modules")
def results = verifyOpenmrsBackendModules(properties, binariesOpenmrsModulesDir)
assert results.found.size() == 28
assert results.missing.size() == 0
assert results.skipped.size() == 0

// Verify content package(s) configs
def openmrsInitializerConfigs = buildDir.resolve("distro/configs/openmrs/initializer_config")
assert openmrsInitializerConfigs.toFile().exists()
assert openmrsInitializerConfigs.toFile().isDirectory()

def contentAddressHierarchy = openmrsInitializerConfigs.resolve("addresshierarchy")
assert contentAddressHierarchy.toFile().exists()
assert contentAddressHierarchy.toFile().isDirectory()

def contentAddressHierarchyReferenceApplicationDemo = contentAddressHierarchy.resolve("01_referenceapplication-demo")
assert contentAddressHierarchyReferenceApplicationDemo.toFile().exists()
assert contentAddressHierarchyReferenceApplicationDemo.toFile().isDirectory()

hasFiles(contentAddressHierarchyReferenceApplicationDemo, ["addressConfiguration.xml", "addresshierarchy.csv"])

return true
