#!/bin/sh
# Source me from the ozone repo to use the Ozone CLI built from source, for this
# shell session only (sh/bash/zsh; in fish use ozone-local.fish):
#   mvn -pl cli package
#   . cli/bin/ozone-local.sh
# then:  ozone --compose-root <dir> --distro <dir> --hostname <h> run
_OZONE_REPO="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")/../.." && pwd)"
ozone() { java -jar "$_OZONE_REPO"/cli/target/cli-*.jar "$@"; }
