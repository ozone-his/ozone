#!/usr/bin/env bash
set -e

# Download the remote dependency report
remoteRepoUrl=https://nexus.mekomsolutions.net/repository/maven-public

# Parse artifact details from project pom.xml
echo "Parsing details from project pom.xml..."
version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
artifactId=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
groupId=$(mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
classifier=$(mvn help:evaluate -Dexpression=dependencyReportClassifier -q -DforceStdout)
artifact=${groupId}:${artifactId}:${version}:txt:${classifier}

# Set the dependency report file name
filename=${artifactId}-${version}-${classifier}.txt
absolutePath=/tmp/$filename

# Remove previous file if any
rm -f ${absolutePath}

# Fetch the remote dependency report
echo "Fetch remote dependency report..."
set +e
mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.0:get -DremoteRepositories=${remoteRepoUrl} -Dartifact=${artifact} -Dtransitive=false
mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.0:copy -Dartifact=${artifact} -DoutputDirectory=/tmp/ -Dmdep.useBaseVersion=true
set -e

# If no dependency report was fetched, create an empty one.
if [ ! -f "$absolutePath" ]; then
    echo "Remote dependency file is not found at '${absolutePath}'. Creating an empty one and continue."
    touch ${absolutePath}
else
    echo "Remote dependency file saved at '${absolutePath}'."
fi

# Build the local dependency report
echo "Compile a local dependency report..."
mvn clean compile

# Compare the 2 files. Will exit with 0 if no change, 1 if changes
echo "Compare both dependency reports..."
set +e
diff ${absolutePath} ./target/${filename}
diff_rc=$?
set -e

if [ $diff_rc -eq 0 ]; then
    echo "No dependency change. Exit (0)"
elif [ $diff_rc -eq 1 ]; then
    echo "One or more dependency has changed. Exit (1)"
else
    echo "Unknown error occured."
fi

set +e
# Export result code to be used in later steps of the GitHub worklow.
echo hasChanged=$diff_rc >> "$GITHUB_OUTPUT"
set -e

exit 0
