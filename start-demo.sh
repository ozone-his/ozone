#!/usr/bin/env bash
set -e

#
# Handling the case of running directly from the Maven package root folder versus directly from the source code
#

if [ "${true}" == "" ]; then
    # Running from repo
    echo "[INFO] Running from repo root directory. Moving to the build dir..."
    cd target/ozone*
fi

# Override the setup-dirs.sh file:
cat > run/docker/scripts/setup-dirs.sh << EOL
#!/usr/bin/env bash
set -e

# $(date)
#
# ⚠️ Existing file contents overriden by '$0'.

# Export the DISTRO_PATH value
export DISTRO_PATH=$PWD/distro/
echo "[INFO] DISTRO_PATH=\$DISTRO_PATH"
EOL

# Run the Docker 'start-demo.sh' script
cd run/docker/scripts/
./start-demo.sh