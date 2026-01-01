# ECR Server Quick Start

## Problem
Payment terminal shows: **"Ikke kontakt med kasse. Vennligst vent"**

## Solution
Run the ECR server on your Mac so the terminal can connect.

## Network Setup

**Your Mac (Cash Register):**
- IP: `192.168.0.41`
- Port: `8009` (ECR protocol)

**Payment Terminal:**
- IP: `192.168.0.4`
- Port: Connects to your Mac on port 8009

## Running the ECR Server

### Method 1: Using the script (recommended)

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./run-ecr-server.sh
```

### Method 2: Using Maven directly

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
mvn exec:java -Dexec.mainClass="no.cloudberries.lpg.payment.EcrServerApp"
```

### Method 3: Via Main.kt

```bash
mvn exec:java -Dexec.args="--ecr-server"
```

## What to Expect

1. **Server starts:**
   ```
   === ECR Server for Payment Terminal ===
   
   Starting ECR server on port 8009...
   ✓ ECR Server is running
   Waiting for terminal connection on port 8009...
   ```

2. **Terminal connects:**
   ```
   Payment terminal connected from 192.168.0.4:xxxxx
   Terminal session started
   ```

3. **Terminal sends messages:**
   ```
   Received XX bytes from terminal
   HEX:   ...
   Terminal sent [message type]
   Sent ACK to terminal
   ```

4. **Terminal should change status** from "Ikke kontakt med kasse" to ready state

## Troubleshooting

### Firewall blocking port 8009

If terminal cannot connect, temporarily disable firewall or allow port 8009:

```bash
# Check if port is already in use
lsof -i :8009

# If firewall blocks, you may need to allow it in System Preferences
```

### Terminal still shows "Ikke kontakt med kasse"

1. Make sure ECR server is running
2. Check that terminal is configured to connect to `192.168.0.41:8009`
3. Restart the terminal (may need physical restart)
4. Check terminal configuration menu for ECR settings

### Port already in use

If port 8009 is already in use, run on different port and configure terminal:

```bash
./run-ecr-server.sh 8010
```

Then configure terminal to use port 8010.

## Next Steps

Once terminal connects successfully:

1. **Observe the messages** - The server logs all communication
2. **Analyze the protocol** - We can implement proper responses
3. **Test payments** - Try a payment and see what terminal sends
4. **Implement full protocol** - Based on captured messages

## Stopping the Server

Press `Ctrl+C` to stop the server gracefully.

## Configuration

### Terminal Configuration
You may need to configure the terminal via its admin menu:
- ECR Host: `192.168.0.41` (your Mac's IP)
- ECR Port: `8009`
- Protocol: ECR/ZVT (check terminal documentation)

### Check Terminal Configuration
Look in terminal's admin/setup menu for:
- Network settings
- ECR/POS settings
- Connection settings
