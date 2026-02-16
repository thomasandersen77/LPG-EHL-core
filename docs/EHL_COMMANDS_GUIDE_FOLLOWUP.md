# EHL COMMANDS GUIDE FOLLOWUP (repo vs MM Petro EHL-x4 manual)

This document compares:
- **What we already knew / assumed** from this repo’s implementation and legacy VB6 evidence (`docs/EHL_COMMANDS_GUIDE.md` and the code), vs
- **What the official MM Petro “EHL-x4 Electronic Counter” manual says** (`/Users/alejandrosaksida/Downloads/MM petro.pdf`)

Goal: identify **confirmed truths**, **new insights**, and **gaps/contradictions** so we understand what the dispenser/counter can *actually* do via RS‑485.

---

## Executive summary (what changed after reading the manual)

The manual **does not** give an explicit “wire protocol opcode table” (no numeric `0x4B/0x45/...` mapping in the extracted text), but it **does** add several high‑impact constraints and confirmations:

- **Multiple nozzles/products are real** at the hardware level (up to 8 nozzles, multiproduct price displays). This supports that something like `PRODUCT_SELECT` is meaningful—at least on some models.
- **RS‑485 addressing appears to be 1..31 per side (L/R) and configurable** via internal functions (Fn‑19/Fn‑41/Fn‑42), which clashes with our field observation of responders at `0x20/0x21` and our code’s broad `1..255` assumption.
- **VOLUME digit count is configurable (5 or 6 bytes)** (Fn‑43), which directly impacts our `VOLUME (0x45)` parser/validator (repo largely assumes 5 digits/bytes).
- **Feature flags exist by product variant** (e.g., the “P” in the model denotes *amount preset programming enabled*). That means some commands we support (`PROG_AMOUNT` / `PROG_VOLUME`) may be **model-dependent**.

---

## What we already knew from the repo (pre-manual baseline)

From `docs/EHL_COMMANDS_GUIDE.md` + Kotlin/Python/VB6 sources, we already had a working “Norges Gass EHL variant” model:

- Binary framing: `STX=0x10/0x20`, `ETX=0x36`, XOR checksum.
- A known command set (repo-exhaustive): `STATE`, `VOLUME`, `PRICE`, `ERROR_QUERY`, `LINETEST`, `UNBLOCK`, `BLOCK`, `ZER/RESET`, `PRODUCT_SELECT`, `PROG_PRC`, `PROG_AMOUNT`, `PROG_VOLUME`, `TANK`, etc.
- VB6-derived state bit interpretation for `STATE` and basic flows (`PRODUCT_SELECT → PROG_PRC → UNBLOCK`, then poll `STATE`/`VOLUME`).
- Evidence that **UNBLOCK toggling state ≠ physical unlock** (depends on additional conditions / physical actions).

This was “internally consistent” within the repo, but not strongly grounded in vendor documentation.

---

## What the MM Petro manual explicitly confirms (high confidence)

### 1) The device is inherently multi-module and multi-nozzle capable

The manual’s product denotation explicitly includes:
- **Number of modules**: 1..4
- **Number of pump nozzles**: 1..8
- Optional **price displays per discharge hose** (multiproduct)

Implication:
- Multi-product/nozzle selection is not hypothetical; it’s an intended hardware capability.
- A command like `PRODUCT_SELECT` (repo’s `0xC3`) likely maps to selecting a **hose/nozzle/product** *or* to a “prepare/prestart” gating step (repo legacy uses constant `0x30`).

### 2) Display semantics match our numeric assumptions (0.01 liter resolution; 4-digit price)

The manual states:
- Delivered volume is displayed with **0.01 liter** precision.
- Price field is **4 digits**.

Implication:
- Our “divide by 100” decoding for `VOLUME` and `PRICE` is consistent with the counter’s stated UI precision.

### 3) RS‑485 is an official supported communication mode

Manual describes RS‑485 wiring, termination (120Ω), and network operation.

Implication:
- RS‑485 is not an aftermarket hack; it’s supported and addressable.

---

## New insights the manual adds (actionable deltas vs the repo)

### A) RS‑485 addressing appears to be **1..31** and **per-side**

The manual describes configurable addresses for RS‑485 network operation:
- Fn‑19: “Setting address … for operation in network RS 485” (range **1..31**) and mentions **independent setting** for right/left sides; the address is displayed in the “unit price” field.
- Fn‑41/Fn‑42: separate STR_L and STR_P address settings (again **1..31**).

**How this differs from the repo:**
- Repo code accepts address `1..255`.
- Field scans in this repo found responders at `0x20` and `0x21` (32/33) and VB6 evidence suggests an offset (+32).

**Interpretation / likely reconciliation:**
- The manual’s “network address” range (1..31) looks like the **configured logical node IDs**.
- The repo’s observed `0x20/0x21` responders could be:
  - a **different protocol layer** (e.g., Norges Gass framing uses another addressing convention), or
  - “device internal address = configuredAddress + 32” as the VB6 system did, or
  - L/R “sides” (two addresses for one physical dispenser) rather than two different dispensers.

**Concrete next step (if you want to verify in field):**
- On real hardware, set/confirm Fn‑19 / Fn‑41 / Fn‑42 and then run `projects/python-test/02_scan_addresses.py` to see if responders shift accordingly.

### B) `VOLUME` response digit count is configurable: **5 or 6 bytes**

Fn‑43 in the manual explicitly says the counter can send **5 or 6 bytes** for delivered quantity to the external system.

**How this differs from the repo:**
- Kotlin `EhlDataParser.parseVolumeDataVb6()` requires exactly 5 bytes.
- `WireTraceService` VB6 validation expects exactly 5 ASCII digits.
- Python `interpret_volume_bytes()` is “best-effort” but the field scripts operationally assume 5.

**Implication:**
- We likely need to support **both 5- and 6-digit variants** in the code and in the “VB6 payload validation” logic *if* we encounter counters configured for 6 digits.

### C) Preset programming is model-dependent (“P” option)

Manual product denotation:
- `P` = “programming amount preset”
- no `P` = no programming amount preset

**How this differs from the repo:**
- Repo treats `PROG_AMOUNT (0x75)` / `PROG_VOLUME (0x70)` as generally available.

**Implication:**
- Some deployed counters may legitimately ignore/reject preset commands; your integration should treat that as **capability negotiation**, not necessarily a wiring fault.

### D) The manual hints at multiple protocol standards/variants (“EHL‑01”, external controllers)

Manual references:
- A transmission protocol “1a EHL‑01” selected via DIP switch (for some dispenser types/versions).
- Cooperation with external systems (EHP‑02 / EHC‑02).

**How this differs from the repo:**
- Repo assumes one framing/command set (“Norges Gass variant”) and reverse-engineers opcodes from VB6.

**Implication:**
- There may be multiple protocol “dialects” depending on firmware version and DIP settings, and our repo’s variant may be only one of them.

---

## What the manual does NOT confirm (still “repo-only” knowledge)

These remain **not vendor-confirmed** based on the extracted text from the manual:

- The numeric opcode mapping: `STATE=0x4B`, `VOLUME=0x45`, `PRICE=0x5C`, etc.
- The `STATE (0x4B)` bit meanings (`0x02/0x04/0x08/0x80`) used by VB6/Kotlin/Python.
- The specific semantics of `UNBLOCK/BLOCK/ZER` and command-specific ACK behavior (`0x30` payload).

So: the repo’s command table is still primarily **empirical / legacy-driven**, not “manual derived”.

---

## Evidence of multiple products vs “always single value” (manual + repo combined)

Combined conclusion:
- **Manual**: definitely supports multi-nozzle/multiproduct configurations (up to 8 nozzles; optional price displays per hose). That is evidence that “multiple products exist” in the hardware ecosystem.
- **Repo legacy**: `PRODUCT_SELECT` payload is very often treated as a constant `0x30` (ASCII “0”), which suggests “select default pistol” or “prestart” rather than true multi-grade selection.
- **Repo modern service**: allows `productId` to vary (suggesting intended multi-product support), but we don’t have in-repo field traces showing multiple IDs accepted by hardware.

Practical takeaway:
- Treat “multiple products” as **real hardware capability**, but treat “payload values other than `0x30`” as **unproven on our deployed hardware** until verified with field tests.

---

## Recommended follow-up validation experiments (minimal risk)

1) **Validate address model**:
   - Confirm Fn‑19/Fn‑41/Fn‑42 on a counter.
   - Scan with `02_scan_addresses.py`.
   - Check whether responders appear at `configured` vs `configured+32` vs dual addresses for L/R.

2) **Validate VOLUME width**:
   - If the counter has Fn‑43 accessible, set it to 5 vs 6 and poll `VOLUME`.
   - Confirm whether payload length changes and whether it remains ASCII digits LSB-first.

3) **Validate multi-product selection**:
   - If the dispenser is truly multiproduct, attempt `PRODUCT_SELECT` with values other than `0x30` and observe:
     - whether `PRICE` changes,
     - whether `STATE`/authorization behavior changes,
     - whether `ERROR_QUERY` flags a rejection.

---

## Source excerpts (manual)

Key manual signals referenced above come from:
- Product denotation including number of modules/nozzles and communication type.
- Fn‑19/Fn‑41/Fn‑42 addressing functions (RS‑485 network address 1..31, per side).
- Fn‑43 “command<VOLUME> … sends 5 or 6 bytes”.

---

## Bottom line

The manual strengthens confidence that:
- multi-nozzle/multi-product is a core hardware feature,
- RS‑485 addressing and external-system integration are real and configurable,
- metering precision matches our parsing model.

But it also signals that:
- address mapping and `VOLUME` payload length may differ from our assumptions,
- preset programming is not universal,
- multiple protocol dialects may exist (DIP-selected).

So the repo’s “EHL command set” is still the best operational reference we have, but we should treat it as **one dialect** with **configuration-dependent edges** that the manual partially explains.

