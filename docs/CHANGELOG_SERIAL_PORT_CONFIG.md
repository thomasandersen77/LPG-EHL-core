# Serial Port Configuration Feature

**Date:** 2026-01-31  
**Branch:** `refactor/serialtransport_master`  
**Author:** Warp Agent

---

## Overview

This document describes the new **Serial Port Configuration** feature added to the LPG EHL system. The feature provides a graphical user interface for scanning, testing, and configuring serial port connections to LPG dispensers.

---

## Problem Statement

### Before This Change

1. **Manual Configuration Required**: Serial port settings (port name, baud rate, parity, etc.) had to be configured manually in `application.yaml` or via command-line arguments.

2. **No Visibility**: Users had no way to see which serial ports were available on the system without using terminal commands.

3. **Trial-and-Error**: Finding the correct configuration (especially for legacy hardware with non-standard addressing) required trial-and-error with different settings.

4. **Watchdog Timeouts**: When the wrong port or configuration was used, the system would show repeated watchdog timeout errors without guidance on how to fix the issue.

5. **Field Deployment Challenges**: Technicians deploying the system in the field had difficulty determining the correct serial port and address for real hardware.

### After This Change

1. **GUI-Based Configuration**: A dedicated web page allows users to scan, select, and test serial ports visually.

2. **Automatic Port Detection**: The system detects both hardware serial ports (USB adapters) and virtual ports (socat PTYs).

3. **Smart Auto-Scan**: One-click scanning tests all ports with common configurations to find what works.

4. **Address Scanner**: Dedicated tool to find which dispenser addresses respond on a given port.

5. **Real-Time Feedback**: Immediate feedback on connection status, health checks, and scan results.

---

## Changes Made

### 1. New Frontend Page: `SerialPortConfigPage.tsx`

**Location:** `lpg-web/src/pages/SerialPortConfigPage.tsx`

A comprehensive React page providing:

#### Port Detection Panel
- Lists all available serial ports on the system
- Shows hardware ports detected by jSerialComm
- Shows virtual ports at `/tmp/vserial*` (commonly used with socat)
- Displays port metadata (description, vendor ID, product ID)
- Auto-refreshes every 10 seconds to detect newly connected devices

#### Configuration Panel
- **Baud Rate**: Dropdown with common rates (9600, 19200, 38400, 57600, 115200)
- **Parity**: NONE, EVEN, ODD selection
- **Data Bits**: 7 or 8 bits
- **Stop Bits**: 1 or 2 stop bits
- **Test Address**: Input field for dispenser address (1-255)
- **Configuration Summary**: Visual display of current settings (e.g., "8N1 @ 9600 baud")

#### Quick Actions
- **Health Check**: Tests communication with the currently selected configuration
- **Auto-detect Parity**: Automatically determines the correct parity mode

#### Smart Auto-Scan
- Tests all available ports with common configurations
- Configurable timeout per test
- Option to stop on first match or scan all
- Returns results sorted by confidence score
- One-click "Apply this configuration" button

#### Address Scanner
- Scans a range of addresses on the selected port
- Supports both standard (1-8) and legacy (32+n) addressing
- Configurable start/end address and timeout
- Shows responding addresses with human-readable descriptions

#### Quick Presets
- **8N1 @ 9600**: Standard for simulators and Python tools
- **8E1 @ 9600**: Standard EHL protocol
- **8N1 @ 19200**: High-speed option
- **Legacy 32+n**: Pre-configures for real hardware with offset addressing

#### Help Section
- In-page documentation explaining each feature

### 2. Updated App Router

**Location:** `lpg-web/src/App.tsx`

Added route for the new page:
```tsx
<Route path="serial-config" element={<SerialPortConfigPage />} />
```

Added import:
```tsx
import { SerialPortConfigPage } from './pages/SerialPortConfigPage';
```

### 3. Updated Home Page Navigation

**Location:** `lpg-web/src/pages/HomePage.tsx`

Added navigation card linking to Serial Port Config:
```tsx
<Link
  to="/serial-config"
  className="p-6 bg-cyan-50 hover:bg-cyan-100 border-2 border-cyan-200 rounded-xl transition text-center group"
>
  <div className="text-4xl mb-3">🔌</div>
  <h4 className="text-lg font-bold text-slate-900 mb-2">Serial Port Config</h4>
  <p className="text-sm text-slate-600">Skann og konfigurer serieporter</p>
</Link>
```

---

## Backend API Endpoints Used

The frontend leverages existing backend endpoints in `SerialDebugController`:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/debug/serial/ports` | GET | List all available serial ports |
| `/api/debug/serial/status` | GET | Get current connection status |
| `/api/debug/serial/health` | GET | Health check with specific address |
| `/api/debug/serial/smart-scan` | POST | Auto-scan all ports and configurations |
| `/api/debug/serial/scan-addresses` | POST | Scan address range on specific port |
| `/api/debug/serial/auto-detect` | POST | Auto-detect parity mode |

---

## Benefits

### For Field Technicians

1. **Faster Deployment**: No need to know exact port names or configurations beforehand
2. **Visual Feedback**: Clear indication of what's working and what's not
3. **Legacy Hardware Support**: Built-in support for 32+n addressing used by older dispensers
4. **Troubleshooting**: Easy to diagnose connection issues without terminal access

### For Developers

1. **Reduced Setup Time**: New team members can quickly find working configurations
2. **Testing Flexibility**: Easy to switch between simulator and real hardware
3. **Debug Visibility**: See exactly which ports and addresses respond

### For Operations

1. **Self-Service**: Station operators can verify connections without IT support
2. **Documentation**: Configuration discovery is documented in the UI
3. **Reliability**: Reduced misconfiguration errors

---

## Usage Guide

### Accessing the Page

1. Navigate to http://localhost:3001/serial-config (dev mode)
2. Or click "🔌 Serial Port Config" on the home page

### Finding a Working Configuration

**Method 1: Smart Auto-Scan (Recommended)**
1. Click "🔍 Start Smart Scan"
2. Wait for scan to complete (10-30 seconds)
3. Review results sorted by confidence
4. Click "Apply this configuration" on the best match

**Method 2: Manual Testing**
1. Select a port from the "Available Ports" list
2. Choose configuration from dropdowns or use a Quick Preset
3. Click "💓 Health Check" to test
4. Adjust settings if needed

**Method 3: Address Discovery**
1. Select a port and configure baud/parity
2. Set address range (e.g., 1-40 for comprehensive scan)
3. Click "📡 Scan Address Range"
4. Use discovered addresses for further testing

### Quick Presets Explained

| Preset | Settings | Use Case |
|--------|----------|----------|
| 8N1 @ 9600 | 9600 baud, no parity, 8 data bits, 1 stop bit | Simulators, Python tools |
| 8E1 @ 9600 | 9600 baud, even parity, 8 data bits, 1 stop bit | Standard EHL hardware |
| 8N1 @ 19200 | 19200 baud, no parity | High-speed connections |
| Legacy 32+n | Address 33, scan range 32-40 | Older hardware with offset addressing |

---

## Technical Details

### Port Detection

The system uses two sources for port detection:

1. **jSerialComm Library**: Detects hardware serial ports (USB-to-serial adapters, built-in ports)
2. **File System Check**: Looks for virtual PTY paths commonly used with socat:
   - `/tmp/vserial0`
   - `/tmp/vserial1`
   - `/tmp/ttyV0`
   - `/tmp/ttyV1`

### Confidence Scoring

The smart scan assigns confidence scores based on:
- **Baud Rate**: 9600 gets +50 (standard EHL)
- **Parity**: EVEN +30, NONE +25, ODD +10
- **Address**: Standard (1-8) or Legacy (33-36) addressing bonuses

### Address Formats

| Format | Range | Description |
|--------|-------|-------------|
| Standard | 1-8 | Most simulators and modern hardware |
| Legacy | 32+n | Real hardware where pump 1 = address 33 |

---

## Testing the Feature

### With Simulator

```bash
# Terminal 1: Create virtual serial port pair
socat -d pty,rawer,echo=0,link=/tmp/vserial0 pty,rawer,echo=0,link=/tmp/vserial1

# Terminal 2: Start simulator on vserial0
java -jar release/pls-sim.jar --port=/tmp/vserial0 --mode=ehl --address=1

# Terminal 3: Start webapp (connects to vserial1)
java -jar release/lpg-ehl-webapp.jar --spring.profiles.active=field

# Terminal 4: Start frontend dev server
cd lpg-web && npm run dev
```

Then open http://localhost:3001/serial-config and:
1. Select `/tmp/vserial1` from the port list
2. Use "8N1 @ 9600" preset
3. Set address to 1
4. Click "Health Check" - should show success

### API Testing

```bash
# List ports
curl http://localhost:8080/api/debug/serial/ports

# Smart scan
curl -X POST "http://localhost:8080/api/debug/serial/smart-scan?timeoutMs=1000&stopOnFirst=true"

# Scan addresses
curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/tmp/vserial1&start=1&end=10&baud=9600&parity=NONE&timeoutMs=500"
```

---

## Future Improvements

1. **Apply Configuration**: Currently shows working config - could add ability to save to application.yaml
2. **Connection History**: Remember previously working configurations
3. **Real-Time Monitoring**: WebSocket-based live data display from selected port
4. **Export/Import**: Save and share configurations between stations
5. **Batch Scanning**: Test multiple stations simultaneously

---

## Related Files

| File | Purpose |
|------|---------|
| `lpg-web/src/pages/SerialPortConfigPage.tsx` | Main UI component |
| `lpg-web/src/App.tsx` | Route registration |
| `lpg-web/src/pages/HomePage.tsx` | Navigation link |
| `lpg-ehl-service/src/main/kotlin/.../SerialPortScanner.kt` | Backend scanning logic |
| `lpg-ehl-webapp/src/main/kotlin/.../SerialDebugController.kt` | REST API endpoints |
| `lpg-transport/src/main/kotlin/.../SerialPortConfig.kt` | Configuration model |

---

## Conclusion

The Serial Port Configuration feature significantly improves the user experience for deploying and troubleshooting LPG EHL systems. By providing visual tools for port discovery, configuration testing, and address scanning, it reduces deployment time and eliminates common configuration errors.
