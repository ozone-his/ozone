#!/usr/bin/env bash
set -e

# Download the remote dependency report
remoteRepoUrl=https://nexus.mekomsolutions.net/repository/maven-public

echo "Parsing details from project pom.xml..."
version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
artifactId=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
groupId=$(mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
classifier=$(mvn help:evaluate -Dexpression=dependencyReportClassifier -q -DforceStdout)
artifact=${groupId}:${artifactId}:${version}:txt:${classifier}
filename=${artifactId}-${version}-${classifier}.txt
absolutePath=/tmp/$filename

rm -f $absolutePath

echo "Fetch remote dependency report..."
set +e
mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.0:get -DremoteRepositories=${remoteRepoUrl} -Dartifact=${artifact} -Dtransitive=false
mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.0:copy -Dartifact=${artifact} -DoutputDirectory=/tmp/ -Dmdep.useBaseVersion=true
set -e

# If no dependency report was fetched, create an empty one.
if [ ! -f "$absolutePath" ]; then
    echo "Remote dependency file is not found at $absolutePath. Creating an empty one and continue."
    touch $absolutePath
fi

# Build the local dependency report
echo "Compile a local dependency report..."
mvn clean compile

# Compare the 2 files. Will exit with 0 if no change, 1 if changes
echo "Compare both dependency reports..."
set +e
diff /tmp/ozone-distro-1.0.0-SNAPSHOT-dependencies.txt ./target/ozone-distro-1.0.0-SNAPSHOT-dependencies.txt
diff_rc=$?
set -e

if [ $diff_rc -eq 0 ]
then
echo "No dependency change. Exit (0)"
elif [ $diff_rc -eq 1 ]
then
echo "One or more dependency has changed. Exit (1)"
else
echo "Unknown error occured."
fi

echo hasChanged=$diff_rc >> "$GITHUB_OUTPUT"

exit 0
