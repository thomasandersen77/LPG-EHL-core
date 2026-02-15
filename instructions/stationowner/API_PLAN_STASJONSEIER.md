# API Plan for Stasjonseier Grensesnitt

**Dato:** 13. februar 2026  
**Versjon:** 1.0  
**Status:** Utkast

## Oversikt

Dette dokumentet beskriver API-endringer og nye endepunkter som må implementeres for å støtte funksjonaliteten i det redesignede stasjonseier-grensesnittet (StationOwnerPage).

## Innholdsfortegnelse

1. [Eksisterende API-er som kan gjenbrukes](#eksisterende-api-er-som-kan-gjenbrukes)
2. [Nye API-endepunkter som må utvikles](#nye-api-endepunkter-som-må-utvikles)
3. [Detaljert API-spesifikasjon](#detaljert-api-spesifikasjon)
4. [Database-endringer](#database-endringer)
5. [Implementasjonsrekkefølge](#implementasjonsrekkefølge)

---

## 1. Eksisterende API-er som kan gjenbrukes

### Allerede implementert og fungerer:

| Funksjon | Endepunkt | Status |
|----------|-----------|--------|
| Pump status | `GET /api/v1/emulator/pump/{address}/status` | ✅ Fungerer |
| Frigjør dispenser | `POST /api/v1/emulator/pump/{address}/release` | ✅ Fungerer |
| Start pumping | `POST /api/v1/emulator/pump/{address}/start-pumping` | ✅ Fungerer |
| Stopp pumping | `POST /api/v1/emulator/pump/{address}/block` | ✅ Fungerer |
| Bekreft betaling | `POST /api/v1/emulator/settle/{address}` | ✅ Fungerer |
| Hent transaksjoner | `GET /api/v1/transactions` | ✅ Fungerer |
| Hent prisinfo | `GET /api/v1/prices` | ✅ Fungerer |
| Oppdater pris | `POST /api/v1/prices/update` | ✅ Fungerer |
| Perioderapport | `GET /api/v1/reports/period` | ✅ Fungerer |
| Dagsrapport | `GET /api/v1/reports/daily` | ✅ Fungerer |

---

## 2. Nye API-endepunkter som må utvikles

### 2.1 Kvitteringer og PDF-generering

| Funksjon | Endepunkt | Prioritet |
|----------|-----------|-----------|
| Hent kvitteringer for periode | `GET /api/v1/receipts` | **Høy** |
| Last ned kvittering som PDF | `GET /api/v1/receipts/{transactionId}/pdf` | **Høy** |

### 2.2 Prisadministrasjon utvidet

| Funksjon | Endepunkt | Prioritet |
|----------|-----------|-----------|
| Hent priser med/uten avgift | `GET /api/v1/prices/station/{stationId}` | **Høy** |
| Oppdater begge priser | `POST /api/v1/prices/station/{stationId}` | **Høy** |

### 2.3 Veibruksavgift-rapportering

| Funksjon | Endepunkt | Prioritet |
|----------|-----------|-----------|
| Hent veibruksavgift-rapport | `GET /api/v1/reports/road-tax` | **Middels** |

### 2.4 Kundeadministrasjon

| Funksjon | Endepunkt | Prioritet |
|----------|-----------|-----------|
| Hent alle kunder for stasjon | `GET /api/v1/customers` | **Middels** |
| Hent enkelt kunde | `GET /api/v1/customers/{customerId}` | **Middels** |
| Opprett kunde | `POST /api/v1/customers` | **Lav** |
| Oppdater kunde | `PUT /api/v1/customers/{customerId}` | **Lav** |

### 2.5 Klippekort

| Funksjon | Endepunkt | Prioritet |
|----------|-----------|-----------|
| Hent klippekort-innstillinger | `GET /api/v1/klippekort/settings` | **Lav** |
| Oppdater klippekort-innstillinger | `PUT /api/v1/klippekort/settings` | **Lav** |
| Hent kundens klippekort-status | `GET /api/v1/customers/{customerId}/klippekort` | **Lav** |
| Registrer klipp | `POST /api/v1/customers/{customerId}/klippekort/clip` | **Lav** |

### 2.6 Stasjonsinfo

| Funksjon | Endepunkt | Prioritet |
|----------|-----------|-----------|
| Hent stasjonsinformasjon | `GET /api/v1/station` | **Høy** |
| Oppdater stasjonsinformasjon | `PUT /api/v1/station` | **Lav** |

---

## 3. Detaljert API-spesifikasjon

### 3.1 Kvitteringer API

#### `GET /api/v1/receipts`

Henter liste over kvitteringer for en periode.

**Query Parameters:**
```yaml
from: 
  type: string
  format: date
  required: true
  description: Startdato (YYYY-MM-DD)
to:
  type: string
  format: date
  required: true
  description: Sluttdato (YYYY-MM-DD)
page:
  type: integer
  default: 0
size:
  type: integer
  default: 50
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "transactionId": "uuid",
      "date": "2026-02-13",
      "time": "14:30",
      "volumeLiters": 25.5,
      "amountKr": 458.97,
      "pricePerLiter": 17.99,
      "paymentMethod": "CARD",
      "includesRoadTax": true,
      "customerName": "Ola Nordmann",
      "customerId": "uuid-or-null"
    }
  ],
  "totalElements": 150,
  "totalPages": 3,
  "currentPage": 0
}
```

#### `GET /api/v1/receipts/{transactionId}/pdf`

Genererer eller henter ferdig PDF-kvittering.

**Response (200 OK):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="kvittering-{transactionId}.pdf"`

**PDF må inneholde:**
- Stasjons-ID (S001, S002, etc.)
- Stasjonsnavn
- Juridisk navn
- Organisasjonsnummer (MVA)
- Adresse
- Transaksjonsdetaljer
- Dato og tid
- Volum og beløp
- Avgiftsinformasjon

---

### 3.2 Utvidet Pris API

#### `GET /api/v1/prices/station/{stationId}`

**Response (200 OK):**
```json
{
  "stationId": "S001",
  "priceWithRoadTax": 17.99,
  "priceWithoutRoadTax": 11.50,
  "roadTaxPerLiter": 2.00,
  "vatRate": 0.25,
  "currency": "NOK",
  "lastUpdated": "2026-02-13T10:30:00Z"
}
```

#### `POST /api/v1/prices/station/{stationId}`

**Request Body:**
```json
{
  "priceWithRoadTax": 18.50,
  "priceWithoutRoadTax": 12.00
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Priser oppdatert",
  "newPrices": {
    "priceWithRoadTax": 18.50,
    "priceWithoutRoadTax": 12.00
  }
}
```

---

### 3.3 Veibruksavgift-rapport API

#### `GET /api/v1/reports/road-tax`

**Query Parameters:**
```yaml
from:
  type: string
  format: date
  required: true
to:
  type: string
  format: date
  required: true
```

**Response (200 OK):**
```json
{
  "fromDate": "2026-02-01",
  "toDate": "2026-02-13",
  "totalVolumeWithTax": 1250.5,
  "totalVolumeWithoutTax": 320.0,
  "totalRoadTaxAmount": 2501.00,
  "roadTaxRatePerLiter": 2.00,
  "transactionCount": 47,
  "breakdown": [
    {
      "date": "2026-02-01",
      "volumeWithTax": 125.5,
      "volumeWithoutTax": 32.0,
      "taxAmount": 251.00
    }
  ]
}
```

---

### 3.4 Kunde API

#### `GET /api/v1/customers`

**Query Parameters:**
```yaml
type:
  type: string
  enum: [PRIVATE, BUSINESS, ALL]
  default: ALL
search:
  type: string
  description: Søk i navn, e-post, telefon
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "customerId": "uuid",
      "name": "Ola Nordmann",
      "companyName": null,
      "type": "PRIVATE",
      "email": "ola@example.com",
      "phone": "99887766",
      "klippekort": {
        "currentClips": 3,
        "targetClips": 6
      },
      "credit": null,
      "allowedTaxExempt": false,
      "createdAt": "2025-06-15T10:00:00Z"
    },
    {
      "customerId": "uuid",
      "name": "Per Hansen",
      "companyName": "Norsk Transport AS",
      "type": "BUSINESS",
      "email": "per@norsktransport.no",
      "phone": "22334455",
      "klippekort": null,
      "credit": {
        "limitKr": 50000,
        "usedKr": 12500,
        "availableKr": 37500
      },
      "allowedTaxExempt": true,
      "createdAt": "2024-03-10T08:00:00Z"
    }
  ]
}
```

---

### 3.5 Klippekort API

#### `GET /api/v1/klippekort/settings`

**Response (200 OK):**
```json
{
  "enabled": true,
  "clipsRequired": 6,
  "rewardDescription": "1 gratis propanflaske",
  "appliesToProducts": ["PROPANE_BOTTLE"],
  "excludedProducts": ["LPG_DISPENSER"]
}
```

#### `PUT /api/v1/klippekort/settings`

**Request Body:**
```json
{
  "enabled": true,
  "clipsRequired": 5
}
```

---

### 3.6 Stasjon API

#### `GET /api/v1/station`

**Response (200 OK):**
```json
{
  "stationId": "S001",
  "name": "NorgesGass Demo Stasjon",
  "legalName": "NorgesGass AS",
  "organizationNumber": "123456789MVA",
  "address": {
    "street": "Eksempelveien 1",
    "postalCode": "0123",
    "city": "Oslo",
    "country": "Norge"
  },
  "contact": {
    "email": "stasjon@norgesgass.no",
    "phone": "22334455"
  },
  "dispensers": [
    {
      "address": 1,
      "name": "Pumpe 1",
      "status": "ONLINE"
    }
  ]
}
```

---

## 4. Database-endringer

### Nye tabeller som må opprettes:

#### `customers`
```sql
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    station_id VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    customer_type VARCHAR(20) NOT NULL, -- PRIVATE, BUSINESS
    email VARCHAR(255),
    phone VARCHAR(50),
    allowed_tax_exempt BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `customer_credit`
```sql
CREATE TABLE customer_credit (
    credit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(customer_id),
    credit_limit_kr DECIMAL(12,2) NOT NULL,
    used_kr DECIMAL(12,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `klippekort`
```sql
CREATE TABLE klippekort (
    klippekort_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(customer_id),
    current_clips INT DEFAULT 0,
    last_clip_date DATE,
    rewards_claimed INT DEFAULT 0
);
```

#### `klippekort_settings`
```sql
CREATE TABLE klippekort_settings (
    station_id VARCHAR(10) PRIMARY KEY,
    enabled BOOLEAN DEFAULT TRUE,
    clips_required INT DEFAULT 6,
    reward_description VARCHAR(255) DEFAULT '1 gratis propanflaske'
);
```

#### `station`
```sql
CREATE TABLE station (
    station_id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    organization_number VARCHAR(20),
    street_address VARCHAR(255),
    postal_code VARCHAR(10),
    city VARCHAR(100),
    country VARCHAR(50) DEFAULT 'Norge',
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50)
);
```

### Endringer i eksisterende tabeller:

#### `transactions` - Legg til:
```sql
ALTER TABLE transactions ADD COLUMN customer_id UUID REFERENCES customers(customer_id);
ALTER TABLE transactions ADD COLUMN includes_road_tax BOOLEAN DEFAULT TRUE;
```

#### `price_history` - Legg til:
```sql
ALTER TABLE price_history ADD COLUMN price_without_road_tax DECIMAL(10,2);
```

---

## 5. Implementasjonsrekkefølge

### Fase 1: Grunnleggende funksjonalitet (Prioritet HØY)

1. **Stasjon API**
   - `GET /api/v1/station` - Hent stasjonsinformasjon
   - Opprett `station`-tabell med seed-data

2. **Utvidet Pris API**
   - `GET /api/v1/prices/station/{stationId}`
   - `POST /api/v1/prices/station/{stationId}`
   - Støtte for både pris med og uten veibruksavgift

3. **Kvitteringer API**
   - `GET /api/v1/receipts` - Hent kvitteringsliste
   - `GET /api/v1/receipts/{transactionId}/pdf` - PDF-generering
   - Bruk iText eller Apache PDFBox for PDF-generering

### Fase 2: Rapportering (Prioritet MIDDELS)

4. **Veibruksavgift-rapport**
   - `GET /api/v1/reports/road-tax`
   - Aggregering basert på `includes_road_tax`-felt

### Fase 3: Kundeadministrasjon (Prioritet MIDDELS/LAV)

5. **Kunde API**
   - Opprett `customers`-tabell
   - CRUD-operasjoner for kunder
   - Koble transaksjoner til kunder

6. **Kreditt API**
   - Opprett `customer_credit`-tabell
   - Integrer med betalingsflyt

### Fase 4: Lojalitetsprogram (Prioritet LAV)

7. **Klippekort API**
   - Opprett klippekort-tabeller
   - Innstillinger per stasjon
   - Automatisk klipp-registrering

---

## 6. Frontend-integrasjon

For hvert nytt API må følgende gjøres i `lpg-web`:

1. **Opprett API-klient** i `src/api/`:
   ```typescript
   // src/api/receipts.ts
   export async function fetchReceipts(from: string, to: string) { ... }
   export async function downloadReceiptPdf(transactionId: string) { ... }
   ```

2. **Oppdater StationOwnerPage** til å bruke ekte API:
   - Erstatt mock-data med API-kall
   - Legg til loading states
   - Håndter feil

3. **Legg til React Query hooks** for caching og refetching

---

## 7. Estimert arbeid

| Fase | Estimat | Avhengigheter |
|------|---------|---------------|
| Fase 1 | 2-3 dager | Ingen |
| Fase 2 | 1 dag | Fase 1 |
| Fase 3 | 3-4 dager | Fase 1 |
| Fase 4 | 2 dager | Fase 3 |

**Totalt:** 8-10 dager utviklingstid

---

## 8. Tekniske notater

### PDF-generering
- Anbefalt bibliotek: **iText** eller **Apache PDFBox**
- PDFer skal genereres on-demand, ikke lagres
- Må støtte norske tegn (UTF-8)

### Sikkerhet
- Alle nye endepunkter skal kreve autentisering
- Stasjonseier skal kun se data for sin egen stasjon
- Rate limiting på PDF-generering

### Testing
- Opprett integrasjonstester for alle nye endepunkter
- Mock-data skal fortsatt fungere når API ikke er tilgjengelig

---

## Vedlegg: OpenAPI-tillegg

Se separat fil `openapi-stationowner-extension.yaml` for full OpenAPI 3.0-spesifikasjon av nye endepunkter.
