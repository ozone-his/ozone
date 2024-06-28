#!/usr/bin/env bash
set -e

#
# Helper script to download and install Ozone for trial purposes
#

# Set colors
export TEXT_BLUE=`tput setaf 4`
export TEXT_RED=`tput setaf 1`
export BOLD=`tput bold`
export RESET_FORMATTING=`tput sgr0`
INFO="$TEXT_BLUE$BOLD[INFO]$RESET_FORMATTING"
WARN="$TEXT_RED$BOLD[WARN]$RESET_FORMATTING"
ERROR="$TEXT_RED$BOLD[ERROR]$RESET_FORMATTING"

# Introduction message
echo " Installation script for ${BOLD}Ozone FOSS${RESET_FORMATTING} (for testing and demo purposes only)."
echo ""
echo " See https://docs.ozone-his.com/ for how to set it up for production."
echo ""


# TODO: Upon release, replace this with the latest stable version
ozoneVersion=${1:-1.0.0-alpha.11}

echo "$INFO Ozone version: $ozoneVersion"

ozoneInstallFolder="$PWD/ozone"
# Check if ozone/ folder is already present
if [ -d "${ozoneInstallFolder}" ]
then
    echo "$WARN Ozone installation directory (${ozoneInstallFolder}/) already exists."
    while true; do
    read -p "Do you want to overwrite? [y/n]" choice
    case "$choice" in 
        y|Y ) rm -r ${ozoneInstallFolder}/;
            break;;
        n|N )  echo "$INFO Aborting.";
            exit 0;;
        * ) echo "Please enter y, Y, n or N.";;
    esac
    done
fi

# Download Maven and install locally
echo "$INFO Installing Maven $mavenVersion..."
mavenVersion="3.9.6"
mvn=apache-maven-${mavenVersion}/bin/mvn
if [ -f "$mvn" ]
then
    echo "$INFO Maven $mavenVersion already present in the current folder."
    echo "$INFO Skipping Maven installation..."
else
    echo "$INFO Downloading Maven $mavenVersion..."
    curl -O https://dlcdn.apache.org/maven/maven-3/${mavenVersion}/binaries/apache-maven-${mavenVersion}-bin.tar.gz
    tar -xzvf apache-maven-${mavenVersion}-bin.tar.gz
fi

# Set up local Maven helper project to workaround issues when using the Maven dependency plugin from CLI.
cat >_temp_install-latest-ozone-pom.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <artifactId>install-latest-ozone-helper</artifactId>
    <groupId>com.ozonehis</groupId>
    <name>Install Latest Ozone Helper</name>
    <description>Helper project to install the latest Ozone HIS</description>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <organization>
        <name>Ozone HIS</name>
        <url>https://www.ozone-his.com</url>
    </organization>
    <developers>
        <developer>
            <name>Mekom Solutions</name>
            <url>https://www.mekomsolutions.com</url>
        </developer>
    </developers>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.ozonehis</groupId>
            <artifactId>ozone</artifactId>
            <type>zip</type>
            <version>${ozoneVersion}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>Fetch Ozone</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>unpack-dependencies</goal>
                        </goals>
                        <configuration>
                            <excludeTransitive>true</excludeTransitive>
                            <excludeTypes>pom</excludeTypes>
                            <outputDirectory>ozone</outputDirectory>
                            <includeArtifactIds>ozone</includeArtifactIds>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>mks-nexus-public</id>
            <url>https://nexus.mekomsolutions.net/repository/maven-public/</url>
        </repository>
    </repositories>

</project>
EOF

echo "$INFO Download and extract Ozone $ozoneVersion..."
$mvn clean package -f _temp_install-latest-ozone-pom.xml
rm _temp_install-latest-ozone-pom.xml

# Move to the scripts/ folder
pushd ozone/run/docker/scripts/

echo ""
echo "$INFO Ozone installed at $ozoneInstallFolder"
echo ""
echo "---"
echo ""
echo " Type in the following command to run Ozone:"
echo ""
echo "    ${BOLD}cd ozone/run/docker/scripts/"
echo "    ./start-demo.sh"$RESET_FORMATTING
echo ""
echo "(💡 Refer to https://docs.ozone-his.com/ for more information)"
echo ""
