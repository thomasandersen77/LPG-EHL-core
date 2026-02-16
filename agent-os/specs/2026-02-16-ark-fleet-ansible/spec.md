# Specification: ark-fleet Ansible Provisioning Skeleton

## Goal
Create a boring, conventional Ansible subrepo (`ark-fleet/`) that can provision a fleet of Debian Bookworm ARK 3360 devices with a minimal “works now” baseline that is easy to extend.

## User Stories
- As an operator, I want to run one playbook against preprod/prod inventories so that devices converge to a known baseline consistently.
- As an engineer, I want roles + vars laid out conventionally so that adding new provisioning steps is predictable and ansible-lint friendly.
- As an SRE, I want the baseline to be idempotent and safe (no lockouts, no secrets committed) so that repeated runs are low-risk.

## Specific Requirements

**Repository skeleton and developer UX**
- Create the exact folder tree under `ark-fleet/` as specified in `planning/requirements.md` (README, inventories, roles, scripts, gitignore).
- Provide a minimal `README.md` with concrete run commands for preprod and targeted prod runs.
- Document inventory conventions (`group_vars` vs `host_vars`) and the chosen secrets approach (Vault placeholders only).
- Include a small `scripts/bootstrap.sh` for first-touch host prep with prominent “do not commit keys/secrets” guidance.
- Ensure the repo is `ansible-lint` friendly by default (YAML style, idempotent modules, minimal `shell`).

**Ansible configuration (`ark-fleet/ansible.cfg`)**
- Set defaults: inventory points to `inventories/preprod/hosts.yml`, `remote_user` placeholder, `host_key_checking = False` with a comment that prod should manage known_hosts.
- Disable retry files and set Python interpreter discovery to `auto_silent`.
- Add SSH performance options (ControlMaster/ControlPersist, pipelining where appropriate) without hardcoding environment-specific paths.

**Inventory layout and variables**
- Implement YAML inventories for `inventories/preprod/hosts.yml` and `inventories/prod/hosts.yml`.
- Define an `ark` group containing all ARK hosts; define an environment group (`preprod`/`prod`) that includes `ark` as children.
- Provide example hosts with placeholder IPs (`ark-001`, `ark-002`, `ark-pp-001`) and include `ansible_become: true` in inventory vars.
- Add `group_vars/all.yml` and `group_vars/ark.yml` per environment, with no secrets and with clear placeholder comments where values are site-specific.

**Global defaults (`group_vars/all.yml`)**
- Provide defaults for timezone, admin/app users/groups, app directories, and a placeholder NIC name.
- Use variable names exactly as specified (`ark_app_user`, `ark_app_group`, `ark_app_base_dir`, etc.) to keep role interfaces stable.

**Fleet defaults (`group_vars/ark.yml`)**
- Define `ark_base_packages` exactly (curl, ca-certificates, jq, unzip, vim-tiny, htop, net-tools, iproute2, rsync, chrony).
- Define journald limit defaults (`SystemMaxUse`, `RuntimeMaxUse`) as variables consumed by the logging role.
- Include a brief comment on hostname conventions (domain/naming), without enforcing a domain in code.

**Host-specific configuration (`host_vars/*.yml`)**
- Provide example `host_vars` for at least `ark-001`, `ark-002`, and `ark-pp-001`.
- Include `ark_hostname`, `ark_site_id`, and `ark_static_ip` (empty placeholder) for prod examples.
- Ensure hostname values align with inventory hostnames to avoid confusion during targeting (`-l ark-001`).

**Top-level playbook (`ark-fleet/site.yml`)**
- Implement a single play targeting group `ark` with `become: true`.
- Run roles in order: `base`, `time`, `logging`, `hardening`, `java`, `app`.
- Add tags per role so operators can run partial convergence (e.g., `--tags logging`).

**Role: base**
- Update apt cache and install `ark_base_packages` idempotently.
- Create `ark_app_group` and `ark_app_user` as a system account, with consistent UID/GID behavior suitable for Debian.
- Create `ark_app_base_dir`, `ark_app_data_dir`, `ark_app_log_dir` with ownership set to the app user/group and mode `0755`.
- If `ark_hostname` is defined, set the system hostname and ensure `/etc/hosts` contains a `127.0.1.1` mapping for that hostname.
- Include a handler to restart `systemd-hostnamed` only when hostname changes warrant it.

**Role: time**
- Set timezone via the Ansible `timezone` module using the `timezone` var.
- Ensure `chrony` is enabled and running (install via packages if not already present due to base role).

**Role: logging**
- Manage `/etc/systemd/journald.conf` via a template that uses the journald limit variables.
- Restart `systemd-journald` only when the config template changes (handler-driven).

**Role: hardening (minimal and non-breaking)**
- Ensure OpenSSH server is installed and `ssh` service is running.
- Apply two safe SSH config lines via idempotent edits: `PermitRootLogin no` and `PasswordAuthentication yes`.
- Add an explicit TODO comment to disable password auth after keys are verified, to avoid accidental lockout.
- Restart the `ssh` service via handler only on config changes; include placeholders for future firewall work without implementing it.

**Role: java (placeholder)**
- Default `java_package` to `openjdk-17-jre-headless` in `defaults/main.yml`.
- Install `java_package` idempotently.

**Role: app (placeholder service)**
- Provide a systemd unit template for a placeholder service that runs as the app user and stays alive.
- Allow service name override via `ark_app_service_name` (default `arkapp`).
- Install the unit into `/etc/systemd/system/`, reload systemd, then enable + start the service idempotently.

## Visual Design
No visuals provided.

## Existing Code to Leverage

**Current `ark-fleet/` stub folder**
- `ark-fleet/` already exists in the repo with placeholder files and role directories (including an extra `roles/networking/` placeholder).
- Use this existing directory as the base location, but update/replace contents to match the required structure (add missing `preprod` inventory, role scaffolding files, templates, scripts, README, and `.gitignore`).
- Remove or ignore any extra placeholder roles/files not in scope for this skeleton (e.g., `roles/networking/`) unless explicitly required later.

## Out of Scope
- Implementing `ansible-pull` operation or wiring devices to self-update via pull.
- Any real application deployment beyond a placeholder systemd service template.
- Committing or managing secrets (Vault password files, private keys, tokens); only placeholders/comments allowed.
- Firewalling, fail2ban, CIS hardening, kernel tuning, or other potentially breaking security changes.
- Network configuration (static IP enforcement, NIC renaming, routing) beyond variable placeholders.
- Fleet monitoring/telemetry integrations (journald forwarding, log shipping, metrics agents).
- CI pipelines and release automation for the `ark-fleet/` subrepo.
