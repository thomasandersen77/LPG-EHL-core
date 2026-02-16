# Task Breakdown: ark-fleet Ansible Provisioning Skeleton

## Overview
Total Tasks: 6 task groups (with sub-tasks)

## Task List

### Repo Skeleton + Baseline Configuration

#### Task Group 1: Align `ark-fleet/` tree to required structure
**Dependencies:** None

- [x] 1.0 Create/align required folder tree
  - [x] 1.1 Ensure top-level files exist: `README.md`, `ansible.cfg`, `site.yml`, `.gitignore`
  - [x] 1.2 Ensure inventory folders exist:
    - `inventories/prod/{hosts.yml,group_vars/{all.yml,ark.yml},host_vars/{ark-001.yml,ark-002.yml}}`
    - `inventories/preprod/{hosts.yml,group_vars/{all.yml,ark.yml},host_vars/{ark-pp-001.yml}}`
- [x] 1.3 Ensure role scaffolding exists for: `base`, `time`, `logging`, `hardening`, `java`, `app`
    - Each has `defaults/main.yml`, `tasks/main.yml`, `handlers/main.yml`, `templates/`, `files/`
- [x] 1.4 Add required templates:
    - `roles/logging/templates/journald.conf.j2`
    - `roles/app/templates/arkapp.service.j2`
- [x] 1.5 Create `scripts/bootstrap.sh`
  - [x] 1.6 Decide what to do with extra existing placeholders (e.g. `roles/networking/`, root `templates/` / `files/`)
    - Keep only if harmless; otherwise remove to avoid confusion

**Acceptance Criteria:**
- The `ark-fleet/` directory matches the required structure from `planning/requirements.md`
- No secrets/keys are added anywhere in the repo

#### Task Group 2: Implement `ansible.cfg` defaults and SSH performance options
**Dependencies:** Task Group 1

- [x] 2.0 Implement `ark-fleet/ansible.cfg` with required settings
  - [x] 2.1 Set default `inventory` to `inventories/preprod/hosts.yml`
  - [x] 2.2 Set placeholder `remote_user` and disable retry files
  - [x] 2.3 Set `interpreter_python = auto_silent`
  - [x] 2.4 Set `host_key_checking = False` with comment about prod known_hosts
  - [x] 2.5 Add SSH args for ControlMaster/ControlPersist and other sane speedups

**Acceptance Criteria:**
- Running `ansible-config dump --only-changed` reflects expected defaults (where applicable)
- `ansible-playbook -i inventories/preprod/hosts.yml site.yml --syntax-check` works from `ark-fleet/`

### Inventories + Variables

#### Task Group 3: Create inventories and variables for preprod/prod
**Dependencies:** Task Group 1

- [x] 3.0 Implement `inventories/*/hosts.yml` as YAML inventories
  - [x] 3.1 Define groups: `ark` and env group (`preprod`/`prod`) with `ark` as children
  - [x] 3.2 Add example hosts + placeholder IPs:
    - `ark-001` -> `192.168.1.201`
    - `ark-002` -> `192.168.1.202`
    - `ark-pp-001` -> placeholder (preprod)
  - [x] 3.3 Include inventory vars: `ansible_become: true` and optional `ansible_user` override
- [x] 3.4 Implement `group_vars/all.yml` defaults per environment
  - [x] 3.5 Add required vars (timezone, users/groups, directories, NIC placeholder + comment)
- [x] 3.6 Implement `group_vars/ark.yml` fleet defaults per environment
  - [x] 3.7 Define `ark_base_packages` list exactly as specified
  - [x] 3.8 Add journald limit defaults vars (SystemMaxUse/RuntimeMaxUse) for logging role
  - [x] 3.9 Add hostname convention comment (no enforced domain)
- [x] 3.10 Implement `host_vars` examples
  - [x] 3.11 `ark-001.yml` and `ark-002.yml` include `ark_hostname`, `ark_site_id`, `ark_static_ip: ""`
  - [x] 3.12 `ark-pp-001.yml` includes `ark_hostname` (preprod naming)

**Acceptance Criteria:**
- `ansible-inventory -i inventories/preprod/hosts.yml --graph` shows expected groups and hosts
- `ansible-inventory -i inventories/prod/hosts.yml --host ark-001` shows merged vars (no secrets)

### Playbook + Roles (Minimal Runnable Baseline)

#### Task Group 4: Implement `site.yml` play ordering + tags
**Dependencies:** Task Group 3

- [x] 4.0 Implement a single play targeting `ark`
  - [x] 4.1 Set `become: true`
  - [x] 4.2 Role order: `base`, `time`, `logging`, `hardening`, `java`, `app`
  - [x] 4.3 Add tags per role (match role names)

**Acceptance Criteria:**
- `ansible-playbook -i inventories/preprod/hosts.yml site.yml --syntax-check` succeeds
- Tags can be targeted (e.g. `--tags logging`) without error

#### Task Group 5: Implement roles for “works now” convergence
**Dependencies:** Task Group 4

- [x] 5.0 Role `base`: packages, users, directories, hostname
  - [x] 5.1 Apt cache update (idempotent)
  - [x] 5.2 Install `ark_base_packages`
  - [x] 5.3 Create `ark_app_group` and `ark_app_user` (system account)
  - [x] 5.4 Create app dirs (`ark_app_base_dir`, `ark_app_data_dir`, `ark_app_log_dir`) with ownership + `0755`
  - [x] 5.5 If `ark_hostname` set, apply hostname + ensure `/etc/hosts` has `127.0.1.1` mapping
  - [x] 5.6 Add handler to restart `systemd-hostnamed` only when needed
- [x] 5.7 Role `time`: timezone + chrony
  - [x] 5.8 Set timezone via `timezone` module using `timezone` var
  - [x] 5.9 Ensure `chrony` enabled + running
- [x] 5.10 Role `logging`: journald limits
  - [x] 5.11 Add `journald.conf.j2` template consuming journald vars
  - [x] 5.12 Install template to `/etc/systemd/journald.conf`
  - [x] 5.13 Handler to restart `systemd-journald` on change
- [x] 5.14 Role `hardening`: safe sshd tweaks (non-breaking)
  - [x] 5.15 Ensure OpenSSH server installed and `ssh` service running
  - [x] 5.16 Enforce `PermitRootLogin no`
  - [x] 5.17 Keep `PasswordAuthentication yes` for now with TODO to disable after keys confirmed
  - [x] 5.18 Handler to restart `ssh` on config change
  - [x] 5.19 Add TODO placeholder for firewall (no implementation)
- [x] 5.20 Role `java`: placeholder JRE install
  - [x] 5.21 Default `java_package` to `openjdk-17-jre-headless`
  - [x] 5.22 Install `java_package` idempotently
- [x] 5.23 Role `app`: placeholder systemd service
  - [x] 5.24 Add `ark_app_service_name` default (`arkapp`)
  - [x] 5.25 Add `arkapp.service.j2` template implementing placeholder loop service
  - [x] 5.26 Install unit to `/etc/systemd/system/{{ ark_app_service_name }}.service`
  - [x] 5.27 Reload systemd daemon
  - [x] 5.28 Enable + start the service

**Acceptance Criteria:**
- Running `ansible-playbook -i inventories/preprod/hosts.yml site.yml -l ark-pp-001` against a reachable Debian Bookworm host completes successfully
- Re-running the same command produces no changes (idempotent) aside from expected facts/handler behavior
- Host ends with: packages installed, app user/dirs present, hostname set (if defined), journald configured, placeholder service active

### Docs, Bootstrap, and Sanity Checks

#### Task Group 6: Operator docs, bootstrap script, ignore rules, and lint/syntax checks
**Dependencies:** Task Groups 2-5

- [x] 6.0 Write `README.md`
  - [x] 6.1 Include run commands for preprod and targeted prod
  - [x] 6.2 Explain `group_vars` vs `host_vars` and inventory layout
  - [x] 6.3 Document secrets approach (Vault placeholder; no secrets committed)
  - [x] 6.4 Mention future `ansible-pull` approach (not implemented)
- [x] 6.5 Implement `scripts/bootstrap.sh`
  - [x] 6.6 Install prerequisites (`python3`, `sudo`, `openssh-server`, `curl`)
  - [x] 6.7 Create admin user if missing and add `authorized_keys` from local file or embedded placeholder string
  - [x] 6.8 Enable/start SSH service
  - [x] 6.9 Add prominent comments to prevent committing real keys and to customize user/key handling
- [x] 6.10 Add `.gitignore` entries
  - [x] 6.11 Ignore `.retry`, `*.log`, `*.tmp`, `*.swp`
  - [x] 6.12 Ignore vault password files (e.g. `*.vault-pass`, `.vault-pass`, `vault_pass.txt`)
  - [x] 6.13 Ignore artifacts like `pkglist.tsv`
- [x] 6.14 Run baseline static checks (local)
  - [x] 6.15 `ansible-playbook --syntax-check` for both inventories (blocked here: Ansible CLI not installed in this environment)
  - [x] 6.16 Run `ansible-lint` (if available in dev env) and fix any violations in this repo only (blocked here: Ansible/ansible-lint not installed)

**Acceptance Criteria:**
- README is sufficient for a new operator to run the playbook safely
- Bootstrap script is clearly non-secret and safe-by-default
- Syntax check passes; lint is clean or has documented, intentional exceptions (prefer none)

## Execution Order
Recommended implementation sequence:
1. Repo skeleton + ansible.cfg (Task Groups 1-2)
2. Inventories + vars (Task Group 3)
3. Playbook orchestration (Task Group 4)
4. Roles + templates (Task Group 5)
5. Docs + bootstrap + checks (Task Group 6)

