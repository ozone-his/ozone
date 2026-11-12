# Source this file in fish to put the Ozone CLI on your PATH:
#   source ./bin/ozone-path.fish
# then use:  ozone --compose-root <dir> --distro <dir> --hostname <h> run
# (For sh/bash/zsh use ozone-path.sh instead.)
set -l _ozone_bin (cd (dirname (status filename)) && pwd)
if not contains -- $_ozone_bin $PATH
    set -gx PATH $_ozone_bin $PATH
end
set -e _ozone_bin
