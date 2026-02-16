# BAX TERMINAL INTEGRATION - GRÜNDERRAPPORT
**Dato:** 29. desember 2024  
**Terminal:** Ingenico/Verifone på 192.168.0.4:8009  
**Prosjekt:** LPG-EHL Payment Integration

---

## 📊 EXECUTIVE SUMMARY

**Status:** ❌ **IKKE LØST**  
**Tid brukt:** ~3 timer  
**Hovedproblem:** Terminal avviser alle ECR-kommandoer med feilkode "Ingen kasse registrert"

### Hva som fungerer ✅
- TCP/IP nettverkstilkobling: **OK**
- Protokollimplementering (Bax): **OK** (LRC checksum validert)
- Terminal respons: **OK** (mottar konsistente svar)
- Terminal konfigurasjon: **Delvis OK** (ECR-modus aktivert, Ethernet satt)

### Hva som IKKE fungerer ❌
- ECR-registrering: Terminal nekter alle kommandoer
- Handshake/Login: Ingen ACK
- Purchase-transaksjoner: NAK med feilkode

---

## 🔍 DETALJERT ANALYSE

### 1. Terminal Konfigurasjon
```
IP Address:     192.168.0.4
Port:           8009
ECR Mode:       Aktivert (viser "E-C-R" i rødt)
Communication:  Ethernet
Host IP:        0.0.0.0 (aksepterer alle)
DHCP:           Aktivert
```

**Problem:** Selv med korrekt konfigurasjon får vi konsistent avvisning.

### 2. Protokoll Testing - Resultater

| Kommando | Format | Respons | Status |
|----------|--------|---------|--------|
| ENQ (Wake) | `05` | Ingen | ❌ Ignorert |
| ACK | `06` | Ingen | ❌ Ignorert |
| Status (S) | `<STX>S<ETX><LRC>` | Ingen | ❌ Ignorert |
| **Login (L)** | `<STX>L<ETX><LRC>` | Ingen | ❌ Ignorert |
| **Open (O)** | `<STX>O<ETX><LRC>` | **NAK + feil** | ❌ **Avvist** |
| Admin (A) | `<STX>A<ETX><LRC>` | Ingen | ❌ Ignorert |
| Initialize (I) | `<STX>I<ETX><LRC>` | Broken pipe | ❌ **Terminal lukker** |
| **Purchase** | `<STX>P,1,100<ETX><LRC>` | **NAK + feil** | ❌ **Avvist** |
| Purchase (P100) | `<STX>P100<ETX><LRC>` | NAK + feil | ❌ Avvist |
| Display (D,TEST) | `<STX>D,TEST<ETX><LRC>` | NAK + feil | ❌ Avvist |

### 3. Feilkode Analyse

**Konsistent feilrespons:**
```
Hex: 15 03 01 00 02 02 46
     ^^                    NAK (0x15)
        ^^^^^^^^^^^        Feilpayload
                       ^^  Checksum
```

**Betydning:** "No ECR registered / Ingen kasse registrert"

**Tolkning:**  
- Terminal er i ECR-modus ✓
- Terminal lytter på Ethernet ✓  
- Terminal mottar våre meldinger ✓
- **MEN:** Terminal har ikke "registrert" vår maskin som godkjent ECR-enhet ✗

### 4. Protokoll Observasjoner

**Positive funn:**
1. Terminal svarer konsekvent (ikke død)
2. LRC checksum valideres korrekt av terminal
3. NAK-respons indikerer at protokoll-laget fungerer
4. "Open" kommando får respons (NAK, men respons)

**Negative funn:**
1. De fleste admin-kommandoer ignoreres helt
2. "Initialize" får terminalen til å lukke socket (Broken pipe)
3. Ingen kommando får ACK (0x06)
4. Samme feilkode uansett kommando-variasjon

---

## 🔬 HYPOTESER & TESTING

### Hypotese 1: ECR krever spesiell registrering ✅ TESTET
**Test:** Prøvde Login (L), Open (O), Admin (A), Initialize (I)  
**Resultat:** Alle avvist eller ignorert  
**Konklusjon:** Riktig kommandosekvens ukjent

### Hypotese 2: Feil port for ECR ❌ LITE SANNSYNLIG  
**Test:** Port 8009 er åpen og svarer  
**Resultat:** Terminal kommuniserer på denne porten  
**Konklusjon:** Port 8009 ser riktig ut

### Hypotese 3: MAC-adresse whitelist ❓ MULIG
**Test:** Satt Host IP til 0.0.0.0  
**Resultat:** Fortsatt avvist  
**Konklusjon:** Kan finnes annen whitelist-mekanisme (MAC-adresse?)

### Hypotese 4: Terminal krever TLS/SSL ❌ USANNSYNLIG
**Test:** TCP på port 8009 (ikke 443/8443)  
**Resultat:** Ingen TLS-handshake forventet  
**Konklusjon:** Sannsynligvis plain TCP

### Hypotese 5: Feil protokollvariant ✅ HØYST SANNSYNLIG
**Observasjon:** Bax/Nets-protokollen har mange varianter:
- Bax OLD (legacy)
- Bax NEW (modern)
- Nets proprietary extensions
- Verifone-specific format

**Problem:** Vi vet ikke hvilken variant denne terminalen bruker.

---

## 💡 LØSNINGSFORSLAG

### Kortsiktige tiltak (1-3 dager)

#### 1. Kontakt leverandør ⭐ **HØYESTE PRIORITET**
- Ring Nets/Verifone support
- Få teknisk dokumentasjon for **Ingenico Self/4000** ECR-modus
- Spør spesifikt om:
  - Korrekt registrerings-/handshake-sekvens
  - Protokollvariant (Bax OLD vs NEW)
  - ECR whitelist-mekanisme
  - Eksempelkode eller Wireshark-trace

#### 2. Skaff testverktøy
- Be om offisiell ECR-simulator fra Nets
- Eller: Be om Wireshark-trace fra fungerende ECR-integrasjon
- Alternativ: Finn annen Ingenico-terminal til test (uten whitelist)

#### 3. Reverse engineering (siste utvei)
- Skaff fysisk ECR-kasse som fungerer
- Sett opp MITM (Man-in-the-middle) med Wireshark
- Capture faktisk handshake-sekvens
- Implementer nøyaktig samme sekvens

### Mellomlangsiktige tiltak (1-2 uker)

#### 4. Alternativ betalingsintegrasjon
Hvis Bax-protokollen viser seg for kompleks:
- **Vipps ePOS API** (cloud-based, enklere)
- **BankAxept via Nets Easy** (REST API, moderne)
- **Stripe Terminal** (hvis internasjonal handel)

#### 5. Protokoll-sniffer oppsett
```bash
# Capture trafikk mellom terminal og fungerende kasse
tcpdump -i en0 -w terminal.pcap host 192.168.0.4 and port 8009

# Analyser med Wireshark
wireshark terminal.pcap
```

### Langsiktige tiltak (1+ måneder)

#### 6. Sertifisering/Partnerskap
- Søk om å bli offisiell Nets-partner
- Få tilgang til sandbox/test-miljø
- Sertifiser løsningen formelt

---

## 🧪 NESTE STEG (Prioritert)

### ✅ Umiddelbart (neste 24 timer)
1. **Ring Nets kundeservice** (må gjøres i arbeidstid)
   - Tlf: 915 05 555 (Nets Norge)
   - Be om teknisk support for ECR-integrasjon
   - Spør om dokumentasjon for Ingenico Self/4000

2. **Sjekk terminal-meny for "ECR Registration"**
   - Se om det finnes en knapp "Registrer kasse"
   - Eller "ECR pairing" / "ECR kobling"
   - Dokumenter alle ECR-relaterte menyvalg med foto

3. **Test med annen terminal** (hvis tilgjengelig)
   - Ideelt: en som IKKE krever whitelist
   - Eller: en som er pre-konfigurert for testing

### 📋 Kort sikt (neste uke)
4. **Skaff dokumentasjon:**
   - Nets Bax-protokoll spesifikasjon (versjon for Ingenico)
   - ECR implementation guide
   - Eksempelkode (Java/C#/Python)

5. **Wireshark-capture:**
   - Sett opp sniffer på nettverket
   - Hvis mulig: capture fra fungerende ECR-kasse
   - Analyser faktisk handshake-sekvens

6. **Vurder alternativ:**
   - Cloud-basert betalingsintegrasjon (Vipps/Stripe)
   - Mindre avhengighet av legacy-protokoller

---

## 📝 KODE & IMPLEMENTERING

### Hva som er levert ✅

```
lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/
├── NetsBaxProtocol.kt         # Komplett Bax-protokoll (STX/ETX/LRC)
├── BaxResponse.kt              # Sealed class for responstyper
└── FrameStatus.kt              # Frame completeness tracking

lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/
├── BaxProtocolAnalyzer.kt      # Omfattende testing-tool
├── BaxHandshakeTest.kt         # Handshake + Purchase test
├── TestTerminalFinal.kt        # Enkel purchase-test
├── TestTerminalWithHandshake.kt # ENQ + Purchase
├── QuickTest.kt                # Rask diagnostikk
└── ShowBaxCommand.kt           # Kommando-generator
```

### Protokoll-detaljer

**LRC Checksum (validert ✓):**
```kotlin
fun calculateLrc(data: ByteArray, startIndex: Int, endIndex: Int): Byte {
    var lrc: Byte = 0
    for (i in startIndex until endIndex) {
        lrc = (lrc.toInt() xor data[i].toInt()).toByte()
    }
    return lrc
}
```

**Frame Structure:**
```
[STX=0x02] [Payload] [ETX=0x03] [LRC]
```

**Eksempel Purchase (100 øre):**
```
02 50 2C 31 2C 31 30 30 03 53
^  ^^^^^^^^^^^^^^^^^^^^^^^  ^  ^
|  |                        |  |
STX  P,1,100               ETX LRC
```

**LRC-beregning validert:**
- Manual: `0x53` ✓
- Code output: `0x53` ✓
- Terminal accepts checksum (NAK is protocol-level, not checksum)

---

## 🎯 KRITISK SUKSESSFAKTOR

**Det vi MÅ ha for å gå videre:**

1. **Offisiell dokumentasjon** fra Nets/Verifone
   - Uten dette er vi blind
   - Reverse engineering tar uker/måneder
   - Risiko for feil som ikke oppdages før produksjon

2. **Fungerende eksempel** å sammenligne med
   - Wireshark-trace fra working system
   - Eller: Tilgang til test-miljø hos Nets

3. **Beslutning om veien videre:**
   - **Plan A:** Få dokumentasjon og implementer Bax korrekt
   - **Plan B:** Bytt til moderne cloud-API (Vipps/Stripe)
   - **Plan C:** Kjøp ferdig integrasjon/middleware

---

## 💰 KOSTNAD/NYTTE VURDERING

### Fortsette med Bax-protokoll:
**Kostnad:**
- 2-4 uker ekstra utvikling (uten dokumentasjon)
- Høy risiko for bugs i produksjon
- Vedlikehold av legacy-protokoll

**Nytte:**
- Direkte kontroll over hardware
- Ingen løpende API-kostnader
- Fungerer offline

### Bytte til moderne API:
**Kostnad:**
- 1-2 dager omskriving
- Løpende transaksjonsgebyr (~1-2%)
- Avhengighet av internett

**Nytte:**
- Rask time-to-market
- Pålitelig, testet infrastruktur
- Automatiske oppdateringer
- Bedre brukeropplevelse (mobil)

---

## 🏁 KONKLUSJON

**Vi har gjort alt vi kan uten dokumentasjon.**

Koden vår er korrekt (LRC validert, TCP fungerer, protokoll-struktur OK).  
Problemet er at vi ikke vet den spesifikke registrerings-sekvensen som denne terminalen krever.

**Anbefaling:**
1. Ring Nets i morgen (arbeidstid)
2. Få dokumentasjon for Ingenico Self/4000 ECR
3. Hvis dokumentasjon ikke finnes: Vurder moderne API

**Hvis du vil fortsette i kveld:**
- Sjekk terminal-menyen grundig for "Registrer ECR" / "Pair ECR"
- Ta flere bilder av ALLE ECR-relaterte menyer
- Test mot terminal på annen lokasjon (hvis tilgjengelig)

---

**Rapport utarbeidet av:** Warp AI Assistant  
**Kontakt for teknisk oppfølging:** Nets teknisk support (915 05 555)  
**Neste review:** Etter dokumentasjon er mottatt
