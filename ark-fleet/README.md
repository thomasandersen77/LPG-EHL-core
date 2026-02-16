# ark-fleet (Ansible)

Provisioning for a fleet of ARK 3360 devices running Debian Bookworm.

This repo is intentionally minimal: roles + inventories + sensible defaults. Extend it as your baseline grows.

## Quick start

- Run against **preprod** (default inventory in `ansible.cfg` points here, but this is explicit):

```bash
ansible-playbook -i inventories/preprod/hosts.yml site.yml
```

- Run against **prod**, limited to a single host:

```bash
ansible-playbook -i inventories/prod/hosts.yml site.yml -l ark-001
```

## Inventory layout

- `inventories/<env>/hosts.yml`: YAML inventory defining hostnames, `ansible_host` IPs, and basic connection vars.
- `inventories/<env>/group_vars/all.yml`: environment-wide defaults (timezone, app user/dirs, etc.).
- `inventories/<env>/group_vars/ark.yml`: fleet defaults for the `ark` group (base packages, journald limits).
- `inventories/<env>/host_vars/<host>.yml`: per-device overrides (e.g. `ark_hostname`, `ark_site_id`).

## Secrets

No secrets are committed here.

Use one of:
- **Ansible Vault** (recommended): add vaulted vars files and keep vault passwords out of git (see `.gitignore`).
- **External secret retrieval**: inject secrets at runtime from CI secret stores, SOPS, etc.

## Future: ansible-pull

We may later support devices self-provisioning via `ansible-pull` (or a systemd timer). This is **not implemented** in the current skeleton.

