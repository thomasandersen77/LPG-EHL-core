# Payment Terminal Integration

## Overview

LPG-EHL Core now includes payment terminal integration for handling card payments at LPG dispensers.

## Architecture

The payment system follows a clean interface-based design:

```
PaymentTerminal (interface)
├── TcpPaymentTerminal (real terminal via TCP/IP)
└── SimulatedPaymentTerminal (testing without hardware)
```

## Terminal Configuration

**Discovered Terminal:**
- IP Address: `192.168.0.4`
- Port: `8009`
- Protocol: TLS/Encrypted (needs protocol specification)

## Usage

### Running the Demo

```bash
# Test with simulated terminal
./run-payment-demo.sh

# Test with real terminal
./run-payment-demo.sh --real
```

### Code Example

```kotlin
import no.cloudberries.lpg.payment.*

// Create terminal instance
val terminal = TcpPaymentTerminal(
    host = "192.168.0.4",
    port = 8009,
    timeout = 30000
)

// Connect
if (terminal.connect()) {
    // Request payment (amount in øre/cents)
    val result = terminal.requestPayment(5000) // 50.00 kr
    
    when (result.status) {
        PaymentStatus.APPROVED -> {
            println("Payment approved: ${result.transactionId}")
            println(result.receiptText)
        }
        PaymentStatus.DECLINED -> {
            println("Payment declined: ${result.errorMessage}")
        }
        PaymentStatus.ERROR -> {
            println("Error: ${result.errorMessage}")
        }
        else -> {
            println("Status: ${result.status}")
        }
    }
    
    terminal.disconnect()
}
```

## Current Status

✅ **Completed:**
- Payment terminal interface design
- TCP/IP communication layer
- Connection to real terminal (192.168.0.4:8009)
- Simulated terminal for testing
- Basic error handling
- Demo application

⏳ **In Progress:**
- ECR protocol implementation (need terminal protocol specification)
- TLS/SSL support (terminal appears to use encrypted connection)

## Next Steps

To complete the integration, we need:

1. **Terminal Documentation**: Protocol specification from terminal manufacturer
   - Likely ZVT (Zentrale Verrechnungsstelle Telekommandos) used by many European terminals
   - Or OPI (Open Payment Initiative)
   - Or proprietary ECR protocol

2. **Terminal Type**: Identify the exact terminal model
   - Check terminal display/label
   - Common brands: Ingenico, Verifone, PAX, Nets

3. **Protocol Analysis**: Capture actual payment transaction
   - Use Wireshark to capture successful payment
   - Analyze protocol messages
   - Implement protocol encoder/decoder

## Testing

The integration includes comprehensive testing:

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Run demo (simulated)
./run-payment-demo.sh

# Run demo (real terminal)
./run-payment-demo.sh --real
```

## Integration with Dispensers

Payment terminal integration works seamlessly with dispenser control:

```kotlin
// Create dispenser and payment terminal
val emulator = EhlDispenserEmulator(address = 1, pricePerLitreCents = 1590)
val port = InMemorySerialPort(emulator)
val communicator = EhlCommunicator(port)
val paymentTerminal = TcpPaymentTerminal("192.168.0.4", 8009)

// Start delivery
communicator.send(EhlPacketBuilder.createUnblock(1))

// Wait for delivery to complete
// ... poll dispenser state until FINISHED

// Get transaction details
val volumePacket = communicator.receive()
val amount = parseAmount(volumePacket)

// Process payment
val paymentResult = paymentTerminal.requestPayment(amount)

if (paymentResult.status == PaymentStatus.APPROVED) {
    // Mark transaction as paid
    communicator.send(EhlPacketBuilder.createZero(1))
}
```

## Files

- `src/main/kotlin/no/cloudberries/lpg/payment/PaymentTerminal.kt` - Core interfaces and implementations
- `src/main/kotlin/no/cloudberries/lpg/payment/PaymentTerminalDemo.kt` - Demo application
- `run-payment-demo.sh` - Convenient run script

## Configuration

Future configuration via environment variables:

```bash
PAYMENT_TERMINAL_HOST=192.168.0.4
PAYMENT_TERMINAL_PORT=8009
PAYMENT_TERMINAL_TIMEOUT=30000
PAYMENT_TERMINAL_TYPE=ZVT  # or OPI, CUSTOM
```

## Troubleshooting

**Connection Issues:**
```bash
# Test connectivity
nc -zv 192.168.0.4 8009

# Check terminal is on same network
ping 192.168.0.4
```

**Terminal Not Responding:**
- Ensure ECR mode is enabled on terminal
- Check terminal configuration (IP, port, protocol)
- Verify network connectivity
- Check firewall settings

## References

- [ZVT Protocol](https://www.terminalhersteller.de/downloads.aspx)
- [OPI Specification](https://www.opi-cash.com/)
- Terminal manufacturer documentation (to be added)
