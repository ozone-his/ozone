/**
 * This script is used to generate a shell script that will be used to import the CSV files into the ERPNext instance.
 * The CSV files are sorted based on the domain order, operation order and then alphabetically within each domain.
 * The domain order and operation order are defined in the script.
 * The script assumes that the file name format is <type>_<doctype>.csv
 * For example: "insert_lead.csv, update_uom.csv"
 * The script also assumes that the ERPNext configuration is installed in /opt/erpnext*/

import java.nio.file.Paths

/**
 * This function is used to get all CSV files in a directory recursively.
 * @param dir The directory to search for CSV files.
 * @return A list of CSV files.
 */
static def getCSVFilesRecursively(File dir) {
    def csvFiles = []
    dir.eachFileRecurse { file ->
        if (file.isFile() && file.name.endsWith('.csv')) {
            csvFiles << file
        }
    }
    return csvFiles
}

// Path to the directory containing the CSV files
def csvDirPath = Paths.get("${project.build.directory}/${project.artifactId}-${project.version}/configs/erpnext/initializer_config")

// Get all CSV files in the directory
def csvFiles = getCSVFilesRecursively(csvDirPath.toFile())

// Define the domain order
def domainOrder = ['Warehouse', 'Fiscal Year', 'UOM', 'Item Group', 'Item', 'Item Price', 'Price List']

// Define the operation order
def operationOrder = ['insert', 'update']

/**
 * Sort the CSV files based on the domain order, operation order and then alphabetically within each domain.
 * If the domain or operation is not in the respective order list, it is assigned a high index so that it comes after all the domains/operations that are in the list.
 * @param file The file to sort.
 * @return A list of sorted CSV files.
 */
def orderedCsvFiles = csvFiles.sort { file ->
    def domain = ''
    def operation = ''
    def parts = file.name.split("_")
    if (parts.size() < 2) {
        throw new IllegalArgumentException("Invalid file name format: ${filename}")
    }
    if (parts.size() == 2) {
        domain = parts[1].split("\\.")[0].capitalize()
        operation = parts[0].toLowerCase()
    } else {
        domain = parts[1..-1].join(" ").split("\\.")[0].split(" ").collect { it.capitalize() }.join(" ")
        operation = parts[0].toLowerCase()
    }
    def domainIndex = domainOrder.indexOf(domain)
    if (domainIndex == -1) {
        domainIndex = Integer.MAX_VALUE
    }
    def operationIndex = operationOrder.indexOf(operation)
    if (operationIndex == -1) {
        operationIndex = Integer.MAX_VALUE
    }
    [domainIndex, operationIndex]
}

log.info("Found ${orderedCsvFiles.size()} ERPNext Configuration CSV files to import")

def scriptFile = "${project.build.directory}/${project.artifactId}-${project.version}/binaries/erpnext/scripts/data-import.sh" as Object

ant.echo(file: scriptFile, message: "#!/bin/bash\n\n", append: false)

/**
 * For each sorted CSV file, generate a shell script command to import the file into the ERPNext instance.
 * The command is appended to the script file.*/
orderedCsvFiles.each { file ->
    def filePath = file.toString()
    def startIndex = filePath.indexOf("/initializer_config/") + "/initializer_config/".length()
    def strippedFilePath = filePath.substring(startIndex)

    // Assumption: File name format is <type>_<doctype>.csv
    // Example: "insert_lead.csv, update_uom.csv"
    def filename = file.getName()
    def parts = filename.split("_")
    def type = parts[0].capitalize()
    def docType = parts[1..-1].join(" ").split("\\.")[0].split(" ").collect { it.capitalize() }.join(" ")

    // Assumption: ERPNext configuration is installed in /opt/erpnext
    def baseConfigDir = "/opt/erpnext/configs"

    def message = "bench --site ozone-erpnext data-import --file ${baseConfigDir}/${strippedFilePath} --doctype '${docType}' --type '${type}' --submit-after-import --mute-emails; \n" as Object
    ant.echo(file: scriptFile, message: message, append: true)
}
