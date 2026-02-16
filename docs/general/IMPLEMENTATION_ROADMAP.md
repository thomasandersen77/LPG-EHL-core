# Implementation Roadmap - LPG EHL Missing Functionality

Basert på `product_all_functoins.md` og `add_more_functions.md`.

## Status

✅ OpenAPI specification laget: `lpg-ehl-api/src/main/resources/openapi.yaml`
✅ Payment API frontend allerede eksisterer: `lpg-web/src/api/payments.ts`
⚠️ Merge conflict i DispenserSimulator.tsx - må løses manuelt

## Backend - Kotlin/Spring Boot

### 1. Payment Gateway (Prioritet: HØY)

**Lokasjon**: `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/payment/`

**Filer å lage**:
- `PaymentGateway.kt` - Interface + data classes (PaymentMethod, PaymentStatus, PaymentRequest, Payment)
- `SimulatedPaymentGateway.kt` - @Service @Profile("local", "dev") implementasjon
- `PaymentController.kt` - @RestController for POST /api/v1/payments og GET /api/v1/payments/{id}

**Nøkkelfunksjonalitet**:
- CASH: instant APPROVED
- CARD/CREDIT: PENDING → auto-resolve til APPROVED etter 2 sekunder (simulert)
- Metadata støtte for `simulateDecline=true` for testing

### 2. Emulator Service (Prioritet: HØY)

**Lokasjon**: `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/emulator/`

**Filer å lage**:
- `EmulatorService.kt` - @Service for scenario-styring
- `EmulatorController.kt` - @RestController for /api/v1/emulator/*

**Scenarier**:
- NORMAL: Vanlig respons
- TIMEOUT: Ingen respons (simuler timeout)
- CHECKSUM_ERROR: Send feil checksum
- NO_CONNECTION: Simuler koblingsfeil

**API**:
- POST /emulator/scenario - Sett scenario per dispenser
- GET /emulator/status/{address} - Hent status
- POST /emulator/reset/{address} - Nullstill

### 3. Transaction Endpoints (Prioritet: MEDIUM)

**Lokasjon**: `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/TransactionController.kt`

**Endepunkter å legge til**:
- GET /api/v1/transactions - Med filtering (from, to, paymentType, customerId) og paginering
- GET /api/v1/transactions/{id} - Enkelt transaksjon
- GET /api/v1/transactions/count - Antall transaksjoner

**Implementasjon**:
- Bruk Spring Data JPA Specification for dynamisk filtering
- Returner Page<Transaction> for paginering
- Mapping til DTO med alle felt fra OpenAPI spec

### 4. Credit Account Endpoints (Prioritet: MEDIUM)

**Lokasjon**: 
- `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/credit/`
- Database: Nye tabeller i `init-db.sql`

**Entiteter**:
```kotlin
@Entity
data class Customer(
    @Id val id: UUID,
    val customerNumber: String,
    val name: String,
    val createdAt: Instant
)

@Entity
data class CreditAccount(
    @Id val id: UUID,
    @ManyToOne val customer: Customer,
    val balanceNok: Double,
    val lastActivityAt: Instant?
)
```

**Controller**:
- GET /api/v1/credit/accounts
- POST /api/v1/credit/accounts
- GET /api/v1/credit/accounts/{id}
- GET /api/v1/credit/accounts/{id}/transactions

### 5. Reports Endpoints (Prioritet: LAV)

**Lokasjon**: `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/ReportController.kt`

**Endepunkter**:
- GET /api/v1/reports/daily?date=YYYY-MM-DD
- GET /api/v1/reports/range?from=YYYY-MM-DD&to=YYYY-MM-DD

**Implementasjon**:
- Aggreger fra transactions table
- Group by paymentType
- Returner DailyReport / RangeReport DTOer

### 6. Sync Endpoints (Prioritet: LAV)

**Lokasjon**: `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/SyncController.kt`

**Endepunkter**:
- GET /api/v1/sync/status - Vis antall usync'ed, sist sync tid
- POST /api/v1/sync/trigger - Manuell re-sync

## Frontend - React/TypeScript

### 1. Installer React Router

```bash
cd lpg-web
npm install react-router-dom
npm install date-fns  # For datoformatering
```

### 2. App Structure med Routing

**Oppdater**: `lpg-web/src/App.tsx`

```typescript
import { BrowserRouter, Routes, Route } from 'react-router-dom';

// Importer sider
import { HomePage } from './pages/HomePage';
import { DispenserSimulator } from './components/DispenserSimulator';
import { TransactionsPage } from './pages/TransactionsPage';
import { CreditAccountsPage } from './pages/CreditAccountsPage';
import { ReportsPage } from './pages/ReportsPage';
import { EmulatorDebugPage } from './pages/EmulatorDebugPage';
import { Layout } from './components/Layout';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="simulator" element={<DispenserSimulator />} />
          <Route path="transactions" element={<TransactionsPage />} />
          <Route path="credit" element={<CreditAccountsPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="emulator-debug" element={<EmulatorDebugPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
```

### 3. Layout Component (Navigation)

**Ny fil**: `lpg-web/src/components/Layout.tsx`

Lag en navbar med linker til alle sider:
- Hjem
- Pumpe Simulator
- Transaksjoner
- Stasjonskreditt
- Rapporter
- Emulator Debug (kun i dev)

### 4. Sider å lage

#### TransactionsPage.tsx
- Filter: dato fra/til, betalingstype, kunde
- Tabell: tid, dispenser, liter, beløp, pris/l, betaling, kunde, vegavgift
- Paginering

#### CreditAccountsPage.tsx
- Venstre: Liste kredittkonti med saldo
- Høyre: Transaksjoner for valgt konto

#### ReportsPage.tsx
- Velg dato
- Vis daglig rapport: totaler per betalingstype
- Ev. eksporter CSV

#### EmulatorDebugPage.tsx
- Dropdown: Velg scenario per dispenser
- Live logg: lastMessage, lastError
- Reset-knapp

### 5. API Clients

**Ny fil**: `lpg-web/src/api/transactions.ts`

```typescript
export interface TransactionDto {
  id: string;
  dispenserAddress: number;
  startedAt: string;
  finishedAt: string | null;
  litres: number;
  amountNok: number;
  pricePerLitreNok: number;
  paymentType: 'CASH' | 'CARD' | 'CREDIT';
  customerName?: string;
  includesRoadTax: boolean;
}

export async function fetchTransactions(filter: TransactionFilter) {
  const res = await axios.get<{content: TransactionDto[]}>('/api/v1/transactions', { params: filter });
  return res.data;
}
```

**Ny fil**: `lpg-web/src/api/credit.ts`

```typescript
export interface CreditAccountDto {
  id: string;
  customerName: string;
  customerNumber: string;
  balanceNok: number;
  lastActivityAt?: string;
}

export async function fetchCreditAccounts() {
  const res = await axios.get<CreditAccountDto[]>('/api/v1/credit/accounts');
  return res.data;
}
```

**Ny fil**: `lpg-web/src/api/emulator.ts`

```typescript
export type EmulatorScenario = 'NORMAL' | 'TIMEOUT' | 'CHECKSUM_ERROR' | 'NO_CONNECTION';

export async function setEmulatorScenario(address: number, scenario: EmulatorScenario) {
  await axios.post('/api/v1/emulator/scenario', { dispenserAddress: address, scenario });
}
```

### 6. HomePage oppdatering

Endre nåværende "landing page" i App.tsx til HomePage.tsx og legg til navigasjonsknapper til:
- Pumpe Simulator
- Transaksjoner
- Stasjonskreditt
- Rapporter  
- Emulator Debug

## Prioritering

### Sprint 1 (Kjernefunksjonalitet):
1. ✅ OpenAPI spec
2. Løs merge conflict i DispenserSimulator
3. Backend: PaymentGateway + SimulatedPaymentGateway
4. Backend: EmulatorService + EmulatorController
5. Frontend: React Router setup
6. Frontend: Layout med navigation
7. Test payment flow i simulator

### Sprint 2 (Transaksj oner og Kreditt):
1. Backend: TransactionController med filtering
2. Frontend: TransactionsPage
3. Backend: CreditAccount entities + controller
4. Frontend: CreditAccountsPage
5. Database migrations for credit tables

### Sprint 3 (Rapporter og Debug):
1. Backend: ReportController
2. Frontend: ReportsPage
3. Frontend: EmulatorDebugPage
4. Backend: SyncController
5. Polering og testing

## Neste Steg

1. Kjør `git status` for å se merge conflicts
2. Løs DispenserSimulator.tsx merge conflict manuelt
3. Start med Payment Gateway implementasjon (se `add_more_functions.md` linje 366-546)
4. Test payment flow med frontend
5. Fortsett med emulator service
6. Bygg frontend sider én om gangen

## Ressurser

- OpenAPI spec: `lpg-ehl-api/src/main/resources/openapi.yaml`
- Detaljert backend-kode: Se `add_more_functions.md` linje 366-675
- Frontend-komponenter: Se `add_more_functions.md` linje 682-1092
- Legacy VB6-kode: `/Users/tandersen/git/NorgesGass/lpg-ehl/legacy/norgesgass_legacy/`

## Testing

For hver feature:
1. Test API med Swagger UI (http://localhost:8080/swagger-ui.html)
2. Test frontend med browser devtools
3. Verifiser data i PostgreSQL
4. Test emulator scenarios
5. Verifiser Azure sync queue

## Database Migrations

Legg til i `init-db.sql` eller lag nye Liquibase changesets:

```sql
-- Credit accounts
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    customer_number VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE credit_accounts (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customers(id),
    balance_nok DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_credit_customer ON credit_accounts(customer_id);
```
