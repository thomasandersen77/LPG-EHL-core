# ark-fleet (Ansible)

Provisioning for a fleet of ARK 3360 devices running Debian Bookworm.

This Ansible setup replicates the configuration of a reference ARK 3360 device, including system packages, networking, users, and services. The reference configuration is captured in `reference/` directory.

## Reference Configuration

The Ansible playbooks are based on a complete capture of a working ARK 3360 device. See [reference/REFERENCE.md](reference/REFERENCE.md) for detailed information about the reference system.

## Quick start

For detailed deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md).

For **zero-touch deployment** without keyboard/monitor access, see [docs/BOOTSTRAP-DEPLOYMENT.md](docs/BOOTSTRAP-DEPLOYMENT.md).

**Quick deploy to preprod:**

```bash
# Add your SSH keys for the arkadmin shared account
# Developer 1:
cp ~/.ssh/id_rsa.pub roles/users/files/ssh-keys/arkadmin-dev1.pub
# Developer 2:
cp ~/.ssh/id_rsa.pub roles/users/files/ssh-keys/arkadmin-dev2.pub

# Edit inventory to add your devices
vim inventories/preprod/hosts.yml

# Run the playbook
ansible-playbook -i inventories/preprod/hosts.yml site.yml
```

**Run against prod, limited to a single host:**

```bash
ansible-playbook -i inventories/prod/hosts.yml site.yml -l ark-001
```

**Run specific roles only:**

```bash
ansible-playbook -i inventories/preprod/hosts.yml site.yml --tags users,networking
```

## Roles

The playbook includes the following roles (in execution order):

1. **base**: System packages, hostname, application user/group setup
2. **users**: Admin user accounts (alejandro, thomas, arkadmin) with sudo access
3. **networking**: systemd-networkd configuration for primary and terminal interfaces
4. **time**: Timezone and NTP configuration (systemd-timesyncd)
5. **logging**: journald configuration
6. **hardening**: Security hardening (SSH, storage/filesystem, locale)
7. **java**: Java runtime installation
8. **app**: Application-specific setup

## Inventory layout

- `inventories/<env>/hosts.yml`: YAML inventory defining hostnames, `ansible_host` IPs, and basic connection vars.
- `inventories/<env>/group_vars/all.yml`: environment-wide defaults (timezone, app user/dirs, etc.).
- `inventories/<env>/group_vars/ark.yml`: fleet defaults for the `ark` group (base packages, networking, journald limits).
- `inventories/<env>/host_vars/<host>.yml`: per-device overrides (e.g. `ark_hostname`, `ark_site_id`).

## SSH Keys

To deploy SSH keys for admin users:

1. Place public keys in `roles/users/files/ssh-keys/<username>.pub`
2. Example: `roles/users/files/ssh-keys/alejandro.pub`
3. The keys will be deployed to `/home/<username>/.ssh/authorized_keys`

**Important**: Only store public keys. Never commit private keys.

## Secrets

No secrets are committed here.

Use one of:
- **Ansible Vault** (recommended): add vaulted vars files and keep vault passwords out of git (see `.gitignore`).
- **External secret retrieval**: inject secrets at runtime from CI secret stores, SOPS, etc.

## Users and Permissions

The ARK system uses multiple user accounts for security and separation of concerns:

- **arkadmin** - Shared admin account for developers (SSH + sudo)
- **alejandro, thomas** - Individual admin accounts (SSH + sudo)
- **appuser** - Application runtime user (no sudo, has serial port access)
- **arkapp** - System account for file ownership

See [docs/USERS-AND-PERMISSIONS.md](docs/USERS-AND-PERMISSIONS.md) for detailed information.

## Key Configuration Variables

See `inventories/preprod/group_vars/ark.yml` for the complete list. Key variables include:

- `ark_base_packages`: List of system packages to install
- `ark_primary_interface`: Primary network interface (default: enp0s25)
- `ark_terminal_interface`: Secondary/terminal interface (default: ens32)
- `ark_terminal_network`: Terminal network address (default: 192.168.50.1/24)
- `timezone`: System timezone (default: US/Eastern)
- `ark_admin_users`: List of admin users to create
- `ark_app_user_name`: Application runtime user (default: appuser)
- `ark_disable_write_cache`: Disable disk write cache for data integrity (default: true)
- `ark_root_device`: Root filesystem device for hardening (default: /dev/sda1)

## Future: ansible-pull

We may later support devices self-provisioning via `ansible-pull` (or a systemd timer). This is **not implemented** in the current skeleton.

