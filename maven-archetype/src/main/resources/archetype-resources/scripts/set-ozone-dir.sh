#!/usr/bin/env bash
set -e

cd ${project.build.directory}/
source ./go-to-scripts-dir.sh

# Override the ozone-dir.env file:
cat > ozone-dir.env << EOL
#
# $(date) | ⚠️ Existing file contents overriden by the '$(basename "$0")' script.
#
export OZONE_DIR=../../distro
EOL
