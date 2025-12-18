### Analysen: Hva vi ber Warp om å fikse

1. **Sikkerhet i `EhlCodec` (The "Sanity Check"):**
* *Problemet:* I dag leser koden byte nr 2 og tror blindt på den. Hvis støy sier "lengden er 250", venter systemet for evig.
* *Løsningen:* Vi tvinger inn en sjekk: `if (length > MAX_PACKET_SIZE) throw Exception`.


2. **Buffer-reparasjon i `EhlCommunicator`:**
* *Problemet:* Hvis checksum feiler, kaster vi bare data. Vi må sjekke om det ligger en *ny* gyldig pakke rett bak den ødelagte bytes-rekken *før* vi leser mer fra porten.


3. **Hjernen i `DispenserService`:**
* *Problemet:* Nå er tjenesten bare et "rør".
* *Løsningen:* Vi innfører en enkel **Tilstandsmaskin (State Machine)**.
* `IDLE`: Kan motta ny pris.
* `AUTHORIZING`: Kort dratt, venter på pumpestart.
* `FILLING`: Pumper gass (IKKE endre pris nå!).
* `FINISHED`: Lagre transaksjon.




4. **Pris-strategi:**
* Pris kan kun sendes når pumpa er `IDLE`. Hvis vi sender ny pris mens kunden pumper, kan det skape kaos i displayet eller revisjonen.



---

### 📋 Instruks til Warp (Kopier dette)

Her er en detaljert teknisk spesifikasjon ("Spec") du limer inn i Warp. Den er skrevet på engelsk fordi LLM-er coder best fra engelske instruksjoner.

---

**PROMPT START**

I need you to refactor and harden the core logic in `lpg-ehl-core` and `lpg-ehl-api`. Currently, the code handles basic communication but lacks robustness for a noisy RS-485 production environment and proper business logic for the dispenser state.

Please perform the following tasks step-by-step.

### PART 1: Harden the Protocol (Critical Safety)

**Target File:** `no.cloudberries.lpg.protocol.EhlCodec.kt`

1. **Implement "Sanity Checks" in `decode`:**
* The current implementation blindly reads the length byte (`data[1]`).
* **Change:** Before reading the full packet, verify that the length byte is within valid bounds (e.g., typically EHL packets are small, max 64 bytes). If `length > 64` or `length < 3`, discard the buffer immediately to prevent waiting for data that will never arrive.


2. **Robust Checksum Validation:**
* Ensure the XOR checksum calculation excludes the STX (Start of Text) if that is required by the specific EHL variant, or verify it includes everything from Length to Data. (Assume standard EHL: XOR of all bytes after STX up to Checksum).



**Target File:** `no.cloudberries.lpg.communication.EhlCommunicator.kt`

1. **Fix Buffer Recovery Loop:**
* In `receive()`, if `tryParseBuffer` fails (returns null due to checksum error or bad length), the current code might drop valid data or wait for IO.
* **Logic update:** If parsing fails but we have remaining bytes in `buffer` that contain another `STX` (0x02), strictly loop and try to parse the *next* potential packet *before* reading more bytes from the serial port. This handles "back-to-back" packets where the first one might be corrupted noise.


2. **Reduce Log Noise:**
* Change the `logger.info` for every packet to `logger.debug` or `logger.trace`. Only log `logger.warn` if a checksum fails or a packet is discarded.



### PART 2: Implement Dispenser Logic (The State Machine)

**Target File:** `no.cloudberries.lpg.api.service.DispenserService.kt`

We need to track the state of the physical pump to know when it is safe to update prices and when to save a transaction.

1. **Create a State Enum:**
```kotlin
enum class DispenserState {
    IDLE,       // Pump is hung up, ready for instructions
    STARTED,    // Nozzle lifted / Start button pressed
    FILLING,    // Pulses are coming in (Gas flowing)
    FINISHED    // Nozzle hung up, transaction ready
}

```


2. **Implement State Logic in `handlePacket` (or equivalent observer):**
* Monitor the status byte from the EHL `Status` packet.
* **Transition IDLE -> STARTED:** When status changes from "Ready" to "Busy/Pumping".
* **Transition STARTED -> FILLING:** When the volume counter in the packet increments > 0.
* **Transition FILLING -> FINISHED:** When status goes back to "Ready" or "End of Transaction" flag is set.
* **Action on FINISHED:** Trigger `TransactionRepository.save()`.



### PART 3: Price Update Safety

**Target File:** `no.cloudberries.lpg.api.service.DispenserService.kt` (and potentially `EhlCommunicator`)

1. **Safe Price Updates:**
* Add a method `queuePriceUpdate(newPrice: Double)`.
* **Constraint:** ONLY send the "Set Price" command (Packet Type `0x..`) to the hardware if `DispenserState` is `IDLE`.
* If the dispenser is `FILLING`, cache the `nextPrice` and wait until the state transitions to `FINISHED` -> `IDLE` before sending it. This is critical to avoid changing unit price during a transaction.



### PART 4: Hardware Watchdog (Self-Healing)

**Target File:** `no.cloudberries.lpg.communication.SerialPortManager.kt`

1. **Connection Watchdog:**
* Implement a simple heartbeat check. If we haven't received *any* valid byte from the serial port in 60 seconds, assume the connection is dead (USB unplugged/driver hang).
* Trigger a `reconnect()` sequence: Close port -> Wait 5s -> Open port.



**Implementation Constraints:**

* Use Kotlin Coroutines for the async logic.
* Keep the clean architecture separation (Protocol code stays in `core`, Logic in `api`).
* Ensure code is exception-safe (don't let a parsing error crash the Spring Boot app).

Please generate the code changes for these files.

**PROMPT END**

---

### Hvordan du bruker dette, Thomas:

1. **Kjør del 1 og 2 først:** Få Warp til å fikse `EhlCodec` og `EhlCommunicator` først. Det er fundamentet. Hvis du prøver å bygge logikk på en ustabil grunnmur, blir du gal av feilsøking senere.
2. **Verifiser:** Når Warp har kodet, se spesielt på `EhlCodec.kt`. Sjekk at den faktisk har lagt inn `if (length > 64)` (eller en annen fornuftig verdi).
3. **Kjør del 3 og 4:** Når protokollen er trygg, implementer `DispenserService` med tilstandsmaskinen.

Dette er "Arkitekt Thomas" som delegerer til "Koder Warp". Det er akkurat slik du skal jobbe. Lykke til! 🚀