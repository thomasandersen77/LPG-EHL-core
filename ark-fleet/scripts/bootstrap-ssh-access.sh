#!/bin/bash
# Bootstrap script for ARK 3360 devices
# Run this on a fresh Debian 12 installation to set up arkadmin SSH access
# This allows subsequent Ansible deployments without keyboard/monitor access

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}ARK 3360 Bootstrap - SSH Access Setup${NC}"
echo "========================================"
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}ERROR: This script must be run as root${NC}"
    echo "Usage: sudo $0"
    exit 1
fi

# Developer 1 SSH public key - REPLACE WITH YOUR ACTUAL KEY
DEV1_KEY="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQC... dev1@laptop"

# Developer 2 SSH public key - REPLACE WITH YOUR ACTUAL KEY
DEV2_KEY="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQD... dev2@laptop"

echo "Step 1: Installing SSH server..."
apt-get update -qq
apt-get install -y -qq openssh-server sudo

echo "Step 2: Creating arkadmin user..."
if id "arkadmin" &>/dev/null; then
    echo -e "${YELLOW}User arkadmin already exists${NC}"
else
    useradd -m -s /bin/bash -G sudo arkadmin
    echo -e "${GREEN}User arkadmin created${NC}"
fi

echo "Step 3: Setting up SSH directory..."
mkdir -p /home/arkadmin/.ssh
chmod 700 /home/arkadmin/.ssh

echo "Step 4: Adding SSH authorized keys..."
cat > /home/arkadmin/.ssh/authorized_keys <<EOF
# Developer 1 SSH key
${DEV1_KEY}

# Developer 2 SSH key
${DEV2_KEY}
EOF

chmod 600 /home/arkadmin/.ssh/authorized_keys
chown -R arkadmin:arkadmin /home/arkadmin/.ssh

echo "Step 5: Configuring passwordless sudo..."
cat > /etc/sudoers.d/90-arkadmin <<EOF
arkadmin ALL=(ALL:ALL) NOPASSWD:ALL
EOF
chmod 440 /etc/sudoers.d/90-arkadmin

echo "Step 6: Enabling SSH service..."
systemctl enable ssh
systemctl start ssh

echo "Step 7: Configuring SSH server..."
# Ensure these settings are present
sed -i 's/^#*PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/^#*PubkeyAuthentication.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config

# Optional: Disable password authentication for security
# Uncomment the next line if you want password auth disabled
# sed -i 's/^#*PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config

systemctl restart ssh

# Get IP addresses
echo ""
echo -e "${GREEN}Bootstrap complete!${NC}"
echo ""
echo "SSH access configured for arkadmin user"
echo "Authorized developers: 2"
echo ""
echo "Device IP addresses:"
ip -4 addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1'
echo ""
echo "You can now SSH to this device:"
echo -e "${GREEN}ssh arkadmin@<device-ip>${NC}"
echo ""
echo "Next steps:"
echo "1. SSH to this device from your laptop"
echo "2. Run Ansible playbook to complete configuration"
echo "   ansible-playbook -i inventories/preprod/hosts.yml site.yml"
