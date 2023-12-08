## (option 1) Try Ozone FOSS in Gitpod

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/ozone-his/ozone-distro)

It may take some time to setup Ozone for the first time, so hang tight :hourglass_flowing_sand:

When ready Gitpod will launch the tab for OpenMRS 3.

## (option 2) Try Ozone locally using the embedded Apache 2 proxy

```bash
git clone https://github.com/ozone-his/ozone-distro
mvn clean package
source target/go-to-scripts-dir.sh
./start-demo.sh
```



## (option 3) Try Ozone locally assuming Traefik is running on the host

```bash
git clone https://github.com/ozone-his/ozone-distro
mvn clean package
source target/go-to-scripts-dir.sh
export TRAEFIK="true"
./start-demo.sh
```

## Browse Ozone

Access each Ozone FOSS components at the following URL:

| HIS Component     | URL                            | Username | Password |
|-------------------|--------------------------------|----------|----------|
| OpenMRS 3         | http://localhost/openmrs/spa   | admin    | Admin123 |
| OpenMRS Legacy UI | http://localhost/openmrs       | admin    | Admin123 |
| SENAITE           | http://localhost:8081/senaite  | admin    | password |
| Odoo              | http://localhost:8069          | admin    | admin    |
| Superset          | http://localhost:8088          | admin    | password |

Ozone FOSS requires you to log into each component separately.

