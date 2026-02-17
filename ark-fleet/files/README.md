# ARK Fleet Configuration Files

This directory contains configuration files used for ARK 3360 device provisioning.

## preseed-template.cfg

Debian preseed configuration for automated installation with SSH access pre-configured.

**Usage**:
1. Copy template to create your custom preseed:
   ```bash
   cp preseed-template.cfg preseed.cfg
   ```

2. Edit `preseed.cfg` and replace SSH key placeholders:
   ```bash
   vim preseed.cfg
   # Find and replace:
   # ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQC_YOUR_DEV1_KEY_HERE dev1@laptop
   # ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQD_YOUR_DEV2_KEY_HERE dev2@laptop
   ```

3. Create installation media with preseed file
   - See [docs/BOOTSTRAP-DEPLOYMENT.md](../docs/BOOTSTRAP-DEPLOYMENT.md) for detailed instructions

**What it does**:
- Installs Debian 12 (Bookworm) automatically
- Creates `arkadmin` user with sudo access
- Configures SSH with your public keys
- Disables root SSH login
- Sets timezone to US/Eastern
- Installs essential packages (SSH, sudo, curl)

## Security Notes

- ⚠️ **preseed.cfg is in .gitignore** - Never commit files with actual SSH keys
- Only commit the template file (`preseed-template.cfg`)
- Each deployment environment should have its own preseed.cfg with appropriate keys
- Store actual preseed files securely, separate from version control

## Related Documentation

- [Bootstrap Deployment Guide](../docs/BOOTSTRAP-DEPLOYMENT.md) - Comprehensive guide for zero-touch deployment
- [Main Deployment Guide](../DEPLOYMENT.md) - Standard Ansible deployment workflow
