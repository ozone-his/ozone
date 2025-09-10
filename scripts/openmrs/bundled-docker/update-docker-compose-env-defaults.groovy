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
def bundledDockerBuildDir = project.properties['ozone.bundled.docker.build.directory']

def dockerRunDir = "${projectBuildDir}/${projectArtifactId}-${projectVersion}/run/docker"
def concatenatedEnvFile = new File("${dockerRunDir}/concatenated.env")
def fallbackEnvFile = new File("${dockerRunDir}/.env")
def dockerComposeFile = new File("${bundledDockerBuildDir}/bundled-docker/${bundledDockerComposeFilename}")

def envFile = concatenatedEnvFile.exists() ? concatenatedEnvFile : fallbackEnvFile.exists() ? fallbackEnvFile : null

if (!envFile) {
    warn("No env file found in: ${dockerRunDir}")
    return
}

if (!dockerComposeFile.exists()) {
    error("Docker Compose file not found: ${dockerComposeFile.absolutePath}")
    return
}

log("Using env file: ${envFile.name}")
log("Processing: ${dockerComposeFile.name}")

def envDefaults = parseEnvFile(envFile)
if (envDefaults.isEmpty()) {
    warn("No environment variables found")
    return
}

log("Loaded ${envDefaults.size()} environment defaults")

def content = dockerComposeFile.text
def updatedContent = processDockerCompose(content, envDefaults)

if (updatedContent != content) {
    dockerComposeFile.text = updatedContent
    log("Successfully updated Docker Compose file")
} else {
    log("No updates needed")
}

static def parseEnvFile(File file) {
    def envVars = [:]
    file.eachLine { line ->
        line = line.trim()
        if (line && !line.startsWith('#')) {
            def matcher = line =~ /^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/
            if (matcher) {
                def key = matcher[0][1]
                def value = matcher[0][2]

                if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value[1..-2]
                }

                envVars[key] = value
            }
        }
    }
    return envVars
}

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

        if (trimmed == 'services:') {
            inServices = true
            serviceIndent = indent + 2
        } else if (inServices && indent <= 0 && trimmed && !trimmed.startsWith('#')) {
            inServices = false
        }

        if (inServices && indent == serviceIndent && trimmed.endsWith(':')) {
            currentService = trimmed[0..-2]
        }

        if (inServices && currentService && trimmed.startsWith('environment:')) {
            result << line
            def processed = processEnvironmentBlock(lines, i + 1, indent + 2, envDefaults)
            result.addAll(processed.lines)
            changes += processed.changes
            i = processed.lastIndex
            continue
        }

        result << line
    }

    if (changes > 0) {
        log("Set defaults for ${changes} variables")
    }

    return result.join('\n')
}

static def processEnvironmentBlock(String[] lines, int startIndex, int expectedIndent, Map<String, String> envDefaults) {
    def result = []
    def changes = 0
    def i = startIndex

    while (i < lines.length) {
        def line = lines[i]
        def indent = getIndentLevel(line)
        def trimmed = line.trim()

        if (indent < expectedIndent && trimmed) {
            break
        }

        if (!trimmed || trimmed.startsWith('#')) {
            result << line
            i++
            continue
        }

        if (indent == expectedIndent) {
            def processed
            if (trimmed.startsWith('- ')) {
                // Handle array format: - VAR=value or - VAR=${OTHER_VAR}
                processed = processArrayEnvVarLine(line, envDefaults)
            } else {
                // Handle key-value format: VAR: value
                processed = processEnvVarLine(line, envDefaults)
            }
            result << processed.line
            if (processed.changed) changes++
        } else {
            result << line
        }

        i++
    }

    return [lines: result, changes: changes, lastIndex: i - 1]
}

static def processArrayEnvVarLine(String line, Map<String, String> envDefaults) {
    def indent = getIndentSpaces(line)
    def trimmed = line.trim()

    // Remove the "- " prefix
    def envVarPart = trimmed.substring(2).trim()

    // Parse VAR=value format
    def varMatch = envVarPart =~ /^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/
    if (!varMatch) {
        return [line: line, changed: false]
    }

    def varName = varMatch[0][1]
    def currentValue = varMatch[0][2]

    if (needsDefaultForArrayFormat(currentValue) && envDefaults.containsKey(varName)) {
        def defaultValue = envDefaults[varName]
        def newValue = "\${${varName}:-${escapeValue(defaultValue)}}"
        def newLine = "${indent}- ${varName}=${newValue}"

        //log("Setting default for ${varName}")
        return [line: newLine, changed: true]
    }

    return [line: line, changed: false]
}

static def processEnvVarLine(String line, Map<String, String> envDefaults) {
    def indent = getIndentSpaces(line)
    def trimmed = line.trim()

    def varMatch = trimmed =~ /^([A-Za-z_][A-Za-z0-9_]*):?\s*(.*)$/
    if (!varMatch) {
        return [line: line, changed: false]
    }

    def varName = varMatch[0][1]
    def currentValue = varMatch[0][2]

    if (needsDefault(currentValue) && envDefaults.containsKey(varName)) {
        def defaultValue = envDefaults[varName]
        def newValue = "\${${varName}:-${escapeValue(defaultValue)}}"
        def newLine = "${indent}${varName}: ${newValue}"

        //log("Setting default for ${varName}")
        return [line: newLine, changed: true]
    }

    return [line: line, changed: false]
}

static def needsDefault(String value) {
    if (!value) return true

    def trimmed = value.trim()
    if (trimmed.startsWith(':')) {
        trimmed = trimmed.substring(1).trim()
    }

    return trimmed.isEmpty() || trimmed in ['""', "''"] || trimmed.matches(/^\$[A-Za-z_][A-Za-z0-9_]*$/) || (trimmed.matches(/^\$\{[A-Za-z_][A-Za-z0-9_]*\}$/) && !trimmed.contains(':-'))
}

static def needsDefaultForArrayFormat(String value) {
    if (!value) return true

    def trimmed = value.trim()

    return trimmed.isEmpty() || trimmed in ['""', "''"] || trimmed.matches(/^\$[A-Za-z_][A-Za-z0-9_]*$/) || (trimmed.matches(/^\$\{[A-Za-z_][A-Za-z0-9_]*\}$/) && !trimmed.contains(':-'))
}

static def getIndentLevel(String line) {
    line.takeWhile { it == ' ' }.length()
}

static def getIndentSpaces(String line) {
    ' ' * getIndentLevel(line)
}

static def escapeValue(String value) {
    if (!value) return ''

    def escaped = value.replace('\\', '\\\\')
            .replace('}', '\\}')
            .replace('$', '\\$')

    if (needsQuoting(escaped)) {
        escaped = escaped.replace("'", "\\'")
        return "'${escaped}'"
    }

    return escaped
}

static def needsQuoting(String value) {
    value.isEmpty() || value.contains(' ') || value.contains('\t') || value.contains(':') || value.contains('#') || value.contains('"') || value.contains('\n') || value.trim() != value
}
