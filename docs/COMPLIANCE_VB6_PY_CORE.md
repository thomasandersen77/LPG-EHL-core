# EHL Compliance Report: VB6 vs Python vs Core (Kotlin)

Date: 2026-01-12

## Scope
This document verifies the Kotlin Core EHL implementation against:
- VB6 original (pumpekontroll.frm, fra_dispenser.bas)
- Python legacy port (legacy-curated/Python/ehl_pumpekontroll_clone)

It covers framing (STX/LEN/CHK/ETX), command set, and payload encodings (PRICE, VOLUME, STATE).

---

## Framing Rules (Wire Level)

| Aspect | VB6 | Python | Core (Kotlin) | Match |
|---|---|---|---|---|
| STX controller→dispenser | 0x10 | 0x10 | 0x10 (EhlProtocolConfig.NORGES_GASS.stxController) | ✅ |
| STX dispenser→controller | 0x20 | 0x20 | 0x20 (EhlProtocolConfig.NORGES_GASS.stxDispenser) | ✅ |
| ETX | 0x36 | 0x36 | 0x36 (EhlProtocolConfig.etx) | ✅ |
| LEN | total frame length incl. ETX | same | same (packetLength) | ✅ |
| CHK | XOR of STX..last DATA | same | same (EhlPacket.calculateChecksum) | ✅ |

Examples (address=1):
- STATE Query: `10 06 01 4B 5C 36`
- STATE Reply: `20 07 01 4B 00 6D 36`
- LINETEST Reply: `20 08 01 6A 55 AA BC 36`

---

## Command Payload Encodings

### PRICE (0x5C)
- VB6: 4 ASCII digits, LSB-first. "15.90" => bytes: `30 39 35 31` ("0" "9" "5" "1").
- Python: `decode_price_from_data` builds string reversed of data.
- Core: `EhlDataParser.parsePriceData` reverses digits → `"%c%c.%c%c"`.
- Result: ✅ 1:1

### VOLUME (0x45)
- VB6: 5 ASCII digits, LSB-first. Example "04550" => 45.50 L.
- Python: `decode_volume_from_data` uses 5 ASCII digits.
- Core: `EhlDataParser.parseVolumeDataVb6` (5 ASCII, LSB-first). Formatter updated to VB6 mode.
- Result: ✅ 1:1

### STATE (0x4B)
- VB6: 1 byte bitfield; UI maps bits (AUTOMODUS 0x08, STARTKNAPP 0x04, ÅPEN 0x02).
- Python: `state_bits_from_byte` uses same mapping.
- Core: `parseStateData` returns byte; formatter maps bits similarly.
- Result: ✅ 1:1

---

## Divergences and Resolutions

- Wire-Test validator originally:
  - XOR computed from LEN (excluded STX) → false negatives. Fixed to include STX.
  - STX/ETX hard-coded (0x10/0x20/0x36). Now uses `EhlProtocolConfig` to remain strict yet configurable.
  - Formatter used legacy 4-byte VOLUME; now uses VB6 5-byte ASCII.

No remaining protocol-level mismatches were found.

---

## References
- VB6: `.../01-VB6-Original-Source/fra_dispenser.bas` (checksum and reply constructors using &H20 STX)
- Python: `legacy-curated/Python/ehl_pumpekontroll_clone/ehl/protocol.py`
- Core: `lpg-ehl-core/protocol/EhlCodec.kt`, `EhlProtocolConfig.kt`, `EhlPacketFormatter.kt`

---

## Conclusion
The Kotlin Core EHL implementation is 1:1 with both the VB6 original and the Python port on wire framing and payload encodings. The GUI wire-test tool has been aligned to VB6 rules and should now pass LINETEST/STATE/VOLUME/PRICE checks against both emulator and hardware.
