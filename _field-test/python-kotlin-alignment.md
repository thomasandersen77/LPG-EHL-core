# Python vs Kotlin UNBLOCK/STATE Alignment (Field Test Follow-up)

## Scope
- Compare Python test server in `_field-test/test-python/` with Kotlin stacks (`lpg-transport/`, `lpg-ehl-core/`, `lpg-ehl-service/`, `lpg-ehl-api/`).
- Focus on UNBLOCK→STATE behaviour that releases the pump without an `OK` ACK.
- Use findings from `logger_pumpe_test.md` as the reference scenario (dispenser responds with `STATE 0x5A`).

## Findings (Code Paths)
- **Service-layer UNBLOCK now mirrors Python flow.**  
  `lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/pump/PumpStateService.kt` sends UNBLOCK, then polls STATE until `OPEN_FOR_DELIVERY (0x02)` is seen, succeeding without requiring `OK`. This matches `python-server/server.py::unblock_verified` and `03_control_unblock_block.py::await_state_effect`.
- **Bit mapping is aligned.**  
  PumpStateService uses `StatusBitMasks.OPEN_FOR_DELIVERY / START_BUTTON_PRESSED / AUTOMODE` from core (same as `ehl_protocol.py::interpret_state_byte`). The old custom masks that conflicted with Python are no longer used.
- **Manager release API added.**  
  `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/controller/PumpController.kt::releasePump` calls `unblock(..., withAuthorization = false)`, matching the Python server’s “just release” behaviour (no card prerequisite). Frontend now calls `/release`.
- **Retry/transport unchanged vs. Python.**  
  `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/EhlCommunicator.kt` still clears the receive buffer on timeout before retry. Python keeps residual bytes. With the new service-level STATE poll (multiple sends/receiveUntil), the risk of discarding the first STATE is lower, but this is the main remaining behavioural difference.

## Behaviour vs. Logger Scenario
- **Observed Python outcome:** UNBLOCK → immediate STATE (`0x5A`), `open_for_delivery=True`, pump actually released.
- **Current Kotlin outcome (expected):**  
  - UNBLOCK sent.  
  - Service polls STATE up to 6s; any STATE with bit `0x02` succeeds.  
  - No failure on missing `OK`.  
  This directly addresses the root cause in `logger_pumpe_test.md`.

## Remaining Gaps / Risks
- **Buffer clear on retry (transport):** Late-but-valid STATE could still be dropped if it arrives just as a timeout triggers. Mitigation: transport-level change to parse before clearing, or service-level tolerance (already polling). Impact now low but still a divergence from Python.
- **Timeout tunings differ:** Kotlin defaults (sendAndReceive 3000ms, retries=3) are longer than Python defaults (~1200ms, retries configurable). Generally safe; note if hardware is sensitive to long holds.
- **No PRODUCT_SELECT/RESET fallback:** Python optional fallback (05_unlock_hold_block.py) not present in Kotlin. Only needed for dispensers that require preconditioning; not observed in current logs.

## Quick Verification Checklist
1. Run field mode against hardware or `test-python` simulator.  
2. Capture TX/RX for UNBLOCK → expect STATE `0x5A` (or any with bit `0x02`).  
3. Confirm PumpStateService transitions to `READY_TO_PUMP` without error.  
4. Verify block/settle path still works (unchanged).  
5. If intermittent: check retries/clearBuffer timing in `EhlCommunicator`.

## Conclusion
- The Kotlin service logic is now functionally aligned with the Python server for pump release: STATE with `OPEN_FOR_DELIVERY` is treated as success, matching the behaviour that already works in Python.  
- Primary residual difference is transport-level buffer clearing on retry; monitor in field tests, adjust if late STATE frames are still lost.  
- Manager flow now releases without card, matching the Python server’s capability to release pumps directly.
