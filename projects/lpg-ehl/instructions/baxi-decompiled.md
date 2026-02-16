# Baxi DLL Decompilation Summary

**Source:** Nets Baxi.DLL (.NET Assembly)  
**Analysis Date:** February 7, 2026  
**Status:** API Surface Fully Documented, Wire Protocol Requires Reverse Engineering

---

## Overview

This document summarizes what we've learned from decompiling the Nets Baxi payment terminal DLL. The Baxi protocol is proprietary and **not publicly documented**, but through .NET decompilation, we have full visibility of the public API surface.

**Key Finding:** We know the **"what"** (API methods, events, data structures) but NOT the **"how"** (wire protocol, TLD tags, message framing).

---

## What We Have from Decompilation

### ✅ Complete Public API

#### Configuration Properties

```csharp
public class BaxiCtrl {
    // Network Configuration
    public string HostIpAddress { get; set; }     // Terminal IP (e.g., "192.168.1.100")
    public int HostPort { get; set; }             // TCP port (typically 3000-3010)
    
    // Serial Fallback
    public int ComPort { get; set; }              // COM port number (1-255)
    public int BaudRate { get; set; }             // 9600, 19200, 38400, 57600, 115200
    
    // Logging
    public string LogFilePath { get; set; }       // Log directory path
    public string LogFilePrefix { get; set; }     // Log filename prefix
    public int TraceLevel { get; set; }           // 0=OFF, 1=ERROR, 2=WARN, 3=INFO, 4=DEBUG, 5=TRACE
    public int LogAutoDeleteDays { get; set; }    // Auto-delete logs older than N days
    
    // Terminal Features
    public int PrinterWidth { get; set; }         // Receipt width in characters
    public int DisplayWidth { get; set; }         // Display width in characters
    public bool CutterSupport { get; set; }       // Paper cutter available
    public bool AutoGetCustomerInfo { get; set; } // Auto-retrieve card data
    public bool Use2KBuffer { get; set; }         // Use 2KB buffer (vs 128 bytes)
    
    // Device Info
    public string DeviceString { get; set; }      // Device identifier
    public string VendorInfoExtended { get; set; }// Vendor information
}
```

#### Public Methods

```csharp
// Connection Management
public bool Open();                    // Open connection to terminal
public void Close();                   // Close connection
public bool IsConnected { get; }       // Connection status

// Transaction Operations
public int TransferAmount(TransferAmountArgs args);
public int TransferAmount_V2(
    string operatorId,   // Operator ID (e.g., "01")
    int type1,           // Primary transaction type
    int amount1,         // Primary amount in øre
    int type2,           // Secondary transaction type (optional)
    int amount2,         // Secondary amount (optional)
    int type3,           // Tertiary transaction type (optional)
    int amount3,         // Tertiary amount (optional)
    string textToPrint,  // Additional receipt text
    string authCode      // Authorization code (for reversals)
);

// Administrative Operations
public int Administration(AdministrationArgs args);

// Low-Level Communication
public void SendTLD(byte[] tldData);   // Send raw TLD data
public void SendJson(string jsonData); // Send JSON data (if supported)
```

#### Event Callbacks

```csharp
// Terminal Events
public event EventHandler OnTerminalReady;     // Terminal initialized and ready
public event EventHandler OnDisplayText;        // Display text updated
public event EventHandler OnPrintText;          // Receipt data available
public event EventHandler OnLastFinancialResult;// Transaction result
public event EventHandler OnError;              // Error occurred
public event EventHandler OnTLDReceived;        // Raw TLD data received
public event EventHandler OnJsonReceived;       // JSON data received
```

#### Data Structures

**Transaction Request:**
```csharp
public class TransferAmountArgs {
    public string OperID;           // Operator ID (e.g., "01", "02")
    public int Type1;               // Transaction type (see below)
    public int Amount1;             // Amount in øre (100 = 1.00 NOK)
    public int Type2;               // Secondary type (optional)
    public int Amount2;             // Secondary amount (optional)
    public int Type3;               // Tertiary type (optional)
    public int Amount3;             // Tertiary amount (optional)
    public string TextToPrint;      // Extra text for receipt
    public string TextOnTerminal;   // Text to show on terminal
    public string AuthorisationCode;// For reversals/refunds
}
```

**Transaction Types (observed from VB6 code):**
- `0x30` (ASCII '0'): Purchase
- `0x31` (ASCII '1'): Refund
- `0x32` (ASCII '2'): Reversal
- Others likely exist but not documented

**Transaction Result:**
```csharp
public class LastFinancialResultEventArgs {
    public int Result;                      // 0=approved, others=rejected
    public string TruncatedPAN;             // Masked card number (e.g., "1234****5678")
    public string DateTime;                 // Transaction timestamp
    public string AuthorisationCode;        // Authorization code from bank
    public string SessionNumber;            // Terminal session ID
    public string TerminalID;               // Terminal identifier
    public int Amount;                      // Transaction amount (øre)
    public int TipAmount;                   // Tip amount (øre)
    public int SurchargeAmount;             // Surcharge (øre)
    public string IssuerName;               // Card issuer (e.g., "VISA", "Mastercard")
    public int VerificationMethod;          // 0=PIN, 1=Signature, etc.
    public string ResponseCode;             // Issuer response code
    public byte[] TerminalDeviceData_TLD;   // Raw TLD data from terminal
}
```

### ✅ Transaction Flow (Inferred from API)

```
1. Create BaxiCtrl instance
   ↓
2. Set configuration properties
   - HostIpAddress = "192.168.1.100"
   - HostPort = 3000
   - TraceLevel = 4 (DEBUG)
   ↓
3. Subscribe to events
   - OnTerminalReady
   - OnDisplayText
   - OnPrintText
   - OnLastFinancialResult
   - OnError
   ↓
4. Call Open()
   ↓
5. Wait for OnTerminalReady event
   ↓
6. Create TransferAmountArgs
   - OperID = "01"
   - Type1 = 0x30 (Purchase)
   - Amount1 = 10000 (100.00 NOK)
   ↓
7. Call TransferAmount(args)
   ↓
8. Receive OnDisplayText events
   - "INSERT CARD"
   - "ENTER PIN"
   - "PROCESSING"
   ↓
9. Receive OnLastFinancialResult event
   - Result = 0 (approved)
   - AuthorisationCode = "123456"
   - TruncatedPAN = "4111****1111"
   ↓
10. Receive OnPrintText events
    - Receipt lines
    ↓
11. Print receipt
    ↓
12. (Optional) Call Close() when done
```

---

## What We're Missing (Wire Protocol)

### ❌ Critical Unknown Information

While we have the API, we **do NOT know** the actual wire protocol:

#### 1. TLD Tag Definitions

**Unknown:** What byte values represent each field?

```
Transaction amount tag:    0x?? (maybe 0x04?)
Transaction type tag:      0x?? (maybe 0x02?)
Operator ID tag:           0x?? (maybe 0x08?)
Result code tag:           0x?? (maybe 0x39?)
Truncated PAN tag:         0x?? (maybe 0x57?)
Timestamp tag:             0x?? (maybe 0x12?)
Auth code tag:             0x?? (maybe 0x38?)
Session number tag:        0x?? (maybe 0x5A?)
Terminal ID tag:           0x???? (maybe 0x9F1C?)
Display text tag:          0x?? (maybe 0xD0?)
Print text tag:            0x?? (maybe 0xD1?)
```

**Impact:** Cannot construct valid TLD messages without these.

#### 2. Message Framing

**Unknown:** How are messages structured?

```
Possible formats:

Option A: STX/ETX with length prefix
[STX:1][LENGTH:2][COMMAND:1][TLD_DATA:N][CRC:2][ETX:1]

Option B: Length prefix only
[LENGTH:2][COMMAND:1][TLD_DATA:N][CRC:2]

Option C: HDLC-like framing
[FLAG:1][ADDRESS:1][CONTROL:1][DATA:N][FCS:2][FLAG:1]

Questions:
- Which format?
- Control byte values (STX, ETX)?
- Length encoding (big-endian, little-endian)?
- Command byte encoding?
```

**Impact:** Cannot parse or construct messages correctly.

#### 3. Command Codes

**Unknown:** What byte values trigger each operation?

```
TransferAmount:         0x?? (maybe 0x20?)
Administration:         0x?? (maybe 0x60?)
Open/Close session:     0x??, 0x??
Terminal → Host:
  Display text:         0x?? (maybe 0x71?)
  Print text:           0x?? (maybe 0x72?)
  Transaction result:   0x?? (maybe 0x73?)
  Terminal ready:       0x?? (maybe 0x70?)
```

**Impact:** Cannot initiate operations or recognize incoming commands.

#### 4. Checksum Algorithm

**Unknown:** What CRC/checksum is used?

```
Possibilities:
- XOR checksum (simple, 1 byte)
- LRC (Longitudinal Redundancy Check)
- CRC-16-CCITT (polynomial 0x1021)
- CRC-16-MODBUS (polynomial 0x8005)
- CRC-32

Questions:
- Which algorithm?
- Initial value?
- Final XOR?
- Which bytes included?
```

**Impact:** Messages may be rejected by terminal.

#### 5. Data Encoding

**Unknown:** How are values encoded?

```
Integers (amounts, codes):
- Binary (big-endian, little-endian)?
- ASCII digits?
- BCD (Binary-Coded Decimal)?

Strings (text, IDs):
- ASCII?
- UTF-8?
- ISO-8859-1 (Latin-1)?

Example: Amount 10000 (100.00 NOK)
- Binary BE: [0x00, 0x00, 0x27, 0x10]
- Binary LE: [0x10, 0x27, 0x00, 0x00]
- ASCII: ['1', '0', '0', '0', '0']
- BCD: [0x01, 0x00, 0x00]
```

**Impact:** Data corruption, incorrect amounts.

---

## How to Discover Missing Information

### Method 1: Wireshark Packet Capture (RECOMMENDED)

**Requirements:**
- Windows PC with original Baxi DLL
- Test payment terminal
- Wireshark installed

**Procedure:**
1. Start Wireshark, filter `tcp.port == 3000`
2. Run test app with Baxi DLL
3. Perform simple purchase (100 NOK)
4. Analyze captured packets
5. Document protocol structure

**Example Test App:**
```csharp
using System;
using BaxiCtrl;

class Program {
    static void Main() {
        var baxi = new BaxiCtrl();
        
        // Log everything
        baxi.OnDisplayText += (s, e) => Console.WriteLine($"[DISPLAY] {e.DisplayText}");
        baxi.OnPrintText += (s, e) => Console.WriteLine($"[PRINT] {e.PrintText}");
        baxi.OnTLDReceived += (s, e) => Console.WriteLine($"[TLD RX] {BitConverter.ToString(e.TldData)}");
        baxi.OnLastFinancialResult += (s, e) => {
            Console.WriteLine($"[RESULT] {e.Result}");
            Console.WriteLine($"  PAN: {e.TruncatedPAN}");
            Console.WriteLine($"  Auth: {e.AuthorisationCode}");
            Console.WriteLine($"  TLD: {BitConverter.ToString(e.TerminalDeviceData_TLD)}");
        };
        
        baxi.HostIpAddress = "192.168.1.100";
        baxi.HostPort = 3000;
        baxi.TraceLevel = 5; // TRACE
        
        Console.WriteLine("Opening connection...");
        if (!baxi.Open()) {
            Console.WriteLine("Failed to connect");
            return;
        }
        
        Console.WriteLine("Waiting for terminal ready...");
        System.Threading.Thread.Sleep(3000);
        
        Console.WriteLine("Starting transaction...");
        var args = new TransferAmountArgs {
            OperID = "01",
            Type1 = 0x30,  // Purchase
            Amount1 = 10000,  // 100.00 NOK
            TextToPrint = "Test Transaction",
            TextOnTerminal = "LPG Test"
        };
        
        int result = baxi.TransferAmount(args);
        Console.WriteLine($"TransferAmount returned: {result}");
        
        Console.WriteLine("Press Enter to close...");
        Console.ReadLine();
        
        baxi.Close();
    }
}
```

### Method 2: DLL Instrumentation

**Using dnSpy (.NET Debugger):**
1. Open Baxi.DLL in dnSpy
2. Set breakpoints on:
   - `SendTLD(byte[])`
   - `ParseMessage(byte[])`
   - `EncodeTransactionRequest()`
3. Inspect byte arrays in memory
4. Document findings

### Method 3: Protocol Fuzzing

**Automated discovery script:**
```python
import socket
import itertools

def fuzz_baxi_protocol(terminal_ip, terminal_port):
    """Try different protocol variations"""
    
    # Test different control bytes
    control_bytes = [0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x10, 0x11, 0x20]
    
    # Test different command codes
    commands = range(0x00, 0x100)
    
    for stx, cmd in itertools.product(control_bytes, commands):
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(1.0)
            sock.connect((terminal_ip, terminal_port))
            
            # Try minimal message
            test_msg = bytes([stx, 0x00, 0x05, cmd, 0x00, 0x00])
            sock.send(test_msg)
            
            # Check response
            response = sock.recv(1024)
            if response:
                print(f"Response for STX=0x{stx:02x}, CMD=0x{cmd:02x}: {response.hex()}")
            
            sock.close()
        except:
            pass
```

### Method 4: Contact Nets

**Official channel:**
- Website: https://developer.nets.eu/
- Email: developer@nets.eu
- Phone: +47 XXXX XXXX

Request:
- Baxi protocol specification
- Integration documentation
- Test terminal access
- May require NDA

---

## VB6 Legacy Code Insights

From the old LPG-EHL VB6 system, we observe:

```vb
' Transaction with 300 øre (3.00 NOK)
baxi.TransferAmount_V2 "0000", &H30, 300, &H30, 0, &H30, 0, "LPG Autogas", ""
'                       └────┘  └───┘  └─┘  └───┘  └  └───┘  └  └─────────┘  └─┘
'                       OpID    Type1  Amt1 Type2  0  Type3  0  Text        Auth

' Observations:
' - Transaction type uses &H30 (ASCII '0' = 48 decimal)
' - Amount is direct integer in øre
' - Empty auth code for normal purchase
' - Synchronous: waits for Bank_answer flag

' Result handling:
Private Sub baxi_OnLocalMode(ByVal Result As Integer, ByVal IssuerID As Integer)
    Select Case Result
        Case 0: ok_to_opendisp = True   ' Approved
        Case 1: ok_to_opendisp = False  ' Declined
        Case 2: ok_to_opendisp = False  ' Error
    End Select
End Sub

' Receipt printing:
Private Sub baxi_OnPrinterText()
    com_print.Output = baxi.PrintText
    com_print.Output = Chr(27) & Chr(112)  ' ESC p = cut paper
End Sub
```

---

## Next Steps

1. **Capture Protocol Traffic** ← START HERE
   - Use Wireshark with original DLL
   - Document all byte sequences
   - Identify patterns

2. **Reverse Engineer Wire Protocol**
   - TLD tags
   - Message framing
   - Command codes
   - CRC algorithm

3. **Implement in Kotlin/Python**
   - See separate implementation guides
   - Start with mock terminal
   - Test incrementally

4. **Validate Against Real Terminal**
   - Simple purchase
   - Refund
   - Administration
   - Error cases

---

## Related Documents

- **Kotlin Implementation:** `docs/BAXI_PROTOCOL_KOTLIN_IMPLEMENTATION.md`
- **Python Implementation:** `docs/BAXI_PROTOCOL_PYTHON_IMPLEMENTATION.md`
- **Comprehensive Analysis:** `docs/BAXI_PROTOCOL_COMPREHENSIVE_ANALYSIS.md`
- **Legacy VB6 Code:** `legacy/legacy-curated/pumpekontroll/pumpekontroll.frm`

---

**Status:** API fully documented, wire protocol requires reverse engineering  
**Last Updated:** February 7, 2026  
**Next Action:** Capture network traffic with Wireshark