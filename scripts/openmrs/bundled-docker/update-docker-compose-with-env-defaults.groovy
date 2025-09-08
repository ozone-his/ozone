// Script to set default values for existing environment variables in Docker Compose from env files
// Maintains ability to override using environment variables
import groovy.transform.Field

@Field def log = { message -> println "[INFO] ${message}" }
@Field def warn = { message -> println "[WARN] ${message}" }
@Field def error = { message -> println "[ERROR] ${message}" }

def projectBuildDir = project.build.directory
def projectArtifactId = project.artifactId
def projectVersion = project.version
def bundledDockerComposeFilename = project.properties['bundled.docker.compose.output.filename']

// Construct paths
def dockerRunDir = "${projectBuildDir}/${projectArtifactId}-${projectVersion}/run/docker"
def concatenatedEnvFile = new File("${dockerRunDir}/concatenated.env")
def fallbackEnvFile = new File("${dockerRunDir}/.env")
def dockerComposeFile = new File("${projectBuildDir}/bundled-docker-tmp/bundled-docker/${bundledDockerComposeFilename}")

// Determine which env file to use
def envFile = concatenatedEnvFile.exists() ? concatenatedEnvFile : fallbackEnvFile.exists() ? fallbackEnvFile : null

if (!envFile) {
    warn("Neither concatenated.env nor .env file found in: ${dockerRunDir}")
    return
}

if (!dockerComposeFile.exists()) {
    error("Docker Compose file not found at: ${dockerComposeFile.absolutePath}")
    return
}

log("Reading environment defaults from: ${envFile.name}")
log("Processing Docker Compose file: ${dockerComposeFile.name}")

// Parse environment variables from file
def envDefaults = parseEnvFile(envFile)
log("Loaded ${envDefaults.size()} environment defaults")

if (envDefaults.isEmpty()) {
    warn("No environment variables found in ${envFile.name}")
    return
}

// Process Docker Compose file
def dockerComposeContent = dockerComposeFile.text
def updatedContent = processDockerCompose(dockerComposeContent, envDefaults)

// Write back only if changes were made
if (updatedContent != dockerComposeContent) {
    dockerComposeFile.text = updatedContent
    log("Successfully updated Docker Compose file with environment defaults")
} else {
    log("No environment variables needed default values")
}

// Parse environment file
static def parseEnvFile(File file) {
    def envVars = [:]
    file.eachLine { line ->
        line = line.trim()
        if (line && !line.startsWith('#')) {
            def matcher = line =~ /^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/
            if (matcher) {
                def key = matcher[0][1]
                def value = matcher[0][2]

                // Remove surrounding quotes
                if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value[1..-2]
                }

                envVars[key] = value
            }
        }
    }
    return envVars
}

// Process Docker Compose content
def processDockerCompose(String content, Map<String, String> envDefaults) {
    def lines = content.split('\n')
    def result = []
    def inServices = false
    def currentService = null
    def serviceIndent = 0
    def changes = 0

    for (int i = 0; i < lines.length; i++) {
        def line = lines[i]
        def trimmed = line.trim()
        def indent = getIndentLevel(line)

        // Track services section
        if (trimmed == 'services:') {
            inServices = true
            serviceIndent = indent + 2
            result << line
            continue
        }

        // Exit services section
        if (inServices && indent <= 0 && trimmed && !trimmed.startsWith('#')) {
            inServices = false
        }

        // Track current service
        if (inServices && indent == serviceIndent && trimmed.endsWith(':')) {
            currentService = trimmed[0..-2]
            result << line
            continue
        }

        // Process environment variables
        if (inServices && currentService && trimmed.startsWith('environment:')) {
            result << line

            // Process environment block
            def envStartIndex = i + 1
            def processed = processEnvironmentBlock(lines, envStartIndex, indent + 2, envDefaults)
            result.addAll(processed.lines)
            changes += processed.changes
            i = processed.lastIndex
            continue
        }

        result << line
    }

    if (changes > 0) {
        log("Set default values for ${changes} environment variables")
    }

    return result.join('\n')
}

// Process environment block in Docker Compose
def processEnvironmentBlock(String[] lines, int startIndex, int expectedIndent, Map<String, String> envDefaults) {
    def result = []
    def changes = 0
    def i = startIndex

    while (i < lines.length) {
        def line = lines[i]
        def indent = getIndentLevel(line)
        def trimmed = line.trim()

        // Exit if we've left the environment block
        if (indent < expectedIndent && trimmed) {
            break
        }

        // Skip empty lines and comments
        if (!trimmed || trimmed.startsWith('#')) {
            result << line
            i++
            continue
        }

        // Process environment variable line
        if (indent == expectedIndent) {
            def processed = processEnvVarLine(line, envDefaults)
            result << processed.line
            if (processed.changed) changes++
        } else {
            result << line
        }

        i++
    }

    return [lines: result, changes: changes, lastIndex: i - 1]
}

// Process individual environment variable line
def processEnvVarLine(String line, Map<String, String> envDefaults) {
    def indent = getIndentSpaces(line)
    def trimmed = line.trim()

    // Handle different environment variable formats
    def varMatch = trimmed =~ /^([A-Za-z_][A-Za-z0-9_]*):?\s*(.*)$/
    if (!varMatch) {
        return [line: line, changed: false]
    }

    def varName = varMatch[0][1]
    def currentValue = varMatch[0][2]

    // Check if variable needs a default value
    if (shouldSetDefault(currentValue) && envDefaults.containsKey(varName)) {
        def defaultValue = envDefaults[varName]
        def newValue = createValueWithEnvOverride(varName, defaultValue)
        def newLine = "${indent}${varName}: ${newValue}"

        log("Setting default for ${varName} with environment override capability")
        return [line: newLine, changed: true]
    }

    return [line: line, changed: false]
}

// Create value that allows environment variable override
static def createValueWithEnvOverride(String varName, String defaultValue) {
    // Use Docker Compose variable substitution with default value
    // Format: ${VAR_NAME:-default_value}
    def escapedDefault = escapeForVariableSubstitution(defaultValue)
    return "\${${varName}:-${escapedDefault}}"
}

// Check if environment variable needs a default value
static def shouldSetDefault(String value) {
    if (!value || value.trim().isEmpty()) return true

    def trimmed = value.trim()

    // Remove colons and whitespace
    if (trimmed.startsWith(':')) {
        trimmed = trimmed.substring(1).trim()
    }

    // Needs default if empty, just a variable reference without default, or placeholder
    return trimmed.isEmpty() || trimmed.matches(/^\$\{[^}]*\}$/) && !trimmed.contains(':-') || trimmed.matches(/^\$[A-Za-z_][A-Za-z0-9_]*$/) || trimmed == '""' || trimmed == "''"
}

// Get indentation level (count of leading spaces)
static def getIndentLevel(String line) {
    def count = 0
    for (int i = 0; i < line.length(); i++) {
        if (line.charAt(i) == ' ') {
            count++
        } else {
            break
        }
    }
    return count
}

// Get indentation spaces as string
static def getIndentSpaces(String line) {
    def count = getIndentLevel(line)
    return ' ' * count
}

// Escape value for use inside Docker Compose variable substitution
static def escapeForVariableSubstitution(String value) {
    if (value == null) return ''

    // Escape special characters that could interfere with variable substitution
    def escaped = value.replace('\\', '\\\\')
            .replace('}', '\\}')
            .replace('$', '\\$')

    // If value contains spaces or special YAML characters, wrap in quotes
    if (needsQuotingInSubstitution(escaped)) {
        // Use single quotes to avoid issues with double quote escaping
        escaped = escaped.replace("'", "\\'")
        return "'${escaped}'"
    }

    return escaped
}

// Check if value needs quoting inside variable substitution
static def needsQuotingInSubstitution(String value) {
    if (value.isEmpty()) return true

    // Special characters that need quoting in variable substitution context
    return value.contains(' ') || value.contains('\t') || value.contains(':') || value.contains('#') || value.contains('"') || value.contains('\n') || value.trim() != value
}
