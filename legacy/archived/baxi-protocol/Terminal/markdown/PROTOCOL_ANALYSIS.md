# EHL Protocol Analysis: VB6 Legacy vs Kotlin Implementation

## Discovered Command Code Mismatches

### VB6 Commands Found in Legacy Code:
- `&H75` (117) - set_preset_amount() 
- `&H69` (105) - disp_block() - **BLOCK COMMAND**
- `&H77` (119) - disp_unblock() - UNBLOCK command 
- `&H4B` (75) - STATE query (multiple locations)
- `&H4C` (76) - ERROR query 
- `&H45` (69) - VOLUME query
- `&HC5` (197) - TANK status query
- `&H70` (112) - Volume preset (set_preset_volume)

### Kotlin Implementation Mismatches:

1. **CRITICAL: STX Value Mismatch**
   - VB6: `y(1) = &H10` (0x10 = 16) - Controller to Dispenser
   - Python: `STX_CONTROLLER = 0x10` ✅ 
   - Kotlin: `const val STX: Byte = 0x20` ❌ **WRONG!**

2. **BLOCK Command Mismatch**
   - VB6: `&H69` (105) for BLOCK operation
   - Kotlin: `BLOCK(105)` ✅ CORRECT
   - Kotlin: `STOP(47)` ❌ **NOT USED BY VB6**

3. **Missing Commands in Kotlin:**
   - `&H4C` (76) - ERROR query (VB6 has this, Kotlin has ERROR(37))
   - `&H70` (112) - Volume preset (VB6 has this, Kotlin has PROG_I(112) - same!)

4. **Correct Commands:**
   - STATE: VB6 `&H4B` (75) = Kotlin `STATE(75)` ✅
   - VOLUME: VB6 `&H45` (69) = Kotlin `VOLUME(69)` ✅  
   - TANK: VB6 `&HC5` (197) = Kotlin `TANK(197)` ✅
   - UNBLOCK: VB6 `&H77` (119) = Kotlin `UNBLOCK(119)` ✅

## Critical Issues Found:

### 1. STX Protocol Violation
**SHOWSTOPPER**: Kotlin uses wrong STX value (0x20 instead of 0x10) for controller-to-dispenser frames.

According to Python documentation:
```python
STX_CONTROLLER = 0x10  # PC/Controller -> Dispenser  
STX_DISPENSER = 0x20   # Dispenser -> PC/Controller
```

VB6 confirms this:
```vb
y(1) = &H10  ' Always 0x10 for outgoing commands
```

**Current Kotlin code assumes 0x20 for everything - THIS WILL NOT WORK WITH REAL DISPENSER!**

### 2. Missing VB6 State Machine Logic
Kotlin core lacks the complex state machine from VB6:
- Periodic polling with state_timer_Timer()
- MSComm1_OnComm() response handling
- Transaction state tracking (new_tank, tank_end, trans_unaccounted)
- Bit-level state decoding (automode, startbuttonpressed, openfordelivery)

### 3. Data Format Verification Needed
Need to verify encoding/decoding of:
- Price format (4 ASCII digits in specific order)
- Volume format (5 ASCII digits with decimal placement)  
- Preset amounts (BCD vs ASCII encoding)

## Recommendations:

### IMMEDIATE (Showstoppers):
1. **Fix STX values in EhlProtocol.kt**
2. **Add bidirectional frame support (0x10 vs 0x20)**
3. **Verify ERROR command code (37 vs 76)**

### HIGH PRIORITY:
1. **Implement VB6-compatible state machine**
2. **Add missing periodic polling logic**
3. **Implement transaction state tracking**

### MEDIUM PRIORITY:  
1. **Update emulator to handle both STX directions**
2. **Add comprehensive protocol testing**
3. **Validate data encoding formats**

## Next Steps:
1. Create protocol compatibility layer
2. Implement VB6 state machine logic  
3. Test against real dispenser hardware
4. Update emulator with correct protocol