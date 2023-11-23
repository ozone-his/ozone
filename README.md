# Ozone - The 'Distro' Project
>_The enterprise-grade health information system that augments OpenMRS 3._

This is the official Maven project for the free and open-source software flavour of Ozone, aka **Ozone FOSS**.
This Maven project makes it easy to build and package Ozone FOSS.

:bulb: **Did you know?** There is a *pro* flavour of Ozone, aka **Ozone Pro**, that adds a number of enterprise features to Ozone FOSS.

## 1. Overview
A **Maven project** that gathers all artefacts that make the **FOSS distribution of Ozone**.

It depends on the OpenMRS 3 distro Ref App
```xml
<dependency>
  <groupId>org.openmrs.distro</groupId>
  <artifactId>referenceapplication-package</artifactId>
  <type>zip</type>
</dependency>
```
and then _augments_ OpenMRS 3 with all components that make Ozone FOSS a fully integrated health information system.

## 2. Try Ozone FOSS in Gitpod
[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/ozone-his/ozone-distro)

It may take some time to setup Ozone for the first time, so hang tight :hourglass_flowing_sand:

When ready Gitpod will launch the tab for OpenMRS 3.

## 2. Try Ozone locally

```bash
git clone https://github.com/ozone-his/ozone-distro
mvn clean package
source target/go-to-scripts-dir.sh
./start-demo.sh
```

Access each Ozone FOSS components at the following URL:

| HIS Component     | URL                            | Username | Password |
|-------------------|--------------------------------|----------|----------|
| OpenMRS 3         | http://localhost/openmrs/spa   | admin    | Admin123 |
| OpenMRS Legacy UI | http://localhost/openmrs       | admin    | Admin123 |
| SENAITE           | http://localhost:8081/senaite  | admin    | password |
| Odoo              | http://localhost:8069          | admin    | admin    |
| Superset          | http://localhost:8088          | admin    | password |

Ozone FOSS requires you to log into each component separately.

## 3. Find us
[Slack](https://openmrs.slack.com/archives/C02PYQD5D0A) - [Forum](https://talk.openmrs.org/c/software/ozone-his) - [Website](http://ozone-his.com)

<sub>:information_source: Self sign-up [here](https://slack.openmrs.org/) before accessing our Slack space for the first time.</sub>
## 4. Report an issue
1. Either start a conversation on [Slack](https://openmrs.slack.com/archives/C02PYQD5D0A) about it,
1. Or start a thread on our [forum](https://talk.openmrs.org/c/software/ozone-his) about it.
