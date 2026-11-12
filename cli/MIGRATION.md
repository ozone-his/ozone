# Migrating a child distro to the Ozone CLI (`meta/`) pattern

Audience: implementers of a child distro (e.g. ozone-c2c) currently using the
old scripts/-based setup: `scripts/start-ozone.sh`, `scripts/docker-compose-files.txt`,
a distro-specific `scripts/distro-<name>.env`, and `scripts/docker-compose-<x>-overrides.yml`
files. This guide maps each old mechanism to its new equivalent.

The new pattern, in one line: the distro declares `meta/selection.yml`,
`meta/vars.yml` and `meta/model-overrides.yml`; the build (`ozone resolve`,
wired via `maven-parent`) flattens the inheritance chain into
`meta/resolved/` plus `meta/resolved/info.yml`; deployment runs the `ozone`
command, which prints the exact `docker compose ...` command. See
`README.md` in this module for the full model.

## Old vs new

| Old mechanism | Old file | New equivalent |
|---|---|---|
| Start script | `scripts/start-ozone.sh` (run from `run/docker/scripts`) | `ozone --compose-root <run/docker> --distro <distro dir> --hostname <h> run` - prints the command to paste |
| Compose file list | `scripts/docker-compose-files.txt` | `meta/selection.yml` (apps + modes); the compose file chain and profiles are computed from the compose model |
| Distro env vars | `scripts/distro-<name>.env`, merged into `concatenated.env` at build time | `meta/vars.yml`, merged over the parent's `resolved/vars.yml` at build time |
| Compose override file | `scripts/docker-compose-<x>-overrides.yml`, appended to the `-f` chain | keep the file inside the distro and declare it in `meta/model-overrides.yml` (path relative to the distro dir); partial service tweaks (volumes, command, labels, extra env) stay in that compose file |
| Database init list | env list consumed by `data/mysql/create_db.sh` / `data/postgresql/create_db.sh` (`DB_INIT_LIST`) | the compose model's `db-init` per app; the CLI computes `DB_INIT_LIST` from the selection - no distro-side list needed |
| Distro identity | `ozone-info.json` patched by the build | `meta/resolved/info.yml`, written from the pom's artifactId / version / description at build time |

## Steps

1. **Pom**: use a `maven-parent` version that has the `meta` profile; declare
   your own `<name>`, `<version>` and `<description>` (the description is
   recorded in `info.yml` - if omitted, Maven inherits the parent's).
2. **Create `meta/` at the distro root** with:
   - `selection.yml` - the apps and modes you run (converted from
     `docker-compose-files.txt`; note the selection *replaces* the parent's
     wholesale - disabling a service means omitting it here).
   - `vars.yml` - your env values (converted from `distro-<name>.env`), in
     blocks mirroring the model (apps / integrations / modes / databases).
     `paths:` values are relative to the distro dir. Do not patch the
     `env-substitution` service to inject values - put them in `vars.yml`.
   - `model-overrides.yml` - compose additions only (additive; never removes).
     Reference your override compose files here with distro-relative paths.
3. **Move override compose files** (e.g. `docker-compose-bahmniemr-overrides.yml`)
   into the distro (e.g. under `compose/`) and reference them from
   `model-overrides.yml`. An overlay may not declare a file that belongs to the
   base compose model - that is a loud build error.
4. **Remove the old files**: `scripts/docker-compose-files.txt`,
   `scripts/*.env`, the start scripts. Keep `scripts/erpnext/` and
   `scripts/openmrs/` if you use them. `configs/`, `data/`, `binaries/` stay
   as they are.
5. **Build**: `mvn clean install`. The published zip then contains
   `distro/meta/` with your sources, `distro/meta/resolved/` (merged chain)
   and `distro/meta/resolved/info.yml` carrying *your* name/version/description.
   Nothing is generated in the source tree; `resolved/`, `.env`, `manifest.yml`
   are gitignored.
6. **Deploy**: from the unpacked artifact, put the CLI on your PATH and run
   the bundled launcher, then paste the printed multi-line command:
   `. ./bin/ozone-path.sh` (in fish: `source ./bin/ozone-path.fish`; or simply
   `export PATH="$PWD/bin:$PATH"`), then
   `ozone --compose-root run/docker --distro distro --hostname <host> run`.
   (`java -jar bin/cli.jar ...` works too.) `HOST_NAME` and `DISTRO_PATH` are
   provided by that command, never stored in files.

## Merge rules to know before editing vars or overrides

Maps merge recursively, lists are appended, scalars: child wins, type
conflicts fail the build loudly. At deployment the CLI reads only
`meta/resolved/` and `meta/selection.yml` - the source files are build inputs.

## Verifying

- Build succeeds and `target/<artifactId>-<version>/distro/meta/resolved/info.yml`
  shows your artifactId, version and description.
- `ozone ... validate` prints the selection check; `run` prints the full
  command with the expected `-f` chain and `--profile` list (compare against
  your old `docker-compose-files.txt`).
- `DB_INIT_LIST` behavior unchanged: databases initialize per the model's
  `db-init` of the selected apps.
