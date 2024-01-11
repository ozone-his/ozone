#!/usr/bin/env bash

# Will be substituted at build time by the Maven Resource plugin filtering.
pushd ${project.build.directory}/${project.artifactId}-${project.version}/run/docker/scripts/
echo "Moved to: $PWD"
echo ""
echo "(💡 Use 'popd' to move back to the repo root directory)"
