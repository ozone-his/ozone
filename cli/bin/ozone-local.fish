# Source me in fish from the ozone repo to use the Ozone CLI built from source,
# for this session only (for sh/bash/zsh use ozone-local.sh):
#   mvn -pl cli package
#   source cli/bin/ozone-local.fish
# then:  ozone --compose-root <dir> --distro <dir> --hostname <h> run
set -g _OZONE_REPO (cd (dirname (status filename))/../.. && pwd)
function ozone
    java -jar "$_OZONE_REPO"/cli/target/cli-*.jar $argv
end
