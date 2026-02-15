Dette er et veldig viktig veivalg. Jeg svarer først som arkitekt, så som JVM-optimaliserer.

---

## DEL 1 – Arkitekturvurdering (Azure → Ark-3360)

### Det du foreslår
- Ark-maskinen eksponerer ikke en offentlig API.
- Ark poller Azure.
- Azure setter et flagg for:
  - stasjon
  - kunde
  - kredittramme
- Ark får respons → frigjør pumpe.
- Ingen direkte toveis sanntidskommunikasjon fra Azure til Ark.
- Ark håndhever lokal kreditttelling under fylling.

### Min vurdering: Ja – dette er riktig strategi
For 50 stasjoner, begrenset tid og sikkerhetskrav er dette:

- ✔️ Pragmatisk
- ✔️ Sikkerhetsmessig enklere
- ✔️ Driftbart
- ✔️ Mindre nettverkskompleksitet

### Hvorfor det er riktig

#### 1) Du unngår inbound connections
- Ingen portåpning på stasjon
- Ingen WebSocket over internett
- Ingen reverse tunnels
- Ingen VPN-avhengighet

Det betyr:
- Mindre angrepsflate
- Mindre NAT-problemer
- Mindre support

#### 2) Polling er helt OK her
Du har:
- Lav trafikk
- En kunde om gangen
- Ikke sanntidsbørs

Polling hvert 2.–5. sekund er mer enn godt nok.

Det er ikke en trading-plattform.

#### 3) Kreditt må håndheves lokalt
Dette er viktig:

Ark må:
- Låse kreditten ved start
- Telle volum under fylling
- Stoppe pumpe når kreditten er brukt

Azure skal ikke sitte og regne under fylling.
Hvis nettet faller ut må Ark fortsatt kunne stoppe pumpa.

Dette er helt riktig separasjon av ansvar.

#### 4) Sikkerhetsmessig riktig modell
Beste praksis her:

Ark:
- Leser kun autorisasjonsdata
- Sender kun ferdige transaksjoner tilbake
- Har ingen direkte brukerinput fra internett

Azure:
- Er autoritativ på kreditt
- Logger og bokfører

Dette er robust.

### Er dette over-engineering?
Nei.

Det ville vært over-engineering hvis:
- Du kjørte Kubernetes på ARK
- Du hadde 10 microservices
- Du krevde event-driven Azure Service Bus

Men det gjør du ikke.

Du har:
- Ett webapp-JAR
- En H2 file DB
- Serialport
- En HTTP-klient mot Azure

Dette er faktisk ganske lean.

---

## DEL 2 – AR-3360 tuning (2 GB RAM, 1 GB swap)

Du har observert:
- CPU går i 100% under oppstart
- Etterpå stabil

Dette er helt normalt for Spring Boot + classpath scanning + Jackson + Hibernate init.

Vi kan redusere dette betydelig.

---

## DEL 3 – Hva du bør disable i Spring Boot

Dette er spesifikt tilpasset:
- Debian 64-bit
- Java 21
- 2GB RAM
- H2 file
- Webapp (React statiske filer)
- Serialport
- Azure polling

### 1) Deaktiver ting du ikke bruker

`application-field.yaml`

```yaml
spring:
  main:
    banner-mode: "off"
    lazy-initialization: true
  jmx:
    enabled: false
  devtools:
    restart:
      enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
      - org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
```

Hvis du ikke bruker Spring Security internt på ARK → disable den.

Den initialiserer mye.

### 2) Hvis du ikke bruker Actuator – fjern dependency

I `build.gradle`:

Fjern:

```
spring-boot-starter-actuator
```

Hvis den må være der:

```yaml
management:
  endpoints:
    enabled-by-default: false
```

### 3) Reduser Hikari pool kraftig

Du trenger ikke default 10 connections.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 2
      minimum-idle: 1
      idle-timeout: 60000
      max-lifetime: 300000
```

Du har:
- Én operatør
- Lav DB-bruk
- H2 file

Pool på 2 er mer enn nok.

### 4) Hibernate optimalisering

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
        jdbc:
          batch_size: 0
```

Viktig:
- `open-in-view: false`
- Unngå unødvendig flush
- Ingen batching nødvendig

### 5) Slå av unødvendig logging

I produksjon:

```yaml
logging:
  level:
    root: INFO
    org.springframework: WARN
    org.hibernate: WARN
```

Debug logging under serial polling vil spise CPU.

### 6) Begrens Tomcat/Undertow threads

Hvis du bruker embedded Tomcat:

```yaml
server:
  tomcat:
    threads:
      max: 10
      min-spare: 2
```

Du trenger ikke 200 threads.

### 7) Reduser polling aggressivitet

Du har nå:

```yaml
lpg:
  polling:
    interval-ms: 2000
```

2 sekunder er OK.

Ikke gå under 1000 ms.
Det er unødvendig CPU-belastning.

### 8) JVM-optimalisering for ARK

Din nåværende:

```
-Xms512m -Xmx512m
```

Bra.

Jeg ville lagt til:

```
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:+AlwaysPreTouch
-XX:+UseStringDeduplication
```

`AlwaysPreTouch` gjør at minnet allokeres ved start (litt lengre startup, men jevnere runtime).

---

## DEL 4 – H2 stabilitet

Viktig for pumpeautorisasjon:

I H2 URL:

```
jdbc:h2:file:/home/thomas/data/lpgdb;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE
```

Og:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        connection.provider_disables_autocommit: true
```

Sørg for at:
- Alle service-metoder som skriver transaksjon er `@Transactional`
- Du ikke holder transaksjoner åpne under polling

---

## DEL 5 – Sikkerhet (Viktig)

Når Ark poller Azure:
- ✔️ Bruk HTTPS
- ✔️ Bruk API-key eller JWT
- ✔️ Ikke lagre kundekreditt permanent
- ✔️ Lagre bare aktiv sesjon

Hvis Ark kompromitteres:
- Ingen sensitiv database skal kunne tappes.

---

## DEL 6 – Instruksjon du kan gi Cursor / IntelliJ AI

Her er klar prompt:

---

### Oppgave: Optimaliser Spring Boot konfig for AR-3360 (2GB RAM)

**Mål:**
- Redusere CPU under startup
- Redusere unødvendige threads
- Redusere connection pool
- Disable ubrukte auto-configurations
- Ikke endre business logic

**Endringer:**

1. Legg til lazy initialization:
   `spring.main.lazy-initialization=true`

2. Disable JMX:
   `spring.jmx.enabled=false`

3. Disable Spring Security auto config hvis ikke eksplisitt brukt.

4. Reduser Hikari pool:
   `maximum-pool-size=2`
   `minimum-idle=1`

5. Set:
   `spring.jpa.open-in-view=false`

6. Reduser embedded server threads til maks 10.

7. Fjern Actuator dependency hvis ikke brukt.

8. Sørg for at alle service-metoder som skriver pumpeautorisasjon er `@Transactional`.

9. Bekreft at ingen polling loop holder DB-transaksjon åpen.

10. Legg inn production logging profile:
    `root=INFO`
    `org.springframework=WARN`
    `org.hibernate=WARN`

**Returner:**
- Diff av `application-field.yaml`
- Diff av `build.gradle`
- Liste over fjernede autoconfigs

---

## Endelig vurdering

Du har ikke over-engineeret.

Du har:
- Forretningslogikk separert fra transport
- Ekstern konfig for stasjonsadresse (helt riktig)
- Lokal autorisasjonskontroll
- Azure som autoritativ backend
- Simulator for felt-test
- H2 file for lav footprint

Det er faktisk en veldig moden løsning.

Det eneste du må være ekstremt disiplinert på nå er:
- Footprint
- Logging
- Polling
- Thread counts

Hvis du vil, kan jeg lage en konkret AR-3360 “production checklist” du kan gi Alejandro før neste feltinstallasjon.