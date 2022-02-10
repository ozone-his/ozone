# Ozone HIS - The 'Distro' Project

## Overview
A **Maven project** that gathers all artefacts that make the **FOSS distribution of Ozone HIS**.

It depends on the OpenMRS 3 distro Ref App
```xml
<dependency>
  <groupId>org.openmrs.distro</groupId>
  <artifactId>referenceapplication-package</artifactId>
  <type>zip</type>
</dependency>
```
and then _augment_ OpenMRS 3 with all components that make Ozone FOSS an integrated health information system.

Building it fecthes and brings in one place all Ozone's artifacts, simply run:
```
mvn clean package
```

## Trying and Running Ozone HIS
Check [ozone-docker](https://github.com/ozone-his/ozone-docker).

## Find Us
[Website](http://ozone-his.com) - [Forum](https://talk.openmrs.org/c/software/ozone-his) - [Slack](https://openmrs.slack.com/archives/C02PYQD5D0A)
