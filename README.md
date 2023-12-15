# Ozone

> The entreprise-grade health information system that augments OpenMRS 3.

# Quick start

## (option 1) Try Ozone FOSS in Gitpod

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/ozone-his/ozone-distro)

It may take some time to setup Ozone for the first time, so hang tight :hourglass_flowing_sand:

When ready Gitpod will launch the tab for OpenMRS 3.

## (option 2) Try Ozone locally using the embedded Apache 2 proxy

```bash
git clone https://github.com/ozone-his/ozone-distro
scripts/mvnw clean package
source target/go-to-scripts-dir.sh
./start-demo.sh
```

## (option 3) Try Ozone locally assuming Traefik is running on the host

```bash
git clone https://github.com/ozone-his/ozone-distro
scripts/mvnw clean package
source target/go-to-scripts-dir.sh
export TRAEFIK="true"
./start-demo.sh
```

## Browse Ozone

Once complete, the startup script will output the URLs to access the services in the terminal.

For example:
![Access Ozone](./readme/browse.png)
Ozone FOSS requires you to log into each component separately.

💡 **Did you know?** Ozone Pro comes with single sign-on (SSO) and all its integration layer is secured with OAuth2.
