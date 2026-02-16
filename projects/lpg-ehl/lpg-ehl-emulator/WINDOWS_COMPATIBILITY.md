# Windows Dispenserkontroll Compatibility Guide

## Problem
Windows applications running in Parallels/VMware cannot connect to Java servers that bind to IPv6 (`::`) by default on macOS.

## Solution
The emulator now forces IPv4 stack (`java.net.preferIPv4Stack=true`) in the main() method, ensuring the TCP server binds to `0.0.0.0` (IPv4) instead of `::` (IPv6).

## Network Configuration

### Mac IP Addresses
Your Mac has multiple network interfaces:
- **Local Network**: `192.168.0.36` (Wi-Fi/Ethernet)
- **Parallels Shared**: `10.211.55.2` ✅ **Use this!**
- **Parallels Host-Only**: `10.37.129.2`

### Windows Client Settings
Configure the Windows Dispenserkontroll program to connect to:
- **IP Address**: `10.211.55.2` (Parallels Shared Network)
- **Port**: `9000`

## Starting the Emulator

### From IntelliJ (Recommended)
1. Open `LpgEhlEmulatorApplication.kt`
2. Run main() - IPv4 is now automatic
3. Emulator listens on port 9000 (IPv4)

### From Command Line
```bash
cd lpg-ehl-emulator
mvn spring-boot:run
```

No additional JVM arguments needed - IPv4 preference is built-in!

## Verification

### Check Port Binding
```bash
# Should show tcp4 (not tcp46)
netstat -an | grep 9000
# Expected: tcp4       0      0  *.9000                 *.*                    LISTEN
```

### Test Connection from Mac
```bash
# Test Parallels IP
nc -zv 10.211.55.2 9000

# Test localhost
nc -zv localhost 9000
```

Both should succeed and emulator logs should show:
```
│ 📱 NEW CLIENT CONNECTION
│ ID: 10.211.55.2:xxxxx
```

## Troubleshooting

### Windows Still Can't Connect
1. Check Parallels networking mode is **Shared Network** (not Bridged)
2. Verify Mac IP with: `ifconfig | grep "inet " | grep 10.211`
3. Check macOS firewall isn't blocking port 9000
4. Restart Windows network adapter in Parallels

### Wrong IP Address
Run this on Mac to find Parallels IP:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```
Look for the `10.211.55.x` address.

## Why IPv4 Only?
- **Emulator Purpose**: Local testing with legacy Windows VB6 software only
- **Not Production**: Production uses modern web frontend, not Windows client
- **Windows .NET Limitation**: Older .NET TCP clients prefer IPv4
- **Parallels Networking**: Works better with IPv4 for VM-to-host communication

## Production Note
⚠️ **The emulator is NOT used in production!**

Production uses:
- Modern React web frontend (lpg-web)
- REST API (lpg-ehl-api)
- No legacy Windows software

The IPv4 preference only affects local testing with the legacy Windows Dispenserkontroll application.
