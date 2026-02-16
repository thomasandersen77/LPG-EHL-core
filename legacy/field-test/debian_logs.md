# Når jeg kjører i lab modus så får jeg følgende feil. Det er et flagg med autorisasjon som skal settes i databasen. I med en prompt som cursor eller june fra JetBrains kan bruke for å finne ut hvorfor. Pumpeautorisasjon med Pump authorizations ikke oppdateres riktig når man trykker på betaling i selve stasjonens eiergrensesnittet eller i kontrollpanelet og knappen for å resette. Alt. Den fungerer ikke. Den resetter ikke alle transaksjoner i databasen som completed for at dette skal fungere.

# logg fra ark

ssh thomas@192.168.0.9
Linux debian 6.1.0-42-amd64 #1 SMP PREEMPT_DYNAMIC Debian 6.1.159-1 (2025-12-30) x86_64

The programs included with the Debian GNU/Linux system are free software;
the exact distribution terms for each program are described in the
individual files in /usr/share/doc/*/copyright.

Debian GNU/Linux comes with ABSOLUTELY NO WARRANTY, to the extent
permitted by applicable law.
Last login: Fri Feb 13 03:56:21 2026 from 192.168.0.8
thomas@debian:~$ ./s
scripts/          start-lpg-ehl.sh  
thomas@debian:~$ ./start-lpg-ehl.sh
^Cthomas@debian:~$ nano start-lpg-ehl.sh
thomas@debian:~$ ./start-lpg-ehl.sh

.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v3.2.1)

04:55:51.359 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Starting LpgEhlApiApplicationKt using Java 21.0.10 with PID 610 (/home/thomas/release/lpg-ehl-webapp.jar started by thomas in /home/thomas)
04:55:51.411 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - The following 1 profile is active: "lab"
Database is up to date, no changesets to execute
04:56:59.839 WARN  [-] o.h.orm.deprecation - HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
04:57:17.588 INFO  [-] n.c.l.a.c.CommunicationConfig -
04:57:17.591 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
04:57:17.597 INFO  [-] n.c.l.a.c.CommunicationConfig -   EHL KOMMUNIKASJON: 🧪 LAB MODE
04:57:17.601 INFO  [-] n.c.l.a.c.CommunicationConfig -   Active profiles: [lab]
04:57:17.604 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
04:57:17.607 INFO  [-] n.c.l.a.c.CommunicationConfig -
04:57:34.324 INFO  [-] n.c.l.a.c.CommunicationConfig - 🧪 Creating EhlDispenserEmulator (address=1, price=1590)
04:57:34.375 INFO  [-] n.c.l.e.i.EhlDispenserEmulatorImpl - EHL Dispenser Emulator initialized: address=1, price=15.9 kr/L
04:57:34.457 INFO  [-] n.c.l.a.c.TransportConfiguration -
04:57:34.460 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
04:57:34.462 INFO  [-] n.c.l.a.c.TransportConfiguration -   🔬 LAB MODE
04:57:34.465 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
04:57:34.469 INFO  [-] n.c.l.a.c.TransportConfiguration -   Transport:  InMemorySerialPort + Emulator
04:57:34.479 INFO  [-] n.c.l.a.c.TransportConfiguration -   Latency:    20ms (simulated)
04:57:34.481 INFO  [-] n.c.l.a.c.TransportConfiguration -   Hardware:   NOT REQUIRED
04:57:34.491 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
04:57:34.494 INFO  [-] n.c.l.a.c.TransportConfiguration -
04:57:34.611 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlCommunicator with InMemorySerialPort
04:57:34.616 INFO  [-] n.c.l.a.c.TransportConfiguration - Raw protocol logging: ENABLED
04:57:34.702 INFO  [-] n.c.l.a.c.TransportConfiguration - 🔄 Retry config: maxRetries=3, initialDelay=100ms, maxDelay=2000ms, backoff=2.0
04:57:34.781 INFO  [-] n.c.l.e.i.InMemorySerialPort - 🔌 InMemorySerialPort: Kobler til emulator (LAB MODE)
04:57:34.784 INFO  [-] n.c.l.a.c.TransportConfiguration - ✅ Transport connected successfully
04:57:39.250 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:57:39.252 INFO  [-] n.c.l.s.p.PumpStateService - 🌟 OPPSTART: Initialiserer pris...
04:57:40.779 INFO  [-] n.c.l.s.p.PumpStateService - 🏷️ STARTUP: Gjenopprettet pris 15.9 kr/L fra database
04:57:40.782 INFO  [-] n.c.l.s.p.PumpStateService -    Satt av: system
04:57:40.785 INFO  [-] n.c.l.s.p.PumpStateService -    Gyldig fra: 2026-02-12T18:11:13.518945
04:57:40.787 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:57:46.094 INFO  [-] n.c.l.a.p.MockPlsService - 🏷️ [MOCK PLS] Initialized with default prices
04:57:51.090 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlOperationsService
04:57:53.402 WARN  [-] o.s.b.a.o.j.JpaBaseConfiguration$JpaWebConfiguration - spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
04:57:53.930 WARN  [-] o.s.b.a.s.s.UserDetailsServiceAutoConfiguration -

Using generated security password: b4892011-e0d2-4f51-81c7-ff1a1dadef2a

This generated password is for development use only. Your security configuration must be updated before running your application in production.

04:58:10.479 INFO  [-] org.xnio - XNIO version 3.8.8.Final
04:58:10.574 INFO  [-] org.xnio.nio - XNIO NIO Implementation Version 3.8.8.Final
04:58:11.916 INFO  [-] org.jboss.threads - JBoss Threads version 3.5.0.Final
04:58:12.595 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Started LpgEhlApiApplicationKt in 151.491 seconds (process running for 160.455)
04:58:14.633 INFO  [-] n.c.l.a.s.WebAppPollingService - 🚀 WebApp polling service started - UI live updates enabled
04:58:24.478 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
04:58:24.755 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:24.760 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
04:58:25.200 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
04:58:25.217 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
04:58:25.242 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
04:58:25.283 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
04:58:25.312 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
04:58:25.361 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
04:58:25.378 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - UNBLOCK: Starting new transaction
04:58:25.395 INFO  [OPERATOR] n.c.l.e.i.DispenserSimulatorImpl - Starting simulation: 0.5 L/s, 15.9 kr/L
04:58:25.456 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: AUTHORIZED → DELIVERING
04:58:25.475 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:25.479 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:25.483 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
04:58:25.490 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:25.494 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:25.499 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:25.506 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:25.532 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:25.535 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:25.545 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:25.601 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 06 5B 36 10 07 01 4B 06 5B 36]
04:58:25.615 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
04:58:25.619 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
04:58:25.642 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:25.650 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
04:58:25.653 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:25.682 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
04:58:25.706 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:25.710 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:25.722 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:25.726 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:25.730 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:25.739 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
04:58:25.759 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK verified: open_for_delivery=1 after 977ms, first STATE raw=0x06
04:58:25.768 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK BEKREFTET - Dispenser klar for pumping
04:58:25.773 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:26.090 INFO  [OPERATOR] n.c.l.s.t.TransactionService - ⛽ Transaksjon opprettet: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, dispenser=1, pris=15.9 kr/L, status=STARTED
04:58:26.154 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.156 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:26.159 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.162 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:26.184 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:26.186 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.194 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.298 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:26.300 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:26.304 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.307 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:26.322 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ HARDWARE PUMPING DETECTED: Raw state 0x06 for pump 1
04:58:26.323 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.325 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ STATE TRANSITION: READY_TO_PUMP → PUMPING (pump 1)
04:58:26.326 INFO  [-] n.c.l.s.p.PumpStateService -    60s timeout cancelled - customer started pumping
04:58:26.338 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.357 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.369 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:26.371 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:26.401 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:26.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.437 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 30 30 30 62 36
04:58:26.456 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 39 34 30 30 30 62 36]
04:58:26.497 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:26.499 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:26.502 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:26.514 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.517 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:26.519 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:26.570 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.573 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:26.575 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.577 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:26.589 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📝 Transaksjon opprettet: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, pris=15.9 kr/L
04:58:26.590 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.592 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ⏱️ 60s TIMEOUT STARTED: Pump 1
04:58:26.593 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på at kunde starter pumping...
04:58:26.594 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.603 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:26.604 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.608 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 30 30 30 69 36
04:58:26.611 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 34 30 30 30], chksum=62)
04:58:26.613 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:26.621 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🔓 PUMPE FRIGJORT: Pump #1 klar til fylling (60s timeout startet)
04:58:26.623 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump released: state=PUMPING
04:58:26.665 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 30 30 30 62 36
04:58:26.668 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.49 L | Bytes: [10 0B 01 45 39 34 30 30 30 62 36] | Checksum: 0x62 ✓
04:58:26.678 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.687 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:26.690 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.692 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:26.715 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:26.717 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.719 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.723 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 30 36 30 30 30 69 36 10 07 01 4B 06 5B 36]
04:58:26.725 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:26.728 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 36 30 30 30], chksum=69)
04:58:26.731 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:26.742 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 30 30 30 69 36
04:58:26.746 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.60 L | Bytes: [10 0B 01 45 30 36 30 30 30 69 36] | Checksum: 0x69 ✓
04:58:26.748 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:27.070 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.073 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:27.076 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.078 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:27.104 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:27.106 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.110 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 38 30 30 30 62 36
04:58:27.122 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:27.124 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:27.127 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:27.131 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:27.570 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.573 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:27.576 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.579 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:27.608 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:27.611 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.615 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 31 31 30 30 6F 36
04:58:27.623 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 35 38 30 30 30 62 36 10 0B 01 45 30 31 31 30 30 6F 36]
04:58:27.626 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
04:58:27.628 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 38 30 30 30], chksum=62)
04:58:27.630 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:27.633 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 38 30 30 30 62 36
04:58:27.636 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.85 L | Bytes: [10 0B 01 45 35 38 30 30 30 62 36] | Checksum: 0x62 ✓
04:58:27.638 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:27.642 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:27.645 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:27.647 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:27.650 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:27.672 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:27.675 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:27.679 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:27.682 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 31 31 30 30], chksum=6F)
04:58:27.684 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:27.686 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 31 31 30 30 6F 36
04:58:27.689 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.10 L | Bytes: [10 0B 01 45 30 31 31 30 30 6F 36] | Checksum: 0x6F ✓
04:58:27.699 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 0.5 L fyllt (13.52 kr)
04:58:28.070 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.073 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:28.075 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.077 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:28.100 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:28.103 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.106 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 33 31 30 30 68 36
04:58:28.110 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 35 33 31 30 30 68 36]
04:58:28.111 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:28.113 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:28.114 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:28.116 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:28.119 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:28.120 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:28.570 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.572 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:28.574 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.576 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:28.598 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:28.600 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.602 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 36 31 30 30 69 36
04:58:28.609 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 33 31 30 30], chksum=68)
04:58:28.611 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:28.614 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 33 31 30 30 68 36
04:58:28.616 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.35 L | Bytes: [10 0B 01 45 35 33 31 30 30 68 36] | Checksum: 0x68 ✓
04:58:28.619 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:28.629 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:28.631 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:28.633 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:28.654 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:28.657 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:28.660 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:28.664 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 31 36 31 30 30 69 36 10 07 01 4B 06 5B 36]
04:58:28.665 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:28.666 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 36 31 30 30], chksum=69)
04:58:28.669 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:28.671 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 36 31 30 30 69 36
04:58:28.674 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.61 L | Bytes: [10 0B 01 45 31 36 31 30 30 69 36] | Checksum: 0x69 ✓
04:58:28.683 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:28.685 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.0 L fyllt (21.47 kr)
04:58:29.071 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.073 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:29.075 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.078 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:29.109 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:29.111 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.115 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 38 31 30 30 60 36
04:58:29.118 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:29.119 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:29.122 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:29.124 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:29.571 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.575 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:29.578 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.590 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:29.613 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:29.616 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.620 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 31 32 30 30 6D 36
04:58:29.624 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 36 38 31 30 30 60 36 10 0B 01 45 31 31 32 30 30 6D 36]
04:58:29.625 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
04:58:29.627 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[36 38 31 30 30], chksum=60)
04:58:29.630 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:29.633 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 38 31 30 30 60 36
04:58:29.641 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.86 L | Bytes: [10 0B 01 45 36 38 31 30 30 60 36] | Checksum: 0x60 ✓
04:58:29.643 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:29.647 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:29.649 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:29.652 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:29.656 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:29.678 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:29.680 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:29.683 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:29.687 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 31 32 30 30], chksum=6D)
04:58:29.692 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:29.695 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 31 32 30 30 6D 36
04:58:29.698 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.11 L | Bytes: [10 0B 01 45 31 31 32 30 30 6D 36] | Checksum: 0x6D ✓
04:58:29.705 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.5 L fyllt (29.57 kr)
04:58:29.874 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🛑 FRI PUMPE: Block request for address 1
04:58:29.886 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:29.889 INFO  [OPERATOR] n.c.lpg.protocol - 🛑 STOPP PUMPE - Sender BLOCK til dispenser #1
04:58:29.896 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:58:29.907 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 69 7E 36] -> BLOCK
04:58:29.909 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:58:29.911 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | BLOCK (Block/stop the dispenser) | Bytes: [10 06 01 69 7E 36] | Checksum: 0x7E ✓
04:58:29.935 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=BLOCK(105), data=[], chksum=7E)
04:58:29.938 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:58:29.940 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - STOP/BLOCK: Stopping delivery
04:58:29.963 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Transaction completed: 2.266 L, 36.03 kr
04:58:29.965 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Totals FROZEN - requires reset before next transaction
04:58:29.968 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: DELIVERING → PAYMENT_PENDING
04:58:29.972 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:29.975 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
04:58:29.977 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:58:29.982 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 0B 01 45 37 32 32 30 30 68 36]
04:58:29.985 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 32 bytes
04:58:29.986 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:29.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:29.989 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:29.995 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:29.999 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:30.004 INFO  [OPERATOR] n.c.lpg.protocol - ✅ BLOCK OK - Respons: STATE
04:58:30.006 INFO  [OPERATOR] n.c.lpg.protocol - 📊 Henter finalt volum...
04:58:30.019 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.023 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:30.026 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.029 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:30.065 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:30.066 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.076 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:58:30.080 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
04:58:30.082 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
04:58:30.085 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:30.087 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
04:58:30.089 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:30.114 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.116 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:30.118 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.121 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:30.123 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:30.135 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🛑 Transaksjon stoppet: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, volum=1.86 L, beløp=29.57 kr, status=PENDING
04:58:30.143 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:30.145 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.148 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:58:30.151 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
04:58:30.153 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:30.155 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
04:58:30.157 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
04:58:30.160 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:30.174 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📋 Transaksjon oppdatert til PENDING: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, 1.86 L = 29.57 kr
04:58:30.178 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🛑 Pumping stoppet: 1.86 L = 29.57 kr - venter betaling
04:58:30.184 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump blocked: state=PAYMENT_PENDING, volume=1.86L
04:59:26.603 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:59:26.607 INFO  [-] n.c.l.s.p.PumpStateService - ⏰ 60s TIMEOUT EXPIRED: Pump 1
04:59:26.609 INFO  [-] n.c.l.s.p.PumpStateService -    Pumping ikke startet - sender BLOCK
04:59:26.611 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:59:26.619 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:59:26.622 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 69 7E 36] -> BLOCK
04:59:26.625 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:59:26.628 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | BLOCK (Block/stop the dispenser) | Bytes: [10 06 01 69 7E 36] | Checksum: 0x7E ✓
04:59:26.649 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=BLOCK(105), data=[], chksum=7E)
04:59:26.651 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:59:26.655 WARN  [-] n.c.l.e.i.EhlDispenserEmulatorImpl - STOP/BLOCK received in state PAYMENT_PENDING - ignoring
04:59:26.656 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:59:26.660 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[37 32 32 30 30], chksum=68)
04:59:26.663 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:59:26.665 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:59:26.668 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.27 L | Bytes: [10 0B 01 45 37 32 32 30 30 68 36] | Checksum: 0x68 ✓
04:59:26.703 INFO  [-] n.c.l.s.p.PumpStateService - 📝 Transaction 960f8cf9-5f64-430f-834b-5462bacbc8c0 marked as CANCELLED (60s timeout)
04:59:26.706 INFO  [-] n.c.l.s.p.PumpStateService - 🛑 BLOCK SENT: Pump 1 blocked after 60s timeout
04:59:26.943 INFO  [DEBUG] n.c.l.a.c.SerialDebugController - Listing available serial ports
04:59:27.056 INFO  [DEBUG] n.c.l.s.s.SerialPortScanner - Found 0 serial ports (0 hardware, 0 virtual, 0 macOS-detected)
04:59:29.775 INFO  [DEBUG] n.c.l.a.c.SerialDebugController - Listing available serial ports
04:59:29.789 INFO  [DEBUG] n.c.l.s.s.SerialPortScanner - Found 0 serial ports (0 hardware, 0 virtual, 0 macOS-detected)
^Cthomas@debian:~$ ./start-lpg-ehl.sh

.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v3.2.1)

05:01:18.740 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Starting LpgEhlApiApplicationKt using Java 21.0.10 with PID 668 (/home/thomas/release/lpg-ehl-webapp.jar started by thomas in /home/thomas)
05:01:18.762 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - The following 1 profile is active: "lab"
Database is up to date, no changesets to execute
05:02:23.758 WARN  [-] o.h.orm.deprecation - HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
05:02:42.209 INFO  [-] n.c.l.a.c.CommunicationConfig -
05:02:42.210 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
05:02:42.219 INFO  [-] n.c.l.a.c.CommunicationConfig -   EHL KOMMUNIKASJON: 🧪 LAB MODE
05:02:42.221 INFO  [-] n.c.l.a.c.CommunicationConfig -   Active profiles: [lab]
05:02:42.222 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
05:02:42.223 INFO  [-] n.c.l.a.c.CommunicationConfig -
05:02:59.125 INFO  [-] n.c.l.a.c.CommunicationConfig - 🧪 Creating EhlDispenserEmulator (address=1, price=1590)
05:02:59.188 INFO  [-] n.c.l.e.i.EhlDispenserEmulatorImpl - EHL Dispenser Emulator initialized: address=1, price=15.9 kr/L
05:02:59.301 INFO  [-] n.c.l.a.c.TransportConfiguration -
05:02:59.302 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
05:02:59.303 INFO  [-] n.c.l.a.c.TransportConfiguration -   🔬 LAB MODE
05:02:59.305 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
05:02:59.307 INFO  [-] n.c.l.a.c.TransportConfiguration -   Transport:  InMemorySerialPort + Emulator
05:02:59.314 INFO  [-] n.c.l.a.c.TransportConfiguration -   Latency:    20ms (simulated)
05:02:59.317 INFO  [-] n.c.l.a.c.TransportConfiguration -   Hardware:   NOT REQUIRED
05:02:59.323 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
05:02:59.324 INFO  [-] n.c.l.a.c.TransportConfiguration -
05:02:59.437 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlCommunicator with InMemorySerialPort
05:02:59.448 INFO  [-] n.c.l.a.c.TransportConfiguration - Raw protocol logging: ENABLED
05:02:59.518 INFO  [-] n.c.l.a.c.TransportConfiguration - 🔄 Retry config: maxRetries=3, initialDelay=100ms, maxDelay=2000ms, backoff=2.0
05:02:59.603 INFO  [-] n.c.l.e.i.InMemorySerialPort - 🔌 InMemorySerialPort: Kobler til emulator (LAB MODE)
05:02:59.607 INFO  [-] n.c.l.a.c.TransportConfiguration - ✅ Transport connected successfully
05:03:03.457 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:03:03.461 INFO  [-] n.c.l.s.p.PumpStateService - 🌟 OPPSTART: Initialiserer pris...
05:03:05.136 INFO  [-] n.c.l.s.p.PumpStateService - 🏷️ STARTUP: Gjenopprettet pris 15.9 kr/L fra database
05:03:05.138 INFO  [-] n.c.l.s.p.PumpStateService -    Satt av: system
05:03:05.143 INFO  [-] n.c.l.s.p.PumpStateService -    Gyldig fra: 2026-02-12T18:11:13.518945
05:03:05.145 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:03:09.753 INFO  [-] n.c.l.a.p.MockPlsService - 🏷️ [MOCK PLS] Initialized with default prices
05:03:15.013 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlOperationsService
05:03:17.231 WARN  [-] o.s.b.a.o.j.JpaBaseConfiguration$JpaWebConfiguration - spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
05:03:17.772 WARN  [-] o.s.b.a.s.s.UserDetailsServiceAutoConfiguration -

Using generated security password: 1bcbe01c-52c1-4a71-9221-a3b3b9a7d5bb

This generated password is for development use only. Your security configuration must be updated before running your application in production.

05:03:34.473 INFO  [-] org.xnio - XNIO version 3.8.8.Final
05:03:34.574 INFO  [-] org.xnio.nio - XNIO NIO Implementation Version 3.8.8.Final
05:03:35.700 INFO  [-] org.jboss.threads - JBoss Threads version 3.5.0.Final
05:03:36.398 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Started LpgEhlApiApplicationKt in 147.766 seconds (process running for 156.368)
05:03:38.426 INFO  [-] n.c.l.a.s.WebAppPollingService - 🚀 WebApp polling service started - UI live updates enabled
05:05:12.992 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
05:05:13.272 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:13.274 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:05:13.654 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:13.676 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:05:13.702 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:13.741 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:05:13.772 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:05:13.820 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:13.836 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - UNBLOCK: Starting new transaction
05:05:13.853 INFO  [OPERATOR] n.c.l.e.i.DispenserSimulatorImpl - Starting simulation: 0.5 L/s, 15.9 kr/L
05:05:13.922 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: AUTHORIZED → DELIVERING
05:05:13.943 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:13.947 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:13.953 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:05:13.962 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:13.969 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:13.974 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:13.983 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:14.011 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:14.015 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.032 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.089 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 06 5B 36 10 07 01 4B 06 5B 36]
05:05:14.104 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:05:14.117 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:05:14.131 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:14.139 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:05:14.142 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:14.169 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:05:14.195 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:14.200 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:14.210 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.216 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:14.220 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:14.228 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:14.249 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK verified: open_for_delivery=1 after 962ms, first STATE raw=0x06
05:05:14.256 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK BEKREFTET - Dispenser klar for pumping
05:05:14.263 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:14.498 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.509 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:14.515 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.526 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:14.547 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:14.549 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.551 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.641 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:14.648 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:14.652 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.660 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:14.668 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ HARDWARE PUMPING DETECTED: Raw state 0x06 for pump 1
05:05:14.682 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:14.686 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ STATE TRANSITION: READY_TO_PUMP → PUMPING (pump 1)
05:05:14.694 INFO  [-] n.c.l.s.p.PumpStateService -    60s timeout cancelled - customer started pumping
05:05:14.696 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:14.724 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.736 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:14.740 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.747 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:14.761 INFO  [OPERATOR] n.c.l.s.t.TransactionService - ⛽ Transaksjon opprettet: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, dispenser=1, pris=15.9 kr/L, status=STARTED
05:05:14.773 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:14.775 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.787 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 34 30 30 30 6E 36
05:05:14.804 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 35 34 30 30 30 6E 36]
05:05:14.844 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:14.858 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:14.861 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:14.872 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.874 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:14.875 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:14.879 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.881 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:14.882 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.884 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:14.913 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:14.915 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.919 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 30 30 30 6A 36
05:05:14.930 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 34 30 30 30], chksum=6E)
05:05:14.931 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:14.972 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 34 30 30 30 6E 36
05:05:14.974 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.45 L | Bytes: [10 0B 01 45 35 34 30 30 30 6E 36] | Checksum: 0x6E ✓
05:05:14.983 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.984 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:14.987 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.989 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:15.010 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:15.012 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:15.014 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:15.017 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 30 35 30 30 30 6A 36 10 07 01 4B 06 5B 36]
05:05:15.018 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:15.019 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 35 30 30 30], chksum=6A)
05:05:15.021 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:15.023 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 30 30 30 6A 36
05:05:15.025 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.50 L | Bytes: [10 0B 01 45 30 35 30 30 30 6A 36] | Checksum: 0x6A ✓
05:05:15.025 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:15.174 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📝 Transaksjon opprettet: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, pris=15.9 kr/L
05:05:15.178 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:15.181 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ⏱️ 60s TIMEOUT STARTED: Pump 1
05:05:15.183 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på at kunde starter pumping...
05:05:15.197 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:15.213 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🔓 PUMPE FRIGJORT: Pump #1 klar til fylling (60s timeout startet)
05:05:15.216 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump released: state=PUMPING
05:05:15.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.371 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:15.373 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.375 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:15.396 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:15.398 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.405 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 30 30 30 6D 36
05:05:15.413 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:15.415 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:15.416 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:15.418 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:15.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.874 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:15.882 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.884 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:15.907 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:15.910 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.919 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 31 30 30 6E 36
05:05:15.925 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 35 37 30 30 30 6D 36 10 0B 01 45 30 30 31 30 30 6E 36]
05:05:15.927 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:15.929 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 37 30 30 30], chksum=6D)
05:05:15.933 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:15.936 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 30 30 30 6D 36
05:05:15.944 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.75 L | Bytes: [10 0B 01 45 35 37 30 30 30 6D 36] | Checksum: 0x6D ✓
05:05:15.947 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:15.951 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:15.953 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:15.956 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:15.972 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:15.998 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:16.000 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.003 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:16.007 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 30 31 30 30], chksum=6E)
05:05:16.009 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:16.011 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 31 30 30 6E 36
05:05:16.014 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.00 L | Bytes: [10 0B 01 45 30 30 31 30 30 6E 36] | Checksum: 0x6E ✓
05:05:16.023 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 0.5 L fyllt (11.93 kr)
05:05:16.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.373 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:16.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:16.405 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:16.407 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.411 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 32 31 30 30 6A 36
05:05:16.416 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 36 32 31 30 30 6A 36]
05:05:16.417 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:16.419 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:16.422 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:16.424 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:16.429 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:16.436 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:16.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:16.876 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.878 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:16.906 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:16.908 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 35 31 30 30 6A 36
05:05:16.915 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[36 32 31 30 30], chksum=6A)
05:05:16.918 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:16.921 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 32 31 30 30 6A 36
05:05:16.924 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.26 L | Bytes: [10 0B 01 45 36 32 31 30 30 6A 36] | Checksum: 0x6A ✓
05:05:16.933 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.935 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:16.945 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.947 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:16.969 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:16.972 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.975 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:16.984 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 31 35 31 30 30 6A 36 10 07 01 4B 06 5B 36]
05:05:16.986 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:16.988 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 35 31 30 30], chksum=6A)
05:05:16.991 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:16.994 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 35 31 30 30 6A 36
05:05:16.997 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.51 L | Bytes: [10 0B 01 45 31 35 31 30 30 6A 36] | Checksum: 0x6A ✓
05:05:16.999 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:17.003 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.0 L fyllt (20.03 kr)
05:05:17.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:17.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:17.400 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:17.402 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.406 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 37 31 30 30 6F 36
05:05:17.409 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:17.411 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:17.412 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:17.415 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:17.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.874 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:17.876 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.878 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:17.914 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:17.917 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.920 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 30 32 30 30 6C 36
05:05:17.925 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 36 37 31 30 30 6F 36 10 0B 01 45 31 30 32 30 30 6C 36]
05:05:17.927 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:17.928 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[36 37 31 30 30], chksum=6F)
05:05:17.931 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:17.933 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 37 31 30 30 6F 36
05:05:17.936 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.76 L | Bytes: [10 0B 01 45 36 37 31 30 30 6F 36] | Checksum: 0x6F ✓
05:05:17.945 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:17.949 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:17.951 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:17.970 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:17.972 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:17.994 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:17.996 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:17.999 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:18.003 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 30 32 30 30], chksum=6C)
05:05:18.004 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:18.006 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 30 32 30 30 6C 36
05:05:18.009 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.01 L | Bytes: [10 0B 01 45 31 30 32 30 30 6C 36] | Checksum: 0x6C ✓
05:05:18.016 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.5 L fyllt (27.98 kr)
05:05:18.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.375 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:18.377 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.386 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:18.410 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:18.412 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.415 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
05:05:18.419 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 37 32 32 30 30 68 36]
05:05:18.420 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:18.422 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:18.424 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:18.427 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:18.429 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:18.437 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:18.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:18.875 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.877 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:18.899 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:18.902 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.916 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 35 32 30 30 6B 36
05:05:18.921 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[37 32 32 30 30], chksum=68)
05:05:18.926 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:18.930 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
05:05:18.933 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.27 L | Bytes: [10 0B 01 45 37 32 32 30 30 68 36] | Checksum: 0x68 ✓
05:05:18.938 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:18.942 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:18.945 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:18.948 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:18.971 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:18.976 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:18.980 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:18.985 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 33 35 32 30 30 6B 36 10 07 01 4B 06 5B 36]
05:05:18.986 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:18.988 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[33 35 32 30 30], chksum=6B)
05:05:18.991 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:18.994 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 35 32 30 30 6B 36
05:05:19.002 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.53 L | Bytes: [10 0B 01 45 33 35 32 30 30 6B 36] | Checksum: 0x6B ✓
05:05:19.004 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:19.006 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 2.0 L fyllt (36.09 kr)
05:05:19.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:19.373 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:19.397 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:19.400 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.406 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 38 37 32 30 30 62 36
05:05:19.410 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:19.411 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:19.413 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:19.418 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:19.871 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.875 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:19.879 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.883 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:19.906 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:19.909 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 30 33 30 30 6F 36
05:05:19.916 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 38 37 32 30 30 62 36 10 0B 01 45 33 30 33 30 30 6F 36]
05:05:19.918 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:19.921 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[38 37 32 30 30], chksum=62)
05:05:19.924 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:19.926 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 38 37 32 30 30 62 36
05:05:19.929 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.78 L | Bytes: [10 0B 01 45 38 37 32 30 30 62 36] | Checksum: 0x62 ✓
05:05:19.931 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:19.934 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:19.936 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:19.938 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:19.940 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:19.962 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:19.965 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:19.967 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:19.977 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[33 30 33 30 30], chksum=6F)
05:05:19.979 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:19.980 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 30 33 30 30 6F 36
05:05:19.983 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.03 L | Bytes: [10 0B 01 45 33 30 33 30 30 6F 36] | Checksum: 0x6F ✓
05:05:19.986 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 2.5 L fyllt (44.2 kr)
05:05:20.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:20.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:20.398 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:20.399 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 32 33 30 30 6D 36
05:05:20.409 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 33 32 33 30 30 6D 36]
05:05:20.411 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:20.414 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:20.416 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:20.419 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:20.422 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:20.424 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:20.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:20.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.875 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:20.897 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:20.899 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.902 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 33 30 30 61 36
05:05:20.905 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[33 32 33 30 30], chksum=6D)
05:05:20.907 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:20.909 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 32 33 30 30 6D 36
05:05:20.913 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.23 L | Bytes: [10 0B 01 45 33 32 33 30 30 6D 36] | Checksum: 0x6D ✓
05:05:20.918 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:20.921 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:20.924 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:20.927 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:20.950 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:20.953 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:20.962 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:20.966 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 39 34 33 30 30 61 36 10 07 01 4B 06 5B 36]
05:05:20.977 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:20.979 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 34 33 30 30], chksum=61)
05:05:20.982 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:20.985 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 33 30 30 61 36
05:05:20.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.49 L | Bytes: [10 0B 01 45 39 34 33 30 30 61 36] | Checksum: 0x61 ✓
05:05:20.990 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:20.994 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 3.0 L fyllt (51.36 kr)
05:05:21.372 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.374 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:21.375 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:21.398 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:21.400 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 37 33 30 30 6F 36
05:05:21.407 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:21.408 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:21.410 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:21.412 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:21.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.871 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:21.872 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:21.894 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:21.896 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.898 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 39 33 30 30 6C 36
05:05:21.901 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 34 37 33 30 30 6F 36 10 0B 01 45 39 39 33 30 30 6C 36]
05:05:21.902 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:21.903 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[34 37 33 30 30], chksum=6F)
05:05:21.904 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:21.906 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 37 33 30 30 6F 36
05:05:21.909 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.74 L | Bytes: [10 0B 01 45 34 37 33 30 30 6F 36] | Checksum: 0x6F ✓
05:05:21.909 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:21.911 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:21.912 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:21.913 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:21.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:21.944 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:21.947 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:21.949 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:21.952 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 39 33 30 30], chksum=6C)
05:05:21.953 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:21.956 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 39 33 30 30 6C 36
05:05:21.959 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.99 L | Bytes: [10 0B 01 45 39 39 33 30 30 6C 36] | Checksum: 0x6C ✓
05:05:21.961 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 3.5 L fyllt (59.47 kr)
05:05:22.371 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.373 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:22.375 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:22.397 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:22.399 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.402 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 32 34 30 30 6D 36
05:05:22.406 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 34 32 34 30 30 6D 36]
05:05:22.407 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:22.408 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:22.411 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:22.418 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:22.421 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:22.424 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:22.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:22.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.875 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:22.897 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:22.898 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.902 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 34 30 30 66 36
05:05:22.905 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[34 32 34 30 30], chksum=6D)
05:05:22.906 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:22.907 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 32 34 30 30 6D 36
05:05:22.909 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=4.24 L | Bytes: [10 0B 01 45 34 32 34 30 30 6D 36] | Checksum: 0x6D ✓
05:05:22.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:22.914 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:22.915 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:22.917 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:22.938 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:22.941 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:22.943 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:22.947 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 39 34 34 30 30 66 36 10 07 01 4B 06 5B 36]
05:05:22.949 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:22.950 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 34 34 30 30], chksum=66)
05:05:22.954 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:22.962 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 34 30 30 66 36
05:05:22.973 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=4.49 L | Bytes: [10 0B 01 45 39 34 34 30 30 66 36] | Checksum: 0x66 ✓
05:05:22.975 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:22.977 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 4.0 L fyllt (67.42 kr)
05:05:23.371 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.373 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:23.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:23.398 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:23.399 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 34 30 30 69 36
05:05:23.406 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:23.407 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:23.410 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:23.413 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:23.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:23.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.877 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:23.899 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:23.901 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.904 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 35 30 30 6A 36
05:05:23.907 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 35 37 34 30 30 69 36 10 0B 01 45 30 30 35 30 30 6A 36]
05:05:23.908 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:23.909 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 37 34 30 30], chksum=69)
05:05:23.910 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:23.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 34 30 30 69 36
05:05:23.914 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=4.75 L | Bytes: [10 0B 01 45 35 37 34 30 30 69 36] | Checksum: 0x69 ✓
05:05:23.914 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:23.918 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:23.920 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:23.921 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:23.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:23.944 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:23.945 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:23.948 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:23.951 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 30 35 30 30], chksum=6A)
05:05:23.953 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:23.954 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 35 30 30 6A 36
05:05:23.955 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=5.00 L | Bytes: [10 0B 01 45 30 30 35 30 30 6A 36] | Checksum: 0x6A ✓
05:05:23.967 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 4.5 L fyllt (75.53 kr)
05:05:24.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:24.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:24.399 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:24.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.408 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 32 35 30 30 6D 36
05:05:24.419 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 35 32 35 30 30 6D 36]
05:05:24.421 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:24.423 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:24.426 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:24.428 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:24.430 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:24.432 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:24.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:24.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.876 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:24.898 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:24.900 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.904 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 35 30 30 6F 36
05:05:24.907 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 32 35 30 30], chksum=6D)
05:05:24.910 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:24.914 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 32 35 30 30 6D 36
05:05:24.918 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=5.25 L | Bytes: [10 0B 01 45 35 32 35 30 30 6D 36] | Checksum: 0x6D ✓
05:05:24.922 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:24.925 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:24.926 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:24.931 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:24.953 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:24.956 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:24.959 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:24.963 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 30 35 35 30 30 6F 36 10 07 01 4B 06 5B 36]
05:05:24.964 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:24.965 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 35 35 30 30], chksum=6F)
05:05:24.968 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:24.971 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 35 30 30 6F 36
05:05:24.994 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=5.50 L | Bytes: [10 0B 01 45 30 35 35 30 30 6F 36] | Checksum: 0x6F ✓
05:05:24.997 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:25.000 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 5.0 L fyllt (83.48 kr)
05:05:25.018 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🛑 FRI PUMPE: Block request for address 1
05:05:25.021 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:25.023 INFO  [OPERATOR] n.c.lpg.protocol - 🛑 STOPP PUMPE - Sender BLOCK til dispenser #1
05:05:25.033 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
05:05:25.034 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 69 7E 36] -> BLOCK
05:05:25.039 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
05:05:25.046 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | BLOCK (Block/stop the dispenser) | Bytes: [10 06 01 69 7E 36] | Checksum: 0x7E ✓
05:05:25.069 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=BLOCK(105), data=[], chksum=7E)
05:05:25.071 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
05:05:25.073 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - STOP/BLOCK: Stopping delivery
05:05:25.097 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Transaction completed: 5.6035 L, 89.1 kr
05:05:25.099 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Totals FROZEN - requires reset before next transaction
05:05:25.101 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: DELIVERING → PAYMENT_PENDING
05:05:25.104 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:25.106 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:25.108 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 35 30 30 6C 36
05:05:25.111 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:25.113 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:25.116 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:25.119 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:25.122 INFO  [OPERATOR] n.c.lpg.protocol - ✅ BLOCK OK - Respons: STATE
05:05:25.123 INFO  [OPERATOR] n.c.lpg.protocol - 📊 Henter finalt volum...
05:05:25.130 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:25.132 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:25.134 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:25.137 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:25.159 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:25.161 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:25.164 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 35 30 30 6C 36
05:05:25.168 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 0B 01 45 30 36 35 30 30 6C 36 10 0B 01 45 30 36 35 30 30 6C 36]
05:05:25.171 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 36 bytes
05:05:25.173 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:05:25.175 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:05:25.178 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:25.180 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:05:25.182 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:25.185 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:25.199 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🛑 Transaksjon stoppet: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, volum=5.25 L, beløp=83.48 kr, status=PENDING
05:05:25.219 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📋 Transaksjon oppdatert til PENDING: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, 5.25 L = 83.48 kr
05:05:25.222 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🛑 Pumping stoppet: 5.25 L = 83.48 kr - venter betaling
05:05:25.230 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump blocked: state=PAYMENT_PENDING, volume=5.25L
05:05:27.717 INFO  [OPERATOR] n.c.l.a.c.PumpController - 💳 Settle payment request: dispenserId=1, method=CARD
05:05:27.733 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 💳 Transaksjon betalt: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, volum=5.2 L, beløp=83.48 kr, metode=CARD
05:05:27.775 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 Betaling fullført: 5.25 L = 83.48 kr via CARD
05:05:27.804 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 Pump 1 settled: 5.25L = 83.48 kr via CARD
05:05:27.806 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Payment settled: 83.48 NOK, 5.25 L
05:05:36.440 INFO  [DEBUG] n.c.l.a.c.SerialDebugController - Listing available serial ports
05:05:36.544 INFO  [DEBUG] n.c.l.s.s.SerialPortScanner - Found 0 serial ports (0 hardware, 0 virtual, 0 macOS-detected)
05:05:42.643 INFO  [-] n.c.l.a.w.WebSocketEventPublisher - 🔌 WebSocket connected: ad45eab3-b182-a4c0-ea94-95fcd3c4bb87
05:05:42.866 INFO  [-] n.c.l.a.w.WebSocketEventPublisher - 📝 Session ad45eab3-b182-a4c0-ea94-95fcd3c4bb87 subscribed to: [API, SERVICE, EMULATOR, PROTOCOL]
05:05:44.183 INFO  [OPERATOR] n.c.l.a.c.PumpController - 💳 SIMULER KORTDRAGNING: Dispenser 1
05:05:44.347 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:44.352 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 💳 KORTDRAGNING SIMULERT
05:05:44.357 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Dispenser: 1
05:05:44.361 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Auth ID: c5554022-d6ce-48d7-b678-689e66dd6c74
05:05:44.366 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Maks beløp: 2000.0 kr
05:05:44.372 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Pris: 15.9 kr/L
05:05:44.377 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Metode: CARD
05:05:44.381 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Status: PENDING → Venter på UNBLOCK
05:05:44.385 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:44.401 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Autorisasjon opprettet: c5554022-d6ce-48d7-b678-689e66dd6c74
05:05:44.405 INFO  [OPERATOR] n.c.l.a.c.PumpController - ⏱️ 60s nedtelling startet - venter på FRI DISPENSER
05:05:44.411 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:44.420 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 KORTDRAGNING: Pump 1 state -> AUTHORIZED_WAITING
05:05:44.425 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Auth ID: c5554022-d6ce-48d7-b678-689e66dd6c74
05:05:44.430 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på FRI DISPENSER (60s timeout)
05:05:44.434 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:46.531 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE: Unblock request for address 1
05:05:46.535 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:46.539 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:05:46.652 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:46.654 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:05:46.655 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:46.657 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:05:46.679 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:05:46.681 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:46.683 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:05:46.688 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:05:46.692 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:05:46.696 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:05:46.701 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:46.703 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:46.704 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:05:46.709 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:46.710 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:46.712 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:46.714 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:46.735 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:46.737 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:46.739 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:46.742 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:46.754 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:05:46.755 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:05:46.756 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:46.762 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:05:46.763 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:46.764 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:05:46.778 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:46.780 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:46.782 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:46.784 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:46.785 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:46.787 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:47.090 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.091 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:47.093 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.094 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:47.115 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:47.117 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.119 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.122 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:47.123 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:47.125 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.128 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:47.129 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:47.431 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.432 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:47.433 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.435 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:47.456 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:47.457 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.459 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.463 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:47.476 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:47.478 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:47.481 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.483 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:47.485 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:47.486 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:47.790 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.791 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:47.793 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.795 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:47.816 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:47.818 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.821 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.823 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:47.825 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:47.827 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.830 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:47.831 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:48.134 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.135 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:48.137 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.139 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:48.160 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:48.161 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.163 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.166 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:48.177 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:48.179 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:48.181 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.183 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:48.184 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:48.185 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:48.488 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.489 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:48.490 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.492 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:48.513 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:48.514 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.516 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.519 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:48.520 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:48.521 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.523 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:48.525 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:48.828 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.829 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:48.830 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.831 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:48.852 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:48.854 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.856 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.860 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:48.872 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:48.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:48.875 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.876 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:48.877 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:48.878 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:49.186 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.187 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:49.188 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.189 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:49.211 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:49.214 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.217 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.220 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:49.222 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:49.226 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.230 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:49.233 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:49.536 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.538 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:49.539 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.540 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:49.561 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:49.562 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.565 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.568 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:49.579 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:49.581 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:49.582 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.584 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:49.585 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:49.585 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:49.887 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.888 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:49.889 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.891 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:49.912 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:49.914 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.917 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.920 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:49.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:49.925 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.928 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:49.929 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:50.232 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.233 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:50.234 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.236 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:50.257 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:50.258 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.260 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.263 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:50.275 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:50.277 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:50.280 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.283 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:50.286 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:50.288 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:50.591 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.593 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:50.597 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.599 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:50.622 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:50.625 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.628 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.630 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:50.631 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:50.637 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.639 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:50.642 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:50.944 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.945 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:50.946 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.949 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:50.971 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:50.973 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.975 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.978 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:50.990 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:50.991 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:50.993 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.995 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:50.998 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:50.999 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:51.303 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.305 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:51.308 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.311 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:51.334 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:51.335 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.337 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.339 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:51.341 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:51.345 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.349 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:51.353 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:51.658 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.660 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:51.661 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.665 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:51.687 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:51.689 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.691 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.694 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:51.707 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:51.710 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:51.713 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.716 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:51.718 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:51.721 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:52.024 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.025 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:52.028 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.031 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:52.051 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:52.053 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.055 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.057 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:52.058 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:52.060 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.061 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:52.062 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:52.363 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.364 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:52.365 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.366 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:52.387 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:52.388 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.390 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.393 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:52.405 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:52.407 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:52.409 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.413 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:52.416 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:52.418 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:52.723 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:05:52.727 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Unblock failed: UNBLOCK: open_for_delivery bit not observed within 6s
05:05:54.886 WARN  [OPERATOR] n.c.l.a.c.AdminController - 🧹 ADMIN CLEANUP: Cancelling all stuck authorizations...
05:05:54.928 WARN  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 🧹 Cancelling 1 stuck authorization(s)...
05:05:54.934 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    ❌ Cancelled: c5554022-d6ce-48d7-b678-689e66dd6c74 (was CANCELLED)
05:05:54.954 WARN  [OPERATOR] n.c.l.s.p.PumpStateService - 🧹 Resetting ALL pumps to IDLE...
05:05:54.970 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    🔄 Pump 1 reset to IDLE
05:05:54.982 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ✅ Reset 1 pump(s) to IDLE
05:05:54.994 INFO  [OPERATOR] n.c.l.a.c.AdminController - ✅ Cleanup completed: 1 authorization(s) cancelled, all pumps reset to IDLE
05:06:03.941 INFO  [OPERATOR] n.c.l.a.c.PumpController - 💳 SIMULER KORTDRAGNING: Dispenser 1
05:06:04.013 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:04.017 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 💳 KORTDRAGNING SIMULERT
05:06:04.021 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Dispenser: 1
05:06:04.025 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Auth ID: 8c8b4479-801e-479b-b83e-5020eb0b7d00
05:06:04.029 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Maks beløp: 2000.0 kr
05:06:04.034 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Pris: 15.9 kr/L
05:06:04.038 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Metode: CARD
05:06:04.042 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Status: PENDING → Venter på UNBLOCK
05:06:04.046 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:04.059 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Autorisasjon opprettet: 8c8b4479-801e-479b-b83e-5020eb0b7d00
05:06:04.063 INFO  [OPERATOR] n.c.l.a.c.PumpController - ⏱️ 60s nedtelling startet - venter på FRI DISPENSER
05:06:04.067 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:06:04.071 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 KORTDRAGNING: Pump 1 state -> AUTHORIZED_WAITING
05:06:04.075 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Auth ID: 8c8b4479-801e-479b-b83e-5020eb0b7d00
05:06:04.079 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på FRI DISPENSER (60s timeout)
05:06:04.088 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:06:09.593 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE: Unblock request for address 1
05:06:09.596 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:09.600 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:06:09.707 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:09.709 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:06:09.711 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:09.713 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:06:09.734 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:06:09.736 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:09.740 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:06:09.745 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:06:09.750 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:06:09.755 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:06:09.764 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:09.765 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:09.767 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:06:09.781 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:09.782 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:09.784 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:09.786 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:09.811 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:09.813 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:09.815 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:09.818 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:09.830 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:06:09.831 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:06:09.833 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:09.834 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:06:09.836 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:09.837 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:06:09.849 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:09.850 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:09.851 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:09.852 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:09.853 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:09.853 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:10.156 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.157 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:10.159 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.161 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:10.182 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:10.184 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.186 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.189 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:10.190 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:10.192 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.195 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:10.196 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:10.499 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.500 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:10.501 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.502 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:10.523 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:10.525 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.527 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.530 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:10.542 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:10.543 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:10.545 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.546 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:10.547 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:10.547 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:10.849 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.850 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:10.852 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.853 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:10.874 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:10.877 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.880 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.882 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:10.884 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:10.885 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.893 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:10.895 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:11.198 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.199 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:11.200 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.202 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:11.224 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:11.225 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.228 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.231 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:11.243 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:11.245 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:11.247 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.248 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:11.249 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:11.250 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:11.552 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.553 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:11.556 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.557 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:11.577 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:11.579 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.581 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.582 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:11.583 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:11.585 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.586 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:11.587 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:11.889 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.890 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:11.891 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.894 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:11.915 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:11.917 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.919 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:11.934 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:11.935 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:11.937 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.939 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:11.940 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:11.941 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:12.243 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.245 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:12.247 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.249 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:12.276 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:12.287 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.291 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.295 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:12.296 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:12.299 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.301 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:12.302 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:12.606 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.607 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:12.609 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.610 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:12.631 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:12.633 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.635 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.638 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:12.650 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:12.650 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:12.652 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.653 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:12.653 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:12.654 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:12.956 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.957 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:12.959 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.961 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:12.982 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:12.983 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.985 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.987 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:12.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:12.991 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.992 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:12.993 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:13.297 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.298 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:13.299 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.301 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:13.322 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:13.323 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.325 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.330 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:13.342 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:13.344 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:13.346 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.349 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:13.352 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:13.354 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:13.657 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.660 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:13.662 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.663 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:13.685 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:13.687 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.689 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.691 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:13.692 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:13.695 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.696 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:13.698 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:14.000 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.002 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:14.003 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.005 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:14.026 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:14.029 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.032 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.036 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:14.049 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:14.050 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:14.051 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.053 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:14.053 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:14.054 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:14.355 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.356 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:14.357 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.358 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:14.381 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:14.383 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.386 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.388 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:14.388 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:14.390 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.391 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:14.394 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:14.697 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.699 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:14.700 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.703 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:14.725 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:14.727 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.729 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.732 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:14.745 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:14.747 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:14.749 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.753 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:14.754 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:14.754 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:15.056 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.057 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:15.058 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.059 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:15.080 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:15.082 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.084 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.088 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:15.091 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:15.095 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.099 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:15.102 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:15.405 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.406 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:15.407 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.409 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:15.431 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:15.433 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.434 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.437 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:15.449 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:15.451 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:15.453 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.454 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:15.454 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:15.455 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:15.757 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.758 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:15.759 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.761 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:15.782 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:15.783 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.785 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.787 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:15.788 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:15.790 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.792 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:15.792 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:16.095 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:06:16.099 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Unblock failed: UNBLOCK: open_for_delivery bit not observed within 6s
05:06:22.735 WARN  [OPERATOR] n.c.l.a.c.AdminController - 🧹 ADMIN CLEANUP: Cancelling all stuck authorizations...
05:06:22.766 WARN  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 🧹 Cancelling 1 stuck authorization(s)...
05:06:22.774 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    ❌ Cancelled: 8c8b4479-801e-479b-b83e-5020eb0b7d00 (was CANCELLED)
05:06:22.796 WARN  [OPERATOR] n.c.l.s.p.PumpStateService - 🧹 Resetting ALL pumps to IDLE...
05:06:22.802 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    🔄 Pump 1 reset to IDLE
05:06:22.808 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ✅ Reset 1 pump(s) to IDLE
05:06:22.812 INFO  [OPERATOR] n.c.l.a.c.AdminController - ✅ Cleanup completed: 1 authorization(s) cancelled, all pumps reset to IDLE
05:06:31.844 INFO  [-] n.c.l.a.w.WebSocketEventPublisher - 🔌 WebSocket disconnected: ad45eab3-b182-a4c0-ea94-95fcd3c4bb87 ()
05:06:50.839 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
05:06:50.865 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:50.866 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:06:50.869 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drain: discarding 7 bytes
05:06:50.995 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drained for 100ms, discarded 7 bytes
05:06:50.997 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:50.998 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:06:50.999 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:51.001 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:06:51.023 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:06:51.025 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:51.026 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:06:51.027 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:06:51.027 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:06:51.028 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:06:51.030 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:51.031 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.032 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:06:51.033 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.034 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:51.035 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.036 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:51.056 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:51.058 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.060 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.063 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:51.075 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:06:51.076 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:06:51.078 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:51.079 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:06:51.080 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:51.081 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:06:51.093 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:51.094 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:51.096 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.097 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:51.097 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:51.098 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:51.400 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.401 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:51.402 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.404 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:51.427 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:51.428 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.430 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.432 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:51.433 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:51.434 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.435 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:51.436 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:51.737 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.738 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:51.739 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.741 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:51.761 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:51.763 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.765 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.767 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:51.779 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:51.780 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:51.782 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.784 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:51.785 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:51.787 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:52.089 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.090 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:52.091 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.094 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:52.115 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:52.119 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.124 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.128 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:52.129 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:52.132 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.137 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:52.140 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:52.444 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.445 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:52.446 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.448 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:52.469 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:52.470 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.473 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.475 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:52.487 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:52.488 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:52.490 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.491 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:52.493 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:52.495 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:52.798 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.799 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:52.800 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.803 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:52.824 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:52.825 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.828 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.830 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:52.831 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:52.832 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.835 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:52.837 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:53.139 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.140 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:53.141 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.143 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:53.164 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:53.165 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.167 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.170 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:53.182 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:53.183 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:53.184 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.185 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:53.186 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:53.186 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:53.489 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.491 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:53.492 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.494 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:53.516 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:53.519 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.522 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.525 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:53.526 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:53.529 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.532 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:53.535 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:53.838 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.840 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:53.841 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.843 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:53.864 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:53.866 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.868 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:53.885 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:53.885 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:53.887 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.888 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:53.890 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:53.891 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:54.194 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.195 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:54.197 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.200 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:54.223 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:54.226 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.229 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.233 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:54.235 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:54.237 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.241 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:54.243 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:54.553 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.555 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:54.557 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.561 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:54.583 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:54.585 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.587 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.591 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:54.602 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:54.603 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:54.606 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.608 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:54.610 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:54.612 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:54.915 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.916 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:54.917 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.918 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:54.938 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:54.940 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.942 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.945 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:54.946 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:54.948 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.950 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:54.952 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:55.255 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.256 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:55.258 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.260 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:55.282 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:55.283 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.285 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.287 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:55.299 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:55.299 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:55.301 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.302 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:55.303 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:55.305 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:55.607 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.608 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:55.609 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.610 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:55.632 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:55.633 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.634 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.637 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:55.638 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:55.639 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.639 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:55.640 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:55.942 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.943 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:55.944 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.945 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:55.966 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:55.968 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.970 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.972 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:55.984 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:55.984 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:55.986 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.987 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:55.987 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:55.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:56.292 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.293 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:56.295 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.296 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:56.317 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:56.319 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.321 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.325 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:56.327 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:56.329 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.331 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:56.333 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:56.636 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.637 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:56.638 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.638 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:56.659 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:56.660 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.662 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.665 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:56.677 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:56.679 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:56.681 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.683 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:56.689 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:56.691 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:56.994 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.995 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:56.995 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.996 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:57.017 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:57.018 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:57.019 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:57.022 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:57.023 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:57.024 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:57.025 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:57.026 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:57.328 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:06:57.331 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Release failed: UNBLOCK: open_for_delivery bit not observed within 6s
05:07:12.589 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:12.593 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:13.057 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:13.079 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:16.588 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:16.591 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:16.622 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:16.626 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:19.662 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:19.664 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:19.698 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:19.703 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:32.847 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:32.850 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:32.876 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:32.879 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:34.643 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:34.649 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:34.684 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:34.687 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:37.641 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
05:07:37.661 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:07:37.662 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:07:37.663 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drain: discarding 7 bytes
05:07:37.766 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drained for 100ms, discarded 7 bytes
05:07:37.767 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:07:37.769 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:07:37.770 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:07:37.771 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:07:37.793 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:07:37.794 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:07:37.795 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:07:37.797 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:07:37.799 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:07:37.801 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:07:37.803 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:07:37.806 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:37.808 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:07:37.813 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:37.814 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:37.816 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:37.818 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:37.840 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:37.843 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:37.846 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:37.849 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:37.861 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:07:37.862 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:07:37.864 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:07:37.866 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:07:37.867 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:37.869 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:07:37.881 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:37.883 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:37.885 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:37.887 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:37.892 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:37.894 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:38.198 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.200 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:38.202 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.205 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:38.228 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:38.230 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.233 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.236 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:38.240 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:38.241 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.243 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:38.244 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:38.549 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.549 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:38.550 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.551 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:38.572 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:38.573 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.574 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.577 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:38.596 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:38.598 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:38.600 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.603 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:38.605 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:38.608 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:38.910 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.911 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:38.911 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.912 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:38.933 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:38.934 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.935 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.938 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:38.939 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:38.940 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.941 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:38.941 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:39.243 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.244 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:39.245 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.246 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:39.267 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:39.268 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.270 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.272 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:39.284 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:39.285 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:39.286 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.287 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:39.289 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:39.290 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:39.592 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.595 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:39.597 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.599 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:39.622 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:39.623 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.625 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.627 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:39.628 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:39.630 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.631 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:39.633 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:39.936 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.938 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:39.939 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.940 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:39.961 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:39.961 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.962 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.964 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:39.976 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:39.977 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:39.978 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.979 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:39.980 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:39.981 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:40.282 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.283 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:40.283 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.284 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:40.305 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:40.306 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.308 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.310 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:40.311 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:40.312 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.313 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:40.313 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:40.616 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.618 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:40.620 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.621 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:40.643 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:40.644 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.647 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.649 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:40.661 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:40.662 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:40.663 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.664 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:40.665 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:40.666 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:40.968 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.969 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:40.969 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.971 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:40.992 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:40.994 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.995 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.997 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:40.998 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:41.000 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.002 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:41.004 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:41.307 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.307 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:41.308 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.309 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:41.329 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:41.331 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.332 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.334 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:41.346 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:41.347 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:41.348 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.351 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:41.353 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:41.355 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:41.656 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.658 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:41.659 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.660 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:41.682 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:41.683 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.686 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.692 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:41.693 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:41.694 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.695 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:41.696 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:41.998 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.998 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:41.999 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.000 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:42.021 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:42.022 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.023 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.025 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:42.037 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:42.038 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:42.039 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.040 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:42.042 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:42.043 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:42.346 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.346 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:42.347 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.348 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:42.370 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:42.372 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.373 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.375 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:42.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:42.377 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.379 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:42.379 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
^C05:07:42.682 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.683 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:42.684 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.686 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:42.708 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:42.709 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.711 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.714 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:42.725 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:42.726 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:42.726 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.728 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:42.729 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:42.729 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:43.031 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.032 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:43.034 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.035 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:43.056 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:43.057 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.059 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.061 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:43.062 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:43.062 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.065 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:43.067 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:43.389 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.390 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:43.392 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.393 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:43.415 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:43.417 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.418 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.420 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:43.432 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:43.432 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:43.434 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.434 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:43.435 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:43.435 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:43.738 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.738 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:43.739 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.741 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:43.762 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:43.764 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.765 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.767 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:43.768 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:43.770 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.771 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:43.772 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:44.078 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:07:44.079 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Release failed: UNBLOCK: open_for_delivery bit not observed within 6s
# Når jeg kjører i lab modus så får jeg følgende feil. Det er et flagg med autorisasjon som skal settes i databasen. I med en prompt som cursor eller june fra JetBrains kan bruke for å finne ut hvorfor. Pumpeautorisasjon med Pump authorizations ikke oppdateres riktig når man trykker på betaling i selve stasjonens eiergrensesnittet eller i kontrollpanelet og knappen for å resette. Alt. Den fungerer ikke. Den resetter ikke alle transaksjoner i databasen som completed for at dette skal fungere.

# logg fra ark

ssh thomas@192.168.0.9
Linux debian 6.1.0-42-amd64 #1 SMP PREEMPT_DYNAMIC Debian 6.1.159-1 (2025-12-30) x86_64

The programs included with the Debian GNU/Linux system are free software;
the exact distribution terms for each program are described in the
individual files in /usr/share/doc/*/copyright.

Debian GNU/Linux comes with ABSOLUTELY NO WARRANTY, to the extent
permitted by applicable law.
Last login: Fri Feb 13 03:56:21 2026 from 192.168.0.8
thomas@debian:~$ ./s
scripts/          start-lpg-ehl.sh  
thomas@debian:~$ ./start-lpg-ehl.sh
^Cthomas@debian:~$ nano start-lpg-ehl.sh
thomas@debian:~$ ./start-lpg-ehl.sh

.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v3.2.1)

04:55:51.359 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Starting LpgEhlApiApplicationKt using Java 21.0.10 with PID 610 (/home/thomas/release/lpg-ehl-webapp.jar started by thomas in /home/thomas)
04:55:51.411 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - The following 1 profile is active: "lab"
Database is up to date, no changesets to execute
04:56:59.839 WARN  [-] o.h.orm.deprecation - HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
04:57:17.588 INFO  [-] n.c.l.a.c.CommunicationConfig -
04:57:17.591 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
04:57:17.597 INFO  [-] n.c.l.a.c.CommunicationConfig -   EHL KOMMUNIKASJON: 🧪 LAB MODE
04:57:17.601 INFO  [-] n.c.l.a.c.CommunicationConfig -   Active profiles: [lab]
04:57:17.604 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
04:57:17.607 INFO  [-] n.c.l.a.c.CommunicationConfig -
04:57:34.324 INFO  [-] n.c.l.a.c.CommunicationConfig - 🧪 Creating EhlDispenserEmulator (address=1, price=1590)
04:57:34.375 INFO  [-] n.c.l.e.i.EhlDispenserEmulatorImpl - EHL Dispenser Emulator initialized: address=1, price=15.9 kr/L
04:57:34.457 INFO  [-] n.c.l.a.c.TransportConfiguration -
04:57:34.460 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
04:57:34.462 INFO  [-] n.c.l.a.c.TransportConfiguration -   🔬 LAB MODE
04:57:34.465 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
04:57:34.469 INFO  [-] n.c.l.a.c.TransportConfiguration -   Transport:  InMemorySerialPort + Emulator
04:57:34.479 INFO  [-] n.c.l.a.c.TransportConfiguration -   Latency:    20ms (simulated)
04:57:34.481 INFO  [-] n.c.l.a.c.TransportConfiguration -   Hardware:   NOT REQUIRED
04:57:34.491 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
04:57:34.494 INFO  [-] n.c.l.a.c.TransportConfiguration -
04:57:34.611 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlCommunicator with InMemorySerialPort
04:57:34.616 INFO  [-] n.c.l.a.c.TransportConfiguration - Raw protocol logging: ENABLED
04:57:34.702 INFO  [-] n.c.l.a.c.TransportConfiguration - 🔄 Retry config: maxRetries=3, initialDelay=100ms, maxDelay=2000ms, backoff=2.0
04:57:34.781 INFO  [-] n.c.l.e.i.InMemorySerialPort - 🔌 InMemorySerialPort: Kobler til emulator (LAB MODE)
04:57:34.784 INFO  [-] n.c.l.a.c.TransportConfiguration - ✅ Transport connected successfully
04:57:39.250 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:57:39.252 INFO  [-] n.c.l.s.p.PumpStateService - 🌟 OPPSTART: Initialiserer pris...
04:57:40.779 INFO  [-] n.c.l.s.p.PumpStateService - 🏷️ STARTUP: Gjenopprettet pris 15.9 kr/L fra database
04:57:40.782 INFO  [-] n.c.l.s.p.PumpStateService -    Satt av: system
04:57:40.785 INFO  [-] n.c.l.s.p.PumpStateService -    Gyldig fra: 2026-02-12T18:11:13.518945
04:57:40.787 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:57:46.094 INFO  [-] n.c.l.a.p.MockPlsService - 🏷️ [MOCK PLS] Initialized with default prices
04:57:51.090 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlOperationsService
04:57:53.402 WARN  [-] o.s.b.a.o.j.JpaBaseConfiguration$JpaWebConfiguration - spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
04:57:53.930 WARN  [-] o.s.b.a.s.s.UserDetailsServiceAutoConfiguration -

Using generated security password: b4892011-e0d2-4f51-81c7-ff1a1dadef2a

This generated password is for development use only. Your security configuration must be updated before running your application in production.

04:58:10.479 INFO  [-] org.xnio - XNIO version 3.8.8.Final
04:58:10.574 INFO  [-] org.xnio.nio - XNIO NIO Implementation Version 3.8.8.Final
04:58:11.916 INFO  [-] org.jboss.threads - JBoss Threads version 3.5.0.Final
04:58:12.595 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Started LpgEhlApiApplicationKt in 151.491 seconds (process running for 160.455)
04:58:14.633 INFO  [-] n.c.l.a.s.WebAppPollingService - 🚀 WebApp polling service started - UI live updates enabled
04:58:24.478 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
04:58:24.755 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:24.760 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
04:58:25.200 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
04:58:25.217 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
04:58:25.242 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
04:58:25.283 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
04:58:25.312 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
04:58:25.361 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
04:58:25.378 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - UNBLOCK: Starting new transaction
04:58:25.395 INFO  [OPERATOR] n.c.l.e.i.DispenserSimulatorImpl - Starting simulation: 0.5 L/s, 15.9 kr/L
04:58:25.456 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: AUTHORIZED → DELIVERING
04:58:25.475 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:25.479 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:25.483 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
04:58:25.490 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:25.494 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:25.499 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:25.506 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:25.532 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:25.535 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:25.545 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:25.601 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 06 5B 36 10 07 01 4B 06 5B 36]
04:58:25.615 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
04:58:25.619 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
04:58:25.642 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:25.650 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
04:58:25.653 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:25.682 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
04:58:25.706 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:25.710 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:25.722 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:25.726 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:25.730 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:25.739 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
04:58:25.759 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK verified: open_for_delivery=1 after 977ms, first STATE raw=0x06
04:58:25.768 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK BEKREFTET - Dispenser klar for pumping
04:58:25.773 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:26.090 INFO  [OPERATOR] n.c.l.s.t.TransactionService - ⛽ Transaksjon opprettet: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, dispenser=1, pris=15.9 kr/L, status=STARTED
04:58:26.154 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.156 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:26.159 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.162 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:26.184 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:26.186 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.194 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.298 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:26.300 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:26.304 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.307 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:26.322 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ HARDWARE PUMPING DETECTED: Raw state 0x06 for pump 1
04:58:26.323 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.325 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ STATE TRANSITION: READY_TO_PUMP → PUMPING (pump 1)
04:58:26.326 INFO  [-] n.c.l.s.p.PumpStateService -    60s timeout cancelled - customer started pumping
04:58:26.338 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.357 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.369 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:26.371 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:26.401 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:26.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.437 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 30 30 30 62 36
04:58:26.456 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 39 34 30 30 30 62 36]
04:58:26.497 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:26.499 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:26.502 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:26.514 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.517 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:26.519 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:26.570 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.573 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:26.575 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.577 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:26.589 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📝 Transaksjon opprettet: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, pris=15.9 kr/L
04:58:26.590 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.592 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ⏱️ 60s TIMEOUT STARTED: Pump 1
04:58:26.593 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på at kunde starter pumping...
04:58:26.594 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:58:26.603 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:26.604 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:26.608 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 30 30 30 69 36
04:58:26.611 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 34 30 30 30], chksum=62)
04:58:26.613 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:26.621 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🔓 PUMPE FRIGJORT: Pump #1 klar til fylling (60s timeout startet)
04:58:26.623 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump released: state=PUMPING
04:58:26.665 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 30 30 30 62 36
04:58:26.668 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.49 L | Bytes: [10 0B 01 45 39 34 30 30 30 62 36] | Checksum: 0x62 ✓
04:58:26.678 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.687 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:26.690 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.692 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:26.715 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:26.717 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:26.719 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:26.723 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 30 36 30 30 30 69 36 10 07 01 4B 06 5B 36]
04:58:26.725 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:26.728 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 36 30 30 30], chksum=69)
04:58:26.731 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:26.742 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 30 30 30 69 36
04:58:26.746 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.60 L | Bytes: [10 0B 01 45 30 36 30 30 30 69 36] | Checksum: 0x69 ✓
04:58:26.748 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:27.070 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.073 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:27.076 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.078 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:27.104 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:27.106 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.110 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 38 30 30 30 62 36
04:58:27.122 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:27.124 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:27.127 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:27.131 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:27.570 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.573 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:27.576 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.579 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:27.608 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:27.611 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:27.615 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 31 31 30 30 6F 36
04:58:27.623 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 35 38 30 30 30 62 36 10 0B 01 45 30 31 31 30 30 6F 36]
04:58:27.626 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
04:58:27.628 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 38 30 30 30], chksum=62)
04:58:27.630 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:27.633 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 38 30 30 30 62 36
04:58:27.636 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.85 L | Bytes: [10 0B 01 45 35 38 30 30 30 62 36] | Checksum: 0x62 ✓
04:58:27.638 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:27.642 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:27.645 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:27.647 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:27.650 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:27.672 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:27.675 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:27.679 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:27.682 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 31 31 30 30], chksum=6F)
04:58:27.684 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:27.686 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 31 31 30 30 6F 36
04:58:27.689 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.10 L | Bytes: [10 0B 01 45 30 31 31 30 30 6F 36] | Checksum: 0x6F ✓
04:58:27.699 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 0.5 L fyllt (13.52 kr)
04:58:28.070 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.073 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:28.075 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.077 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:28.100 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:28.103 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.106 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 33 31 30 30 68 36
04:58:28.110 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 35 33 31 30 30 68 36]
04:58:28.111 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:28.113 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:28.114 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:28.116 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:28.119 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:28.120 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:28.570 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.572 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:28.574 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.576 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:28.598 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:28.600 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:28.602 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 36 31 30 30 69 36
04:58:28.609 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 33 31 30 30], chksum=68)
04:58:28.611 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:28.614 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 33 31 30 30 68 36
04:58:28.616 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.35 L | Bytes: [10 0B 01 45 35 33 31 30 30 68 36] | Checksum: 0x68 ✓
04:58:28.619 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:28.629 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:28.631 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:28.633 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:28.654 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:28.657 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:28.660 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:28.664 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 31 36 31 30 30 69 36 10 07 01 4B 06 5B 36]
04:58:28.665 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
04:58:28.666 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 36 31 30 30], chksum=69)
04:58:28.669 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:28.671 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 36 31 30 30 69 36
04:58:28.674 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.61 L | Bytes: [10 0B 01 45 31 36 31 30 30 69 36] | Checksum: 0x69 ✓
04:58:28.683 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:28.685 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.0 L fyllt (21.47 kr)
04:58:29.071 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.073 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:29.075 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.078 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:29.109 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:29.111 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.115 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 38 31 30 30 60 36
04:58:29.118 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:29.119 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:29.122 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:29.124 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:29.571 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.575 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:29.578 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.590 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:29.613 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:29.616 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:29.620 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 31 32 30 30 6D 36
04:58:29.624 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 36 38 31 30 30 60 36 10 0B 01 45 31 31 32 30 30 6D 36]
04:58:29.625 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
04:58:29.627 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[36 38 31 30 30], chksum=60)
04:58:29.630 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:29.633 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 38 31 30 30 60 36
04:58:29.641 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.86 L | Bytes: [10 0B 01 45 36 38 31 30 30 60 36] | Checksum: 0x60 ✓
04:58:29.643 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:29.647 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:29.649 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
04:58:29.652 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:29.656 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
04:58:29.678 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
04:58:29.680 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
04:58:29.683 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:29.687 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 31 32 30 30], chksum=6D)
04:58:29.692 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:58:29.695 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 31 32 30 30 6D 36
04:58:29.698 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.11 L | Bytes: [10 0B 01 45 31 31 32 30 30 6D 36] | Checksum: 0x6D ✓
04:58:29.705 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.5 L fyllt (29.57 kr)
04:58:29.874 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🛑 FRI PUMPE: Block request for address 1
04:58:29.886 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:29.889 INFO  [OPERATOR] n.c.lpg.protocol - 🛑 STOPP PUMPE - Sender BLOCK til dispenser #1
04:58:29.896 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:58:29.907 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 69 7E 36] -> BLOCK
04:58:29.909 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:58:29.911 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | BLOCK (Block/stop the dispenser) | Bytes: [10 06 01 69 7E 36] | Checksum: 0x7E ✓
04:58:29.935 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=BLOCK(105), data=[], chksum=7E)
04:58:29.938 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:58:29.940 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - STOP/BLOCK: Stopping delivery
04:58:29.963 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Transaction completed: 2.266 L, 36.03 kr
04:58:29.965 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Totals FROZEN - requires reset before next transaction
04:58:29.968 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: DELIVERING → PAYMENT_PENDING
04:58:29.972 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:29.975 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
04:58:29.977 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:58:29.982 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 0B 01 45 37 32 32 30 30 68 36]
04:58:29.985 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 32 bytes
04:58:29.986 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
04:58:29.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:29.989 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
04:58:29.995 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
04:58:29.999 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:30.004 INFO  [OPERATOR] n.c.lpg.protocol - ✅ BLOCK OK - Respons: STATE
04:58:30.006 INFO  [OPERATOR] n.c.lpg.protocol - 📊 Henter finalt volum...
04:58:30.019 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.023 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:30.026 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.029 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:30.065 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:30.066 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.076 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:58:30.080 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
04:58:30.082 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
04:58:30.085 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:58:30.087 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
04:58:30.089 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:30.114 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.116 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
04:58:30.118 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.121 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
04:58:30.123 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
04:58:30.135 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🛑 Transaksjon stoppet: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, volum=1.86 L, beløp=29.57 kr, status=PENDING
04:58:30.143 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
04:58:30.145 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
04:58:30.148 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:58:30.151 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
04:58:30.153 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
04:58:30.155 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
04:58:30.157 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
04:58:30.160 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
04:58:30.174 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📋 Transaksjon oppdatert til PENDING: ID=960f8cf9-5f64-430f-834b-5462bacbc8c0, 1.86 L = 29.57 kr
04:58:30.178 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🛑 Pumping stoppet: 1.86 L = 29.57 kr - venter betaling
04:58:30.184 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump blocked: state=PAYMENT_PENDING, volume=1.86L
04:59:26.603 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:59:26.607 INFO  [-] n.c.l.s.p.PumpStateService - ⏰ 60s TIMEOUT EXPIRED: Pump 1
04:59:26.609 INFO  [-] n.c.l.s.p.PumpStateService -    Pumping ikke startet - sender BLOCK
04:59:26.611 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
04:59:26.619 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:59:26.622 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 69 7E 36] -> BLOCK
04:59:26.625 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:59:26.628 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | BLOCK (Block/stop the dispenser) | Bytes: [10 06 01 69 7E 36] | Checksum: 0x7E ✓
04:59:26.649 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=BLOCK(105), data=[], chksum=7E)
04:59:26.651 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
04:59:26.655 WARN  [-] n.c.l.e.i.EhlDispenserEmulatorImpl - STOP/BLOCK received in state PAYMENT_PENDING - ignoring
04:59:26.656 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
04:59:26.660 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[37 32 32 30 30], chksum=68)
04:59:26.663 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
04:59:26.665 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
04:59:26.668 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.27 L | Bytes: [10 0B 01 45 37 32 32 30 30 68 36] | Checksum: 0x68 ✓
04:59:26.703 INFO  [-] n.c.l.s.p.PumpStateService - 📝 Transaction 960f8cf9-5f64-430f-834b-5462bacbc8c0 marked as CANCELLED (60s timeout)
04:59:26.706 INFO  [-] n.c.l.s.p.PumpStateService - 🛑 BLOCK SENT: Pump 1 blocked after 60s timeout
04:59:26.943 INFO  [DEBUG] n.c.l.a.c.SerialDebugController - Listing available serial ports
04:59:27.056 INFO  [DEBUG] n.c.l.s.s.SerialPortScanner - Found 0 serial ports (0 hardware, 0 virtual, 0 macOS-detected)
04:59:29.775 INFO  [DEBUG] n.c.l.a.c.SerialDebugController - Listing available serial ports
04:59:29.789 INFO  [DEBUG] n.c.l.s.s.SerialPortScanner - Found 0 serial ports (0 hardware, 0 virtual, 0 macOS-detected)
^Cthomas@debian:~$ ./start-lpg-ehl.sh

.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v3.2.1)

05:01:18.740 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Starting LpgEhlApiApplicationKt using Java 21.0.10 with PID 668 (/home/thomas/release/lpg-ehl-webapp.jar started by thomas in /home/thomas)
05:01:18.762 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - The following 1 profile is active: "lab"
Database is up to date, no changesets to execute
05:02:23.758 WARN  [-] o.h.orm.deprecation - HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
05:02:42.209 INFO  [-] n.c.l.a.c.CommunicationConfig -
05:02:42.210 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
05:02:42.219 INFO  [-] n.c.l.a.c.CommunicationConfig -   EHL KOMMUNIKASJON: 🧪 LAB MODE
05:02:42.221 INFO  [-] n.c.l.a.c.CommunicationConfig -   Active profiles: [lab]
05:02:42.222 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
05:02:42.223 INFO  [-] n.c.l.a.c.CommunicationConfig -
05:02:59.125 INFO  [-] n.c.l.a.c.CommunicationConfig - 🧪 Creating EhlDispenserEmulator (address=1, price=1590)
05:02:59.188 INFO  [-] n.c.l.e.i.EhlDispenserEmulatorImpl - EHL Dispenser Emulator initialized: address=1, price=15.9 kr/L
05:02:59.301 INFO  [-] n.c.l.a.c.TransportConfiguration -
05:02:59.302 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
05:02:59.303 INFO  [-] n.c.l.a.c.TransportConfiguration -   🔬 LAB MODE
05:02:59.305 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
05:02:59.307 INFO  [-] n.c.l.a.c.TransportConfiguration -   Transport:  InMemorySerialPort + Emulator
05:02:59.314 INFO  [-] n.c.l.a.c.TransportConfiguration -   Latency:    20ms (simulated)
05:02:59.317 INFO  [-] n.c.l.a.c.TransportConfiguration -   Hardware:   NOT REQUIRED
05:02:59.323 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
05:02:59.324 INFO  [-] n.c.l.a.c.TransportConfiguration -
05:02:59.437 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlCommunicator with InMemorySerialPort
05:02:59.448 INFO  [-] n.c.l.a.c.TransportConfiguration - Raw protocol logging: ENABLED
05:02:59.518 INFO  [-] n.c.l.a.c.TransportConfiguration - 🔄 Retry config: maxRetries=3, initialDelay=100ms, maxDelay=2000ms, backoff=2.0
05:02:59.603 INFO  [-] n.c.l.e.i.InMemorySerialPort - 🔌 InMemorySerialPort: Kobler til emulator (LAB MODE)
05:02:59.607 INFO  [-] n.c.l.a.c.TransportConfiguration - ✅ Transport connected successfully
05:03:03.457 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:03:03.461 INFO  [-] n.c.l.s.p.PumpStateService - 🌟 OPPSTART: Initialiserer pris...
05:03:05.136 INFO  [-] n.c.l.s.p.PumpStateService - 🏷️ STARTUP: Gjenopprettet pris 15.9 kr/L fra database
05:03:05.138 INFO  [-] n.c.l.s.p.PumpStateService -    Satt av: system
05:03:05.143 INFO  [-] n.c.l.s.p.PumpStateService -    Gyldig fra: 2026-02-12T18:11:13.518945
05:03:05.145 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:03:09.753 INFO  [-] n.c.l.a.p.MockPlsService - 🏷️ [MOCK PLS] Initialized with default prices
05:03:15.013 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlOperationsService
05:03:17.231 WARN  [-] o.s.b.a.o.j.JpaBaseConfiguration$JpaWebConfiguration - spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
05:03:17.772 WARN  [-] o.s.b.a.s.s.UserDetailsServiceAutoConfiguration -

Using generated security password: 1bcbe01c-52c1-4a71-9221-a3b3b9a7d5bb

This generated password is for development use only. Your security configuration must be updated before running your application in production.

05:03:34.473 INFO  [-] org.xnio - XNIO version 3.8.8.Final
05:03:34.574 INFO  [-] org.xnio.nio - XNIO NIO Implementation Version 3.8.8.Final
05:03:35.700 INFO  [-] org.jboss.threads - JBoss Threads version 3.5.0.Final
05:03:36.398 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Started LpgEhlApiApplicationKt in 147.766 seconds (process running for 156.368)
05:03:38.426 INFO  [-] n.c.l.a.s.WebAppPollingService - 🚀 WebApp polling service started - UI live updates enabled
05:05:12.992 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
05:05:13.272 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:13.274 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:05:13.654 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:13.676 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:05:13.702 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:13.741 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:05:13.772 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:05:13.820 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:13.836 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - UNBLOCK: Starting new transaction
05:05:13.853 INFO  [OPERATOR] n.c.l.e.i.DispenserSimulatorImpl - Starting simulation: 0.5 L/s, 15.9 kr/L
05:05:13.922 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: AUTHORIZED → DELIVERING
05:05:13.943 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:13.947 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:13.953 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:05:13.962 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:13.969 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:13.974 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:13.983 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:14.011 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:14.015 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.032 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.089 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 06 5B 36 10 07 01 4B 06 5B 36]
05:05:14.104 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:05:14.117 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:05:14.131 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:14.139 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:05:14.142 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:14.169 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:05:14.195 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:14.200 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:14.210 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.216 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:14.220 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:14.228 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:14.249 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK verified: open_for_delivery=1 after 962ms, first STATE raw=0x06
05:05:14.256 INFO  [OPERATOR] n.c.lpg.protocol - ✅ UNBLOCK BEKREFTET - Dispenser klar for pumping
05:05:14.263 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:14.498 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.509 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:14.515 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.526 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:14.547 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:14.549 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.551 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.641 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:14.648 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:14.652 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.660 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:14.668 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ HARDWARE PUMPING DETECTED: Raw state 0x06 for pump 1
05:05:14.682 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:14.686 INFO  [-] n.c.l.s.p.PumpStateService - ⛽ STATE TRANSITION: READY_TO_PUMP → PUMPING (pump 1)
05:05:14.694 INFO  [-] n.c.l.s.p.PumpStateService -    60s timeout cancelled - customer started pumping
05:05:14.696 INFO  [-] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:14.724 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.736 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:14.740 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.747 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:14.761 INFO  [OPERATOR] n.c.l.s.t.TransactionService - ⛽ Transaksjon opprettet: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, dispenser=1, pris=15.9 kr/L, status=STARTED
05:05:14.773 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:14.775 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.787 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 34 30 30 30 6E 36
05:05:14.804 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 35 34 30 30 30 6E 36]
05:05:14.844 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:14.858 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:14.861 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:14.872 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:14.874 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:14.875 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:14.879 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.881 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:14.882 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.884 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:14.913 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:14.915 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:14.919 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 30 30 30 6A 36
05:05:14.930 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 34 30 30 30], chksum=6E)
05:05:14.931 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:14.972 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 34 30 30 30 6E 36
05:05:14.974 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.45 L | Bytes: [10 0B 01 45 35 34 30 30 30 6E 36] | Checksum: 0x6E ✓
05:05:14.983 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.984 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:14.987 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:14.989 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:15.010 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:15.012 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:15.014 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:15.017 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 30 35 30 30 30 6A 36 10 07 01 4B 06 5B 36]
05:05:15.018 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:15.019 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 35 30 30 30], chksum=6A)
05:05:15.021 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:15.023 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 30 30 30 6A 36
05:05:15.025 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.50 L | Bytes: [10 0B 01 45 30 35 30 30 30 6A 36] | Checksum: 0x6A ✓
05:05:15.025 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:15.174 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📝 Transaksjon opprettet: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, pris=15.9 kr/L
05:05:15.178 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:15.181 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ⏱️ 60s TIMEOUT STARTED: Pump 1
05:05:15.183 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på at kunde starter pumping...
05:05:15.197 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:15.213 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🔓 PUMPE FRIGJORT: Pump #1 klar til fylling (60s timeout startet)
05:05:15.216 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump released: state=PUMPING
05:05:15.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.371 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:15.373 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.375 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:15.396 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:15.398 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.405 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 30 30 30 6D 36
05:05:15.413 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:15.415 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:15.416 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:15.418 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:15.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.874 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:15.882 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.884 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:15.907 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:15.910 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:15.919 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 31 30 30 6E 36
05:05:15.925 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 35 37 30 30 30 6D 36 10 0B 01 45 30 30 31 30 30 6E 36]
05:05:15.927 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:15.929 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 37 30 30 30], chksum=6D)
05:05:15.933 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:15.936 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 30 30 30 6D 36
05:05:15.944 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=0.75 L | Bytes: [10 0B 01 45 35 37 30 30 30 6D 36] | Checksum: 0x6D ✓
05:05:15.947 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:15.951 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:15.953 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:15.956 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:15.972 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:15.998 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:16.000 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.003 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:16.007 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 30 31 30 30], chksum=6E)
05:05:16.009 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:16.011 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 31 30 30 6E 36
05:05:16.014 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.00 L | Bytes: [10 0B 01 45 30 30 31 30 30 6E 36] | Checksum: 0x6E ✓
05:05:16.023 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 0.5 L fyllt (11.93 kr)
05:05:16.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.373 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:16.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:16.405 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:16.407 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.411 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 32 31 30 30 6A 36
05:05:16.416 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 36 32 31 30 30 6A 36]
05:05:16.417 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:16.419 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:16.422 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:16.424 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:16.429 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:16.436 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:16.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:16.876 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.878 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:16.906 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:16.908 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:16.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 35 31 30 30 6A 36
05:05:16.915 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[36 32 31 30 30], chksum=6A)
05:05:16.918 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:16.921 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 32 31 30 30 6A 36
05:05:16.924 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.26 L | Bytes: [10 0B 01 45 36 32 31 30 30 6A 36] | Checksum: 0x6A ✓
05:05:16.933 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.935 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:16.945 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.947 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:16.969 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:16.972 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:16.975 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:16.984 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 31 35 31 30 30 6A 36 10 07 01 4B 06 5B 36]
05:05:16.986 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:16.988 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 35 31 30 30], chksum=6A)
05:05:16.991 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:16.994 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 35 31 30 30 6A 36
05:05:16.997 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.51 L | Bytes: [10 0B 01 45 31 35 31 30 30 6A 36] | Checksum: 0x6A ✓
05:05:16.999 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:17.003 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.0 L fyllt (20.03 kr)
05:05:17.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:17.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:17.400 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:17.402 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.406 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 37 31 30 30 6F 36
05:05:17.409 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:17.411 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:17.412 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:17.415 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:17.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.874 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:17.876 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.878 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:17.914 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:17.917 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:17.920 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 30 32 30 30 6C 36
05:05:17.925 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 36 37 31 30 30 6F 36 10 0B 01 45 31 30 32 30 30 6C 36]
05:05:17.927 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:17.928 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[36 37 31 30 30], chksum=6F)
05:05:17.931 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:17.933 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 36 37 31 30 30 6F 36
05:05:17.936 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=1.76 L | Bytes: [10 0B 01 45 36 37 31 30 30 6F 36] | Checksum: 0x6F ✓
05:05:17.945 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:17.949 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:17.951 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:17.970 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:17.972 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:17.994 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:17.996 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:17.999 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:18.003 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[31 30 32 30 30], chksum=6C)
05:05:18.004 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:18.006 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 31 30 32 30 30 6C 36
05:05:18.009 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.01 L | Bytes: [10 0B 01 45 31 30 32 30 30 6C 36] | Checksum: 0x6C ✓
05:05:18.016 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 1.5 L fyllt (27.98 kr)
05:05:18.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.375 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:18.377 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.386 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:18.410 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:18.412 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.415 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
05:05:18.419 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 37 32 32 30 30 68 36]
05:05:18.420 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:18.422 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:18.424 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:18.427 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:18.429 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:18.437 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:18.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:18.875 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.877 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:18.899 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:18.902 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:18.916 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 35 32 30 30 6B 36
05:05:18.921 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[37 32 32 30 30], chksum=68)
05:05:18.926 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:18.930 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 37 32 32 30 30 68 36
05:05:18.933 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.27 L | Bytes: [10 0B 01 45 37 32 32 30 30 68 36] | Checksum: 0x68 ✓
05:05:18.938 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:18.942 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:18.945 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:18.948 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:18.971 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:18.976 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:18.980 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:18.985 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 33 35 32 30 30 6B 36 10 07 01 4B 06 5B 36]
05:05:18.986 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:18.988 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[33 35 32 30 30], chksum=6B)
05:05:18.991 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:18.994 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 35 32 30 30 6B 36
05:05:19.002 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.53 L | Bytes: [10 0B 01 45 33 35 32 30 30 6B 36] | Checksum: 0x6B ✓
05:05:19.004 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:19.006 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 2.0 L fyllt (36.09 kr)
05:05:19.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:19.373 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:19.397 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:19.400 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.406 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 38 37 32 30 30 62 36
05:05:19.410 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:19.411 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:19.413 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:19.418 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:19.871 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.875 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:19.879 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.883 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:19.906 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:19.909 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:19.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 30 33 30 30 6F 36
05:05:19.916 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 38 37 32 30 30 62 36 10 0B 01 45 33 30 33 30 30 6F 36]
05:05:19.918 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:19.921 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[38 37 32 30 30], chksum=62)
05:05:19.924 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:19.926 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 38 37 32 30 30 62 36
05:05:19.929 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=2.78 L | Bytes: [10 0B 01 45 38 37 32 30 30 62 36] | Checksum: 0x62 ✓
05:05:19.931 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:19.934 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:19.936 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:19.938 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:19.940 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:19.962 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:19.965 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:19.967 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:19.977 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[33 30 33 30 30], chksum=6F)
05:05:19.979 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:19.980 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 30 33 30 30 6F 36
05:05:19.983 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.03 L | Bytes: [10 0B 01 45 33 30 33 30 30 6F 36] | Checksum: 0x6F ✓
05:05:19.986 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 2.5 L fyllt (44.2 kr)
05:05:20.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:20.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:20.398 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:20.399 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 32 33 30 30 6D 36
05:05:20.409 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 33 32 33 30 30 6D 36]
05:05:20.411 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:20.414 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:20.416 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:20.419 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:20.422 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:20.424 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:20.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:20.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.875 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:20.897 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:20.899 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:20.902 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 33 30 30 61 36
05:05:20.905 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[33 32 33 30 30], chksum=6D)
05:05:20.907 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:20.909 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 33 32 33 30 30 6D 36
05:05:20.913 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.23 L | Bytes: [10 0B 01 45 33 32 33 30 30 6D 36] | Checksum: 0x6D ✓
05:05:20.918 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:20.921 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:20.924 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:20.927 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:20.950 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:20.953 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:20.962 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:20.966 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 39 34 33 30 30 61 36 10 07 01 4B 06 5B 36]
05:05:20.977 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:20.979 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 34 33 30 30], chksum=61)
05:05:20.982 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:20.985 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 33 30 30 61 36
05:05:20.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.49 L | Bytes: [10 0B 01 45 39 34 33 30 30 61 36] | Checksum: 0x61 ✓
05:05:20.990 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:20.994 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 3.0 L fyllt (51.36 kr)
05:05:21.372 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.374 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:21.375 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:21.398 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:21.400 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 37 33 30 30 6F 36
05:05:21.407 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:21.408 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:21.410 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:21.412 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:21.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.871 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:21.872 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:21.894 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:21.896 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:21.898 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 39 33 30 30 6C 36
05:05:21.901 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 34 37 33 30 30 6F 36 10 0B 01 45 39 39 33 30 30 6C 36]
05:05:21.902 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:21.903 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[34 37 33 30 30], chksum=6F)
05:05:21.904 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:21.906 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 37 33 30 30 6F 36
05:05:21.909 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.74 L | Bytes: [10 0B 01 45 34 37 33 30 30 6F 36] | Checksum: 0x6F ✓
05:05:21.909 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:21.911 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:21.912 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:21.913 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:21.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:21.944 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:21.947 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:21.949 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:21.952 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 39 33 30 30], chksum=6C)
05:05:21.953 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:21.956 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 39 33 30 30 6C 36
05:05:21.959 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=3.99 L | Bytes: [10 0B 01 45 39 39 33 30 30 6C 36] | Checksum: 0x6C ✓
05:05:21.961 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 3.5 L fyllt (59.47 kr)
05:05:22.371 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.373 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:22.375 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:22.397 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:22.399 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.402 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 32 34 30 30 6D 36
05:05:22.406 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 34 32 34 30 30 6D 36]
05:05:22.407 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:22.408 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:22.411 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:22.418 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:22.421 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:22.424 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:22.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:22.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.875 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:22.897 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:22.898 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:22.902 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 34 30 30 66 36
05:05:22.905 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[34 32 34 30 30], chksum=6D)
05:05:22.906 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:22.907 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 34 32 34 30 30 6D 36
05:05:22.909 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=4.24 L | Bytes: [10 0B 01 45 34 32 34 30 30 6D 36] | Checksum: 0x6D ✓
05:05:22.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:22.914 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:22.915 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:22.917 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:22.938 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:22.941 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:22.943 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:22.947 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 39 34 34 30 30 66 36 10 07 01 4B 06 5B 36]
05:05:22.949 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:22.950 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[39 34 34 30 30], chksum=66)
05:05:22.954 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:22.962 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 39 34 34 30 30 66 36
05:05:22.973 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=4.49 L | Bytes: [10 0B 01 45 39 34 34 30 30 66 36] | Checksum: 0x66 ✓
05:05:22.975 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:22.977 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 4.0 L fyllt (67.42 kr)
05:05:23.371 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.373 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:23.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:23.398 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:23.399 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 34 30 30 69 36
05:05:23.406 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:23.407 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:23.410 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:23.413 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:23.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:23.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.877 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:23.899 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:23.901 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:23.904 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 35 30 30 6A 36
05:05:23.907 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 35 37 34 30 30 69 36 10 0B 01 45 30 30 35 30 30 6A 36]
05:05:23.908 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 22 bytes
05:05:23.909 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 37 34 30 30], chksum=69)
05:05:23.910 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:23.912 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 37 34 30 30 69 36
05:05:23.914 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=4.75 L | Bytes: [10 0B 01 45 35 37 34 30 30 69 36] | Checksum: 0x69 ✓
05:05:23.914 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:23.918 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:23.920 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:23.921 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:23.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:23.944 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:23.945 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:23.948 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:23.951 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 30 35 30 30], chksum=6A)
05:05:23.953 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:23.954 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 30 35 30 30 6A 36
05:05:23.955 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=5.00 L | Bytes: [10 0B 01 45 30 30 35 30 30 6A 36] | Checksum: 0x6A ✓
05:05:23.967 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 4.5 L fyllt (75.53 kr)
05:05:24.370 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.372 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:24.374 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.377 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:24.399 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:24.403 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.408 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 32 35 30 30 6D 36
05:05:24.419 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 06 5B 36 10 0B 01 45 35 32 35 30 30 6D 36]
05:05:24.421 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:24.423 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:24.426 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:24.428 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:24.430 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:24.432 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:24.870 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:24.874 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.876 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:24.898 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:24.900 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:24.904 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 35 30 30 6F 36
05:05:24.907 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[35 32 35 30 30], chksum=6D)
05:05:24.910 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:24.914 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 35 32 35 30 30 6D 36
05:05:24.918 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=5.25 L | Bytes: [10 0B 01 45 35 32 35 30 30 6D 36] | Checksum: 0x6D ✓
05:05:24.922 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:24.925 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:24.926 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:24.931 DEBUG [-] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:24.953 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:24.956 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:24.959 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:24.963 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 0B 01 45 30 35 35 30 30 6F 36 10 07 01 4B 06 5B 36]
05:05:24.964 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 18 bytes
05:05:24.965 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[30 35 35 30 30], chksum=6F)
05:05:24.968 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: VOLUME from addr 1
05:05:24.971 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 35 35 30 30 6F 36
05:05:24.994 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | VOLUME (Give/take the fuel amount) | Volume=5.50 L | Bytes: [10 0B 01 45 30 35 35 30 30 6F 36] | Checksum: 0x6F ✓
05:05:24.997 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:25.000 INFO  [-] n.c.lpg.protocol - ⛽ MILEPÆL: 5.0 L fyllt (83.48 kr)
05:05:25.018 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🛑 FRI PUMPE: Block request for address 1
05:05:25.021 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:25.023 INFO  [OPERATOR] n.c.lpg.protocol - 🛑 STOPP PUMPE - Sender BLOCK til dispenser #1
05:05:25.033 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
05:05:25.034 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 69 7E 36] -> BLOCK
05:05:25.039 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
05:05:25.046 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | BLOCK (Block/stop the dispenser) | Bytes: [10 06 01 69 7E 36] | Checksum: 0x7E ✓
05:05:25.069 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=BLOCK(105), data=[], chksum=7E)
05:05:25.071 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 69 7E 36
05:05:25.073 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - STOP/BLOCK: Stopping delivery
05:05:25.097 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Transaction completed: 5.6035 L, 89.1 kr
05:05:25.099 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - Totals FROZEN - requires reset before next transaction
05:05:25.101 INFO  [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - State: DELIVERING → PAYMENT_PENDING
05:05:25.104 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:25.106 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:25.108 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 35 30 30 6C 36
05:05:25.111 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[06], chksum=5B)
05:05:25.113 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:25.116 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 06 5B 36
05:05:25.119 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x06 bits=00000110 open_for_delivery=true startbutton=true automode=false error=false (PUMPING (Fuel flowing)) | Bytes: [10 07 01 4B 06 5B 36] | Checksum: 0x5B ✓
05:05:25.122 INFO  [OPERATOR] n.c.lpg.protocol - ✅ BLOCK OK - Respons: STATE
05:05:25.123 INFO  [OPERATOR] n.c.lpg.protocol - 📊 Henter finalt volum...
05:05:25.130 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:25.132 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 45 52 36] -> VOLUME
05:05:25.134 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:25.137 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | VOLUME (Give/take the fuel amount) | Bytes: [10 06 01 45 52 36] | Checksum: 0x52 ✓
05:05:25.159 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=VOLUME(69), data=[], chksum=52)
05:05:25.161 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 45 52 36
05:05:25.164 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 0B 01 45 30 36 35 30 30 6C 36
05:05:25.168 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 0B 01 45 30 36 35 30 30 6C 36 10 0B 01 45 30 36 35 30 30 6C 36]
05:05:25.171 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 36 bytes
05:05:25.173 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:05:25.175 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:05:25.178 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:25.180 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:05:25.182 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:25.185 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:25.199 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🛑 Transaksjon stoppet: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, volum=5.25 L, beløp=83.48 kr, status=PENDING
05:05:25.219 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 📋 Transaksjon oppdatert til PENDING: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, 5.25 L = 83.48 kr
05:05:25.222 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 🛑 Pumping stoppet: 5.25 L = 83.48 kr - venter betaling
05:05:25.230 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Pump blocked: state=PAYMENT_PENDING, volume=5.25L
05:05:27.717 INFO  [OPERATOR] n.c.l.a.c.PumpController - 💳 Settle payment request: dispenserId=1, method=CARD
05:05:27.733 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 💳 Transaksjon betalt: ID=84f32d1c-189b-4922-9d8a-7c3877a8714a, volum=5.2 L, beløp=83.48 kr, metode=CARD
05:05:27.775 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 Betaling fullført: 5.25 L = 83.48 kr via CARD
05:05:27.804 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 Pump 1 settled: 5.25L = 83.48 kr via CARD
05:05:27.806 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Payment settled: 83.48 NOK, 5.25 L
05:05:36.440 INFO  [DEBUG] n.c.l.a.c.SerialDebugController - Listing available serial ports
05:05:36.544 INFO  [DEBUG] n.c.l.s.s.SerialPortScanner - Found 0 serial ports (0 hardware, 0 virtual, 0 macOS-detected)
05:05:42.643 INFO  [-] n.c.l.a.w.WebSocketEventPublisher - 🔌 WebSocket connected: ad45eab3-b182-a4c0-ea94-95fcd3c4bb87
05:05:42.866 INFO  [-] n.c.l.a.w.WebSocketEventPublisher - 📝 Session ad45eab3-b182-a4c0-ea94-95fcd3c4bb87 subscribed to: [API, SERVICE, EMULATOR, PROTOCOL]
05:05:44.183 INFO  [OPERATOR] n.c.l.a.c.PumpController - 💳 SIMULER KORTDRAGNING: Dispenser 1
05:05:44.347 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:44.352 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 💳 KORTDRAGNING SIMULERT
05:05:44.357 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Dispenser: 1
05:05:44.361 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Auth ID: c5554022-d6ce-48d7-b678-689e66dd6c74
05:05:44.366 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Maks beløp: 2000.0 kr
05:05:44.372 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Pris: 15.9 kr/L
05:05:44.377 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Metode: CARD
05:05:44.381 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Status: PENDING → Venter på UNBLOCK
05:05:44.385 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:44.401 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Autorisasjon opprettet: c5554022-d6ce-48d7-b678-689e66dd6c74
05:05:44.405 INFO  [OPERATOR] n.c.l.a.c.PumpController - ⏱️ 60s nedtelling startet - venter på FRI DISPENSER
05:05:44.411 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:44.420 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 KORTDRAGNING: Pump 1 state -> AUTHORIZED_WAITING
05:05:44.425 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Auth ID: c5554022-d6ce-48d7-b678-689e66dd6c74
05:05:44.430 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på FRI DISPENSER (60s timeout)
05:05:44.434 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:05:46.531 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE: Unblock request for address 1
05:05:46.535 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:05:46.539 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:05:46.652 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:46.654 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:05:46.655 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:46.657 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:05:46.679 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:05:46.681 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:05:46.683 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:05:46.688 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:05:46.692 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:05:46.696 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:05:46.701 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:46.703 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:46.704 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:05:46.709 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:46.710 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:46.712 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:46.714 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:46.735 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:46.737 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:46.739 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:46.742 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:46.754 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:05:46.755 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:05:46.756 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:05:46.762 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:05:46.763 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:46.764 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:05:46.778 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:46.780 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:46.782 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:46.784 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:46.785 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:46.787 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:47.090 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.091 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:47.093 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.094 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:47.115 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:47.117 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.119 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.122 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:47.123 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:47.125 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.128 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:47.129 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:47.431 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.432 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:47.433 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.435 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:47.456 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:47.457 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.459 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.463 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:47.476 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:47.478 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:47.481 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.483 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:47.485 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:47.486 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:47.790 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.791 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:47.793 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.795 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:47.816 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:47.818 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:47.821 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.823 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:47.825 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:47.827 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:47.830 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:47.831 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:48.134 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.135 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:48.137 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.139 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:48.160 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:48.161 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.163 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.166 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:48.177 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:48.179 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:48.181 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.183 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:48.184 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:48.185 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:48.488 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.489 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:48.490 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.492 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:48.513 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:48.514 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.516 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.519 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:48.520 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:48.521 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.523 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:48.525 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:48.828 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.829 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:48.830 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.831 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:48.852 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:48.854 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:48.856 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.860 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:48.872 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:48.873 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:48.875 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:48.876 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:48.877 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:48.878 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:49.186 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.187 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:49.188 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.189 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:49.211 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:49.214 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.217 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.220 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:49.222 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:49.226 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.230 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:49.233 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:49.536 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.538 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:49.539 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.540 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:49.561 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:49.562 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.565 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.568 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:49.579 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:49.581 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:49.582 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.584 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:49.585 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:49.585 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:49.887 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.888 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:49.889 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.891 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:49.912 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:49.914 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:49.917 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.920 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:49.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:49.925 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:49.928 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:49.929 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:50.232 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.233 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:50.234 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.236 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:50.257 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:50.258 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.260 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.263 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:50.275 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:50.277 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:50.280 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.283 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:50.286 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:50.288 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:50.591 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.593 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:50.597 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.599 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:50.622 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:50.625 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.628 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.630 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:50.631 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:50.637 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.639 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:50.642 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:50.944 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.945 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:50.946 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.949 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:50.971 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:50.973 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:50.975 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.978 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:50.990 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:50.991 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:50.993 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:50.995 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:50.998 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:50.999 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:51.303 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.305 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:51.308 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.311 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:51.334 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:51.335 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.337 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.339 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:51.341 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:51.345 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.349 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:51.353 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:51.658 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.660 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:51.661 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.665 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:51.687 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:51.689 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:51.691 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.694 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:51.707 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:51.710 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:51.713 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:51.716 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:51.718 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:51.721 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:52.024 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.025 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:52.028 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.031 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:52.051 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:52.053 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.055 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.057 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:52.058 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:52.060 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.061 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:52.062 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:52.363 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.364 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:05:52.365 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.366 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:05:52.387 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:05:52.388 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:05:52.390 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.393 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:05:52.405 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:05:52.407 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:05:52.409 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:05:52.413 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:05:52.416 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:05:52.418 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:05:52.723 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:05:52.727 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Unblock failed: UNBLOCK: open_for_delivery bit not observed within 6s
05:05:54.886 WARN  [OPERATOR] n.c.l.a.c.AdminController - 🧹 ADMIN CLEANUP: Cancelling all stuck authorizations...
05:05:54.928 WARN  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 🧹 Cancelling 1 stuck authorization(s)...
05:05:54.934 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    ❌ Cancelled: c5554022-d6ce-48d7-b678-689e66dd6c74 (was CANCELLED)
05:05:54.954 WARN  [OPERATOR] n.c.l.s.p.PumpStateService - 🧹 Resetting ALL pumps to IDLE...
05:05:54.970 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    🔄 Pump 1 reset to IDLE
05:05:54.982 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ✅ Reset 1 pump(s) to IDLE
05:05:54.994 INFO  [OPERATOR] n.c.l.a.c.AdminController - ✅ Cleanup completed: 1 authorization(s) cancelled, all pumps reset to IDLE
05:06:03.941 INFO  [OPERATOR] n.c.l.a.c.PumpController - 💳 SIMULER KORTDRAGNING: Dispenser 1
05:06:04.013 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:04.017 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 💳 KORTDRAGNING SIMULERT
05:06:04.021 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Dispenser: 1
05:06:04.025 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Auth ID: 8c8b4479-801e-479b-b83e-5020eb0b7d00
05:06:04.029 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Maks beløp: 2000.0 kr
05:06:04.034 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Pris: 15.9 kr/L
05:06:04.038 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Metode: CARD
05:06:04.042 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    Status: PENDING → Venter på UNBLOCK
05:06:04.046 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:04.059 INFO  [OPERATOR] n.c.l.a.c.PumpController - ✅ Autorisasjon opprettet: 8c8b4479-801e-479b-b83e-5020eb0b7d00
05:06:04.063 INFO  [OPERATOR] n.c.l.a.c.PumpController - ⏱️ 60s nedtelling startet - venter på FRI DISPENSER
05:06:04.067 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:06:04.071 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - 💳 KORTDRAGNING: Pump 1 state -> AUTHORIZED_WAITING
05:06:04.075 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Auth ID: 8c8b4479-801e-479b-b83e-5020eb0b7d00
05:06:04.079 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    Venter på FRI DISPENSER (60s timeout)
05:06:04.088 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ═══════════════════════════════════════════════════════════
05:06:09.593 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE: Unblock request for address 1
05:06:09.596 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:09.600 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:06:09.707 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:09.709 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:06:09.711 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:09.713 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:06:09.734 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:06:09.736 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:09.740 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:06:09.745 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:06:09.750 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:06:09.755 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:06:09.764 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:09.765 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:09.767 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:06:09.781 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:09.782 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:09.784 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:09.786 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:09.811 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:09.813 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:09.815 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:09.818 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:09.830 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:06:09.831 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:06:09.833 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:09.834 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:06:09.836 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:09.837 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:06:09.849 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:09.850 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:09.851 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:09.852 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:09.853 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:09.853 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:10.156 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.157 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:10.159 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.161 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:10.182 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:10.184 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.186 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.189 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:10.190 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:10.192 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.195 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:10.196 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:10.499 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.500 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:10.501 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.502 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:10.523 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:10.525 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.527 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.530 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:10.542 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:10.543 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:10.545 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.546 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:10.547 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:10.547 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:10.849 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.850 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:10.852 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.853 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:10.874 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:10.877 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:10.880 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.882 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:10.884 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:10.885 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:10.893 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:10.895 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:11.198 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.199 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:11.200 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.202 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:11.224 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:11.225 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.228 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.231 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:11.243 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:11.245 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:11.247 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.248 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:11.249 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:11.250 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:11.552 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.553 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:11.556 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.557 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:11.577 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:11.579 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.581 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.582 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:11.583 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:11.585 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.586 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:11.587 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:11.889 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.890 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:11.891 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.894 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:11.915 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:11.917 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:11.919 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.922 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:11.934 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:11.935 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:11.937 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:11.939 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:11.940 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:11.941 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:12.243 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.245 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:12.247 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.249 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:12.276 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:12.287 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.291 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.295 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:12.296 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:12.299 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.301 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:12.302 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:12.606 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.607 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:12.609 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.610 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:12.631 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:12.633 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.635 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.638 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:12.650 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:12.650 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:12.652 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.653 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:12.653 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:12.654 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:12.956 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.957 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:12.959 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.961 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:12.982 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:12.983 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:12.985 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.987 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:12.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:12.991 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:12.992 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:12.993 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:13.297 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.298 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:13.299 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.301 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:13.322 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:13.323 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.325 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.330 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:13.342 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:13.344 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:13.346 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.349 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:13.352 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:13.354 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:13.657 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.660 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:13.662 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.663 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:13.685 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:13.687 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:13.689 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.691 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:13.692 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:13.695 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:13.696 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:13.698 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:14.000 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.002 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:14.003 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.005 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:14.026 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:14.029 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.032 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.036 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:14.049 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:14.050 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:14.051 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.053 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:14.053 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:14.054 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:14.355 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.356 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:14.357 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.358 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:14.381 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:14.383 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.386 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.388 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:14.388 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:14.390 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.391 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:14.394 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:14.697 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.699 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:14.700 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.703 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:14.725 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:14.727 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:14.729 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.732 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:14.745 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:14.747 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:14.749 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:14.753 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:14.754 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:14.754 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:15.056 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.057 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:15.058 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.059 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:15.080 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:15.082 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.084 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.088 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:15.091 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:15.095 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.099 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:15.102 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:15.405 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.406 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:15.407 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.409 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:15.431 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:15.433 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.434 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.437 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:15.449 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:15.451 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:15.453 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.454 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:15.454 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:15.455 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:15.757 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.758 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:15.759 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.761 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:15.782 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:15.783 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:15.785 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.787 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:15.788 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:15.790 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:15.792 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:15.792 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:16.095 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:06:16.099 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Unblock failed: UNBLOCK: open_for_delivery bit not observed within 6s
05:06:22.735 WARN  [OPERATOR] n.c.l.a.c.AdminController - 🧹 ADMIN CLEANUP: Cancelling all stuck authorizations...
05:06:22.766 WARN  [OPERATOR] n.c.l.s.p.PumpAuthorizationService - 🧹 Cancelling 1 stuck authorization(s)...
05:06:22.774 INFO  [OPERATOR] n.c.l.s.p.PumpAuthorizationService -    ❌ Cancelled: 8c8b4479-801e-479b-b83e-5020eb0b7d00 (was CANCELLED)
05:06:22.796 WARN  [OPERATOR] n.c.l.s.p.PumpStateService - 🧹 Resetting ALL pumps to IDLE...
05:06:22.802 INFO  [OPERATOR] n.c.l.s.p.PumpStateService -    🔄 Pump 1 reset to IDLE
05:06:22.808 INFO  [OPERATOR] n.c.l.s.p.PumpStateService - ✅ Reset 1 pump(s) to IDLE
05:06:22.812 INFO  [OPERATOR] n.c.l.a.c.AdminController - ✅ Cleanup completed: 1 authorization(s) cancelled, all pumps reset to IDLE
05:06:31.844 INFO  [-] n.c.l.a.w.WebSocketEventPublisher - 🔌 WebSocket disconnected: ad45eab3-b182-a4c0-ea94-95fcd3c4bb87 ()
05:06:50.839 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
05:06:50.865 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:06:50.866 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:06:50.869 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drain: discarding 7 bytes
05:06:50.995 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drained for 100ms, discarded 7 bytes
05:06:50.997 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:50.998 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:06:50.999 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:51.001 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:06:51.023 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:06:51.025 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:06:51.026 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:06:51.027 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:06:51.027 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:06:51.028 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:06:51.030 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:51.031 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.032 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:06:51.033 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.034 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:51.035 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.036 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:51.056 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:51.058 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.060 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.063 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:51.075 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:06:51.076 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:06:51.078 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:06:51.079 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:06:51.080 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:51.081 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:06:51.093 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:51.094 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:51.096 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.097 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:51.097 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:51.098 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:51.400 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.401 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:51.402 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.404 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:51.427 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:51.428 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.430 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.432 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:51.433 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:51.434 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.435 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:51.436 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:51.737 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.738 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:51.739 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.741 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:51.761 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:51.763 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:51.765 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.767 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:51.779 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:51.780 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:51.782 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:51.784 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:51.785 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:51.787 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:52.089 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.090 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:52.091 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.094 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:52.115 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:52.119 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.124 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.128 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:52.129 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:52.132 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.137 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:52.140 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:52.444 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.445 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:52.446 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.448 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:52.469 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:52.470 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.473 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.475 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:52.487 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:52.488 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:52.490 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.491 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:52.493 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:52.495 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:52.798 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.799 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:52.800 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.803 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:52.824 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:52.825 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:52.828 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.830 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:52.831 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:52.832 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:52.835 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:52.837 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:53.139 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.140 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:53.141 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.143 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:53.164 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:53.165 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.167 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.170 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:53.182 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:53.183 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:53.184 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.185 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:53.186 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:53.186 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:53.489 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.491 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:53.492 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.494 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:53.516 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:53.519 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.522 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.525 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:53.526 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:53.529 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.532 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:53.535 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:53.838 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.840 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:53.841 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.843 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:53.864 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:53.866 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:53.868 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.872 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:53.885 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:53.885 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:53.887 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:53.888 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:53.890 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:53.891 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:54.194 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.195 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:54.197 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.200 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:54.223 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:54.226 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.229 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.233 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:54.235 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:54.237 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.241 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:54.243 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:54.553 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.555 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:54.557 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.561 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:54.583 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:54.585 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.587 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.591 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:54.602 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:54.603 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:54.606 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.608 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:54.610 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:54.612 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:54.915 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.916 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:54.917 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.918 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:54.938 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:54.940 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:54.942 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.945 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:54.946 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:54.948 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:54.950 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:54.952 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:55.255 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.256 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:55.258 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.260 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:55.282 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:55.283 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.285 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.287 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:55.299 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:55.299 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:55.301 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.302 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:55.303 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:55.305 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:55.607 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.608 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:55.609 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.610 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:55.632 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:55.633 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.634 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.637 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:55.638 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:55.639 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.639 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:55.640 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:55.942 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.943 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:55.944 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.945 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:55.966 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:55.968 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:55.970 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.972 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:55.984 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:55.984 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:55.986 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:55.987 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:55.987 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:55.988 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:56.292 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.293 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:56.295 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.296 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:56.317 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:56.319 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.321 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.325 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:56.327 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:56.329 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.331 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:56.333 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:56.636 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.637 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:56.638 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.638 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:56.659 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:56.660 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.662 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.665 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:06:56.677 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:56.679 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:56.681 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:56.683 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:56.689 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:06:56.691 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:56.994 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.995 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:06:56.995 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:56.996 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:06:57.017 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:06:57.018 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:06:57.019 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:57.022 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:06:57.023 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:06:57.024 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:06:57.025 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:06:57.026 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:06:57.328 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:06:57.331 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Release failed: UNBLOCK: open_for_delivery bit not observed within 6s
05:07:12.589 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:12.593 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:13.057 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:13.079 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:16.588 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:16.591 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:16.622 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:16.626 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:19.662 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:19.664 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:19.698 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:19.703 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:32.847 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:32.850 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:32.876 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:32.879 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:34.643 INFO  [OPERATOR] n.c.l.a.c.TransactionController - 📋 List transactions: page=0, size=20, dispenser=null, paymentType=null, paymentStatus=null
05:07:34.649 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 getTransactions: dispenserAddress=null, paymentType=null, paymentStatus=null
05:07:34.684 INFO  [OPERATOR] n.c.l.s.t.TransactionService - 🔍 findWithFilters returned 2 transactions
05:07:34.687 INFO  [OPERATOR] n.c.l.a.c.TransactionController - ✅ Returned 2 transactions (total=2)
05:07:37.641 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE (MANAGER): Release request for address 1
05:07:37.661 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
05:07:37.662 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #1
05:07:37.663 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drain: discarding 7 bytes
05:07:37.766 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Drained for 100ms, discarded 7 bytes
05:07:37.767 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:07:37.769 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 77 60 36] -> UNBLOCK
05:07:37.770 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:07:37.771 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | UNBLOCK (Start delivery mode) | Bytes: [10 06 01 77 60 36] | Checksum: 0x60 ✓
05:07:37.793 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=UNBLOCK(119), data=[], chksum=60)
05:07:37.794 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 77 60 36
05:07:37.795 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)
05:07:37.797 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Totals frozen: 5.6035 L, 89.1 kr
05:07:37.799 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Must settle payment before starting new transaction
05:07:37.801 ERROR [OPERATOR] n.c.l.e.i.EhlDispenserEmulatorImpl - ❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment
05:07:37.803 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:07:37.806 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:37.808 INFO  [OPERATOR] n.c.lpg.protocol - Awaiting STATE(open bit 0x02) for addr 1
05:07:37.813 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:37.814 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:37.816 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:37.818 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:37.840 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:37.843 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:37.846 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:37.849 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 1E 30 38 36 10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:37.861 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=OK(30), data=[30], chksum=38)
05:07:37.862 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: OK from addr 1
05:07:37.864 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 1E 30 38 36
05:07:37.866 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | OK (Command acknowledgement) | Data: [30] | Bytes: [10 07 01 1E 30 38 36] | Checksum: 0x38 ✓
05:07:37.867 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:37.869 DEBUG [-] n.c.l.c.EhlCommunicator - Ignored OK addr=1 while awaiting STATE(open bit 0x02)
05:07:37.881 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:37.883 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:37.885 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:37.887 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:37.892 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:37.894 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:38.198 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.200 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:38.202 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.205 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:38.228 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:38.230 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.233 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.236 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:38.240 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:38.241 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.243 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:38.244 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:38.549 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.549 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:38.550 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.551 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:38.572 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:38.573 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.574 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.577 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:38.596 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:38.598 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:38.600 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.603 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:38.605 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:38.608 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:38.910 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.911 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:38.911 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.912 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:38.933 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:38.934 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:38.935 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.938 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:38.939 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:38.940 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:38.941 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:38.941 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:39.243 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.244 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:39.245 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.246 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:39.267 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:39.268 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.270 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.272 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:39.284 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:39.285 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:39.286 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.287 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:39.289 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:39.290 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:39.592 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.595 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:39.597 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.599 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:39.622 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:39.623 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.625 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.627 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:39.628 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:39.630 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.631 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:39.633 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:39.936 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.938 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:39.939 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.940 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:39.961 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:39.961 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:39.962 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.964 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:39.976 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:39.977 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:39.978 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:39.979 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:39.980 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:39.981 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:40.282 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.283 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:40.283 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.284 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:40.305 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:40.306 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.308 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.310 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:40.311 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:40.312 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.313 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:40.313 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:40.616 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.618 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:40.620 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.621 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:40.643 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:40.644 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.647 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.649 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:40.661 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:40.662 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:40.663 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.664 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:40.665 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:40.666 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:40.968 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.969 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:40.969 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.971 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:40.992 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:40.994 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:40.995 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:40.997 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:40.998 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:41.000 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.002 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:41.004 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:41.307 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.307 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:41.308 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.309 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:41.329 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:41.331 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.332 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.334 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:41.346 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:41.347 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:41.348 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.351 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:41.353 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:41.355 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:41.656 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.658 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:41.659 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.660 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:41.682 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:41.683 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.686 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.692 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:41.693 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:41.694 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:41.695 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:41.696 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:41.998 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:41.998 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:41.999 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.000 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:42.021 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:42.022 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.023 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.025 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:42.037 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:42.038 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:42.039 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.040 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:42.042 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:42.043 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:42.346 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.346 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:42.347 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.348 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:42.370 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:42.372 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.373 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.375 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:42.376 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:42.377 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.379 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:42.379 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
^C05:07:42.682 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.683 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:42.684 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.686 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:42.708 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:42.709 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:42.711 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.714 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:42.725 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:42.726 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:42.726 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:42.728 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:42.729 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:42.729 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:43.031 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.032 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:43.034 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.035 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:43.056 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:43.057 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.059 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.061 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:43.062 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:43.062 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.065 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:43.067 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:43.389 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.390 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:43.392 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.393 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:43.415 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:43.417 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.418 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.420 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [10 07 01 4B 08 55 36 10 07 01 4B 08 55 36]
05:07:43.432 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:43.432 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:43.434 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.434 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:43.435 DEBUG [-] n.c.l.c.EhlCommunicator - 🔄 Buffer contains additional STX bytes - will be processed on next iteration
05:07:43.435 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:43.738 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.738 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 01 4B 5C 36] -> STATE
05:07:43.739 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.741 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #1 | STATE (Give/take the calculator state) | Bytes: [10 06 01 4B 5C 36] | Checksum: 0x5C ✓
05:07:43.762 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=5C)
05:07:43.764 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 01 4B 5C 36
05:07:43.765 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.767 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=1, cmd=STATE(75), data=[08], chksum=55)
05:07:43.768 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 1
05:07:43.770 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 01 4B 08 55 36
05:07:43.771 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #1 | STATE (Give/take the calculator state) | State=0x08 bits=00001000 open_for_delivery=false startbutton=false automode=true error=false (PAYMENT_PENDING (Awaiting settlement)) | Bytes: [10 07 01 4B 08 55 36] | Checksum: 0x55 ✓
05:07:43.772 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 Matched: STATE addr=1
05:07:44.078 ERROR [OPERATOR] n.c.lpg.protocol - ❌ UNBLOCK FEILET: UNBLOCK: open_for_delivery bit not observed within 6s
05:07:44.079 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Release failed: UNBLOCK: open_for_delivery bit not observed within 6s
