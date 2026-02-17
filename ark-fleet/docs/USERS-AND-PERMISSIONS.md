# ARK Fleet Users and Permissions

This document explains the user accounts and permission model on ARK 3360 devices.

## User Hierarchy

The ARK system uses a layered user approach for security and separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│ Admin Users (SSH Access + Sudo)                        │
├─────────────────────────────────────────────────────────┤
│ arkadmin  - Shared developer account                   │
│ alejandro - Individual admin (Alejandro)                │
│ thomas    - Individual admin (Thomas)                   │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Application User (No Sudo, Serial Port Access)         │
├─────────────────────────────────────────────────────────┤
│ appuser   - Runs the ARK application                   │
│           - Has read/write access to serial ports      │
│           - Member of: dialout, plugdev groups          │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ System User (Used by Ansible/system services)          │
├─────────────────────────────────────────────────────────┤
│ arkapp    - System account for file ownership          │
│           - Owns /opt/arkapp, /var/lib/arkapp          │
└─────────────────────────────────────────────────────────┘
```

## User Details

### Admin Users

#### arkadmin (Shared Developer Account)
- **Purpose**: Shared SSH access for multiple developers
- **Sudo**: Yes (passwordless)
- **Groups**: sudo
- **SSH Keys**: Multiple (arkadmin-dev1.pub, arkadmin-dev2.pub)
- **Use case**: Ansible deployments, system maintenance, debugging

#### alejandro / thomas (Individual Admin Accounts)
- **Purpose**: Personal admin accounts for direct access
- **Sudo**: Yes (passwordless)
- **Groups**: sudo
- **SSH Keys**: Individual keys per user
- **Use case**: Personal system administration, development

### Application User

#### appuser (Application Runtime)
- **Purpose**: Runs the ARK application with hardware access
- **Sudo**: **NO** (non-privileged)
- **Groups**:
  - `dialout` - Serial port access (/dev/ttyS*, /dev/ttyUSB*, /dev/ttyACM*)
  - `plugdev` - USB device access
  - `gpio` - GPIO access (if available)
  - `i2c` - I2C bus access (if available)
- **Home**: `/home/appuser`
- **Directories**:
  - `/opt/app` - Application installation directory
  - `/var/lib/app` - Application data
  - `/var/log/app` - Application logs
  - `/etc/app` - Application configuration
- **Use case**: Running the ARK application, accessing serial ports

### System User

#### arkapp (System Account)
- **Purpose**: File ownership for system-managed directories
- **Sudo**: NO
- **Shell**: /usr/sbin/nologin (system account)
- **Directories**:
  - `/opt/arkapp` - System files
  - `/var/lib/arkapp` - System data
  - `/var/log/arkapp` - System logs
- **Use case**: Owned by system, not for login

## Serial Port Access

### How Serial Port Access Works

On Linux, serial ports are accessed via device files in `/dev`:
- `/dev/ttyS0`, `/dev/ttyS1`, ... - Built-in serial ports
- `/dev/ttyUSB0`, `/dev/ttyUSB1`, ... - USB-to-serial adapters
- `/dev/ttyACM0`, `/dev/ttyACM1`, ... - USB CDC ACM devices

By default, these devices are owned by root and group `dialout`:
```bash
$ ls -l /dev/ttyS0
crw-rw---- 1 root dialout 4, 64 Feb 17 10:00 /dev/ttyS0
```

### appuser Serial Port Access

The `appuser` is a member of the `dialout` group, granting read/write access:

```bash
# Check appuser groups
$ groups appuser
appuser : appuser dialout plugdev gpio i2c

# Verify serial port access
$ ls -l /dev/ttyS* /dev/ttyUSB* /dev/ttyACM* 2>/dev/null
crw-rw---- 1 root dialout 4, 64 Feb 17 10:00 /dev/ttyS0
crw-rw---- 1 root dialout 4, 65 Feb 17 10:00 /dev/ttyS1
```

### Systemd Service Configuration

The ARK application systemd service runs as `appuser`:

```ini
[Service]
User=appuser
Group=appuser
SupplementaryGroups=dialout plugdev gpio i2c

# Explicitly allow serial port device access
DeviceAllow=/dev/ttyS* rw
DeviceAllow=/dev/ttyUSB* rw
DeviceAllow=/dev/ttyACM* rw
DevicePolicy=closed
```

This configuration:
1. Runs the service as the unprivileged `appuser`
2. Grants all supplementary group memberships (dialout, etc.)
3. Uses systemd device policy to explicitly allow serial port access
4. Blocks access to other devices (DevicePolicy=closed)

## Permission Model

### What Each User CAN Do

#### arkadmin / alejandro / thomas
✅ Full system access via sudo
✅ Install packages
✅ Modify system configuration
✅ Restart services
✅ Access all files
✅ Read serial ports (via sudo or su to appuser)
✅ Deploy application updates

#### appuser
✅ Run the ARK application
✅ Read/write serial ports (ttyS*, ttyUSB*, ttyACM*)
✅ Access USB devices
✅ Read/write to /opt/app, /var/lib/app, /var/log/app
✅ Read configuration from /etc/app
❌ Cannot install packages (no sudo)
❌ Cannot modify system configuration (no sudo)
❌ Cannot restart system services (no sudo)
❌ Cannot access other users' files

#### arkapp (system)
✅ Owns system directories (/opt/arkapp, etc.)
❌ Cannot login (nologin shell)
❌ Not for interactive use

## Security Rationale

### Why Separate Users?

1. **Principle of Least Privilege**
   - Application runs with minimal permissions
   - Compromise of application doesn't grant system access

2. **Attack Surface Reduction**
   - appuser cannot install malicious packages
   - appuser cannot modify system configuration
   - appuser cannot escalate privileges

3. **Audit Trail**
   - Clear separation between admin actions and app actions
   - Logs show which user performed what action

4. **Defense in Depth**
   - Even if application is compromised, attacker has limited capabilities
   - Must escalate to admin user for system-level access

### Why appuser Needs Serial Port Access

The ARK application communicates with hardware via serial ports:
- Reading sensor data
- Controlling actuators
- Communicating with external devices
- Protocol implementation (Modbus, etc.)

Direct hardware access requires `dialout` group membership.

## Common Operations

### Deploy Application as Admin

```bash
# SSH as admin
ssh arkadmin@ark-device

# Deploy application
sudo cp my-application /opt/app/
sudo chown appuser:appuser /opt/app/my-application
sudo chmod 755 /opt/app/my-application

# Update systemd service
sudo systemctl restart arkapp
```

### Test Serial Port Access

```bash
# As admin, become appuser
ssh arkadmin@ark-device
sudo -u appuser bash

# Now as appuser, test serial port
cat /dev/ttyS0  # Should work
echo "test" > /dev/ttyS0  # Should work
```

### Check Application Permissions

```bash
# As admin
ssh arkadmin@ark-device

# Check appuser groups
groups appuser

# Check serial port permissions
ls -l /dev/ttyS* /dev/ttyUSB* /dev/ttyACM*

# Check application directories
ls -la /opt/app /var/lib/app /var/log/app
```

### View Application Logs

```bash
# As admin
ssh arkadmin@ark-device

# View systemd service logs
sudo journalctl -u arkapp -f

# View application log files
sudo tail -f /var/log/app/*.log

# Or as appuser (owns the logs)
sudo -u appuser tail -f /var/log/app/*.log
```

## Troubleshooting

### Application Can't Access Serial Port

**Symptom**: Permission denied on /dev/ttyS0

**Check**:
```bash
# Verify appuser is in dialout group
groups appuser | grep dialout

# Verify serial port permissions
ls -l /dev/ttyS0

# Verify systemd service configuration
systemctl cat arkapp | grep -A5 '\[Service\]'
```

**Fix**:
```bash
# Add appuser to dialout group
sudo usermod -aG dialout appuser

# Restart service
sudo systemctl restart arkapp
```

### Application Needs Additional Hardware Access

**Example**: Need access to SPI devices

**Solution**:
```bash
# Add appuser to spi group
sudo usermod -aG spi appuser

# Update systemd service
sudo systemctl edit arkapp
# Add: SupplementaryGroups=dialout plugdev gpio i2c spi

# Restart service
sudo systemctl restart arkapp
```

### Need to Run Command as appuser

```bash
# As admin
sudo -u appuser bash
# Now in appuser shell

# Or run single command
sudo -u appuser /opt/app/my-application --test
```

## Best Practices

1. **Never run applications as root**
   - Always use dedicated application user (appuser)
   - Grant only necessary permissions

2. **Use sudo only when necessary**
   - Application should run as appuser, not via sudo
   - System administration done via admin users

3. **Separate development from production**
   - Use individual accounts (alejandro, thomas) for development
   - Use arkadmin for shared/automated tasks

4. **Log application actions**
   - Application logs to /var/log/app
   - Systemd captures stdout/stderr to journal
   - Review logs regularly

5. **Test permission changes**
   - After adding groups or permissions, test as appuser
   - Verify application works before deploying to all devices

## Related Documentation

- [DEPLOYMENT.md](../DEPLOYMENT.md) - Deployment workflow
- [BOOTSTRAP-DEPLOYMENT.md](BOOTSTRAP-DEPLOYMENT.md) - Zero-touch deployment
- [README.md](../README.md) - Overview
