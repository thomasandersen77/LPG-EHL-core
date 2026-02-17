# Pump Steering .NET Application

This application simulates a pump controller connected to Azure IoT Hub.
It is a simple .NET Console Application that simulates fueling and allows remote control via Azure IoT Hub Direct Methods.

## Prerequisites

- .NET 8.0 SDK
- An Azure IoT Hub with a device registered.
- The Device Connection String for the registered device.

## Building

```bash
cd projects/pump-steering
dotnet build
```

## Running

Run the application with the device connection string as an argument:

```bash
dotnet run -- "<your_device_connection_string>"
```

## Features

### Telemetry

The application streams telemetry data every second to IoT Hub:

```json
{
  "price": 15.90,
  "volume": 0.00,
  "is_locked": true,
  "timestamp": "2023-10-27T10:00:00Z"
}
```

### Watching Telemetry

To watch the live stream of data:

**Azure CLI:**
```bash
az iot hub monitor-events --hub-name iothub-norgesgass-station-prod-norwayeast-01 --device-id <your-device-id>
```

**Azure Portal:**
1. Navigate to your IoT Hub.
2. Open Cloud Shell (`>_` icon at the top).
3. Run the command above.

### Sending Commands (Direct Methods)

You can send commands to the running application using **Direct Methods** in Azure.

#### Method 1: Azure Portal (GUI)
1. Go to your **IoT Hub** -> **Devices**.
2. Click on your device (e.g. `norgesgass-station-01`).
3. Click on the **Direct Method** tab at the top.
4. **Method Name**: Enter `Unlock`, `Lock`, `SetPrice`, or `Reset`.
5. **Payload**:
   - For `Unlock`, `Lock`, `Reset`: `{}`
   - For `SetPrice`: `{"price": 16.50}`
6. Click **Invoke Method**.

#### Method 2: Azure CLI

Replace `<your-device-id>` with your actual device ID.

**Unlock Pump:**
```bash
az iot hub invoke-device-method \
  --hub-name iothub-norgesgass-station-prod-norwayeast-01 \
  --device-id <your-device-id> \
  --method-name Unlock \
  --method-payload '{}'
```

**Lock Pump:**
```bash
az iot hub invoke-device-method \
  --hub-name iothub-norgesgass-station-prod-norwayeast-01 \
  --device-id <your-device-id> \
  --method-name Lock \
  --method-payload '{}'
```

**Set Price:**
```bash
az iot hub invoke-device-method \
  --hub-name iothub-norgesgass-station-prod-norwayeast-01 \
  --device-id <your-device-id> \
  --method-name SetPrice \
  --method-payload '{"price": 16.50}'
```

**Reset Pump:**
```bash
az iot hub invoke-device-method \
  --hub-name iothub-norgesgass-station-prod-norwayeast-01 \
  --device-id <your-device-id> \
  --method-name Reset \
  --method-payload '{}'
```
