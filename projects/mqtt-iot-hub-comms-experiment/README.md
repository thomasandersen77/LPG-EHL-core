# Azure IoT Hub Kotlin Script Demo

This a simple Kotlin script app to test bidirectional communication with Azure IoT Hub.

## Prerequisites

- **Kotlin**: Ensure you have Kotlin installed (`kotlin` command in your path).
- **Azure IoT Device Connection String**: You will need a connection string for a device registered in your IoT Hub.

## Usage

The script supports two tests:

### Test 1: Connect and Send Data
Connects to Azure, sends a telemetry message, and waits for connection confirmation.

```bash
kotlin iot_demo.main.kts send "HostName=XXXXX.azure-devices.net;DeviceId=XXXXX;SharedAccessKey=XXXXX"
```

### Test 2: Listen and Respond
Sits and waits for incoming Cloud-to-Device messages or Direct Method calls, and responds back.

```bash
kotlin iot_demo.main.kts listen "HostName=XXXXX.azure-devices.net;DeviceId=XXXXX;SharedAccessKey=XXXXX"
```

## How it works

- **Test 1**: Uses `DeviceClient.sendEventAsync` to send a JSON payload.
- **Test 2 (C2D)**: Uses `setMessageCallback` to receive messages and sends a telemetry response back.
## How to get a Connection String from Azure Portal

1.  **Log in** to your [Azure Portal](https://portal.azure.com).
2.  Navigate to your **IoT Hub** resource.
3.  In the left sidebar, click on **Devices** (under Device Management).
4.  Select your **Device ID** (or click `+ Add Device` to create one, then select it).
5.  Copy the **Primary Connection String**. It will be in the format:
    `HostName=<hub-name>.azure-devices.net;DeviceId=<device-id>;SharedAccessKey=<key>`
