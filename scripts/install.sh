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

# Default to current code version
ozoneVersion=${1:-1.0.0-SNAPSHOT}

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

# Download Ozone
echo "$INFO Downloading Ozone $ozoneVersion..."
$mvn dependency:get \
-DgroupId=com.ozonehis \
-DartifactId=ozone \
-Dversion=${ozoneVersion} \
-Dpackaging=zip \
-DremoteRepositories=https://nexus.mekomsolutions.net/repository/maven-public \
-Dtransitive=false

# Copy and Unpack
echo "$INFO Extracting Ozone..."
$mvn dependency:copy \
-Dartifact=com.ozonehis:ozone:${ozoneVersion}:zip \
-DoutputDirectory=./ \
-DuseBaseVersion=true

mv ozone-1.0.0-*.zip ozone-${ozoneVersion}.zip  
unzip ozone-${ozoneVersion}.zip -d ${ozoneInstallFolder}
rm ozone-${ozoneVersion}.zip
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