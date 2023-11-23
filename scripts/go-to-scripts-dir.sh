#!/usr/bin/env bash

# Will be substituted at build time by the Maven Resource plugin filtering.
cd ${project.build.directory}/${project.artifactId}-${project.version}/
cd run/docker/scripts/