# Missing EHL Commands Analysis - Based on Complete Documentation

## 🚨 Critical Commands Missing from Kotlin Implementation

Based on `docs/EHL_komplett_tekst_dump.txt`, several essential EHL commands are missing or incorrectly implemented:

### **Missing Commands**:

| VB6 Code | Decimal | Current Kotlin | Status | Impact |
|----------|---------|----------------|--------|---------|
| `0xC3` | 195 | ❌ Missing | **CRITICAL** | Product/pistol selection required before pricing |
| `0xA9` | 169 | ❌ Wrong impl | **CRITICAL** | Alternative price programming method |
| `0x81` | 129 | ✅ ZER(129) | **Need verify** | RESET command - verify response format |
| `0x6A` | 106 | ✅ LINETEST(106) | **OK** | Communication test |

### **Incorrect Implementations**:

| Command | VB6 Expected | Kotlin Current | Issue |
|---------|-------------|----------------|-------|
| PROG_PRC | 0xA9 (169) with 4 ASCII LSB | PROG_PRC(169) unknown format | Wrong data format |
| ERROR response | 2 ASCII bytes (hoved/under) | 1 byte | Response format mismatch |
| ZER/RESET | 0x81 response: 1 byte = 0x1E | Unknown | Response format unknown |

## 📋 Complete VB6 Command Reference

From detailed analysis, VB6 uses these commands:

### **Query Commands** (Host → Dispenser, no payload):
- `0x4B` (75) - STATE ✅ Implemented correctly  
- `0x4C` (76) - ERROR_QUERY ✅ Implemented correctly
- `0x45` (69) - VOLUME ✅ Implemented correctly  
- `0xC5` (197) - TANK ✅ Implemented correctly
- `0x5C` (92) - PRICE ✅ Implemented correctly
- `0x6A` (106) - LINETEST ✅ Implemented correctly

### **Control Commands** (Host → Dispenser):
- `0x69` (105) - BLOCK ✅ Implemented correctly
- `0x77` (119) - UNBLOCK ✅ Implemented correctly  
- `0x81` (129) - RESET ⚠️ Need to verify response format

### **Configuration Commands** (Host → Dispenser):
- `0xC3` (195) - **MISSING** Product/pistol selection (1 byte: 0x30)
- `0xA9` (169) - **WRONG** Price programming (4 ASCII LSB-first)
- `0x75` (117) - PROG_AMOUNT ✅ Implemented correctly
- `0x70` (112) - PROG_VOLUME ✅ Implemented correctly

## 🔧 Required Fixes

### 1. Add PRODUCT_SELECT Command
```kotlin
/** Product/pistol selection (VB6: 0xC3) - used before pricing */
PRODUCT_SELECT(195, "Product/pistol selection"),
```

### 2. Fix Price Programming  
Current `PROG_PRC(169)` should be:
```kotlin
/** Alternative price programming (VB6: 0xA9) - 4 ASCII digits LSB-first */
PROG_PRICE_ALT(169, "Alternative price programming"),
```

### 3. Verify RESET Response Format
VB6 expects `RESET` response with 1 data-byte = 0x1E (OK)

### 4. Fix ERROR Response Format
VB6 ERROR response contains **2 ASCII bytes** (main code + sub code), not 1 byte

## 📊 Impact Analysis

### **CRITICAL MISSING (SHOWSTOPPER)**:
- **PRODUCT_SELECT (0xC3)**: Used before all pricing operations
- **Incorrect PROG_PRC format**: Wrong price programming will fail

### **HIGH PRIORITY**:
- **ERROR response format**: 2-byte vs 1-byte format mismatch
- **RESET response verification**: Ensure proper OK response handling

### **VB6 Transaction Flow Requiring Missing Commands**:
1. `PRODUCT_SELECT` (0xC3) with pistol selection (0x30)
2. `PROG_PRICE_ALT` (0xA9) with 4 ASCII digits  
3. `PRICE` (0x5C) query to verify
4. `PROG_AMOUNT` (0x75) or `PROG_VOLUME` (0x70) for presets
5. `UNBLOCK` (0x77) to start delivery
6. Periodic `STATE`, `VOLUME`, `TANK` queries during delivery
7. `RESET` (0x81) when transaction complete

**Without PRODUCT_SELECT and correct price programming, the dispenser may reject all pricing operations.**

## 🛠️ Implementation Priority

1. **IMMEDIATE**: Add PRODUCT_SELECT(195) command
2. **IMMEDIATE**: Fix PROG_PRC → PROG_PRICE_ALT with correct format  
3. **HIGH**: Verify ERROR response format (2 bytes)
4. **HIGH**: Verify RESET response format (0x1E OK)
5. **MEDIUM**: Update emulator to handle new commands

This analysis shows the Kotlin implementation is missing critical commands needed for real dispenser operation.