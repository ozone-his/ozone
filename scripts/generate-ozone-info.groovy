import groovy.json.JsonOutput

def json = [
        name: 'ozone',
        version: project.version,
        description: project.description
]

new File(project.build.directory, 'ozone-info.json').text = JsonOutput.prettyPrint(JsonOutput.toJson(json))
