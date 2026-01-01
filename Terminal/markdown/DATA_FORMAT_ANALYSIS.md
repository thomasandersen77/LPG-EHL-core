# Data Format Analysis: VB6 vs Python vs Kotlin

## VB6 Legacy Format Analysis

### 1. Amount Preset (Command &H75 = 117)
**VB6 set_preset_amount():**
```vb
y(4) = &H75                         ' Command: 117 (decimal)
y(5) = Asc(Mid(amount, 6, 1))       ' Last digit (position 6)
y(6) = Asc(Mid(amount, 5, 1))       ' Position 5
y(7) = Asc(Mid(amount, 4, 1))       ' Position 4
y(8) = Asc(Mid(amount, 3, 1))       ' Position 3
y(9) = Asc(Mid(amount, 2, 1))       ' Position 2
```

**Data Order**: Reverse byte order (LSB first)
- For amount "12345" -> bytes are: '5', '4', '3', '2', '1'
- Uses ASCII codes of digit characters

### 2. Volume Preset (Command &H70 = 112)
**VB6 set_preset_volume():**
```vb
y(4) = &H70                         ' Command: 112 (decimal) 
y(5) = Asc(Mid(volume, 6, 1))       ' Position 6 (last)
y(6) = Asc(Mid(volume, 5, 1))       ' Position 5
y(7) = Asc(Mid(volume, 4, 1))       ' Position 4
y(8) = Asc(Mid(volume, 3, 1))       ' Position 3
y(9) = Asc(Mid(volume, 2, 1))       ' Position 2
y(10) = Asc(Mid(volume, 1, 1))      ' Position 1 (first)
```

**Data Order**: Reverse byte order (LSB first)
- 6 ASCII bytes for volume string

## Python Implementation (Correct Reference)

### Price Decoding:
```python
def decode_price_from_data(data: bytes) -> float:
    # data[0]=p0 (0.01), data[1]=p1 (0.1), data[2]=p2 (1), data[3]=p3 (10)
    p0, p1, p2, p3 = [chr(b) for b in data]
    s = f"{p3}{p2}{p1}{p0}"  # f.eks "1604" for 16.04
    return int(s) / 100.0
```

### Volume Decoding:
```python
def decode_volume_from_data(data: bytes) -> float:
    # data[0]=d0 (0.01 L), data[1]=d1 (0.1), data[2]=d2 (1), data[3]=d3 (10), data[4]=d4 (100)
    d0, d1, d2, d3, d4 = [chr(b) for b in data]
    s = f"{d4}{d3}{d2}{d1}{d0}"  # f.eks "04550" for 45.50 L
    return int(s) / 100.0
```

## Kotlin Implementation Issues

### 1. PROG_PRC (Price Programming) - Possibly Incorrect
Current Kotlin:
```kotlin
fun createPriceProgram(address: Int, price: String): EhlPacket {
    val parts = price.split(".")
    val data = byteArrayOf(
        parts[1][1].code.toByte(),  // Last decimal digit
        parts[1][0].code.toByte(),  // First decimal digit  
        parts[0][1].code.toByte(),  // Last whole digit
        parts[0][0].code.toByte()   // First whole digit
    )
}
```

**Problem**: Order might be wrong - VB6 uses positions 6,5,4,3,2 (LSB first)

### 2. PROG_W (Value Preset) - Incorrect Command Code
Current Kotlin uses PROG_W(117) but VB6 uses &H75 (117) for **amount preset**, not value preset.
VB6 volume preset uses &H70 (112).

**Issue**: Kotlin conflates amount and volume presets.

### 3. Missing Volume Preset Command
VB6 has separate volume preset (&H70) with 6-byte data, but Kotlin doesn't have this.

## Critical Corrections Needed

### 1. Fix Command Mapping
- VB6 &H75 (117) = Amount preset (money) 
- VB6 &H70 (112) = Volume preset (liters)
- Kotlin PROG_W(117) should be PROG_AMOUNT(117)
- Kotlin PROG_I(112) should be PROG_VOLUME(112)

### 2. Fix Data Encoding Order
All preset data should be LSB-first (reverse order) to match VB6.

### 3. Add Missing Command
Add support for &H4C (76) ERROR_QUERY command found in VB6.

## Validation Required
1. Test amount "12345" encodes as ['5', '4', '3', '2', '1'] ASCII bytes
2. Test volume "123456" encodes as ['6', '5', '4', '3', '2', '1'] ASCII bytes
3. Test price "15.90" encodes correctly in LSB-first order
4. Verify decimal point handling matches VB6/Python exactly

## Impact Assessment
**CRITICAL**: Wrong data encoding will cause dispenser to receive incorrect preset values, potentially causing:
- Wrong fuel amounts dispensed
- Wrong price calculations  
- Transaction failures
- Customer disputes

These must be fixed before any real dispenser testing.