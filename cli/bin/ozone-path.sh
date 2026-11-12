#!/bin/sh
# Source this file to put the Ozone CLI on your PATH:
#   . ./bin/ozone-path.sh
# then use:  ozone --compose-root <dir> --distro <dir> --hostname <h> run
# (Running it directly has no effect on your shell - it must be sourced.
#  Equivalent one-liner: export PATH="$PWD/bin:$PATH")
_ozone_bin="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
case ":$PATH:" in
  *":$_ozone_bin:"*) ;;
  *) export PATH="$_ozone_bin:$PATH" ;;
esac
unset _ozone_bin
