# Requirements: ark-fleet Ansible subrepo skeleton

## Goal
Create an Ansible subrepo (`ark-fleet/`) for provisioning a fleet of ~50 ARK 3360 devices running Debian Bookworm.

This is the **initial skeleton only**: conventional folder structure + minimal runnable playbook + role scaffolding + basic defaults.

## Constraints / assumptions
- Ansible control plane: run from laptop/CI (SSH) or later via `ansible-pull` (not implemented in this spec).
- Targets: Debian Bookworm; SSH reachable; `python3` available (or bootstrapped separately).
- Use **roles**, `group_vars`, `host_vars`.
- **No secrets committed**. Provide placeholders/comments for Ansible Vault or external secret retrieval.
- YAML and `ansible-lint` friendly. Prefer idempotent Ansible modules; avoid `shell` unless truly needed.

## Required folder structure (must match)
`ark-fleet/`
- `README.md`
- `ansible.cfg`
- `site.yml`
- `inventories/`
  - `prod/`
    - `hosts.yml`
    - `group_vars/`
      - `ark.yml`
      - `all.yml`
    - `host_vars/`
      - `ark-001.yml`
      - `ark-002.yml`
  - `preprod/`
    - `hosts.yml`
    - `group_vars/`
      - `ark.yml`
      - `all.yml`
    - `host_vars/`
      - `ark-pp-001.yml`
- `roles/`
  - `base/` (`defaults/main.yml`, `tasks/main.yml`, `handlers/main.yml`, `templates/`, `files/`)
  - `hardening/` (same structure)
  - `time/` (same structure)
  - `java/` (same structure)
  - `app/` (same structure + `templates/arkapp.service.j2`)
  - `logging/` (same structure + `templates/journald.conf.j2`)
- `scripts/`
  - `bootstrap.sh`
- `.gitignore`

## Content requirements (high level)
### README.md
- Explain how to run:
  - `ansible-playbook -i inventories/preprod/hosts.yml site.yml`
  - `ansible-playbook -i inventories/prod/hosts.yml site.yml -l ark-001`
- Explain inventory layout (`group_vars` vs `host_vars`).
- Mention secrets approach (Ansible Vault placeholder).
- Mention future `ansible-pull` approach (do not implement yet).

### ansible.cfg
- Defaults:
  - `inventory = inventories/preprod/hosts.yml`
  - `remote_user = alejandro` (placeholder)
  - `host_key_checking = False` (comment that production should manage known_hosts)
  - `retry_files_enabled = False`
  - `interpreter_python = auto_silent`
- Set sane SSH args for performance (e.g., ControlMaster auto).

### inventories/*/hosts.yml
- YAML inventory.
- Define groups:
  - `ark` (all ARKs)
  - an environment group (`preprod` or `prod`) that includes `ark` as children
- Example hosts with `ansible_host` placeholders:
  - `ark-001` -> `192.168.1.201`
  - `ark-002` -> `192.168.1.202`
- Include vars:
  - `ansible_user` (optional override)
  - `ansible_become: true`

### group_vars/all.yml
- Global defaults:
  - `timezone: Europe/Oslo`
  - `ark_admin_user: alejandro` (placeholder)
  - `ark_app_user: arkapp`
  - `ark_app_group: arkapp`
  - `ark_app_base_dir: /opt/arkapp`
  - `ark_app_data_dir: /var/lib/arkapp`
  - `ark_app_log_dir: /var/log/arkapp`
  - `ark_nic_name: enp0s25` (placeholder, comment)

### group_vars/ark.yml
- Fleet-wide packages and baseline settings:
  - `ark_base_packages`: `[curl, ca-certificates, jq, unzip, vim-tiny, htop, net-tools, iproute2, rsync, chrony]`
  - journald limits defaults (`SystemMaxUse`, `RuntimeMaxUse`)
  - comment about hostname domain/naming convention
- No secrets.

### host_vars examples
- `ark-001.yml`:
  - `ark_hostname: ark-001`
  - `ark_site_id: "site-oslo-01"`
  - `ark_static_ip: ""` (empty placeholder)
- `ark-002.yml` similar
- Preprod hosts use different names (e.g., `ark-pp-001.yml`).

### site.yml
- One play targeting group `ark`
- `become: true`
- Roles in order: `base`, `time`, `logging`, `hardening`, `java`, `app`
- Tags per role.

## Role behavior requirements
### roles/base/tasks/main.yml
- Ensure apt cache update.
- Install `ark_base_packages`.
- Create `ark_app_group` + `ark_app_user` as a system account.
- Create directories: `ark_app_base_dir`, `ark_app_data_dir`, `ark_app_log_dir` with correct ownership and `0755`.
- Set hostname if `ark_hostname` is defined:
  - Use `hostname` module
  - Ensure `/etc/hosts` has `127.0.1.1` mapping for the hostname (via `lineinfile`)
- Handler: restart `systemd-hostnamed` if needed.

### roles/time/tasks/main.yml
- Set timezone via `timezone` module.
- Ensure `chrony` service enabled and running (package may already be installed via base packages).

### roles/logging
- Template `/etc/systemd/journald.conf` based on vars (limits from `group_vars/ark.yml`).
- Notify handler to restart `systemd-journald`.

### roles/hardening/tasks/main.yml (minimal, non-breaking)
- Ensure `sshd` is installed.
- Set `sshd_config` basics via `lineinfile` without locking access out:
  - `PermitRootLogin no`
  - `PasswordAuthentication yes` for now, with TODO to disable after SSH keys are confirmed
- Restart `ssh` service via handler.
- TODO placeholder for firewall later.

### roles/java/tasks/main.yml
- Placeholder role:
  - Install `openjdk-17-jre-headless` (default)
  - Variable `java_package` in `defaults/main.yml`

### roles/app
- Install placeholder systemd unit from template to `/etc/systemd/system/arkapp.service` (service name configurable via `ark_app_service_name`, default `arkapp`).
- `daemon-reload`.
- Enable and start the service.
- Template details (conceptual):
  - Description indicates placeholder
  - Runs as `ark_app_user`
  - `WorkingDirectory` is `ark_app_base_dir`
  - `ExecStart` runs a long-lived placeholder loop
  - `Restart=always`, `RestartSec=3`

## scripts/bootstrap.sh
- Bootstrap a fresh Debian host:
  - Install `python3`, `sudo`, `openssh-server`, `curl`
  - Create an admin user (if missing) and install `authorized_keys` either from a local file next to script or an embedded placeholder string
  - Enable SSH
- Prominent comments: adjust admin user/key handling; do not commit real keys.

## .gitignore
- Ignore `.retry`, `*.log`, `*.tmp`, `*.swp`
- Ignore vault password files
- Ignore artifacts like `pkglist.tsv`

## Acceptance criteria
Running `ansible-playbook` against a reachable Debian Bookworm host in the `ark` group must:
- Install the base packages
- Create the application user and directories
- Set hostname from `host_vars`
- Configure journald limits
- Install and start the placeholder systemd service
- Complete without failures

