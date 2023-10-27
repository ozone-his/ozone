#!/usr/bin/env bash
set -e

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