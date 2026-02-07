# Baxi Protocol - Kotlin Implementation Guide

**Document Version:** 1.0  
**Date:** February 7, 2026  
**Purpose:** Implementation guide for Nets Baxi payment terminal protocol in Kotlin

---

## Table of Contents

1. [Protocol Overview](#protocol-overview)
2. [Architecture Analysis](#architecture-analysis)
3. [What We Have (From Decompilation)](#what-we-have-from-decompilation)
4. [What We're Missing](#what-were-missing)
5. [Kotlin Implementation Strategy](#kotlin-implementation-strategy)
6. [Core Protocol Implementation](#core-protocol-implementation)
7. [TCP Communication Layer](#tcp-communication-layer)
8. [Event Handling](#event-handling)
9. [Complete Implementation Example](#complete-implementation-example)
10. [Testing Strategy](#testing-strategy)
11. [Integration with LPG-EHL](#integration-with-lpg-ehl)

---

## Protocol Overview

### What is Baxi?

The Baxi protocol is Nets' proprietary communication protocol for payment terminals. It enables:
- **Payment transactions** (purchase, refund, reversal)
- **Administrative operations** (end-of-day, reports)
- **Terminal management** (configuration, status monitoring)
- **Real-time display** and receipt printing events

### Communication Method

**TCP/IP Socket Communication:**
- Host acts as TCP client
- Terminal acts as TCP server (or vice versa, configuration-dependent)
- Default port: Configurable (commonly 3000-3010 range)
- Protocol: Binary with TLD (Tag-Length-Data) encoding

### Protocol Characteristics

1. **Event-Driven**: Asynchronous callbacks for display, print, errors
2. **TLD Format**: Similar to EMV TLV but proprietary tags
3. **Stateful**: Maintains connection state and transaction context
4. **Synchronous Commands**: Commands block until response or timeout

---

## Architecture Analysis

### Original .NET DLL Structure

```
BaxiCtrl (Main Controller)
    ├── Configuration Properties (IP, Port, Logging, etc.)
    ├── Methods (Open, Close, TransferAmount, Administration)
    ├── Events (OnDisplayText, OnPrintText, OnError, etc.)
    └── Internal Communication Layer (TCP + Serial fallback)
```

### Kotlin Architecture (Proposed)

```
BaxiClient (Kotlin Implementation)
    ├── BaxiConfiguration (data class)
    ├── BaxiConnection (TCP socket management)
    ├── BaxiProtocol (TLD encoding/decoding)
    ├── BaxiEventHandler (callback interface)
    └── BaxiTransactionManager (transaction lifecycle)
```

---

## What We Have (From Decompilation)

### ✅ Complete API Surface

We have **full visibility** of:

1. **All Public Methods:**
   - `Open()` / `Close()` - Connection management
   - `TransferAmount()` - Payment transactions
   - `Administration()` - Admin operations
   - `SendTLD()` / `SendJson()` - Low-level communication

2. **All Configuration Properties:**
   - Network: `HostIpAddress`, `HostPort`
   - Serial: `ComPort`, `BaudRate` (fallback)
   - Logging: `LogFilePath`, `TraceLevel`
   - Features: `AutoGetCustomerInfo`, `Use2KBuffer`, etc.

3. **All Event Types:**
   - `OnDisplayText` - Terminal display updates
   - `OnPrintText` - Receipt data
   - `OnLastFinancialResult` - Transaction result
   - `OnError` - Error notifications
   - `OnTLDReceived` - Raw TLD data

4. **Complete Data Structures:**
   - `TransferAmountArgs` - Transaction parameters
   - `LastFinancialResultEventArgs` - Result fields
   - `AdministrationArgs` - Admin command parameters

### ✅ Clear Protocol Flow

From the API structure, we can infer:

```
1. Configure BaxiCtrl properties
2. Call Open() → OnTerminalReady event
3. Call TransferAmount() → OnDisplayText events → OnLastFinancialResult
4. Handle OnPrintText for receipts
5. Call Close() on shutdown
```

---

## What We're Missing

### ❌ Wire Protocol Details

**Critical Missing Information:**

1. **TLD Tag Definitions**
   - What byte values represent transaction amount? (e.g., tag 0x04?)
   - What tag is the result code? (e.g., tag 0x39?)
   - What tags are in `TerminalDeviceData_TLD`?
   - **Impact:** Cannot construct valid TLD messages

2. **Message Framing**
   - How are messages delimited? (STX/ETX bytes? Length prefix?)
   - Is there a message header? (command code, length, checksum?)
   - Example: `[LEN][CMD][TLD_DATA][CRC]` or `[STX][DATA][ETX]`?
   - **Impact:** Cannot parse incoming messages or frame outgoing

3. **Command Codes**
   - What byte value triggers `TransferAmount`? (e.g., 0x06?)
   - What byte value triggers `Administration`? (e.g., 0x60?)
   - Are there acknowledgment codes? (ACK=0x06, NAK=0x15?)
   - **Impact:** Cannot initiate operations

4. **Checksum/CRC Algorithm**
   - XOR checksum? CRC-16? LRC?
   - Which bytes are included in calculation?
   - **Impact:** Messages may be rejected as invalid

5. **Response Format**
   - How does terminal indicate success vs. failure?
   - What's the structure of `OnLastFinancialResult` in wire format?
   - **Impact:** Cannot parse transaction results

6. **Display Text Encoding**
   - ASCII? UTF-8? ISO-8859-1?
   - Are there special control codes for display positioning?
   - **Impact:** Display text may be garbled

### 🔍 How to Discover Missing Details

**Recommended Approaches:**

1. **Network Traffic Capture**
   ```bash
   # Use Wireshark on Windows PC running original DLL
   # Filter: tcp.port == 3000 (or configured port)
   # Capture during:
   #   - Open() call
   #   - Simple purchase transaction
   #   - Close() call
   ```

2. **DLL Interception**
   ```csharp
   // Create C# test app that logs all method calls
   var baxi = new BaxiCtrl();
   baxi.OnDisplayText += (s, e) => Log($"Display: {e.DisplayText}");
   baxi.OnTLDReceived += (s, e) => Log($"TLD: {BitConverter.ToString(e.TldData)}");
   
   baxi.HostIpAddress = "192.168.1.100";
   baxi.HostPort = 3000;
   baxi.Open();
   
   var args = new TransferAmountArgs {
       OperID = "01",
       Type1 = 0,  // Purchase
       Amount1 = 10000  // 100.00 NOK
   };
   baxi.TransferAmount(args);
   ```

3. **Protocol Fuzzing**
   ```kotlin
   // Send incrementing byte sequences to terminal
   // Observe responses to deduce structure
   socket.send(byteArrayOf(0x01, 0x00, 0x05, 0x00, 0x00))
   val response = socket.receive()
   ```

4. **Nets Documentation Request**
   - Contact Nets developer support
   - Request Baxi protocol specification
   - May require NDA

---

## Kotlin Implementation Strategy

### Design Principles

1. **Coroutine-Based**: Use Kotlin coroutines for async I/O
2. **Type-Safe**: Leverage Kotlin sealed classes and data classes
3. **Event-Driven**: Callback-based event handling
4. **Testable**: Interface-based design for mocking
5. **Resilient**: Automatic reconnection and error recovery

### Module Structure

```
no.cloudberries.lpg.payment.baxi/
├── BaxiClient.kt              // Main API
├── BaxiConfiguration.kt        // Configuration data class
├── BaxiConnection.kt           // TCP socket management
├── BaxiProtocol.kt             // TLD encoding/decoding
├── BaxiEventHandler.kt         // Event callback interface
├── BaxiTransactionManager.kt   // Transaction lifecycle
├── model/
│   ├── TransactionRequest.kt   // Transaction parameters
│   ├── TransactionResult.kt    // Transaction result
│   ├── AdministrationRequest.kt
│   └── DisplayTextEvent.kt
└── protocol/
    ├── TLDCodec.kt             // TLD format codec
    ├── MessageFraming.kt       // Message framing
    └── CommandCodes.kt         // Protocol command constants
```

---

## Core Protocol Implementation

### 1. Configuration

```kotlin
data class BaxiConfiguration(
    // Network Configuration
    val hostIpAddress: String = "192.168.1.100",
    val hostPort: Int = 3000,
    
    // Connection Settings
    val connectionTimeout: Duration = 10.seconds,
    val readTimeout: Duration = 60.seconds,
    val reconnectDelay: Duration = 5.seconds,
    val maxReconnectAttempts: Int = 3,
    
    // Logging
    val logFilePath: String = "/var/log/lpg-ehl/baxi",
    val logFilePrefix: String = "baxi",
    val traceLevel: TraceLevel = TraceLevel.INFO,
    val logAutoDeleteDays: Int = 30,
    
    // Terminal Features
    val printerWidth: Int = 40,
    val displayWidth: Int = 20,
    val cutterSupport: Boolean = true,
    val autoGetCustomerInfo: Boolean = true,
    val use2KBuffer: Boolean = true,
    
    // Serial Fallback (optional)
    val serialEnabled: Boolean = false,
    val comPort: Int = 1,
    val baudRate: Int = 115200,
    
    // Optional Features
    val deviceString: String = "LPG-EHL-Terminal",
    val vendorInfoExtended: String = "CloudBerries LPG System",
    val operatorId: String = "01"
)

enum class TraceLevel {
    OFF, ERROR, WARN, INFO, DEBUG, TRACE
}
```

### 2. TLD (Tag-Length-Data) Codec

```kotlin
/**
 * TLD Format (Hypothetical - needs verification):
 * [TAG:1-2 bytes][LENGTH:1-2 bytes][DATA:N bytes]
 */
object TLDCodec {
    
    // Hypothetical tag definitions (NEED VERIFICATION from traffic capture)
    object Tags {
        const val TRANSACTION_AMOUNT: Byte = 0x04
        const val TRANSACTION_TYPE: Byte = 0x02
        const val OPERATOR_ID: Byte = 0x08
        const val RESULT_CODE: Byte = 0x39
        const val TRUNCATED_PAN: Byte = 0x57
        const val TIMESTAMP: Byte = 0x12
        const val AUTH_CODE: Byte = 0x38
        const val SESSION_NUMBER: Byte = 0x5A
        const val TERMINAL_ID: Byte = 0x9F1C
        const val DISPLAY_TEXT: Byte = 0xD0.toByte()
        const val PRINT_TEXT: Byte = 0xD1.toByte()
    }
    
    /**
     * Encode TLD field
     */
    fun encodeTLD(tag: Int, data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        
        // Tag (1 or 2 bytes)
        if (tag <= 0xFF) {
            output.write(tag)
        } else {
            output.write((tag shr 8) and 0xFF)
            output.write(tag and 0xFF)
        }
        
        // Length (1 or 2 bytes)
        if (data.size <= 0x7F) {
            output.write(data.size)
        } else {
            output.write(0x80 or ((data.size shr 8) and 0xFF))
            output.write(data.size and 0xFF)
        }
        
        // Data
        output.write(data)
        
        return output.toByteArray()
    }
    
    /**
     * Decode TLD fields from buffer
     */
    fun decodeTLD(buffer: ByteArray): Map<Int, ByteArray> {
        val result = mutableMapOf<Int, ByteArray>()
        var index = 0
        
        while (index < buffer.size) {
            // Parse tag
            var tag = buffer[index].toInt() and 0xFF
            index++
            
            // Check for 2-byte tag
            if ((tag and 0x1F) == 0x1F && index < buffer.size) {
                tag = (tag shl 8) or (buffer[index].toInt() and 0xFF)
                index++
            }
            
            // Parse length
            if (index >= buffer.size) break
            var length = buffer[index].toInt() and 0xFF
            index++
            
            // Check for extended length
            if ((length and 0x80) != 0 && index < buffer.size) {
                val lengthBytes = length and 0x7F
                length = 0
                repeat(lengthBytes) {
                    if (index < buffer.size) {
                        length = (length shl 8) or (buffer[index].toInt() and 0xFF)
                        index++
                    }
                }
            }
            
            // Extract data
            if (index + length <= buffer.size) {
                val data = buffer.copyOfRange(index, index + length)
                result[tag] = data
                index += length
            } else {
                break  // Malformed TLD
            }
        }
        
        return result
    }
    
    /**
     * Build transaction request TLD
     */
    fun buildTransactionRequest(
        operatorId: String,
        transactionType: Int,
        amount: Int,
        authCode: String? = null
    ): ByteArray {
        val output = ByteArrayOutputStream()
        
        // Operator ID
        output.write(encodeTLD(Tags.OPERATOR_ID.toInt(), operatorId.toByteArray()))
        
        // Transaction type
        output.write(encodeTLD(Tags.TRANSACTION_TYPE.toInt(), byteArrayOf(transactionType.toByte())))
        
        // Amount (4 bytes, big-endian)
        val amountBytes = ByteBuffer.allocate(4).putInt(amount).array()
        output.write(encodeTLD(Tags.TRANSACTION_AMOUNT.toInt(), amountBytes))
        
        // Optional auth code
        authCode?.let {
            output.write(encodeTLD(Tags.AUTH_CODE.toInt(), it.toByteArray()))
        }
        
        return output.toByteArray()
    }
    
    /**
     * Parse transaction result TLD
     */
    fun parseTransactionResult(tldData: ByteArray): TransactionResult {
        val fields = decodeTLD(tldData)
        
        return TransactionResult(
            resultCode = fields[Tags.RESULT_CODE.toInt()]?.get(0)?.toInt() ?: -1,
            truncatedPan = fields[Tags.TRUNCATED_PAN.toInt()]?.toString(Charsets.UTF_8),
            timestamp = fields[Tags.TIMESTAMP.toInt()]?.toString(Charsets.UTF_8),
            authCode = fields[Tags.AUTH_CODE.toInt()]?.toString(Charsets.UTF_8),
            sessionNumber = fields[Tags.SESSION_NUMBER.toInt()]?.toString(Charsets.UTF_8),
            terminalId = fields[Tags.TERMINAL_ID.toInt()]?.toString(Charsets.UTF_8),
            amount = fields[Tags.TRANSACTION_AMOUNT.toInt()]?.let {
                ByteBuffer.wrap(it).int
            } ?: 0
        )
    }
}
```

### 3. Message Framing

```kotlin
/**
 * Message framing (Hypothetical - needs verification)
 * 
 * Possible formats:
 * Format A: [STX][LEN:2][CMD][TLD_DATA][CRC:2][ETX]
 * Format B: [LEN:2][CMD][TLD_DATA][CRC:2]
 * Format C: [CMD][LEN:2][TLD_DATA][CRC:2]
 */
object MessageFraming {
    
    // Control bytes (hypothetical)
    const val STX: Byte = 0x02
    const val ETX: Byte = 0x03
    const val ACK: Byte = 0x06
    const val NAK: Byte = 0x15
    
    // Command codes (hypothetical - NEED VERIFICATION)
    object Commands {
        const val OPEN_SESSION: Byte = 0x10
        const val CLOSE_SESSION: Byte = 0x11
        const val TRANSFER_AMOUNT: Byte = 0x20
        const val ADMINISTRATION: Byte = 0x60
        const val SEND_TLD: Byte = 0x30
        const val TERMINAL_READY: Byte = 0x70.toByte()
        const val DISPLAY_TEXT: Byte = 0x71.toByte()
        const val PRINT_TEXT: Byte = 0x72.toByte()
        const val TRANSACTION_RESULT: Byte = 0x73.toByte()
    }
    
    /**
     * Frame a message for transmission
     */
    fun frameMessage(command: Byte, tldData: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        
        // STX
        output.write(STX.toInt())
        
        // Length (2 bytes, big-endian): command + data + CRC
        val length = 1 + tldData.size + 2
        output.write((length shr 8) and 0xFF)
        output.write(length and 0xFF)
        
        // Command
        output.write(command.toInt())
        
        // TLD Data
        output.write(tldData)
        
        // CRC-16 (hypothetical - might be XOR checksum instead)
        val crc = calculateCRC(command, tldData)
        output.write((crc shr 8) and 0xFF)
        output.write(crc and 0xFF)
        
        // ETX
        output.write(ETX.toInt())
        
        return output.toByteArray()
    }
    
    /**
     * Parse received message
     */
    fun parseMessage(rawData: ByteArray): ParsedMessage? {
        if (rawData.size < 6) return null  // Minimum: STX+LEN+CMD+CRC+ETX
        
        var index = 0
        
        // Check STX
        if (rawData[index] != STX) return null
        index++
        
        // Parse length
        val length = ((rawData[index].toInt() and 0xFF) shl 8) or (rawData[index + 1].toInt() and 0xFF)
        index += 2
        
        // Check we have enough data
        if (rawData.size < index + length + 1) return null  // +1 for ETX
        
        // Parse command
        val command = rawData[index]
        index++
        
        // Extract TLD data
        val tldDataLength = length - 1 - 2  // -1 for command, -2 for CRC
        val tldData = rawData.copyOfRange(index, index + tldDataLength)
        index += tldDataLength
        
        // Parse CRC
        val receivedCrc = ((rawData[index].toInt() and 0xFF) shl 8) or (rawData[index + 1].toInt() and 0xFF)
        index += 2
        
        // Check ETX
        if (rawData[index] != ETX) return null
        
        // Verify CRC
        val calculatedCrc = calculateCRC(command, tldData)
        if (receivedCrc != calculatedCrc) {
            return null  // CRC mismatch
        }
        
        return ParsedMessage(command, tldData)
    }
    
    /**
     * Calculate CRC-16 (CCITT)
     * Note: Might be different algorithm - verify with traffic capture
     */
    private fun calculateCRC(command: Byte, data: ByteArray): Int {
        var crc = 0xFFFF
        
        // Include command in CRC
        crc = updateCRC(crc, command.toInt() and 0xFF)
        
        // Include data in CRC
        for (b in data) {
            crc = updateCRC(crc, b.toInt() and 0xFF)
        }
        
        return crc xor 0xFFFF
    }
    
    private fun updateCRC(crc: Int, byte: Int): Int {
        var c = crc
        c = c xor (byte shl 8)
        repeat(8) {
            c = if ((c and 0x8000) != 0) {
                (c shl 1) xor 0x1021
            } else {
                c shl 1
            }
        }
        return c and 0xFFFF
    }
}

data class ParsedMessage(
    val command: Byte,
    val tldData: ByteArray
)
```

---

## TCP Communication Layer

```kotlin
/**
 * TCP connection manager for Baxi terminal
 */
class BaxiConnection(
    private val config: BaxiConfiguration,
    private val eventHandler: BaxiEventHandler
) {
    private val logger = LoggerFactory.getLogger(BaxiConnection::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    private val isConnected = AtomicBoolean(false)
    private val receiveJob = AtomicReference<Job?>(null)
    
    /**
     * Open connection to terminal
     */
    suspend fun open(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.info("Connecting to Baxi terminal at ${config.hostIpAddress}:${config.hostPort}")
            
            // Create socket with timeout
            socket = Socket()
            socket?.connect(
                InetSocketAddress(config.hostIpAddress, config.hostPort),
                config.connectionTimeout.inWholeMilliseconds.toInt()
            )
            socket?.soTimeout = config.readTimeout.inWholeMilliseconds.toInt()
            
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()
            
            isConnected.set(true)
            
            // Start receive loop
            startReceiveLoop()
            
            // Send OPEN_SESSION command
            sendCommand(MessageFraming.Commands.OPEN_SESSION, byteArrayOf())
            
            logger.info("Connected to Baxi terminal")
            Result.success(Unit)
            
        } catch (e: Exception) {
            logger.error("Failed to connect to Baxi terminal", e)
            close()
            Result.failure(e)
        }
    }
    
    /**
     * Close connection
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        if (!isConnected.getAndSet(false)) return@withContext
        
        logger.info("Closing Baxi terminal connection")
        
        try {
            // Send CLOSE_SESSION command
            sendCommand(MessageFraming.Commands.CLOSE_SESSION, byteArrayOf())
            
            // Stop receive loop
            receiveJob.getAndSet(null)?.cancel()
            
            // Close streams
            outputStream?.close()
            inputStream?.close()
            socket?.close()
            
        } catch (e: Exception) {
            logger.error("Error closing connection", e)
        } finally {
            socket = null
            inputStream = null
            outputStream = null
        }
        
        logger.info("Baxi terminal connection closed")
    }
    
    /**
     * Send command with TLD data
     */
    suspend fun sendCommand(command: Byte, tldData: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            return@withContext Result.failure(IOException("Not connected"))
        }
        
        try {
            val framedMessage = MessageFraming.frameMessage(command, tldData)
            
            if (logger.isDebugEnabled) {
                logger.debug("Sending command 0x${command.toString(16)}: ${framedMessage.toHexString()}")
            }
            
            outputStream?.write(framedMessage)
            outputStream?.flush()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            logger.error("Failed to send command", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start receive loop (coroutine)
     */
    private fun startReceiveLoop() {
        val job = scope.launch {
            val buffer = ByteArray(2048)
            val messageBuffer = ByteArrayOutputStream()
            
            while (isConnected.get()) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    
                    if (bytesRead == -1) {
                        logger.warn("Connection closed by terminal")
                        handleConnectionLost()
                        break
                    }
                    
                    if (bytesRead > 0) {
                        messageBuffer.write(buffer, 0, bytesRead)
                        
                        // Try to parse complete message
                        val messageData = messageBuffer.toByteArray()
                        val parsedMessage = MessageFraming.parseMessage(messageData)
                        
                        if (parsedMessage != null) {
                            // Complete message received
                            handleReceivedMessage(parsedMessage)
                            messageBuffer.reset()
                        }
                        // else: wait for more data
                    }
                    
                } catch (e: SocketTimeoutException) {
                    // Normal timeout, continue
                } catch (e: Exception) {
                    logger.error("Error in receive loop", e)
                    handleConnectionLost()
                    break
                }
            }
        }
        
        receiveJob.set(job)
    }
    
    /**
     * Handle received message
     */
    private suspend fun handleReceivedMessage(message: ParsedMessage) {
        if (logger.isDebugEnabled) {
            logger.debug("Received command 0x${message.command.toString(16)}")
        }
        
        when (message.command) {
            MessageFraming.Commands.TERMINAL_READY -> {
                eventHandler.onTerminalReady()
            }
            
            MessageFraming.Commands.DISPLAY_TEXT -> {
                val tldFields = TLDCodec.decodeTLD(message.tldData)
                val displayText = tldFields[TLDCodec.Tags.DISPLAY_TEXT.toInt()]
                    ?.toString(Charsets.UTF_8) ?: ""
                eventHandler.onDisplayText(displayText)
            }
            
            MessageFraming.Commands.PRINT_TEXT -> {
                val tldFields = TLDCodec.decodeTLD(message.tldData)
                val printText = tldFields[TLDCodec.Tags.PRINT_TEXT.toInt()]
                    ?.toString(Charsets.UTF_8) ?: ""
                eventHandler.onPrintText(printText)
            }
            
            MessageFraming.Commands.TRANSACTION_RESULT -> {
                val result = TLDCodec.parseTransactionResult(message.tldData)
                eventHandler.onTransactionResult(result)
            }
            
            else -> {
                logger.warn("Unknown command: 0x${message.command.toString(16)}")
                eventHandler.onError(BaxiError(-1, "Unknown command: ${message.command}"))
            }
        }
    }
    
    /**
     * Handle connection lost
     */
    private suspend fun handleConnectionLost() {
        close()
        eventHandler.onError(BaxiError(-2, "Connection lost"))
        
        // Attempt reconnection if configured
        if (config.maxReconnectAttempts > 0) {
            attemptReconnection()
        }
    }
    
    /**
     * Attempt reconnection
     */
    private suspend fun attemptReconnection() {
        repeat(config.maxReconnectAttempts) { attempt ->
            logger.info("Reconnection attempt ${attempt + 1}/${config.maxReconnectAttempts}")
            
            delay(config.reconnectDelay)
            
            val result = open()
            if (result.isSuccess) {
                logger.info("Reconnection successful")
                return
            }
        }
        
        logger.error("Reconnection failed after ${config.maxReconnectAttempts} attempts")
    }
}

// Helper extension
fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }
```

---

## Event Handling

```kotlin
/**
 * Event handler interface for Baxi callbacks
 */
interface BaxiEventHandler {
    fun onTerminalReady()
    fun onDisplayText(text: String)
    fun onPrintText(text: String)
    fun onTransactionResult(result: TransactionResult)
    fun onError(error: BaxiError)
}

/**
 * Transaction result data class
 */
data class TransactionResult(
    val resultCode: Int,  // 0 = approved, others = rejected
    val truncatedPan: String? = null,
    val timestamp: String? = null,
    val authCode: String? = null,
    val sessionNumber: String? = null,
    val terminalId: String? = null,
    val amount: Int = 0,
    val tipAmount: Int = 0,
    val surchargeAmount: Int = 0,
    val issuerName: String? = null,
    val verificationMethod: Int = 0,
    val responseCode: String? = null
) {
    val isApproved: Boolean get() = resultCode == 0
    val totalAmount: Int get() = amount + tipAmount + surchargeAmount
}

/**
 * Error data class
 */
data class BaxiError(
    val errorCode: Int,
    val errorMessage: String
)

/**
 * Default event handler implementation
 */
class DefaultBaxiEventHandler : BaxiEventHandler {
    private val logger = LoggerFactory.getLogger(DefaultBaxiEventHandler::class.java)
    
    override fun onTerminalReady() {
        logger.info("Terminal is ready")
    }
    
    override fun onDisplayText(text: String) {
        logger.info("Display: $text")
    }
    
    override fun onPrintText(text: String) {
        logger.info("Print: $text")
    }
    
    override fun onTransactionResult(result: TransactionResult) {
        logger.info("Transaction result: ${if (result.isApproved) "APPROVED" else "REJECTED"}")
        logger.info("  Amount: ${result.totalAmount}")
        logger.info("  PAN: ${result.truncatedPan}")
        logger.info("  Auth: ${result.authCode}")
    }
    
    override fun onError(error: BaxiError) {
        logger.error("Baxi error [${error.errorCode}]: ${error.errorMessage}")
    }
}
```

---

## Complete Implementation Example

```kotlin
/**
 * Main Baxi client
 */
class BaxiClient(
    private val config: BaxiConfiguration = BaxiConfiguration(),
    private val eventHandler: BaxiEventHandler = DefaultBaxiEventHandler()
) {
    private val logger = LoggerFactory.getLogger(BaxiClient::class.java)
    private val connection = BaxiConnection(config, eventHandler)
    
    private val transactionLock = Mutex()
    private var currentTransaction = CompletableDeferred<TransactionResult>()
    
    /**
     * Open connection to terminal
     */
    suspend fun open(): Result<Unit> {
        return connection.open()
    }
    
    /**
     * Close connection
     */
    suspend fun close() {
        connection.close()
    }
    
    /**
     * Perform payment transaction
     */
    suspend fun transferAmount(
        amount: Int,  // In øre (smallest currency unit)
        transactionType: TransactionType = TransactionType.PURCHASE,
        authCode: String? = null
    ): Result<TransactionResult> = transactionLock.withLock {
        try {
            logger.info("Starting transaction: type=$transactionType, amount=$amount")
            
            // Prepare transaction
            currentTransaction = CompletableDeferred()
            
            // Build TLD request
            val tldData = TLDCodec.buildTransactionRequest(
                operatorId = config.operatorId,
                transactionType = transactionType.code,
                amount = amount,
                authCode = authCode
            )
            
            // Send command
            connection.sendCommand(MessageFraming.Commands.TRANSFER_AMOUNT, tldData)
                .getOrElse { return Result.failure(it) }
            
            // Wait for result (with timeout)
            val result = withTimeoutOrNull(config.readTimeout) {
                currentTransaction.await()
            }
            
            if (result == null) {
                return Result.failure(TimeoutException("Transaction timeout"))
            }
            
            logger.info("Transaction completed: ${if (result.isApproved) "APPROVED" else "REJECTED"}")
            Result.success(result)
            
        } catch (e: Exception) {
            logger.error("Transaction failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Perform administrative operation
     */
    suspend fun administration(
        adminCode: AdministrationCode,
        operatorId: String = config.operatorId
    ): Result<Unit> {
        try {
            logger.info("Administration: $adminCode")
            
            // Build TLD request
            val tldData = ByteArrayOutputStream().apply {
                write(TLDCodec.encodeTLD(TLDCodec.Tags.OPERATOR_ID.toInt(), operatorId.toByteArray()))
                write(TLDCodec.encodeTLD(0x60, byteArrayOf(adminCode.code.toByte())))
            }.toByteArray()
            
            // Send command
            connection.sendCommand(MessageFraming.Commands.ADMINISTRATION, tldData)
                .getOrElse { return Result.failure(it) }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            logger.error("Administration failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Handle transaction result from event
     */
    fun completeTransaction(result: TransactionResult) {
        currentTransaction.complete(result)
    }
}

/**
 * Transaction types
 */
enum class TransactionType(val code: Int) {
    PURCHASE(0),
    REFUND(1),
    REVERSAL(2),
    CASH_ADVANCE(3),
    BALANCE_INQUIRY(4)
}

/**
 * Administration codes
 */
enum class AdministrationCode(val code: Int) {
    END_OF_DAY(1),
    X_REPORT(2),
    Z_REPORT(3),
    RECONCILIATION(10),
    REPRINT_LAST_RECEIPT(20)
}
```

### Usage Example

```kotlin
suspend fun main() {
    // Configure Baxi client
    val config = BaxiConfiguration(
        hostIpAddress = "192.168.1.100",
        hostPort = 3000,
        operatorId = "01",
        logFilePath = "/var/log/lpg-ehl/baxi"
    )
    
    // Custom event handler
    val eventHandler = object : BaxiEventHandler {
        override fun onTerminalReady() {
            println("✓ Terminal is ready for transactions")
        }
        
        override fun onDisplayText(text: String) {
            println("📺 Display: $text")
        }
        
        override fun onPrintText(text: String) {
            println("🖨 Print: $text")
        }
        
        override fun onTransactionResult(result: TransactionResult) {
            if (result.isApproved) {
                println("✓ Transaction APPROVED")
                println("  Amount: ${result.totalAmount / 100.0} NOK")
                println("  Card: ${result.truncatedPan}")
                println("  Auth: ${result.authCode}")
            } else {
                println("✗ Transaction REJECTED: ${result.responseCode}")
            }
        }
        
        override fun onError(error: BaxiError) {
            System.err.println("✗ Error: ${error.errorMessage}")
        }
    }
    
    // Create client
    val baxi = BaxiClient(config, eventHandler)
    
    try {
        // Open connection
        baxi.open().getOrThrow()
        println("Connected to Baxi terminal")
        
        // Wait for terminal ready
        delay(2000)
        
        // Perform transaction
        val amount = 10000  // 100.00 NOK
        val result = baxi.transferAmount(amount, TransactionType.PURCHASE).getOrThrow()
        
        if (result.isApproved) {
            println("Payment successful!")
            // Save transaction to database, etc.
        } else {
            println("Payment failed: ${result.responseCode}")
        }
        
        // End of day
        baxi.administration(AdministrationCode.END_OF_DAY).getOrThrow()
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        baxi.close()
    }
}
```

---

## Testing Strategy

### Unit Tests

```kotlin
class TLDCodecTest {
    @Test
    fun `encode TLD field correctly`() {
        val data = "TEST".toByteArray()
        val tld = TLDCodec.encodeTLD(0x04, data)
        
        assertEquals(0x04, tld[0].toInt())  // Tag
        assertEquals(4, tld[1].toInt())     // Length
        assertArrayEquals(data, tld.copyOfRange(2, 6))  // Data
    }
    
    @Test
    fun `decode TLD fields correctly`() {
        val buffer = byteArrayOf(
            0x04, 0x04, 'T'.code.toByte(), 'E'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(),
            0x08, 0x02, '0'.code.toByte(), '1'.code.toByte()
        )
        
        val fields = TLDCodec.decodeTLD(buffer)
        
        assertEquals(2, fields.size)
        assertEquals("TEST", fields[0x04]?.toString(Charsets.UTF_8))
        assertEquals("01", fields[0x08]?.toString(Charsets.UTF_8))
    }
    
    @Test
    fun `build transaction request with correct structure`() {
        val tld = TLDCodec.buildTransactionRequest(
            operatorId = "01",
            transactionType = 0,
            amount = 10000
        )
        
        val fields = TLDCodec.decodeTLD(tld)
        
        assertTrue(fields.containsKey(TLDCodec.Tags.OPERATOR_ID.toInt()))
        assertTrue(fields.containsKey(TLDCodec.Tags.TRANSACTION_TYPE.toInt()))
        assertTrue(fields.containsKey(TLDCodec.Tags.TRANSACTION_AMOUNT.toInt()))
    }
}
```

### Integration Tests (with Mock Terminal)

```kotlin
class BaxiClientIntegrationTest {
    
    private lateinit var mockTerminal: MockBaxiTerminal
    private lateinit var baxi: BaxiClient
    
    @BeforeEach
    fun setup() {
        // Start mock terminal server
        mockTerminal = MockBaxiTerminal(port = 3000)
        mockTerminal.start()
        
        // Create client
        val config = BaxiConfiguration(
            hostIpAddress = "localhost",
            hostPort = 3000
        )
        baxi = BaxiClient(config)
    }
    
    @AfterEach
    fun teardown() {
        runBlocking {
            baxi.close()
        }
        mockTerminal.stop()
    }
    
    @Test
    fun `open connection successfully`() = runBlocking {
        val result = baxi.open()
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `transfer amount successfully`() = runBlocking {
        baxi.open().getOrThrow()
        
        // Mock terminal will approve
        mockTerminal.setNextResponse(approved = true, authCode = "123456")
        
        val result = baxi.transferAmount(
            amount = 10000,
            transactionType = TransactionType.PURCHASE
        ).getOrThrow()
        
        assertTrue(result.isApproved)
        assertEquals("123456", result.authCode)
    }
    
    @Test
    fun `handle rejection correctly`() = runBlocking {
        baxi.open().getOrThrow()
        
        // Mock terminal will reject
        mockTerminal.setNextResponse(approved = false, responseCode = "05")
        
        val result = baxi.transferAmount(
            amount = 10000,
            transactionType = TransactionType.PURCHASE
        ).getOrThrow()
        
        assertFalse(result.isApproved)
        assertEquals("05", result.responseCode)
    }
}
```

---

## Integration with LPG-EHL

### Spring Boot Configuration

```kotlin
@Configuration
@ConditionalOnProperty("payment.terminal.enabled", havingValue = "true")
class BaxiPaymentConfiguration {
    
    @Bean
    fun baxiConfiguration(
        @Value("\${payment.terminal.host}") host: String,
        @Value("\${payment.terminal.port}") port: Int,
        @Value("\${payment.terminal.operator-id}") operatorId: String
    ): BaxiConfiguration {
        return BaxiConfiguration(
            hostIpAddress = host,
            hostPort = port,
            operatorId = operatorId,
            logFilePath = "/var/log/lpg-ehl/baxi",
            autoGetCustomerInfo = true
        )
    }
    
    @Bean
    fun baxiClient(
        config: BaxiConfiguration,
        eventHandler: BaxiEventHandler
    ): BaxiClient {
        return BaxiClient(config, eventHandler)
    }
    
    @Bean
    fun baxiEventHandler(
        transactionService: TransactionService,
        eventPublisher: EventPublisher
    ): BaxiEventHandler {
        return LpgBaxiEventHandler(transactionService, eventPublisher)
    }
}
```

### LPG-Specific Event Handler

```kotlin
class LpgBaxiEventHandler(
    private val transactionService: TransactionService,
    private val eventPublisher: EventPublisher
) : BaxiEventHandler {
    
    private val logger = LoggerFactory.getLogger(LpgBaxiEventHandler::class.java)
    
    override fun onTerminalReady() {
        logger.info("Baxi terminal is ready")
        eventPublisher.publishEvent(PaymentTerminalReadyEvent())
    }
    
    override fun onDisplayText(text: String) {
        logger.debug("Display: $text")
        eventPublisher.publishEvent(PaymentDisplayTextEvent(text))
    }
    
    override fun onPrintText(text: String) {
        logger.debug("Receipt text received")
        eventPublisher.publishEvent(PaymentReceiptEvent(text))
    }
    
    override fun onTransactionResult(result: TransactionResult) {
        logger.info("Payment result: ${if (result.isApproved) "APPROVED" else "REJECTED"}")
        
        // Update transaction in database
        if (result.isApproved) {
            transactionService.markAsPaid(
                authCode = result.authCode,
                truncatedPan = result.truncatedPan,
                timestamp = result.timestamp
            )
        }
        
        // Publish event
        eventPublisher.publishEvent(PaymentResultEvent(result))
    }
    
    override fun onError(error: BaxiError) {
        logger.error("Baxi error: ${error.errorMessage}")
        eventPublisher.publishEvent(PaymentErrorEvent(error))
    }
}
```

### Service Integration

```kotlin
@Service
class PaymentService(
    private val baxiClient: BaxiClient,
    private val transactionRepository: TransactionRepository
) {
    
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)
    
    @PostConstruct
    fun initialize() {
        runBlocking {
            baxiClient.open().onFailure {
                logger.error("Failed to open Baxi terminal", it)
            }
        }
    }
    
    @PreDestroy
    fun shutdown() {
        runBlocking {
            baxiClient.close()
        }
    }
    
    suspend fun processPayment(transaction: Transaction): PaymentResult {
        logger.info("Processing payment for transaction ${transaction.transactionId}")
        
        val result = baxiClient.transferAmount(
            amount = transaction.amountOre,
            transactionType = TransactionType.PURCHASE
        ).getOrElse {
            logger.error("Payment failed", it)
            return PaymentResult.Error(it.message ?: "Unknown error")
        }
        
        return if (result.isApproved) {
            PaymentResult.Approved(result)
        } else {
            PaymentResult.Rejected(result.responseCode)
        }
    }
    
    suspend fun performEndOfDay(): Result<Unit> {
        return baxiClient.administration(AdministrationCode.END_OF_DAY)
    }
}

sealed class PaymentResult {
    data class Approved(val result: TransactionResult) : PaymentResult()
    data class Rejected(val responseCode: String?) : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}
```

---

## Summary: Kotlin Implementation

### ✅ Strengths

1. **Type Safety**: Kotlin's type system catches errors at compile-time
2. **Coroutines**: Natural async/await for I/O operations
3. **Null Safety**: Prevents null pointer exceptions
4. **Interoperability**: Can call Java libraries if needed
5. **Data Classes**: Concise model definitions
6. **Sealed Classes**: Type-safe result handling

### ⚠️ Challenges

1. **Protocol Reverse Engineering**: Need Wireshark captures to determine:
   - Exact TLD tag values
   - Message framing format
   - Checksum algorithm
   - Command codes

2. **Testing**: Requires real terminal or comprehensive mock

3. **Error Handling**: Need comprehensive error code mapping

### 📋 Next Steps

1. **Capture Network Traffic**: Use original DLL with Wireshark
2. **Implement Mock Terminal**: For unit/integration testing
3. **Test Against Real Terminal**: Validate protocol implementation
4. **Add Retry Logic**: Handle transient failures
5. **Implement Logging**: Comprehensive protocol tracing
6. **Add Metrics**: Monitor payment success rates
7. **Security**: Encrypt sensitive logs (PAN, auth codes)

---

**End of Kotlin Implementation Guide**
