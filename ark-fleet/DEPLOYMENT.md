# ARK Fleet Deployment Guide

This guide walks you through deploying the ARK 3360 configuration to new devices.

## Prerequisites

1. **Control Machine** (where you run Ansible):
   - Ansible 2.9 or later installed
   - SSH access to target devices
   - Python 3.x

2. **Target Device** (ARK 3360):
   - Fresh Debian 12 (Bookworm) installation
   - SSH server running
   - Network connectivity
   - A user account with sudo access for initial setup

## Initial Device Setup

Before running Ansible, ensure the target device has:

1. A base Debian 12 installation
2. SSH server installed and running
3. A temporary admin account (will be replaced by managed users)
4. Network connectivity to your control machine

```bash
# On the target device (if needed)
apt update
apt install openssh-server sudo
systemctl enable ssh
systemctl start ssh

# Add your temporary user to sudo group
usermod -aG sudo <your-temp-user>
```

## Step 1: Configure Inventory

1. Edit the inventory file for your environment:

```bash
vim inventories/preprod/hosts.yml
```

2. Add your device(s):

```yaml
---
all:
  vars:
    ansible_become: true
    # For initial setup, use a temporary user with sudo access
    ansible_user: <your-temp-user>
    # After initial deployment, change to: arkadmin

  children:
    preprod:
      children:
        ark:
          hosts:
            ark-pp-001:
              ansible_host: 192.168.1.101
              ark_hostname: ark-pp-001
            ark-pp-002:
              ansible_host: 192.168.1.102
              ark_hostname: ark-pp-002
```

## Step 2: Configure SSH Keys

The ARK fleet uses three admin accounts:
- **arkadmin** - Shared account for developers (primary account for Ansible)
- **alejandro** - Individual admin account
- **thomas** - Individual admin account

### Configure arkadmin Shared Account

The `arkadmin` account allows multiple developers to connect using their individual SSH keys:

```bash
# Developer 1: Add your SSH public key
cp ~/.ssh/id_rsa.pub roles/users/files/ssh-keys/arkadmin-dev1.pub

# Developer 2: Add your SSH public key
cp ~/.ssh/id_rsa.pub roles/users/files/ssh-keys/arkadmin-dev2.pub
```

### Configure Individual Admin Accounts (Optional)

```bash
# Copy SSH public keys for individual accounts
cp ~/.ssh/id_rsa.pub roles/users/files/ssh-keys/alejandro.pub
cp /path/to/thomas/key.pub roles/users/files/ssh-keys/thomas.pub
```

### Verify Keys

```bash
ls -l roles/users/files/ssh-keys/
# Should show: arkadmin-dev1.pub, arkadmin-dev2.pub, and optionally alejandro.pub, thomas.pub
```

## Step 3: Verify Connectivity

Test SSH connectivity to your devices:

```bash
ansible -i inventories/preprod/hosts.yml ark -m ping
```

Expected output:
```
ark-pp-001 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

## Step 4: Run the Playbook

### Full Deployment

Deploy the complete configuration:

```bash
ansible-playbook -i inventories/preprod/hosts.yml site.yml
```

### Selective Deployment with Tags

Deploy specific components:

```bash
# Only configure networking
ansible-playbook -i inventories/preprod/hosts.yml site.yml --tags networking

# Only set up users
ansible-playbook -i inventories/preprod/hosts.yml site.yml --tags users

# Configure base system and users
ansible-playbook -i inventories/preprod/hosts.yml site.yml --tags base,users
```

### Deploy to Specific Hosts

Target a single device:

```bash
ansible-playbook -i inventories/preprod/hosts.yml site.yml -l ark-pp-001
```

## Step 5: Verify Deployment

After deployment, verify the configuration:

```bash
# SSH as the arkadmin shared user (both developers can use this)
ssh arkadmin@192.168.1.101

# Or SSH as individual users
ssh alejandro@192.168.1.101
ssh thomas@192.168.1.101

# Verify system information
hostnamectl
timedatectl
locale

# Check network interfaces
ip addr show

# Verify services
systemctl status systemd-networkd
systemctl status systemd-timesyncd
systemctl status dnsmasq

# Check installed packages
dpkg -l | grep -E "temurin|dotnet|mono|hdparm"

# Verify storage hardening
sudo tune2fs -l /dev/sda1 | grep "Errors behavior"
sudo hdparm -W /dev/sda

# Check filesystem mount options
findmnt -no OPTIONS /
```

## Common Deployment Scenarios

### Scenario 1: New Device Provisioning

```bash
# 1. Add device to inventory
# 2. Run full playbook
ansible-playbook -i inventories/preprod/hosts.yml site.yml -l ark-pp-003
```

### Scenario 2: Update Existing Device

```bash
# Update specific role only
ansible-playbook -i inventories/preprod/hosts.yml site.yml --tags base -l ark-pp-001
```

### Scenario 3: Network Reconfiguration

```bash
# Update network settings in inventory, then:
ansible-playbook -i inventories/preprod/hosts.yml site.yml --tags networking
```

## Troubleshooting

### SSH Connection Issues

```bash
# Test connectivity
ansible -i inventories/preprod/hosts.yml ark-pp-001 -m ping -vvv

# Check SSH config
ssh -v alejandro@192.168.1.101
```

### Package Installation Failures

```bash
# Run with verbose output
ansible-playbook -i inventories/preprod/hosts.yml site.yml --tags base -vvv

# Check repository configuration on target
ssh alejandro@192.168.1.101
apt update
apt-cache policy
```

### Service Start Failures

```bash
# Check service status on target
systemctl status <service-name>
journalctl -u <service-name> -n 50
```

## Post-Deployment

After successful deployment:

### 1. Switch to arkadmin for Future Deployments

Update the inventory to use `arkadmin` instead of the temporary user:

```bash
vim inventories/preprod/group_vars/all.yml
```

Change:
```yaml
ansible_user: arkadmin  # Already set by default
```

Or in hosts.yml, remove the temporary user override:
```yaml
all:
  vars:
    ansible_become: true
    # ansible_user defaults to arkadmin from group_vars/all.yml
```

### 2. Remove Temporary User (Optional)

If you created a temporary admin user for initial setup:

```bash
ssh arkadmin@192.168.1.101
sudo deluser --remove-home <temp-user>
```

### 3. Configure SSH Client

Update your SSH config for convenience:

```bash
# ~/.ssh/config
Host ark-pp-*
    User arkadmin
    IdentityFile ~/.ssh/id_rsa

Host ark-prod-*
    User arkadmin
    IdentityFile ~/.ssh/id_rsa
```

Now you can connect with just: `ssh ark-pp-001`

### 4. Document the Device

Document the device in your asset management system with:
- Hostname
- IP address
- Admin users (arkadmin, alejandro, thomas)
- Deployment date

## Customization

### Adding More Packages

Edit `inventories/<env>/group_vars/ark.yml`:

```yaml
ark_base_packages:
  - existing-package
  - your-new-package
```

### Changing Network Configuration

Edit per-host variables in `inventories/<env>/host_vars/<hostname>.yml`:

```yaml
ark_primary_interface: enp0s25
ark_primary_dhcp: false
ark_primary_address: 192.168.1.100/24
ark_primary_gateway: 192.168.1.1
ark_primary_dns: 8.8.8.8
```

### Adding More Admin Users

Edit `inventories/<env>/group_vars/ark.yml` or role defaults:

```yaml
ark_admin_users:
  - name: newuser
    comment: "New User"
    groups: ["sudo"]
    shell: /bin/bash
```

Don't forget to add their SSH key to `roles/users/files/ssh-keys/newuser.pub`.

## Security Considerations

1. **SSH Keys**: Always use SSH key authentication, disable password authentication
2. **Ansible Vault**: Use Ansible Vault for sensitive variables
3. **Sudo Access**: Review and audit sudo configurations regularly
4. **Firewall**: Configure firewall rules in the hardening role
5. **Updates**: Regularly update packages and rerun playbooks

## Further Reading

- [Ansible Documentation](https://docs.ansible.com/)
- [Debian Administration Guide](https://www.debian.org/doc/manuals/debian-handbook/)
- [systemd-networkd Documentation](https://www.freedesktop.org/software/systemd/man/systemd.network.html)
