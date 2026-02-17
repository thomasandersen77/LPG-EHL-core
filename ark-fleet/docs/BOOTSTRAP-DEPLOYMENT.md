# Bootstrap Deployment - Zero-Touch SSH Access

This guide explains how to deploy ARK 3360 devices with SSH access pre-configured, eliminating the need for keyboard/monitor access to each device.

## Problem Statement

**Challenge**: How to provision multiple ARK 3360 devices without physical keyboard/monitor access to each device?

**Solution**: Pre-configure SSH keys during OS installation so devices are immediately accessible over the network.

## Deployment Approaches

### Approach 1: Automated Installation with Preseed (Recommended)

Use Debian preseed to fully automate OS installation including SSH keys.

**Advantages**:
- Completely automated installation
- No manual steps required per device
- Consistent configuration across fleet
- Can be deployed via PXE boot or USB

**See**: [preseed.cfg](../files/preseed.cfg) for full configuration

### Approach 2: Golden Image with Pre-configured SSH

Create a master OS image with SSH keys already configured, then clone to devices.

**Advantages**:
- Fastest deployment method
- Guaranteed identical configuration
- Can include all base packages

**See**: [Creating a Golden Image](#creating-a-golden-image) below

### Approach 3: Bootstrap Script on First Boot

Run a bootstrap script on first boot (via USB stick or first-boot script).

**Advantages**:
- Works with standard Debian installation
- Simple to understand and modify
- Good for small deployments

**See**: [scripts/bootstrap-ssh-access.sh](../scripts/bootstrap-ssh-access.sh)

## Method 1: Preseed Automated Installation

### Step 1: Prepare SSH Keys

Extract your SSH public keys:

```bash
# Developer 1
cat ~/.ssh/id_rsa.pub
# Copy the output

# Developer 2
cat ~/.ssh/id_rsa.pub
# Copy the output
```

### Step 2: Customize Preseed File

Edit [files/preseed.cfg](../files/preseed.cfg) and add your SSH keys:

```bash
cd ark-fleet/files
cp preseed-template.cfg preseed.cfg

# Edit the late_command section
vim preseed.cfg
```

Replace the SSH key placeholders:
```
echo "ssh-rsa AAAAB3... dev1@laptop" > /target/home/arkadmin/.ssh/authorized_keys;
echo "ssh-rsa AAAAB3... dev2@laptop" >> /target/home/arkadmin/.ssh/authorized_keys;
```

### Step 3: Create Installation Media

**Option A: USB Installation**

```bash
# Download Debian 12 netinst ISO
wget https://cdimage.debian.org/debian-cd/current/amd64/iso-cd/debian-12.x.x-amd64-netinst.iso

# Mount ISO and extract
mkdir -p /tmp/debian-iso
sudo mount -o loop debian-12.x.x-amd64-netinst.iso /tmp/debian-iso

# Copy to working directory
mkdir -p /tmp/debian-custom
cp -rT /tmp/debian-iso /tmp/debian-custom

# Add preseed file
cp preseed.cfg /tmp/debian-custom/preseed.cfg

# Modify boot config to use preseed
cat >> /tmp/debian-custom/isolinux/isolinux.cfg <<EOF
label autopreseed
  menu label ^Automated ARK Install
  kernel /install.amd/vmlinuz
  append initrd=/install.amd/initrd.gz auto=true priority=critical preseed/file=/cdrom/preseed.cfg
EOF

# Create new ISO
sudo apt-get install genisoimage
genisoimage -r -J -b isolinux/isolinux.bin -c isolinux/boot.cat \
  -no-emul-boot -boot-load-size 4 -boot-info-table \
  -o ark-debian-auto.iso /tmp/debian-custom

# Write to USB
sudo dd if=ark-debian-auto.iso of=/dev/sdX bs=4M status=progress
```

**Option B: PXE Network Boot**

```bash
# Set up TFTP server
sudo apt-get install tftpd-hpa dnsmasq

# Configure dnsmasq for PXE
cat > /etc/dnsmasq.d/pxe.conf <<EOF
dhcp-range=192.168.1.50,192.168.1.150,12h
dhcp-boot=pxelinux.0
enable-tftp
tftp-root=/srv/tftp
EOF

# Copy Debian netboot files
mkdir -p /srv/tftp
wget http://ftp.debian.org/debian/dists/bookworm/main/installer-amd64/current/images/netboot/netboot.tar.gz
tar -xzf netboot.tar.gz -C /srv/tftp

# Add preseed to TFTP
cp preseed.cfg /srv/tftp/preseed.cfg

# Restart services
sudo systemctl restart dnsmasq
```

### Step 4: Install ARK Devices

1. **Boot device from USB or PXE**
2. **Select "Automated ARK Install"** from boot menu
3. **Wait for installation** (10-20 minutes)
4. **Device reboots** with SSH access configured

### Step 5: Verify and Complete Setup

```bash
# Find device IP (check DHCP server or use network scanner)
nmap -sn 192.168.1.0/24

# SSH to device (no password needed)
ssh arkadmin@<device-ip>

# Add device to inventory
vim inventories/preprod/hosts.yml

# Run Ansible to complete configuration
ansible-playbook -i inventories/preprod/hosts.yml site.yml -l ark-pp-new
```

## Method 2: Creating a Golden Image

### Step 1: Set Up Reference Device

Install Debian 12 on one ARK 3360 device with keyboard/monitor.

### Step 2: Run Bootstrap Script

```bash
# Copy bootstrap script to device (via USB stick)
sudo bash bootstrap-ssh-access.sh
```

Or manually create arkadmin:

```bash
# Create user
sudo useradd -m -s /bin/bash -G sudo arkadmin

# Add SSH keys
sudo mkdir -p /home/arkadmin/.ssh
sudo nano /home/arkadmin/.ssh/authorized_keys
# Paste both developer SSH public keys

sudo chmod 600 /home/arkadmin/.ssh/authorized_keys
sudo chown -R arkadmin:arkadmin /home/arkadmin/.ssh

# Configure sudo
echo "arkadmin ALL=(ALL:ALL) NOPASSWD:ALL" | sudo tee /etc/sudoers.d/90-arkadmin
sudo chmod 440 /etc/sudoers.d/90-arkadmin
```

### Step 3: Clean Up for Imaging

```bash
# Remove machine-specific data
sudo rm -f /etc/ssh/ssh_host_*
sudo rm -f /var/lib/dhcp/*
sudo truncate -s 0 /etc/machine-id
sudo rm /var/lib/dbus/machine-id
sudo ln -s /etc/machine-id /var/lib/dbus/machine-id

# Clear logs
sudo find /var/log -type f -exec truncate -s 0 {} \;

# Clear bash history
history -c
cat /dev/null > ~/.bash_history
```

### Step 4: Create Image

**Using Clonezilla** (recommended):

```bash
# Boot reference device with Clonezilla Live USB
# Select: device-image mode
# Choose: savedisk
# Name: ark-3360-golden-image
# Select source disk: /dev/sda
# Save to network share or external drive
```

**Using dd**:

```bash
# From a live Linux USB
sudo dd if=/dev/sda of=/mnt/usb/ark-3360.img bs=4M status=progress

# Compress for storage
sudo gzip /mnt/usb/ark-3360.img
```

### Step 5: Deploy Image to New Devices

```bash
# Using Clonezilla
# Boot target device with Clonezilla Live USB
# Select: device-image mode
# Choose: restoredisk
# Select image: ark-3360-golden-image
# Select target disk: /dev/sda

# Using dd
sudo dd if=/mnt/usb/ark-3360.img.gz | gunzip | sudo dd of=/dev/sda bs=4M status=progress
```

### Step 6: First Boot Configuration

```bash
# On first boot, SSH will generate new host keys automatically
# Each device gets unique SSH host keys

# SSH to device
ssh arkadmin@<device-ip>

# Run final setup
sudo dpkg-reconfigure openssh-server  # Regenerate host keys if needed
sudo systemd-machine-id-setup  # Generate unique machine-id
```

## Method 3: Bootstrap Script

For small deployments or when you have console access for initial setup:

### Step 1: Customize Bootstrap Script

Edit `scripts/bootstrap-ssh-access.sh` and add your SSH keys:

```bash
DEV1_KEY="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQC... dev1@laptop"
DEV2_KEY="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQD... dev2@laptop"
```

### Step 2: Deploy to Device

**Option A: Via USB Stick**

```bash
# Copy to USB
cp scripts/bootstrap-ssh-access.sh /media/usb/

# On device (with keyboard/monitor)
sudo bash /media/usb/bootstrap-ssh-access.sh
```

**Option B: Via Network (if device already has network)**

```bash
# From your laptop
scp scripts/bootstrap-ssh-access.sh root@device-ip:/tmp/

# On device
ssh root@device-ip
cd /tmp
bash bootstrap-ssh-access.sh
```

### Step 3: Verify Access

```bash
# From your laptop
ssh arkadmin@<device-ip>

# Should connect without password
```

## Deployment Workflow Comparison

| Method | Setup Time | Per-Device Time | Requires Physical Access | Best For |
|--------|-----------|----------------|-------------------------|----------|
| **Preseed** | 2-3 hours | 15-20 min | No | Large deployments (10+ devices) |
| **Golden Image** | 1-2 hours | 5-10 min | No | Medium deployments (5-20 devices) |
| **Bootstrap Script** | 15 min | 5 min | Yes (initial) | Small deployments (1-5 devices) |

## Security Considerations

### SSH Key Management

**DO**:
- ✅ Use different SSH keys for production vs development
- ✅ Store private keys securely (encrypted, password-protected)
- ✅ Rotate SSH keys periodically
- ✅ Use strong SSH keys (4096-bit RSA or Ed25519)

**DON'T**:
- ❌ Commit private keys to version control
- ❌ Share private keys between developers
- ❌ Use the same keys across environments
- ❌ Leave SSH keys on USB sticks

### Password Authentication

The preseed and golden image methods set up SSH key-only authentication. To completely disable password authentication:

```bash
sudo sed -i 's/^#*PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo systemctl restart ssh
```

### Root Access

All methods disable root SSH login. Root access via sudo only:

```bash
ssh arkadmin@device-ip
sudo su -  # If root shell needed
```

## Troubleshooting

### Can't SSH After Deployment

**Check network connectivity**:
```bash
ping <device-ip>
```

**Check SSH service**:
```bash
# On device (with console access)
sudo systemctl status ssh
sudo systemctl start ssh
```

**Check SSH keys**:
```bash
# Verify keys are installed
ssh arkadmin@device-ip
# If prompted for password, keys are not properly installed
```

**Debug SSH connection**:
```bash
ssh -v arkadmin@device-ip
# Look for "Offering public key" and "Server accepts key" messages
```

### SSH Key Permissions Errors

```bash
# On device
sudo chmod 700 /home/arkadmin/.ssh
sudo chmod 600 /home/arkadmin/.ssh/authorized_keys
sudo chown -R arkadmin:arkadmin /home/arkadmin/.ssh
```

### Golden Image: Devices Have Same SSH Host Keys

```bash
# On each device after imaging
sudo rm /etc/ssh/ssh_host_*
sudo dpkg-reconfigure openssh-server
sudo systemctl restart ssh
```

## Post-Bootstrap: Complete Configuration

After SSH access is established, complete the full ARK configuration:

```bash
# Add device to inventory
vim inventories/preprod/hosts.yml

# Run Ansible playbook
ansible-playbook -i inventories/preprod/hosts.yml site.yml

# Verify
ssh arkadmin@device-ip
hostnamectl
systemctl status systemd-networkd
```

## Production Recommendations

For production deployments:

1. **Use preseed or golden image** - Eliminates manual steps
2. **Separate SSH keys per environment** - Different keys for prod/staging/dev
3. **Document MAC addresses** - Track which device gets which image
4. **Test one device first** - Verify process before mass deployment
5. **Create deployment checklist** - Standardize the process
6. **Keep backup image** - Store golden image securely

## Related Documentation

- [DEPLOYMENT.md](../DEPLOYMENT.md) - Full Ansible deployment guide
- [README.md](../README.md) - Overview and quick start
- [scripts/bootstrap-ssh-access.sh](../scripts/bootstrap-ssh-access.sh) - Bootstrap script
