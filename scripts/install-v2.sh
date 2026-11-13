#!/usr/bin/env bash
set -e

#
# Helper script to download and install Ozone for trial purposes.
# The downloaded artifact bundles the Ozone CLI (bin/ozone) - the tool used
# to run the distribution afterwards; see the instructions printed at the end.
#

# Set colors (kept empty when not attached to a terminal, e.g. 'curl ... | bash')
if [ -t 1 ]; then
    export TEXT_BLUE=`tput setaf 4`
    export TEXT_RED=`tput setaf 1`
    export BOLD=`tput bold`
    export RESET_FORMATTING=`tput sgr0`
else
    export TEXT_BLUE='' TEXT_RED='' BOLD='' RESET_FORMATTING=''
fi
INFO="$TEXT_BLUE$BOLD[INFO]$RESET_FORMATTING"
WARN="$TEXT_RED$BOLD[WARN]$RESET_FORMATTING"
ERROR="$TEXT_RED$BOLD[ERROR]$RESET_FORMATTING"

# Introduction message
echo " Installation script for ${BOLD}Ozone FOSS${RESET_FORMATTING} (for evaluation purposes only)."
echo ""
echo " See https://docs.ozone-his.com/ for how to set it up for production."
echo ""

# Prerequisites: the distro zip is fetched with curl and unpacked with unzip;
# the Ozone CLI launcher (bin/ozone) needs Java, running the stack needs Docker.
for tool in curl unzip java docker; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "$ERROR Missing required tool '$tool' - please install it and re-run this script."
        exit 1
    fi
done

# TODO: Upon release, replace this with the latest stable version
ozoneVersion=${1:-1.0.0-alpha.15}

echo "$INFO Ozone version: $ozoneVersion"

ozoneFolderName="ozone"
ozoneInstallFolder="$PWD/$ozoneFolderName"
# Check if ozone/ folder is already present
if [ -d "${ozoneInstallFolder}" ]; then
    echo "$WARN Ozone installation directory (${ozoneInstallFolder}/) already exists."
    suffix=1
    while [ -d "${ozoneInstallFolder}_${suffix}" ]; do
        suffix=$((suffix + 1))
    done
    newOzoneInstallFolder="${ozoneInstallFolder}_${suffix}"
    echo "$INFO Creating new installation directory (${newOzoneInstallFolder}/)."
    ozoneInstallFolder="$newOzoneInstallFolder"
    echo "$INFO If you want to overwrite the existing '${ozoneFolderName}/' folder, please delete it first."
fi
installFolderDisplay="${ozoneInstallFolder#$PWD/}"

# Download the Ozone distribution zip straight from the Maven repository
# (same coordinates as the com.ozonehis:ozone:<version>:zip artifact)
ozoneRepoUrl=${OZONE_REPO_URL:-https://nexus.mekomsolutions.net/repository/maven-public}
ozoneZipName="ozone-${ozoneVersion}.zip"
ozoneZipUrl="${ozoneRepoUrl}/com/ozonehis/ozone/${ozoneVersion}/${ozoneZipName}"

echo "$INFO Downloading $ozoneZipUrl ..."
# Show curl's progress bar when stderr is a terminal; stay silent otherwise (e.g. output redirected to a file)
if [ -t 2 ]; then
    curlArgs=(-fSL --retry 3 --progress-bar)
else
    curlArgs=(-fsSL --retry 3)
fi
if ! curl "${curlArgs[@]}" -o "$ozoneZipName" "$ozoneZipUrl"; then
    echo "$ERROR Failed to download $ozoneZipUrl"
    echo "$ERROR Check that version '$ozoneVersion' exists (pass it as the first argument to this script)."
    exit 1
fi

# Verify the published checksum, when available next to the artifact
if curl -fsSL -o "${ozoneZipName}.sha1" "${ozoneZipUrl}.sha1" 2>/dev/null; then
    expectedSha1=$(tr '[:upper:]' '[:lower:]' < "${ozoneZipName}.sha1" | awk '{print $1}')
    if command -v sha1sum >/dev/null 2>&1; then
        actualSha1=$(sha1sum "$ozoneZipName" | awk '{print $1}')
    else
        actualSha1=$(shasum -a 1 "$ozoneZipName" | awk '{print $1}')
    fi
    if [ "$expectedSha1" != "$(echo "$actualSha1" | tr '[:upper:]' '[:lower:]')" ]; then
        echo "$ERROR Checksum mismatch for $ozoneZipName (expected $expectedSha1, got $actualSha1)."
        exit 1
    fi
    echo "$INFO Checksum verified."
else
    echo "$WARN No .sha1 checksum published next to the artifact - skipping verification."
fi

echo "$INFO Extracting Ozone $ozoneVersion into $ozoneInstallFolder ..."
unzip -q -o "$ozoneZipName" -d "$ozoneInstallFolder"
chmod +x "$ozoneInstallFolder/bin/ozone" 2>/dev/null || true
rm -f "$ozoneZipName" "${ozoneZipName}.sha1"

# Suggest the machine's IP as --hostname for local runs ('localhost' breaks SSO, e.g. on macOS)
suggestedHostname=$(hostname -I 2>/dev/null | awk '{print $1}')
if [ -z "$suggestedHostname" ]; then
    suggestedHostname=$( (ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null) || true )
fi
if [ -n "$suggestedHostname" ]; then
    hostnameArg="$suggestedHostname"
else
    hostnameArg="<your-hostname-or-ip>"
fi

echo ""
echo "$INFO Ozone $ozoneVersion installed at $ozoneInstallFolder"
echo ""
echo "---"
echo ""
echo " Type in the following commands to run Ozone with the Ozone CLI:"
echo ""
echo "    ${BOLD}cd $installFolderDisplay"
echo "    . ./bin/ozone-path.sh"
echo "    ozone --compose-root run/docker --distro distro --hostname $hostnameArg up${RESET_FORMATTING}"
echo ""
echo " (The bundled launcher also runs directly: ./bin/ozone --compose-root run/docker --distro distro --hostname $hostnameArg up)"
echo " (For a local run, pass your machine's IP as --hostname - 'localhost' breaks SSO, e.g. on macOS.)"
echo " (Stop the stack with: ozone --compose-root run/docker --distro distro --hostname $hostnameArg down)"
echo ""
echo "(💡 Refer to https://docs.ozone-his.com/ for more information)"
echo ""
