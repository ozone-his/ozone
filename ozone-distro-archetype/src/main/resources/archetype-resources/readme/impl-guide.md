#  Ozone ${distributionName} - Implementer Guide

This distribution can be run using the Ozone Docker project, which is the default configuration for this. The quick start command below is for demonstration and trial purposes and would not be suitable for a stable environment.

## Quick Start

Build
```bash
./mvnw clean package
```

Run
```bash
source target/go-to-scripts-dir.sh
./start-ozone.sh
```

### Working on configurations:

If needed to work on the distro configurations and see the results, you have several options:
- (1) Turn down the whole project with its volumes, build again and run.
- (2) Replace files in the mounted Docker volume (all files or only individual files)

#### Option 1. Turn down the whole project and start afresh
```bash
source target/go-to-scripts-dir.sh
./destroy-demo.sh
```

Re-build:
```bash
./mvnw clean package
```

Then start afresh:
```bash
source target/go-to-scripts-dir.sh
./start-ozone.sh
```

#### Option 2. Replace only the files needed, directly in the mounted Docker volume
```bash
rsync -av configs/ target/ozone-kenya-<version>/distro/configs
```
(replace `<version>` with the current version of ozone-kenya)

### Excluding inherited files from Ozone Distro:

It is possible to exclude some of the files inherited from the parent Ozone Distro transitive dependencies (thus the OpenMRS Distro Reference Application).
This can be achieved by providing your exclusion path in the main pom.xml, using the Maven Resource plugin `excludes`:

Eg.:
```xml
<directory>${project.build.directory}/ozone</directory>
  <excludes>
    <exclude>distro/**/appointment*</exclude>
    <exclude>distro/**/concepts*demo.csv</exclude>
    ...
  <excludes>
```
