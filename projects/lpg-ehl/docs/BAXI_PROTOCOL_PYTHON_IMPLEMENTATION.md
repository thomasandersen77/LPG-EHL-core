# Baxi Protocol - Python Implementation Guide

**Document Version:** 1.0  
**Date:** February 7, 2026  
**Purpose:** Implementation guide for Nets Baxi payment terminal protocol in Python

---

## Table of Contents

1. [Protocol Overview](#protocol-overview)
2. [Python Advantages for Protocol Implementation](#python-advantages-for-protocol-implementation)
3. [What We Have (From Decompilation)](#what-we-have-from-decompilation)
4. [What We're Missing](#what-were-missing)
5. [Python Implementation Strategy](#python-implementation-strategy)
6. [Core Protocol Implementation](#core-protocol-implementation)
7. [TCP Communication Layer](#tcp-communication-layer)
8. [Event Handling](#event-handling)
9. [Complete Implementation Example](#complete-implementation-example)
10. [Testing Strategy](#testing-strategy)
11. [Deployment Considerations](#deployment-considerations)

---

## Protocol Overview

### Baxi Protocol Essentials

The Baxi protocol from Nets is a proprietary binary protocol for payment terminal communication:

- **Transport**: TCP/IP (primary) or Serial (fallback)
- **Format**: TLD (Tag-Length-Data) encoding similar to EMV TLV
- **Pattern**: Command-response with asynchronous events
- **Data**: Binary byte streams with text embedded in TLD fields

### Why Python for Protocol Reverse Engineering?

Python is **excellent** for protocol reverse engineering and implementation:

✅ **Interactive REPL**: Test ideas immediately  
✅ **Byte Manipulation**: Clean syntax for binary data  
✅ **Rich Libraries**: `struct`, `socket`, `asyncio` built-in  
✅ **Quick Prototyping**: Fast iteration cycle  
✅ **Excellent Tooling**: Wireshark + `scapy` for analysis  

---

## Python Advantages for Protocol Implementation

### 1. Byte Handling is Natural

```python
# Python makes binary data easy
data = b'\x02\x00\x10\x04\x04TEST\x12\x34\x03'
tag = data[3]  # 0x04
length = data[4]  # 0x04
value = data[5:9]  # b'TEST'

# Kotlin equivalent is more verbose
val tag = data[3].toInt() and 0xFF
val length = data[4].toInt() and 0xFF
val value = data.copyOfRange(5, 9)
```

### 2. Struct Packing/Unpacking

```python
import struct

# Pack transaction amount (4 bytes, big-endian)
amount_bytes = struct.pack('>I', 10000)  # b'\x00\x00\x27\x10'

# Unpack received data
result_code, amount = struct.unpack('>BI', data[0:5])
```

### 3. Async/Await (Python 3.7+)

```python
import asyncio

async def handle_transaction(terminal, amount):
    await terminal.connect()
    result = await terminal.transfer_amount(amount)
    return result

# Run
result = asyncio.run(handle_transaction(terminal, 10000))
```

### 4. Interactive Debugging with REPL

```python
# Live session - test protocol encoding
>>> from baxi_protocol import TLDCodec
>>> tld = TLDCodec.encode_tld(0x04, b'\x00\x00\x27\x10')
>>> tld.hex()
'04040000271001'
>>> TLDCodec.decode_tld(tld)
{4: b'\x00\x00\x27\x10'}
```

### 5. Excellent Libraries for Protocol Work

- **`socket`**: TCP/UDP communication
- **`asyncio`**: Async I/O
- **`struct`**: Binary data packing
- **`logging`**: Built-in logging
- **`scapy`**: Packet crafting and analysis
- **`pyserial`**: Serial communication (fallback)
- **`pytest`**: Testing framework

---

## What We Have (From Decompilation)

### ✅ Complete API Surface

From the .NET DLL decompilation, we have:

1. **Configuration Properties**: IP, port, logging, feature flags
2. **Methods**: `Open()`, `TransferAmount()`, `Administration()`, `Close()`
3. **Events**: `OnDisplayText`, `OnPrintText`, `OnTransactionResult`, etc.
4. **Data Structures**: Transaction args, result fields, error codes

### ✅ Inferred Protocol Flow

```
1. Configure terminal settings (IP, port)
2. Open() → OnTerminalReady event
3. TransferAmount(args) → OnDisplayText (multiple) → OnTransactionResult
4. Handle OnPrintText for receipt
5. Close() on shutdown
```

---

## What We're Missing

### ❌ Wire Protocol Specifics

**Critical Unknown Details:**

1. **TLD Tag Definitions**
   - What is the tag for amount? (0x04? 0x9F02?)
   - What tag represents result code?
   - How are nested TLD structures handled?

2. **Message Framing**
   - STX/ETX bytes? Length prefix?
   - Message header structure?
   - Example: `[LEN:2][CMD:1][DATA][CRC:2]`?

3. **Command Codes**
   - What byte triggers `TransferAmount`?
   - What byte triggers `Administration`?
   - ACK/NAK handling?

4. **Checksum Algorithm**
   - CRC-16? XOR? LRC?
   - Which bytes included?

5. **Character Encoding**
   - ASCII? UTF-8? ISO-8859-1?

### 🔍 Discovery Methods

**Recommended Approach:**

1. **Wireshark Capture** (Most Important)
   ```bash
   # Capture on Windows PC running original DLL
   # Filter: tcp.port == 3000
   # Capture full transaction: Open → Pay → Close
   ```

2. **Python Packet Analysis**
   ```python
   from scapy.all import *
   
   # Read pcap file
   packets = rdpcap('baxi_capture.pcap')
   
   # Analyze
   for pkt in packets:
       if TCP in pkt and pkt[TCP].dport == 3000:
           print(pkt[TCP].payload.hex())
   ```

3. **Create Test Harness**
   ```python
   # C# test app that logs all calls
   var baxi = new BaxiCtrl();
   baxi.OnTLDReceived += (s, e) => {
       File.AppendAllText("tld_log.txt", 
           $"{DateTime.Now}: {BitConverter.ToString(e.TldData)}\n");
   };
   ```

---

## Python Implementation Strategy

### Design Philosophy

1. **Pythonic**: Use Python idioms and conventions
2. **Async-First**: Use `asyncio` for I/O operations
3. **Type Hints**: Use Python 3.10+ type annotations
4. **Dataclasses**: Use `@dataclass` for clean models
5. **Logging**: Comprehensive structured logging
6. **Testing**: Unit tests with `pytest`, mocks with `pytest-asyncio`

### Project Structure

```
baxi_protocol/
├── __init__.py
├── client.py              # Main BaxiClient class
├── config.py              # Configuration dataclass
├── connection.py          # TCP socket management
├── protocol/
│   ├── __init__.py
│   ├── tld_codec.py       # TLD encoding/decoding
│   ├── framing.py         # Message framing
│   └── commands.py        # Command constants
├── models/
│   ├── __init__.py
│   ├── transaction.py     # Transaction models
│   ├── events.py          # Event models
│   └── errors.py          # Error types
├── handlers/
│   ├── __init__.py
│   └── event_handler.py   # Event handler base
└── utils/
    ├── __init__.py
    └── logging.py         # Logging utilities

tests/
├── test_tld_codec.py
├── test_framing.py
├── test_client.py
└── test_integration.py
```

---

## Core Protocol Implementation

### 1. Configuration

```python
from dataclasses import dataclass, field
from typing import Optional
from enum import IntEnum

@dataclass
class BaxiConfiguration:
    """Baxi terminal configuration"""
    
    # Network settings
    host_ip: str = "192.168.1.100"
    host_port: int = 3000
    
    # Connection settings
    connection_timeout: float = 10.0  # seconds
    read_timeout: float = 60.0
    reconnect_delay: float = 5.0
    max_reconnect_attempts: int = 3
    
    # Logging
    log_file_path: str = "/var/log/lpg-ehl/baxi"
    log_file_prefix: str = "baxi"
    trace_level: str = "INFO"  # DEBUG, INFO, WARN, ERROR
    log_auto_delete_days: int = 30
    
    # Terminal features
    printer_width: int = 40
    display_width: int = 20
    cutter_support: bool = True
    auto_get_customer_info: bool = True
    use_2k_buffer: bool = True
    
    # Serial fallback (optional)
    serial_enabled: bool = False
    com_port: int = 1
    baud_rate: int = 115200
    
    # Identification
    device_string: str = "LPG-EHL-Terminal"
    vendor_info_extended: str = "CloudBerries LPG System"
    operator_id: str = "01"


class TraceLevel(IntEnum):
    """Logging trace levels"""
    OFF = 0
    ERROR = 1
    WARN = 2
    INFO = 3
    DEBUG = 4
    TRACE = 5
```

### 2. TLD (Tag-Length-Data) Codec

```python
"""
TLD encoding/decoding module

Format (hypothetical - needs verification):
    [TAG: 1-2 bytes][LENGTH: 1-2 bytes][DATA: N bytes]
"""

import struct
from typing import Dict, Optional, Tuple
from io import BytesIO


class TLDTags:
    """TLD tag constants (HYPOTHETICAL - verify with capture)"""
    TRANSACTION_AMOUNT = 0x04
    TRANSACTION_TYPE = 0x02
    OPERATOR_ID = 0x08
    RESULT_CODE = 0x39
    TRUNCATED_PAN = 0x57
    TIMESTAMP = 0x12
    AUTH_CODE = 0x38
    SESSION_NUMBER = 0x5A
    TERMINAL_ID = 0x9F1C
    DISPLAY_TEXT = 0xD0
    PRINT_TEXT = 0xD1


class TLDCodec:
    """TLD encoding and decoding"""
    
    @staticmethod
    def encode_tld(tag: int, data: bytes) -> bytes:
        """
        Encode a TLD field
        
        Args:
            tag: Tag identifier (1 or 2 bytes)
            data: Data to encode
            
        Returns:
            Encoded TLD bytes
        """
        output = BytesIO()
        
        # Encode tag (1 or 2 bytes)
        if tag <= 0xFF:
            output.write(struct.pack('B', tag))
        else:
            output.write(struct.pack('>H', tag))
        
        # Encode length (1 or 2 bytes)
        length = len(data)
        if length <= 0x7F:
            output.write(struct.pack('B', length))
        else:
            # Extended length: first byte has high bit set + length of length
            output.write(struct.pack('B', 0x80 | ((length.bit_length() + 7) // 8)))
            output.write(struct.pack('>H' if length <= 0xFFFF else '>I', length))
        
        # Write data
        output.write(data)
        
        return output.getvalue()
    
    @staticmethod
    def decode_tld(buffer: bytes) -> Dict[int, bytes]:
        """
        Decode TLD fields from buffer
        
        Args:
            buffer: Raw TLD data
            
        Returns:
            Dictionary mapping tag -> data
        """
        result = {}
        index = 0
        
        while index < len(buffer):
            # Parse tag
            tag = buffer[index]
            index += 1
            
            # Check for 2-byte tag
            if (tag & 0x1F) == 0x1F and index < len(buffer):
                tag = (tag << 8) | buffer[index]
                index += 1
            
            if index >= len(buffer):
                break
            
            # Parse length
            length = buffer[index]
            index += 1
            
            # Check for extended length
            if length & 0x80:
                length_bytes = length & 0x7F
                length = 0
                for _ in range(length_bytes):
                    if index >= len(buffer):
                        break
                    length = (length << 8) | buffer[index]
                    index += 1
            
            # Extract data
            if index + length <= len(buffer):
                data = buffer[index:index + length]
                result[tag] = data
                index += length
            else:
                break  # Malformed TLD
        
        return result
    
    @staticmethod
    def build_transaction_request(
        operator_id: str,
        transaction_type: int,
        amount: int,
        auth_code: Optional[str] = None
    ) -> bytes:
        """
        Build a transaction request TLD message
        
        Args:
            operator_id: Operator identifier
            transaction_type: Transaction type code
            amount: Amount in smallest currency unit (øre)
            auth_code: Optional authorization code
            
        Returns:
            TLD-encoded transaction request
        """
        output = BytesIO()
        
        # Operator ID
        output.write(TLDCodec.encode_tld(
            TLDTags.OPERATOR_ID,
            operator_id.encode('ascii')
        ))
        
        # Transaction type
        output.write(TLDCodec.encode_tld(
            TLDTags.TRANSACTION_TYPE,
            struct.pack('B', transaction_type)
        ))
        
        # Amount (4 bytes, big-endian)
        output.write(TLDCodec.encode_tld(
            TLDTags.TRANSACTION_AMOUNT,
            struct.pack('>I', amount)
        ))
        
        # Optional auth code
        if auth_code:
            output.write(TLDCodec.encode_tld(
                TLDTags.AUTH_CODE,
                auth_code.encode('ascii')
            ))
        
        return output.getvalue()
    
    @staticmethod
    def parse_transaction_result(tld_data: bytes) -> Dict:
        """
        Parse transaction result from TLD data
        
        Args:
            tld_data: Raw TLD data
            
        Returns:
            Dictionary of result fields
        """
        fields = TLDCodec.decode_tld(tld_data)
        
        result = {
            'result_code': struct.unpack('B', fields.get(TLDTags.RESULT_CODE, b'\xff'))[0],
            'truncated_pan': fields.get(TLDTags.TRUNCATED_PAN, b'').decode('ascii', errors='ignore'),
            'timestamp': fields.get(TLDTags.TIMESTAMP, b'').decode('ascii', errors='ignore'),
            'auth_code': fields.get(TLDTags.AUTH_CODE, b'').decode('ascii', errors='ignore'),
            'session_number': fields.get(TLDTags.SESSION_NUMBER, b'').decode('ascii', errors='ignore'),
            'terminal_id': fields.get(TLDTags.TERMINAL_ID, b'').decode('ascii', errors='ignore'),
        }
        
        # Amount (if present)
        if TLDTags.TRANSACTION_AMOUNT in fields:
            result['amount'] = struct.unpack('>I', fields[TLDTags.TRANSACTION_AMOUNT])[0]
        
        return result
```

### 3. Message Framing

```python
"""
Message framing module

Hypothetical format (NEEDS VERIFICATION):
    [STX:1][LENGTH:2][COMMAND:1][TLD_DATA:N][CRC:2][ETX:1]
"""

import struct
from typing import Optional, Tuple
from dataclasses import dataclass


class ControlBytes:
    """Control byte constants"""
    STX = 0x02
    ETX = 0x03
    ACK = 0x06
    NAK = 0x15


class CommandCodes:
    """Command code constants (HYPOTHETICAL - verify with capture)"""
    OPEN_SESSION = 0x10
    CLOSE_SESSION = 0x11
    TRANSFER_AMOUNT = 0x20
    ADMINISTRATION = 0x60
    SEND_TLD = 0x30
    
    # Response/Event codes
    TERMINAL_READY = 0x70
    DISPLAY_TEXT = 0x71
    PRINT_TEXT = 0x72
    TRANSACTION_RESULT = 0x73


@dataclass
class ParsedMessage:
    """Parsed message structure"""
    command: int
    tld_data: bytes


class MessageFraming:
    """Message framing and parsing"""
    
    @staticmethod
    def frame_message(command: int, tld_data: bytes) -> bytes:
        """
        Frame a message for transmission
        
        Args:
            command: Command code
            tld_data: TLD-encoded data
            
        Returns:
            Framed message bytes
        """
        # Calculate length: command + data + CRC
        length = 1 + len(tld_data) + 2
        
        # Build message
        msg = bytearray()
        msg.append(ControlBytes.STX)
        msg.extend(struct.pack('>H', length))  # 2-byte length, big-endian
        msg.append(command)
        msg.extend(tld_data)
        
        # Calculate CRC
        crc = MessageFraming._calculate_crc(bytes([command]) + tld_data)
        msg.extend(struct.pack('>H', crc))
        
        msg.append(ControlBytes.ETX)
        
        return bytes(msg)
    
    @staticmethod
    def parse_message(raw_data: bytes) -> Optional[ParsedMessage]:
        """
        Parse received message
        
        Args:
            raw_data: Raw received bytes
            
        Returns:
            ParsedMessage if valid, None otherwise
        """
        if len(raw_data) < 6:  # Minimum: STX+LEN+CMD+CRC+ETX
            return None
        
        index = 0
        
        # Check STX
        if raw_data[index] != ControlBytes.STX:
            return None
        index += 1
        
        # Parse length
        length = struct.unpack('>H', raw_data[index:index+2])[0]
        index += 2
        
        # Check we have enough data
        if len(raw_data) < index + length + 1:  # +1 for ETX
            return None
        
        # Parse command
        command = raw_data[index]
        index += 1
        
        # Extract TLD data
        tld_data_length = length - 1 - 2  # -1 for command, -2 for CRC
        tld_data = raw_data[index:index + tld_data_length]
        index += tld_data_length
        
        # Parse CRC
        received_crc = struct.unpack('>H', raw_data[index:index+2])[0]
        index += 2
        
        # Check ETX
        if raw_data[index] != ControlBytes.ETX:
            return None
        
        # Verify CRC
        calculated_crc = MessageFraming._calculate_crc(bytes([command]) + tld_data)
        if received_crc != calculated_crc:
            return None  # CRC mismatch
        
        return ParsedMessage(command=command, tld_data=tld_data)
    
    @staticmethod
    def _calculate_crc(data: bytes) -> int:
        """
        Calculate CRC-16 (CCITT)
        Note: Algorithm may be different - verify with capture
        
        Args:
            data: Data to calculate CRC for
            
        Returns:
            CRC value (16-bit)
        """
        crc = 0xFFFF
        
        for byte in data:
            crc ^= (byte << 8)
            for _ in range(8):
                if crc & 0x8000:
                    crc = (crc << 1) ^ 0x1021
                else:
                    crc <<= 1
                crc &= 0xFFFF
        
        return crc ^ 0xFFFF
```

---

## TCP Communication Layer

```python
"""
TCP connection management for Baxi terminal
"""

import asyncio
import logging
from typing import Optional, Callable
from dataclasses import dataclass

from .protocol.framing import MessageFraming, ParsedMessage, CommandCodes
from .protocol.tld_codec import TLDCodec, TLDTags
from .models.events import DisplayTextEvent, PrintTextEvent, TransactionResultEvent, ErrorEvent
from .config import BaxiConfiguration


logger = logging.getLogger(__name__)


class BaxiConnection:
    """TCP connection manager for Baxi terminal"""
    
    def __init__(
        self,
        config: BaxiConfiguration,
        event_callback: Callable[[str, dict], None]
    ):
        self.config = config
        self.event_callback = event_callback
        
        self._reader: Optional[asyncio.StreamReader] = None
        self._writer: Optional[asyncio.StreamWriter] = None
        self._connected = False
        self._receive_task: Optional[asyncio.Task] = None
        self._message_buffer = bytearray()
    
    async def open(self) -> None:
        """Open connection to terminal"""
        try:
            logger.info(f"Connecting to Baxi terminal at {self.config.host_ip}:{self.config.host_port}")
            
            # Connect with timeout
            self._reader, self._writer = await asyncio.wait_for(
                asyncio.open_connection(
                    self.config.host_ip,
                    self.config.host_port
                ),
                timeout=self.config.connection_timeout
            )
            
            self._connected = True
            
            # Start receive loop
            self._receive_task = asyncio.create_task(self._receive_loop())
            
            # Send OPEN_SESSION command
            await self.send_command(CommandCodes.OPEN_SESSION, b'')
            
            logger.info("Connected to Baxi terminal")
            
        except Exception as e:
            logger.error(f"Failed to connect to Baxi terminal: {e}")
            await self.close()
            raise
    
    async def close(self) -> None:
        """Close connection"""
        if not self._connected:
            return
        
        logger.info("Closing Baxi terminal connection")
        
        try:
            # Send CLOSE_SESSION command
            if self._writer:
                await self.send_command(CommandCodes.CLOSE_SESSION, b'')
            
            # Stop receive loop
            if self._receive_task:
                self._receive_task.cancel()
                try:
                    await self._receive_task
                except asyncio.CancelledError:
                    pass
            
            # Close writer
            if self._writer:
                self._writer.close()
                await self._writer.wait_closed()
            
        except Exception as e:
            logger.error(f"Error closing connection: {e}")
        finally:
            self._connected = False
            self._reader = None
            self._writer = None
        
        logger.info("Baxi terminal connection closed")
    
    async def send_command(self, command: int, tld_data: bytes) -> None:
        """
        Send command with TLD data
        
        Args:
            command: Command code
            tld_data: TLD-encoded data
        """
        if not self._connected or not self._writer:
            raise ConnectionError("Not connected")
        
        framed_message = MessageFraming.frame_message(command, tld_data)
        
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug(f"Sending command 0x{command:02x}: {framed_message.hex()}")
        
        self._writer.write(framed_message)
        await self._writer.drain()
    
    async def _receive_loop(self) -> None:
        """Receive loop (coroutine)"""
        try:
            while self._connected and self._reader:
                # Read data
                data = await self._reader.read(2048)
                
                if not data:
                    logger.warning("Connection closed by terminal")
                    await self._handle_connection_lost()
                    break
                
                # Add to buffer
                self._message_buffer.extend(data)
                
                # Try to parse complete message
                parsed_message = MessageFraming.parse_message(bytes(self._message_buffer))
                
                if parsed_message:
                    # Complete message received
                    await self._handle_received_message(parsed_message)
                    
                    # Clear buffer (in real implementation, should only clear parsed bytes)
                    self._message_buffer.clear()
                
        except asyncio.CancelledError:
            logger.debug("Receive loop cancelled")
        except Exception as e:
            logger.error(f"Error in receive loop: {e}")
            await self._handle_connection_lost()
    
    async def _handle_received_message(self, message: ParsedMessage) -> None:
        """Handle received message"""
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug(f"Received command 0x{message.command:02x}")
        
        try:
            if message.command == CommandCodes.TERMINAL_READY:
                self.event_callback('terminal_ready', {})
            
            elif message.command == CommandCodes.DISPLAY_TEXT:
                tld_fields = TLDCodec.decode_tld(message.tld_data)
                display_text = tld_fields.get(TLDTags.DISPLAY_TEXT, b'').decode('utf-8', errors='ignore')
                self.event_callback('display_text', {'text': display_text})
            
            elif message.command == CommandCodes.PRINT_TEXT:
                tld_fields = TLDCodec.decode_tld(message.tld_data)
                print_text = tld_fields.get(TLDTags.PRINT_TEXT, b'').decode('utf-8', errors='ignore')
                self.event_callback('print_text', {'text': print_text})
            
            elif message.command == CommandCodes.TRANSACTION_RESULT:
                result = TLDCodec.parse_transaction_result(message.tld_data)
                self.event_callback('transaction_result', result)
            
            else:
                logger.warning(f"Unknown command: 0x{message.command:02x}")
                self.event_callback('error', {
                    'error_code': -1,
                    'error_message': f"Unknown command: {message.command}"
                })
        
        except Exception as e:
            logger.error(f"Error handling message: {e}")
            self.event_callback('error', {
                'error_code': -3,
                'error_message': str(e)
            })
    
    async def _handle_connection_lost(self) -> None:
        """Handle connection lost"""
        await self.close()
        self.event_callback('error', {
            'error_code': -2,
            'error_message': "Connection lost"
        })
        
        # Attempt reconnection if configured
        if self.config.max_reconnect_attempts > 0:
            await self._attempt_reconnection()
    
    async def _attempt_reconnection(self) -> None:
        """Attempt reconnection"""
        for attempt in range(self.config.max_reconnect_attempts):
            logger.info(f"Reconnection attempt {attempt + 1}/{self.config.max_reconnect_attempts}")
            
            await asyncio.sleep(self.config.reconnect_delay)
            
            try:
                await self.open()
                logger.info("Reconnection successful")
                return
            except Exception as e:
                logger.error(f"Reconnection failed: {e}")
        
        logger.error(f"Reconnection failed after {self.config.max_reconnect_attempts} attempts")
```

---

## Event Handling

```python
"""
Event handling module
"""

from dataclasses import dataclass
from typing import Optional, Protocol
from enum import IntEnum


@dataclass
class TransactionResult:
    """Transaction result data"""
    result_code: int  # 0 = approved, others = rejected
    truncated_pan: Optional[str] = None
    timestamp: Optional[str] = None
    auth_code: Optional[str] = None
    session_number: Optional[str] = None
    terminal_id: Optional[str] = None
    amount: int = 0
    tip_amount: int = 0
    surcharge_amount: int = 0
    issuer_name: Optional[str] = None
    verification_method: int = 0
    response_code: Optional[str] = None
    
    @property
    def is_approved(self) -> bool:
        return self.result_code == 0
    
    @property
    def total_amount(self) -> int:
        return self.amount + self.tip_amount + self.surcharge_amount


@dataclass
class BaxiError:
    """Error data"""
    error_code: int
    error_message: str


class BaxiEventHandler(Protocol):
    """Event handler protocol (interface)"""
    
    def on_terminal_ready(self) -> None:
        """Terminal is ready"""
        ...
    
    def on_display_text(self, text: str) -> None:
        """Display text from terminal"""
        ...
    
    def on_print_text(self, text: str) -> None:
        """Receipt text for printing"""
        ...
    
    def on_transaction_result(self, result: TransactionResult) -> None:
        """Transaction result"""
        ...
    
    def on_error(self, error: BaxiError) -> None:
        """Error occurred"""
        ...


class DefaultBaxiEventHandler:
    """Default event handler implementation"""
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
    
    def on_terminal_ready(self) -> None:
        self.logger.info("Terminal is ready")
    
    def on_display_text(self, text: str) -> None:
        self.logger.info(f"Display: {text}")
    
    def on_print_text(self, text: str) -> None:
        self.logger.info(f"Print: {text}")
    
    def on_transaction_result(self, result: TransactionResult) -> None:
        status = "APPROVED" if result.is_approved else "REJECTED"
        self.logger.info(f"Transaction result: {status}")
        self.logger.info(f"  Amount: {result.total_amount}")
        self.logger.info(f"  PAN: {result.truncated_pan}")
        self.logger.info(f"  Auth: {result.auth_code}")
    
    def on_error(self, error: BaxiError) -> None:
        self.logger.error(f"Baxi error [{error.error_code}]: {error.error_message}")
```

---

## Complete Implementation Example

```python
"""
Main Baxi client implementation
"""

import asyncio
import logging
from typing import Optional
from enum import IntEnum

from .config import BaxiConfiguration
from .connection import BaxiConnection
from .protocol.framing import CommandCodes
from .protocol.tld_codec import TLDCodec
from .models.events import TransactionResult, BaxiError, BaxiEventHandler, DefaultBaxiEventHandler


logger = logging.getLogger(__name__)


class TransactionType(IntEnum):
    """Transaction types"""
    PURCHASE = 0
    REFUND = 1
    REVERSAL = 2
    CASH_ADVANCE = 3
    BALANCE_INQUIRY = 4


class AdministrationCode(IntEnum):
    """Administration codes"""
    END_OF_DAY = 1
    X_REPORT = 2
    Z_REPORT = 3
    RECONCILIATION = 10
    REPRINT_LAST_RECEIPT = 20


class BaxiClient:
    """Main Baxi payment terminal client"""
    
    def __init__(
        self,
        config: Optional[BaxiConfiguration] = None,
        event_handler: Optional[BaxiEventHandler] = None
    ):
        self.config = config or BaxiConfiguration()
        self.event_handler = event_handler or DefaultBaxiEventHandler()
        
        self._connection: Optional[BaxiConnection] = None
        self._transaction_lock = asyncio.Lock()
        self._transaction_future: Optional[asyncio.Future] = None
    
    async def open(self) -> None:
        """Open connection to terminal"""
        self._connection = BaxiConnection(self.config, self._event_callback)
        await self._connection.open()
    
    async def close(self) -> None:
        """Close connection"""
        if self._connection:
            await self._connection.close()
    
    async def transfer_amount(
        self,
        amount: int,  # In øre (smallest currency unit)
        transaction_type: TransactionType = TransactionType.PURCHASE,
        auth_code: Optional[str] = None
    ) -> TransactionResult:
        """
        Perform payment transaction
        
        Args:
            amount: Amount in øre
            transaction_type: Type of transaction
            auth_code: Optional authorization code
            
        Returns:
            Transaction result
        """
        async with self._transaction_lock:
            try:
                logger.info(f"Starting transaction: type={transaction_type.name}, amount={amount}")
                
                # Prepare transaction future
                self._transaction_future = asyncio.Future()
                
                # Build TLD request
                tld_data = TLDCodec.build_transaction_request(
                    operator_id=self.config.operator_id,
                    transaction_type=transaction_type,
                    amount=amount,
                    auth_code=auth_code
                )
                
                # Send command
                if not self._connection:
                    raise ConnectionError("Not connected")
                
                await self._connection.send_command(CommandCodes.TRANSFER_AMOUNT, tld_data)
                
                # Wait for result (with timeout)
                result = await asyncio.wait_for(
                    self._transaction_future,
                    timeout=self.config.read_timeout
                )
                
                status = "APPROVED" if result.is_approved else "REJECTED"
                logger.info(f"Transaction completed: {status}")
                
                return result
                
            except asyncio.TimeoutError:
                logger.error("Transaction timeout")
                raise TimeoutError("Transaction timeout")
            except Exception as e:
                logger.error(f"Transaction failed: {e}")
                raise
    
    async def administration(
        self,
        admin_code: AdministrationCode,
        operator_id: Optional[str] = None
    ) -> None:
        """
        Perform administrative operation
        
        Args:
            admin_code: Administration code
            operator_id: Optional operator ID (uses config default if not provided)
        """
        try:
            logger.info(f"Administration: {admin_code.name}")
            
            op_id = operator_id or self.config.operator_id
            
            # Build TLD request
            tld_data = (
                TLDCodec.encode_tld(TLDTags.OPERATOR_ID, op_id.encode('ascii')) +
                TLDCodec.encode_tld(0x60, bytes([admin_code]))
            )
            
            # Send command
            if not self._connection:
                raise ConnectionError("Not connected")
            
            await self._connection.send_command(CommandCodes.ADMINISTRATION, tld_data)
            
        except Exception as e:
            logger.error(f"Administration failed: {e}")
            raise
    
    def _event_callback(self, event_type: str, event_data: dict) -> None:
        """Internal event callback"""
        try:
            if event_type == 'terminal_ready':
                self.event_handler.on_terminal_ready()
            
            elif event_type == 'display_text':
                self.event_handler.on_display_text(event_data['text'])
            
            elif event_type == 'print_text':
                self.event_handler.on_print_text(event_data['text'])
            
            elif event_type == 'transaction_result':
                result = TransactionResult(**event_data)
                self.event_handler.on_transaction_result(result)
                
                # Complete transaction future
                if self._transaction_future and not self._transaction_future.done():
                    self._transaction_future.set_result(result)
            
            elif event_type == 'error':
                error = BaxiError(**event_data)
                self.event_handler.on_error(error)
        
        except Exception as e:
            logger.error(f"Error in event callback: {e}")


# Usage Example
async def main():
    """Example usage"""
    
    # Configure
    config = BaxiConfiguration(
        host_ip="192.168.1.100",
        host_port=3000,
        operator_id="01"
    )
    
    # Custom event handler
    class MyEventHandler:
        def on_terminal_ready(self):
            print("✓ Terminal is ready for transactions")
        
        def on_display_text(self, text: str):
            print(f"📺 Display: {text}")
        
        def on_print_text(self, text: str):
            print(f"🖨 Print: {text}")
        
        def on_transaction_result(self, result: TransactionResult):
            if result.is_approved:
                print("✓ Transaction APPROVED")
                print(f"  Amount: {result.total_amount / 100:.2f} NOK")
                print(f"  Card: {result.truncated_pan}")
                print(f"  Auth: {result.auth_code}")
            else:
                print(f"✗ Transaction REJECTED: {result.response_code}")
        
        def on_error(self, error: BaxiError):
            print(f"✗ Error: {error.error_message}")
    
    # Create client
    client = BaxiClient(config, MyEventHandler())
    
    try:
        # Open connection
        await client.open()
        print("Connected to Baxi terminal")
        
        # Wait for terminal ready
        await asyncio.sleep(2)
        
        # Perform transaction
        amount = 10000  # 100.00 NOK
        result = await client.transfer_amount(amount, TransactionType.PURCHASE)
        
        if result.is_approved:
            print("Payment successful!")
            # Save to database, etc.
        else:
            print(f"Payment failed: {result.response_code}")
        
        # End of day
        await client.administration(AdministrationCode.END_OF_DAY)
        
    except Exception as e:
        print(f"Error: {e}")
    finally:
        await client.close()


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    asyncio.run(main())
```

---

## Testing Strategy

### Unit Tests (pytest)

```python
"""
Test TLD codec
"""

import pytest
from baxi_protocol.protocol.tld_codec import TLDCodec, TLDTags


def test_encode_tld_simple():
    """Test simple TLD encoding"""
    data = b'TEST'
    tld = TLDCodec.encode_tld(0x04, data)
    
    assert tld[0] == 0x04  # Tag
    assert tld[1] == 4     # Length
    assert tld[2:6] == data  # Data


def test_decode_tld_simple():
    """Test simple TLD decoding"""
    buffer = b'\x04\x04TEST\x08\x0201'
    
    fields = TLDCodec.decode_tld(buffer)
    
    assert len(fields) == 2
    assert fields[0x04] == b'TEST'
    assert fields[0x08] == b'01'


def test_build_transaction_request():
    """Test transaction request building"""
    tld = TLDCodec.build_transaction_request(
        operator_id="01",
        transaction_type=0,
        amount=10000
    )
    
    fields = TLDCodec.decode_tld(tld)
    
    assert TLDTags.OPERATOR_ID in fields
    assert TLDTags.TRANSACTION_TYPE in fields
    assert TLDTags.TRANSACTION_AMOUNT in fields


def test_parse_transaction_result():
    """Test transaction result parsing"""
    # Mock TLD data
    tld_data = (
        TLDCodec.encode_tld(TLDTags.RESULT_CODE, b'\x00') +
        TLDCodec.encode_tld(TLDTags.AUTH_CODE, b'123456') +
        TLDCodec.encode_tld(TLDTags.TRUNCATED_PAN, b'****1234')
    )
    
    result = TLDCodec.parse_transaction_result(tld_data)
    
    assert result['result_code'] == 0
    assert result['auth_code'] == '123456'
    assert result['truncated_pan'] == '****1234'
```

### Integration Tests (pytest-asyncio)

```python
"""
Integration tests with mock terminal
"""

import pytest
import asyncio
from baxi_protocol import BaxiClient, BaxiConfiguration, TransactionType
from tests.mock_terminal import MockBaxiTerminal


@pytest.fixture
async def mock_terminal():
    """Fixture for mock terminal"""
    terminal = MockBaxiTerminal(port=3000)
    await terminal.start()
    yield terminal
    await terminal.stop()


@pytest.fixture
async def baxi_client(mock_terminal):
    """Fixture for Baxi client"""
    config = BaxiConfiguration(
        host_ip="localhost",
        host_port=3000
    )
    client = BaxiClient(config)
    await client.open()
    yield client
    await client.close()


@pytest.mark.asyncio
async def test_open_connection(baxi_client):
    """Test opening connection"""
    # Connection already opened by fixture
    assert baxi_client._connection is not None


@pytest.mark.asyncio
async def test_transfer_amount_approved(baxi_client, mock_terminal):
    """Test approved transaction"""
    # Mock terminal will approve
    mock_terminal.set_next_response(approved=True, auth_code="123456")
    
    result = await baxi_client.transfer_amount(
        amount=10000,
        transaction_type=TransactionType.PURCHASE
    )
    
    assert result.is_approved
    assert result.auth_code == "123456"


@pytest.mark.asyncio
async def test_transfer_amount_rejected(baxi_client, mock_terminal):
    """Test rejected transaction"""
    # Mock terminal will reject
    mock_terminal.set_next_response(approved=False, response_code="05")
    
    result = await baxi_client.transfer_amount(
        amount=10000,
        transaction_type=TransactionType.PURCHASE
    )
    
    assert not result.is_approved
    assert result.response_code == "05"


@pytest.mark.asyncio
async def test_administration(baxi_client):
    """Test administration command"""
    from baxi_protocol import AdministrationCode
    
    # Should not raise
    await baxi_client.administration(AdministrationCode.END_OF_DAY)
```

### Mock Terminal (for testing)

```python
"""
Mock Baxi terminal for testing
"""

import asyncio
import logging
from typing import Optional

from baxi_protocol.protocol.framing import MessageFraming, CommandCodes
from baxi_protocol.protocol.tld_codec import TLDCodec, TLDTags


logger = logging.getLogger(__name__)


class MockBaxiTerminal:
    """Mock Baxi terminal server for testing"""
    
    def __init__(self, port: int = 3000):
        self.port = port
        self._server: Optional[asyncio.Server] = None
        self._next_response = {'approved': True, 'auth_code': '123456'}
    
    async def start(self) -> None:
        """Start mock terminal server"""
        self._server = await asyncio.start_server(
            self._handle_client,
            'localhost',
            self.port
        )
        logger.info(f"Mock terminal started on port {self.port}")
    
    async def stop(self) -> None:
        """Stop mock terminal server"""
        if self._server:
            self._server.close()
            await self._server.wait_closed()
        logger.info("Mock terminal stopped")
    
    def set_next_response(self, approved: bool, auth_code: str = '', response_code: str = ''):
        """Configure next transaction response"""
        self._next_response = {
            'approved': approved,
            'auth_code': auth_code,
            'response_code': response_code
        }
    
    async def _handle_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
        """Handle client connection"""
        try:
            while True:
                # Read message
                data = await reader.read(2048)
                if not data:
                    break
                
                # Parse message
                message = MessageFraming.parse_message(data)
                if not message:
                    continue
                
                # Handle command
                if message.command == CommandCodes.OPEN_SESSION:
                    # Send terminal ready
                    response = MessageFraming.frame_message(CommandCodes.TERMINAL_READY, b'')
                    writer.write(response)
                    await writer.drain()
                
                elif message.command == CommandCodes.TRANSFER_AMOUNT:
                    # Send display text
                    display_tld = TLDCodec.encode_tld(TLDTags.DISPLAY_TEXT, b'INSERT CARD')
                    response = MessageFraming.frame_message(CommandCodes.DISPLAY_TEXT, display_tld)
                    writer.write(response)
                    await writer.drain()
                    
                    # Simulate processing delay
                    await asyncio.sleep(0.5)
                    
                    # Send transaction result
                    result_tld = self._build_transaction_result()
                    response = MessageFraming.frame_message(CommandCodes.TRANSACTION_RESULT, result_tld)
                    writer.write(response)
                    await writer.drain()
                
                elif message.command == CommandCodes.CLOSE_SESSION:
                    break
        
        except Exception as e:
            logger.error(f"Error handling client: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
    
    def _build_transaction_result(self) -> bytes:
        """Build mock transaction result TLD"""
        result_code = 0 if self._next_response['approved'] else 1
        
        tld = TLDCodec.encode_tld(TLDTags.RESULT_CODE, bytes([result_code]))
        
        if self._next_response.get('auth_code'):
            tld += TLDCodec.encode_tld(
                TLDTags.AUTH_CODE,
                self._next_response['auth_code'].encode('ascii')
            )
        
        if self._next_response.get('response_code'):
            tld += TLDCodec.encode_tld(
                0x8A,  # Response code tag (hypothetical)
                self._next_response['response_code'].encode('ascii')
            )
        
        # Add mock PAN
        tld += TLDCodec.encode_tld(TLDTags.TRUNCATED_PAN, b'****1234')
        
        return tld
```

---

## Deployment Considerations

### Package Structure

```bash
# requirements.txt
asyncio>=3.4.3
pytest>=7.4.0
pytest-asyncio>=0.21.0
pyserial>=3.5  # For serial fallback
```

### Logging Configuration

```python
import logging
import logging.handlers

def setup_logging(log_path: str, level: str = 'INFO'):
    """Setup logging configuration"""
    
    # Create logger
    logger = logging.getLogger('baxi_protocol')
    logger.setLevel(getattr(logging, level))
    
    # File handler with rotation
    file_handler = logging.handlers.RotatingFileHandler(
        f"{log_path}/baxi.log",
        maxBytes=10*1024*1024,  # 10 MB
        backupCount=10
    )
    file_handler.setFormatter(logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    ))
    logger.addHandler(file_handler)
    
    # Console handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(logging.Formatter(
        '%(levelname)s: %(message)s'
    ))
    logger.addHandler(console_handler)
    
    return logger
```

### Systemd Service (Linux)

```ini
[Unit]
Description=LPG-EHL Baxi Payment Integration
After=network.target

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl
ExecStart=/usr/bin/python3 -m baxi_protocol.main
Restart=always
RestartSec=10
Environment="BAXI_HOST=192.168.1.100"
Environment="BAXI_PORT=3000"

[Install]
WantedBy=multi-user.target
```

---

## Summary: Python Implementation

### ✅ Strengths

1. **Rapid Prototyping**: Fast iteration for protocol reverse engineering
2. **Clean Byte Handling**: Native byte strings and struct module
3. **Async/Await**: Modern async I/O with `asyncio`
4. **Interactive Testing**: REPL for live experimentation
5. **Rich Ecosystem**: Excellent libraries for networking and testing
6. **Readable Code**: Clear syntax, easy to understand

### ⚠️ Challenges

1. **Protocol Reverse Engineering**: Same as Kotlin - need Wireshark captures
2. **Type Safety**: Less compile-time checking (mitigated with type hints)
3. **Performance**: Slower than Kotlin for high-throughput scenarios
4. **Deployment**: Requires Python runtime (vs. single JAR for Kotlin)

### 📋 Next Steps

1. **Capture Network Traffic**: Use original DLL with Wireshark
2. **Implement Protocol Analysis Tools**: Use `scapy` to analyze pcap files
3. **Build Mock Terminal**: For testing without hardware
4. **Test Against Real Terminal**: Validate implementation
5. **Package for Distribution**: Create pip-installable package
6. **CI/CD Pipeline**: Automated testing and deployment
7. **Documentation**: Complete API documentation with Sphinx

---

**End of Python Implementation Guide**
