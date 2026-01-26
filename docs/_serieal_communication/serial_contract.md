# Serial Contract – LPG EHL Dispenser Communication

## Status
**FINAL – VERIFIED AGAINST LEGACY VB6 SYSTEM**

This document defines the complete and authoritative serial communication
contract used to control LPG dispensers.

---

## 1. Physical Layer

- **Medium:** RS-485
- **Topology:** Multi-drop bus
- **Direction:** Half-duplex
- **Termination:** Required at bus ends (field responsibility)

---

## 2. Transport Layer (Serial Settings)

| Parameter   | Value |
|------------|-------|
| Baudrate   | 9600  |
| Data bits  | 8     |
| Parity     | EVEN  |
| Stop bits  | 1     |
| Flow ctrl  | None  |

**Notation:** 9600 / 8E1

> Parity is mandatory and verified in legacy VB6 code (`MSComm.Settings = "9600,e,8,1"`)

---

## 3. Port Assignment (Default)

| Logical Device      | Address | Default COM |
|--------------------|---------|-------------|
| Autogas dispenser | 0x01    | COM2        |
| Container unit    | 0x02    | COM2        |

> COM port MUST be configurable. Field installations may use COM4.

---

## 4. Protocol Layer

- **Protocol:** EHL (European Hexadecimal Language)
- **Framing:**
    - STX = `0x10`
    - ETX = `0x36`
- **Checksum:** XOR over payload
- **Encoding:** Binary (not ASCII)

### Example Frame

```10 LEN ADR CMD DATA… XOR 36```

---

## 5. Core Commands

| Command   | Hex |
|----------|-----|
| PRESTART | C3  |
| UNBLOCK  | 77  |
| BLOCK    | 69  |
| POLL     | 45  |

---

## 6. Error Handling

- Parity errors are actively detected
- Legacy system uses ParityReplace and error events
- New system MUST log framing/parity errors verbosely

---

## 7. Emulator & Testing

- Emulator must behave as an RS-485 device
- socat PTY pairs are the reference test method
- Emulator + Core must communicate using RealSerialTransport

---

## 8. Migration Notes

- No Modbus involved in pump control
- No TCP/IP in dispenser control path
- Legacy ADAM modules are telemetry-only

---

## 9. Authority

This contract is derived from:
- Original VB6 source code
- serverinnstillinger.frm
- Registry (Docklight)
- Physical ARK installations
- Emulator validation

Any deviation must be justified against this document.