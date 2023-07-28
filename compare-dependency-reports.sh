#!/usr/bin/env bash
set -e

# Download the remote dependency report
remoteRepoUrl=https://nexus.mekomsolutions.net/repository/maven-public

echo "Parsing details from project pom.xml..."
version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
artifactId=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
groupId=$(mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
classifier=$(mvn help:evaluate -Dexpression=dependencyReportClassifier -q -DforceStdout)
artifact=${groupId}:${artifactId}:${version}::${classifier}

echo "Fetch remote dependency report..."
mvn org.apache.maven.plugins:maven-dependency-plugin:3.2.0:get -DremoteRepositories=${remoteRepoUrl} -Dartifact=${artifact} -Dtransitive=false
mvn org.apache.maven.plugins:maven-dependency-plugin:3.2.0:unpack -Dproject.basedir=/tmp/ -Dartifact=${artifact} -DoutputDirectory=/tmp/

# Build the local dependency report
echo "Compile updated dependency report..."
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
exit 0
elif [ $diff_rc -eq 1 ]
then
echo "One or more dependency has changed. Exit (1)"
exit 1
else
exit $diff_rc
fi
