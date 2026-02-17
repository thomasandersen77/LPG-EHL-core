# Storage Hardening for ARK 3360 Devices

This document explains the storage hardening configuration applied to ARK 3360 devices for industrial/embedded applications.

## Overview

Storage hardening is critical for embedded systems that may experience:
- Sudden power loss
- Unstable power supplies
- Industrial environments with electrical noise
- Mission-critical applications requiring data integrity

The ARK 3360 configuration implements multiple layers of protection to ensure filesystem integrity and data consistency.

## Hardening Measures

### 1. Filesystem Error Behavior

**Configuration**: ext4 filesystem with `errors=remount-ro`

```bash
# Applied via tune2fs
tune2fs -e remount-ro /dev/sda1
```

**What it does**:
- When the filesystem encounters an error, it immediately remounts as read-only
- Prevents further writes that could compound corruption
- System remains operational in read-only mode for diagnostics
- Requires manual intervention to fix and remount read-write

**Verification**:
```bash
sudo tune2fs -l /dev/sda1 | grep "Errors behavior"
# Should show: Errors behavior:          Remount read-only
```

### 2. Write Cache Disabled

**Configuration**: Disk write cache disabled via hdparm

```bash
# Applied immediately
hdparm -W0 /dev/sda

# Persisted in /etc/hdparm.conf
/dev/sda {
    write_cache = off
}
```

**What it does**:
- Forces all writes to be physically committed to disk immediately
- Prevents data loss on sudden power failure
- Ensures filesystem consistency without requiring fsync() calls
- Trade-off: Slightly reduced write performance for guaranteed integrity

**Why this matters**:
When write cache is enabled (default on many systems), the disk may report write completion before data is physically on disk. On power loss, this "pending" data is lost, potentially causing:
- Incomplete transactions
- Corrupted file metadata
- Journal inconsistencies
- Lost configuration changes

**Verification**:
```bash
sudo hdparm -W /dev/sda
# Should show: write-caching =  0 (off)
```

### 3. Mount Options

**Configuration**: Root filesystem mounted with safe options

```
/dev/sda1 on / type ext4 (rw,relatime,errors=remount-ro)
```

**Mount options explained**:
- `rw`: Read-write (normal operation)
- `relatime`: Update access times efficiently (reduces writes)
- `errors=remount-ro`: Remount read-only on error (see above)

**Default mount options** (from tune2fs):
- `user_xattr`: Extended user attributes support
- `acl`: Access Control Lists support

### 4. ext4 Journal

**Configuration**: ext4 journaling enabled (default)

**What it does**:
- Journals metadata changes before committing to main filesystem
- Enables fast recovery after unexpected shutdown
- Reduces filesystem check time after crash
- Combined with write-cache-off, provides strong consistency

**Verification**:
```bash
sudo tune2fs -l /dev/sda1 | grep -i journal
# Should show journal is enabled
```

## Reference System Configuration

The reference ARK 3360 device uses:

**Storage Device**:
- Model: SQF-P10S2-8G-P8C (Industrial-grade 8GB SSD)
- Type: SATA SSD (ROTA=1 in lsblk, but it's an SSD)

**Filesystem Layout**:
- `/dev/sda1`: Root filesystem (ext4)
- `/dev/sda5`: Swap partition

**Performance Characteristics**:
- Industrial SSD designed for reliability over performance
- Optimized for random I/O typical in embedded systems
- Enhanced wear leveling and power-loss protection

## Ansible Implementation

The hardening role automatically configures all storage hardening measures:

```yaml
# Default variables (inventories/<env>/group_vars/ark.yml)
ark_root_device: "/dev/sda1"        # Root filesystem device
ark_root_disk: "/dev/sda"           # Root disk device
ark_disable_write_cache: true       # Disable write cache
```

### Tasks Performed

1. **Install hdparm**: Tool for disk parameter management
2. **Set filesystem error behavior**: Configure tune2fs
3. **Configure hdparm**: Create `/etc/hdparm.conf`
4. **Apply settings**: Disable write cache immediately
5. **Persist configuration**: Ensure settings survive reboot

### Customization

To adjust settings per device, override in host_vars:

```yaml
# inventories/<env>/host_vars/ark-pp-001.yml
ark_root_device: "/dev/nvme0n1p1"  # Different device
ark_disable_write_cache: false      # Keep cache enabled (not recommended)
```

## Operational Considerations

### Expected Behavior

**Normal Operation**:
- System operates normally with slightly conservative write performance
- All data writes are guaranteed on disk before operation completes
- Filesystem remains consistent even on unexpected shutdown

**On Filesystem Error**:
- Filesystem remounts read-only immediately
- System logs error to journal
- Manual intervention required to repair filesystem
- After repair: `sudo mount -o remount,rw /`

**On Power Loss**:
- No data loss for completed operations
- Filesystem remains consistent (may need journal replay)
- Fast recovery on reboot (journal recovery)
- No lengthy fsck required in most cases

### Performance Impact

**Write Performance**:
- Slightly slower than with write cache enabled
- Impact depends on workload (sequential vs random)
- Typically 10-30% reduction in write throughput
- Critical for industrial reliability - worth the trade-off

**Read Performance**:
- Unaffected (read cache still active)
- SSD provides fast random read access

### Monitoring

Monitor filesystem health:

```bash
# Check filesystem status
sudo tune2fs -l /dev/sda1 | grep -i "filesystem state\|last checked\|mount count"

# Check for errors in system log
sudo journalctl -b | grep -i "ext4\|filesystem\|readonly"

# Monitor disk health (SMART)
sudo smartctl -a /dev/sda
```

### Recovery Procedures

**If filesystem remounts read-only**:

```bash
# 1. Check for filesystem errors
sudo dmesg | tail -50

# 2. Remount other filesystems if needed
sync

# 3. Check and repair filesystem
sudo fsck.ext4 -f /dev/sda1

# 4. Remount read-write
sudo mount -o remount,rw /

# 5. Investigate root cause
sudo journalctl -xe
```

**Preventive maintenance**:

```bash
# Schedule regular filesystem checks
# Edit /etc/fstab to adjust check frequency
# Or use tune2fs:
sudo tune2fs -c 20 -i 2w /dev/sda1
# Check every 20 mounts or 2 weeks, whichever comes first
```

## Best Practices

1. **Test power-loss scenarios**: Verify system recovers gracefully
2. **Monitor filesystem checks**: Ensure scheduled checks complete
3. **Keep backups**: Hardware can still fail despite hardening
4. **Log monitoring**: Watch for filesystem errors in logs
5. **Document changes**: Any modifications to disk/filesystem config

## Troubleshooting

### Write cache re-enabled after reboot

**Problem**: `hdparm -W /dev/sda` shows cache enabled

**Solution**:
- Check `/etc/hdparm.conf` is correctly configured
- Verify hdparm systemd service is enabled
- Check for conflicting power management settings

### Performance concerns

**Problem**: Writes are too slow

**Options**:
1. **Use battery-backed cache**: Hardware RAID with BBU
2. **Application-level caching**: Cache in RAM, flush periodically
3. **Different storage**: Consider enterprise SSD with power-loss protection
4. **Re-evaluate requirements**: Is write cache safety really needed?

**Do NOT** disable write-cache safety without understanding risks.

### Filesystem errors after power loss

**Problem**: Filesystem has errors despite hardening

**Investigation**:
1. Verify write cache was actually disabled: `hdparm -W /dev/sda`
2. Check disk SMART data: `smartctl -a /dev/sda`
3. Review system logs before crash: `journalctl --since "1 hour ago"`
4. Consider hardware issue: Power supply, cable, disk failure

## Technical References

- [ext4 Documentation](https://www.kernel.org/doc/html/latest/filesystems/ext4/index.html)
- [hdparm Manual](https://linux.die.net/man/8/hdparm)
- [tune2fs Manual](https://linux.die.net/man/8/tune2fs)
- [Write Barriers in Linux](https://lwn.net/Articles/283161/)
- [Filesystem Consistency](https://www.usenix.org/legacy/event/osdi14/tech/full_papers/Pillai/pillai.pdf)

## Related Configuration

- See [DEPLOYMENT.md](../DEPLOYMENT.md) for deployment procedures
- See [REFERENCE.md](../reference/REFERENCE.md) for reference system details
- See [README.md](../README.md) for overall architecture
