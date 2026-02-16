# Gjenskape Dispenserkontroll-grensesnitt

**Dato:** 9. februar 2026  
**Formål:** Komplett guide for å gjenskape dispenserkontroll-grensesnittet i et nytt repo

---

## 📋 Innholdsfortegnelse

1. [Oversikt](#oversikt)
2. [Forutsetninger](#forutsetninger)
3. [Filstruktur](#filstruktur)
4. [Steg-for-steg Implementering](#steg-for-steg-implementering)
5. [Fullstendig Kode](#fullstendig-kode)
6. [Testing](#testing)
7. [Feilsøking](#feilsøking)

---

## Oversikt

Dette grensesnittet består av:
- **StationOwnerPage.jsx** - Hovedkomponent for dispenserkontroll
- **DispenserControl.css** - Mørk tema styling
- **TransactionsPage.jsx** - Separat side for transaksjoner (flyttet fra StationOwnerPage)
- **api.js** - Utvidet med dispenserkontroll API-funksjoner
- **DispenserController.kt** - Backend mock API (Kotlin/Spring Boot)
- **App.jsx** - Oppdatert routing

### Funksjoner
- ✅ To-panel layout (venstre meny, høyre kontroll)
- ✅ Mørk tema (blå/lilla gradient)
- ✅ Status-indikator (ONLINE/OFFLINE)
- ✅ START/STOPP knapper
- ✅ Sanntids fylling-simulering
- ✅ Prisadministrasjon
- ✅ Responsivt design

---

## Forutsetninger

### Frontend
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.x.x"
  }
}
```

### Backend (Kotlin/Spring Boot)
- Spring Boot 3.2.0+
- Kotlin 2.1.10+
- Java 21
- Spring Web
- Spring Data JPA
- PostgreSQL

---

## Filstruktur

```
your-repo/
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   │   ├── StationOwnerPage.jsx          ← NY
│   │   │   └── TransactionsPage.jsx          ← NY
│   │   ├── styles/
│   │   │   ├── DispenserControl.css          ← NY
│   │   │   └── TransactionsPage.css          ← NY (kopi av eksisterende)
│   │   ├── services/
│   │   │   └── api.js                        ← OPPDATER
│   │   ├── components/
│   │   │   └── common/
│   │   │       └── Layout.jsx                ← EKSISTERENDE
│   │   └── App.jsx                           ← OPPDATER
│   └── package.json
└── backend/
    └── src/main/kotlin/com/yourpackage/
        └── controller/
            └── DispenserController.kt        ← NY
```

---

## Steg-for-steg Implementering

### Steg 1: Installer dependencies (hvis nødvendig)

```bash
cd frontend
npm install react-router-dom
```

### Steg 2: Opprett nye filer

Opprett følgende filer i riktig mappe:

#### Frontend
1. `frontend/src/pages/StationOwnerPage.jsx`
2. `frontend/src/pages/TransactionsPage.jsx`
3. `frontend/src/styles/DispenserControl.css`
4. `frontend/src/styles/TransactionsPage.css` (kopi av eksisterende StationOwnerPage.css)

#### Backend
1. `backend/src/main/kotlin/com/yourpackage/controller/DispenserController.kt`

### Steg 3: Kopier kode

Se [Fullstendig Kode](#fullstendig-kode) seksjonen nedenfor for komplett kode for hver fil.

### Steg 4: Oppdater eksisterende filer

#### 4.1 Oppdater `api.js`

Legg til dispensers API-funksjoner i slutten av filen (se [api.js kode](#apijes)).

#### 4.2 Oppdater `App.jsx`

Legg til import og route for TransactionsPage (se [App.jsx kode](#appjsx)).

### Steg 5: Test implementeringen

Se [Testing](#testing) seksjonen.

---

## Fullstendig Kode

### 1. StationOwnerPage.jsx

**Filplassering:** `frontend/src/pages/StationOwnerPage.jsx`

```jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Layout from '../components/common/Layout';
import { dispensers } from '../services/api';
import '../styles/DispenserControl.css';

export default function StationOwnerPage() {
  // State management
  const [selectedDispenser, setSelectedDispenser] = useState(null);
  const [dispenserStatus, setDispenserStatus] = useState('OFFLINE');
  const [isOnline, setIsOnline] = useState(false);
  const [activeFilling, setActiveFilling] = useState(null);
  const [pricePerKg, setPricePerKg] = useState(17.99);
  const [dispenserGroup, setDispenserGroup] = useState('');
  const [verificationRequired, setVerificationRequired] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activePanel, setActivePanel] = useState('status'); // status, reports, history, pricing

  // Load dispenser on mount
  useEffect(() => {
    loadDispenser();
    const interval = setInterval(pollDispenserStatus, 1000); // Poll every second
    return () => clearInterval(interval);
  }, [selectedDispenser]);

  const loadDispenser = async () => {
    try {
      // Mock: Load dispenser with ID 1 for station S001
      const dispenser = await dispensers.getByStation('S001');
      if (dispenser && dispenser.length > 0) {
        setSelectedDispenser(dispenser[0]);
      }
    } catch (err) {
      console.error('Failed to load dispenser:', err);
    }
  };

  const pollDispenserStatus = async () => {
    if (!selectedDispenser) return;
    
    try {
      const status = await dispensers.getStatus(selectedDispenser.id);
      setDispenserStatus(status.status);
      setIsOnline(status.status !== 'OFFLINE');
      setActiveFilling(status.activeFilling);
    } catch (err) {
      // Silently fail - don't spam errors
      setIsOnline(false);
    }
  };

  const handleStart = async () => {
    if (!selectedDispenser || loading) return;
    
    setLoading(true);
    setError(null);
    
    try {
      await dispensers.start(selectedDispenser.id, {
        pricePerKg,
        verificationRequired,
        dispenserGroup
      });
      // Status will be updated by polling
    } catch (err) {
      setError('Kunne ikke starte fylling: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleStop = async () => {
    if (!selectedDispenser || loading) return;
    
    setLoading(true);
    setError(null);
    
    try {
      await dispensers.stop(selectedDispenser.id);
      // Status will be updated by polling
    } catch (err) {
      setError('Kunne ikke stoppe fylling: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSendConfirmation = () => {
    alert('Send bekreftelse - funksjonalitet kommer');
  };

  const handleUpdateClients = () => {
    alert('Oppdatere kunder - funksjonalitet kommer');
  };

  // Format display values
  const displayAmount = activeFilling ? activeFilling.currentAmount.toFixed(2) : '0000,00';
  const displayVolume = activeFilling ? activeFilling.currentVolume.toFixed(2) : '0000,00';

  return (
    <Layout title="Dispenserkontroll Dashboard">
      <div className="dispenser-control">
        {/* Error Banner */}
        {error && (
          <div className="error-banner">
            {error}
          </div>
        )}

        <div className="control-container">
          {/* Left Panel - Menu and Overview */}
          <div className="left-panel">
            <div className="panel-header">
              <h2>Oversikt, status og rapporter</h2>
              <p className="panel-subtitle">
                Bruk til statuskontroll, rapportering, historikk og kvitteringer, og prisadministrasjon
              </p>
            </div>

            <nav className="control-menu">
              <button 
                className={`menu-item ${activePanel === 'status' ? 'active' : ''}`}
                onClick={() => setActivePanel('status')}
              >
                📊 Statuskontroll
              </button>
              <button 
                className={`menu-item ${activePanel === 'reports' ? 'active' : ''}`}
                onClick={() => setActivePanel('reports')}
              >
                📈 Rapportering
              </button>
              <button 
                className={`menu-item ${activePanel === 'history' ? 'active' : ''}`}
                onClick={() => setActivePanel('history')}
              >
                📜 Historikk og kvitteringer
              </button>
              <button 
                className={`menu-item ${activePanel === 'pricing' ? 'active' : ''}`}
                onClick={() => setActivePanel('pricing')}
              >
                💰 Prisadministrasjon
              </button>
              <Link to="/transactions" className="menu-item link">
                💳 Transaksjoner
              </Link>
            </nav>

            <div className="panel-content">
              {activePanel === 'status' && (
                <div className="info-section">
                  <h3>Statuskontroll</h3>
                  <p>Gi sanntidsoversikt over dispenserstatus</p>
                  <ul>
                    <li>Aktiv kommunikasjon med dispenser</li>
                    <li>Fylling kan startes</li>
                    <li>Sporbar og regelstyrt bruk</li>
                  </ul>
                </div>
              )}
              
              {activePanel === 'reports' && (
                <div className="info-section">
                  <h3>Rapportering</h3>
                  <p>Generer rapporter for fylleoperasjoner</p>
                  <button className="secondary-btn">Generer rapport</button>
                </div>
              )}
              
              {activePanel === 'history' && (
                <div className="info-section">
                  <h3>Historikk</h3>
                  <p>Se tidligere fylleoperasjoner og kvitteringer</p>
                  <Link to="/transactions" className="secondary-btn">
                    Vis historikk
                  </Link>
                </div>
              )}
              
              {activePanel === 'pricing' && (
                <div className="info-section">
                  <h3>Prisadministrasjon</h3>
                  <div className="price-control">
                    <label>
                      Pris per kg:
                      <input 
                        type="number" 
                        step="0.01" 
                        value={pricePerKg}
                        onChange={(e) => setPricePerKg(parseFloat(e.target.value))}
                        className="price-input"
                      />
                    </label>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Right Panel - Active Dispenser Control */}
          <div className="right-panel">
            <div className="control-header">
              <h2>Aktiv dispenserstyring</h2>
              <p className="control-subtitle">
                Brukes til valg av avgift, inntasting av beløp eller liter, start og stopp av dispenser
              </p>
            </div>

            {/* Status Indicator */}
            <div className="status-section">
              <div className="status-label">STATUS</div>
              <div className={`status-indicator ${isOnline ? 'online' : 'offline'}`}>
                <span className="status-dot"></span>
                <span className="status-text">{isOnline ? 'ONLINE' : 'OFFLINE'}</span>
              </div>
            </div>

            {/* Control Options */}
            <div className="control-options">
              <div className="option-group">
                <label>Dispensergruppe</label>
                <input 
                  type="text" 
                  value={dispenserGroup}
                  onChange={(e) => setDispenserGroup(e.target.value)}
                  placeholder="Velg gruppe"
                  className="option-input"
                />
              </div>

              <div className="option-group">
                <label>Vakselvægt</label>
                <select className="option-select">
                  <option>Standard</option>
                </select>
              </div>

              <div className="option-group">
                <label>Uttransport</label>
                <select className="option-select">
                  <option>Normal</option>
                </select>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="action-buttons">
              <button 
                className="action-btn send-confirmation"
                onClick={handleSendConfirmation}
                disabled={!isOnline}
              >
                Send bekreftelse
              </button>
              <button 
                className="action-btn update-clients"
                onClick={handleUpdateClients}
                disabled={!isOnline}
              >
                Oppdragere
              </button>
            </div>

            {/* Verification Option */}
            <div className="verification-section">
              <label className="verification-toggle">
                <input 
                  type="checkbox"
                  checked={verificationRequired}
                  onChange={(e) => setVerificationRequired(e.target.checked)}
                />
                <span>MED VERIFIKASJON?</span>
              </label>
            </div>

            {/* Amount Displays */}
            <div className="displays">
              <div className="display-box">
                <div className="display-label">Beløp (kr)</div>
                <div className="display-value">{displayAmount}</div>
              </div>

              <div className="display-box">
                <div className="display-label">Volum (kg)</div>
                <div className="display-value">{displayVolume}</div>
              </div>
            </div>

            {/* Price Display */}
            <div className="price-display">
              <div className="price-label">PRIS KR/L</div>
              <div className="price-value">{pricePerKg.toFixed(2)}</div>
            </div>

            {/* Control Buttons */}
            <div className="control-buttons">
              <button 
                className="control-btn start-btn"
                onClick={handleStart}
                disabled={!isOnline || loading || dispenserStatus === 'FILLING'}
              >
                {loading ? '⏳ Starter...' : 'START'}
              </button>
              <button 
                className="control-btn stop-btn"
                onClick={handleStop}
                disabled={!isOnline || loading || dispenserStatus !== 'FILLING'}
              >
                {loading ? '⏳ Stopper...' : 'STOPP'}
              </button>
            </div>

            {/* Status Message */}
            {dispenserStatus === 'FILLING' && (
              <div className="filling-status">
                ⚡ Fylling pågår...
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
```

---

### 2. DispenserControl.css

**Filplassering:** `frontend/src/styles/DispenserControl.css`

```css
/* Dispenser Control Dashboard - Dark Theme */

.dispenser-control {
  width: 100%;
  min-height: calc(100vh - 80px);
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 1rem;
  color: #ffffff;
}

.control-container {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
  max-width: 1600px;
  margin: 0 auto;
}

/* Tablet and up: 2 columns */
@media (min-width: 1024px) {
  .control-container {
    grid-template-columns: 400px 1fr;
    gap: 2rem;
  }
}

/* ==================== Error Banner ==================== */
.error-banner {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
  padding: 1rem 1.5rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(231, 76, 60, 0.3);
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ==================== Left Panel ==================== */
.left-panel {
  background: rgba(30, 30, 46, 0.9);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
}

.panel-header h2 {
  color: #4caf50;
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.panel-subtitle {
  color: #b0b0b0;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.control-menu {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.menu-item {
  background: rgba(52, 73, 94, 0.5);
  color: #ecf0f1;
  border: none;
  border-radius: 8px;
  padding: 0.875rem 1rem;
  text-align: left;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  transition: all 0.2s ease;
  text-decoration: none;
  display: block;
}

.menu-item:hover {
  background: rgba(52, 73, 94, 0.8);
  transform: translateX(4px);
}

.menu-item.active {
  background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.4);
}

.menu-item.link {
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: white;
}

.menu-item.link:hover {
  background: linear-gradient(135deg, #2980b9 0%, #1f5f8b 100%);
}

.panel-content {
  background: rgba(44, 62, 80, 0.3);
  border-radius: 8px;
  padding: 1.25rem;
}

.info-section h3 {
  color: #4caf50;
  font-size: 1.1rem;
  margin-bottom: 0.75rem;
}

.info-section p {
  color: #b0b0b0;
  margin-bottom: 1rem;
  line-height: 1.5;
}

.info-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-section li {
  color: #95a5a6;
  padding: 0.5rem 0;
  padding-left: 1.5rem;
  position: relative;
}

.info-section li:before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #4caf50;
  font-weight: bold;
}

.secondary-btn {
  background: rgba(52, 152, 219, 0.8);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 0.75rem 1.5rem;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-top: 0.5rem;
  text-decoration: none;
  display: inline-block;
}

.secondary-btn:hover {
  background: rgba(52, 152, 219, 1);
  transform: translateY(-1px);
}

.price-control {
  margin-top: 1rem;
}

.price-control label {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  color: #ecf0f1;
}

.price-input {
  background: rgba(52, 73, 94, 0.6);
  border: 2px solid rgba(76, 175, 80, 0.3);
  border-radius: 6px;
  padding: 0.75rem;
  color: white;
  font-size: 1.1rem;
  font-weight: bold;
  width: 120px;
}

.price-input:focus {
  outline: none;
  border-color: #4caf50;
  box-shadow: 0 0 8px rgba(76, 175, 80, 0.3);
}

/* ==================== Right Panel ==================== */
.right-panel {
  background: rgba(30, 30, 46, 0.9);
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
}

.control-header h2 {
  color: #3498db;
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.control-subtitle {
  color: #b0b0b0;
  font-size: 0.9rem;
  margin-bottom: 2rem;
  line-height: 1.5;
}

/* ==================== Status Section ==================== */
.status-section {
  margin-bottom: 2rem;
}

.status-label {
  color: #95a5a6;
  font-size: 0.9rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: bold;
  font-size: 1.1rem;
  transition: all 0.3s ease;
}

.status-indicator.online {
  background: rgba(46, 204, 113, 0.2);
  border: 2px solid #2ecc71;
  color: #2ecc71;
}

.status-indicator.offline {
  background: rgba(231, 76, 60, 0.2);
  border: 2px solid #e74c3c;
  color: #e74c3c;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-indicator.online .status-dot {
  background: #2ecc71;
  box-shadow: 0 0 8px #2ecc71;
}

.status-indicator.offline .status-dot {
  background: #e74c3c;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* ==================== Control Options ==================== */
.control-options {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

@media (min-width: 600px) {
  .control-options {
    grid-template-columns: repeat(3, 1fr);
  }
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.option-group label {
  color: #95a5a6;
  font-size: 0.9rem;
  font-weight: 500;
}

.option-input,
.option-select {
  background: rgba(52, 73, 94, 0.6);
  border: 2px solid rgba(52, 152, 219, 0.3);
  border-radius: 6px;
  padding: 0.75rem;
  color: white;
  font-size: 0.95rem;
}

.option-input:focus,
.option-select:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 8px rgba(52, 152, 219, 0.3);
}

/* ==================== Action Buttons ==================== */
.action-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.action-btn {
  background: rgba(243, 156, 18, 0.8);
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.875rem 1rem;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.95rem;
  transition: all 0.2s ease;
}

.action-btn:hover:not(:disabled) {
  background: rgba(243, 156, 18, 1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(243, 156, 18, 0.4);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ==================== Verification ==================== */
.verification-section {
  margin-bottom: 2rem;
}

.verification-toggle {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  color: #3498db;
  font-weight: 600;
  font-size: 1rem;
}

.verification-toggle input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

/* ==================== Displays ==================== */
.displays {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.display-box {
  background: rgba(44, 62, 80, 0.8);
  border: 3px solid rgba(52, 152, 219, 0.5);
  border-radius: 12px;
  padding: 1.5rem;
  text-align: center;
}

.display-label {
  color: #95a5a6;
  font-size: 0.85rem;
  font-weight: 500;
  margin-bottom: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.display-value {
  font-family: 'Courier New', monospace;
  font-size: 2.5rem;
  font-weight: bold;
  color: #3498db;
  letter-spacing: 2px;
}

/* ==================== Price Display ==================== */
.price-display {
  background: rgba(44, 62, 80, 0.8);
  border: 3px solid rgba(243, 156, 18, 0.5);
  border-radius: 12px;
  padding: 1rem 1.5rem;
  text-align: center;
  margin-bottom: 2rem;
}

.price-label {
  color: #95a5a6;
  font-size: 0.85rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.price-value {
  font-family: 'Courier New', monospace;
  font-size: 2rem;
  font-weight: bold;
  color: #f39c12;
  letter-spacing: 2px;
}

/* ==================== Control Buttons ==================== */
.control-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.control-btn {
  border: none;
  border-radius: 12px;
  padding: 1.5rem 2rem;
  cursor: pointer;
  font-weight: bold;
  font-size: 1.5rem;
  text-transform: uppercase;
  letter-spacing: 2px;
  transition: all 0.2s ease;
  min-height: 80px;
}

.start-btn {
  background: linear-gradient(135deg, #2ecc71 0%, #27ae60 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(46, 204, 113, 0.4);
}

.start-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #27ae60 0%, #229954 100%);
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(46, 204, 113, 0.6);
}

.stop-btn {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(231, 76, 60, 0.4);
}

.stop-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #c0392b 0%, #a93226 100%);
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(231, 76, 60, 0.6);
}

.control-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.control-btn:active:not(:disabled) {
  transform: translateY(0);
}

/* ==================== Filling Status ==================== */
.filling-status {
  background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
  color: white;
  padding: 1rem;
  border-radius: 8px;
  text-align: center;
  font-weight: bold;
  font-size: 1.1rem;
  animation: fillPulse 1.5s infinite;
}

@keyframes fillPulse {
  0%, 100% {
    box-shadow: 0 0 0 rgba(243, 156, 18, 0.4);
  }
  50% {
    box-shadow: 0 0 20px rgba(243, 156, 18, 0.8);
  }
}

/* ==================== Mobile Responsiveness ==================== */
@media (max-width: 600px) {
  .dispenser-control {
    padding: 0.5rem;
  }
  
  .left-panel,
  .right-panel {
    padding: 1rem;
  }
  
  .control-header h2 {
    font-size: 1.25rem;
  }
  
  .displays {
    grid-template-columns: 1fr;
  }
  
  .display-value {
    font-size: 2rem;
  }
  
  .control-buttons {
    grid-template-columns: 1fr;
  }
  
  .control-btn {
    font-size: 1.25rem;
    padding: 1.25rem;
  }
}
```

---

### 3. api.js

**Filplassering:** `frontend/src/services/api.js`

**Legg til følgende kode i slutten av filen:**

```javascript
// Dispensers API
export const dispensers = {
  // Get all dispensers for a station
  getByStation: (stationId) => fetchWithCredentials(`${API_BASE}/stations/${stationId}/dispensers`),
  
  // Start filling operation
  start: (dispenserId, options) => fetchWithCredentials(`${API_BASE}/dispensers/${dispenserId}/start`, {
    method: 'POST',
    body: JSON.stringify(options),
  }),
  
  // Stop filling operation
  stop: (dispenserId) => fetchWithCredentials(`${API_BASE}/dispensers/${dispenserId}/stop`, {
    method: 'POST',
  }),
  
  // Get real-time dispenser status
  getStatus: (dispenserId) => fetchWithCredentials(`${API_BASE}/dispensers/${dispenserId}/status`),
};

// Oppdater default export
export default {
  auth,
  transactions,
  stations,
  customers,
  dispensers,  // ← Legg til denne linjen
};
```

---

### 4. App.jsx

**Filplassering:** `frontend/src/App.jsx`

**Legg til import øverst:**

```javascript
import TransactionsPage from './pages/TransactionsPage';
```

**Legg til route i Routes-komponenten:**

```jsx
<Route
  path="/transactions"
  element={
    <ProtectedRoute>
      <TransactionsPage />
    </ProtectedRoute>
  }
/>
```

**Fullstendig App.jsx:**

```jsx
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import LoginPage from './pages/LoginPage';
import StationOwnerPage from './pages/StationOwnerPage';
import TransactionsPage from './pages/TransactionsPage';  // ← LEGG TIL
import CustomerLoginPage from './pages/CustomerLoginPage';
import CustomerDashboardPage from './pages/CustomerDashboardPage';
import './styles/theme.css';

// Protected Route Component
function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Laster...</div>;
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  return children;
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<LoginPage />} />
          
          <Route
            path="/station"
            element={
              <ProtectedRoute>
                <StationOwnerPage />
              </ProtectedRoute>
            }
          />
          
          {/* ← LEGG TIL DETTE */}
          <Route
            path="/transactions"
            element={
              <ProtectedRoute>
                <TransactionsPage />
              </ProtectedRoute>
            }
          />
          
          {/* Customer Portal Routes - No Authentication Required */}
          <Route path="/kunde" element={<CustomerLoginPage />} />
          <Route path="/kunde/portal" element={<CustomerDashboardPage />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
```

---

### 5. TransactionsPage.jsx

**Filplassering:** `frontend/src/pages/TransactionsPage.jsx`

**Merk:** Dette er den gamle StationOwnerPage-funksjonaliteten.

```jsx
import { useState, useEffect } from 'react';
import Layout from '../components/common/Layout';
import { transactions } from '../services/api';
import '../styles/TransactionsPage.css';

export default function TransactionsPage() {
  const [txData, setTxData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [payingTx, setPayingTx] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    loadTransactions();
    const interval = setInterval(loadTransactions, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadTransactions = async () => {
    try {
      const data = await transactions.getAll();
      setTxData(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePayment = async (transactionId, amount, paymentType = 'CARD') => {
    setPayingTx(transactionId);
    try {
      await transactions.pay(transactionId, paymentType);
      setSuccessMessage(`✅ Betaling på ${amount.toFixed(2)} kr godkjent!`);
      setTimeout(() => setSuccessMessage(null), 5000);
      await loadTransactions();
    } catch (err) {
      alert('Betalingsfeil: ' + err.message);
    } finally {
      setPayingTx(null);
    }
  };

  // Beregn statistikk
  const pendingTxs = txData.filter(tx => tx.paymentStatus === 'PENDING');
  const paidTxs = txData.filter(tx => tx.paymentStatus === 'PAID');
  
  const stats = {
    totalVolume: txData.reduce((sum, tx) => sum + tx.volumeLiters, 0),
    totalRevenue: txData.reduce((sum, tx) => sum + tx.amountKr, 0),
    transactionCount: txData.length,
    paidCount: paidTxs.length,
    pendingCount: pendingTxs.length,
    pendingAmount: pendingTxs.reduce((sum, tx) => sum + tx.amountKr, 0),
  };

  return (
    <Layout title="Transaksjoner">
      <div className="transactions-dashboard">
        {/* Success Message */}
        {successMessage && (
          <div className="success-banner">
            {successMessage}
          </div>
        )}

        {/* Stats */}
        <div className="stats-grid">
          <div className="stat-card">
            <h3>Totalt Volum</h3>
            <p className="stat-value">{stats.totalVolume.toFixed(2)} L</p>
          </div>
          <div className="stat-card">
            <h3>Total Omsetning</h3>
            <p className="stat-value">{stats.totalRevenue.toFixed(2)} kr</p>
          </div>
          <div className="stat-card">
            <h3>Antall Transaksjoner</h3>
            <p className="stat-value">{stats.transactionCount}</p>
          </div>
          <div className="stat-card pending-highlight">
            <h3>⚠️ Venter på Betaling</h3>
            <p className="stat-value">{stats.pendingCount}</p>
            <p className="stat-subtext">{stats.pendingAmount.toFixed(2)} kr</p>
          </div>
        </div>

        {/* Pending Payments Section */}
        {!loading && pendingTxs.length > 0 && (
          <div className="section pending-section">
            <div className="section-header">
              <h2>💳 Transaksjoner som venter på betaling</h2>
              <p className="section-subtitle">
                Klikk "Betal" for å godkjenne betalingen
              </p>
            </div>
            <div className="pending-cards">
              {pendingTxs.map(tx => (
                <div key={tx.transactionId} className="pending-card">
                  <div className="pending-card-header">
                    <span className="pending-card-time">
                      {new Date(tx.timestamp).toLocaleString('no-NO')}
                    </span>
                    <span className="pending-card-pump">
                      Pumpe #{tx.dispenserAddress}
                    </span>
                  </div>
                  <div className="pending-card-body">
                    <div className="pending-card-amount">
                      {tx.amountKr.toFixed(2)} kr
                    </div>
                    <div className="pending-card-details">
                      {tx.volumeLiters.toFixed(2)} L @ {tx.pricePerLiter.toFixed(2)} kr/L
                    </div>
                  </div>
                  <button
                    className={`btn-pay-large ${payingTx === tx.transactionId ? 'loading' : ''}`}
                    onClick={() => handlePayment(tx.transactionId, tx.amountKr, 'CARD')}
                    disabled={payingTx === tx.transactionId}
                  >
                    {payingTx === tx.transactionId ? '⏳ Behandler...' : '💳 Betal med kort'}
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Transaksjoner */}
        <div className="section">
          <h2>Alle Transaksjoner</h2>
          {loading && <p>Laster...</p>}
          {error && <p className="error">{error}</p>}

          {!loading && !error && (
            <div className="table-container">
              <table className="transactions-table">
                <thead>
                  <tr>
                    <th>Tidspunkt</th>
                    <th>Pumpe</th>
                    <th>Volum (L)</th>
                    <th>Beløp (kr)</th>
                    <th>Pris/L</th>
                    <th>Betaling</th>
                    <th>Status</th>
                    <th>Handling</th>
                  </tr>
                </thead>
                <tbody>
                  {txData.length === 0 ? (
                    <tr>
                      <td colSpan="8">Ingen transaksjoner ennå</td>
                    </tr>
                  ) : (
                    txData.map(tx => (
                      <tr key={tx.transactionId} className={tx.paymentStatus.toLowerCase()}>
                        <td>{new Date(tx.timestamp).toLocaleString('no-NO')}</td>
                        <td>#{tx.dispenserAddress}</td>
                        <td>{tx.volumeLiters.toFixed(2)}</td>
                        <td>{tx.amountKr.toFixed(2)}</td>
                        <td>{tx.pricePerLiter.toFixed(2)}</td>
                        <td>{tx.paymentType}</td>
                        <td>
                          <span className={`status-badge ${tx.paymentStatus.toLowerCase()}`}>
                            {tx.paymentStatus}
                          </span>
                        </td>
                        <td>
                          {tx.paymentStatus === 'PENDING' && (
                            <button 
                              className="btn-pay"
                              onClick={() => handlePayment(tx.transactionId, tx.amountKr, 'CARD')}
                              disabled={payingTx === tx.transactionId}
                            >
                              {payingTx === tx.transactionId ? '⏳' : '💳 Betal'}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}
```

---

### 6. TransactionsPage.css

**Filplassering:** `frontend/src/styles/TransactionsPage.css`

**Merk:** Dette er en kopi av den gamle StationOwnerPage.css. Kopier din eksisterende CSS-fil, eller bruk følgende grunnleggende styling:

```css
/* Kopier din eksisterende StationOwnerPage.css hit */
/* Eller bruk din eksisterende transaction/stats styling */

.transactions-dashboard {
  /* Din eksisterende styling */
}
```

---

### 7. DispenserController.kt (Backend)

**Filplassering:** `backend/src/main/kotlin/com/yourpackage/controller/DispenserController.kt`

**MERK:** Endre `com.yourpackage` til din faktiske package-path.

```kotlin
package com.yourpackage.controller

import com.yourpackage.model.Dispenser
import com.yourpackage.repository.DispenserRepository
import com.yourpackage.repository.StationRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:5173", "http://localhost:3001", "http://localhost:3000"], allowCredentials = "true")
class DispenserController(
    private val dispenserRepository: DispenserRepository,
    private val stationRepository: StationRepository
) {
    private val logger = LoggerFactory.getLogger(DispenserController::class.java)
    
    // In-memory storage for active filling operations (mock)
    private val activeFillings = mutableMapOf<Long, ActiveFillingData>()
    
    /**
     * Get all dispensers for a station
     */
    @GetMapping("/stations/{stationId}/dispensers")
    fun getDispensers(@PathVariable stationId: String): ResponseEntity<List<Dispenser>> {
        logger.info("📋 Getting dispensers for station: $stationId")
        
        val station = stationRepository.findByStationCode(stationId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        
        val dispensers = dispenserRepository.findByStation(station)
        return ResponseEntity.ok(dispensers)
    }
    
    /**
     * Start filling operation
     */
    @PostMapping("/dispensers/{dispenserId}/start")
    fun startFilling(
        @PathVariable dispenserId: Long,
        @RequestBody request: StartFillingRequest
    ): ResponseEntity<DispenserOperationResponse> {
        logger.info("🟢 START filling request for dispenser $dispenserId")
        logger.debug("Request: pricePerKg=${request.pricePerKg}, verificationRequired=${request.verificationRequired}")
        
        val dispenser = dispenserRepository.findById(dispenserId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        
        // Check if already filling
        if (activeFillings.containsKey(dispenserId)) {
            return ResponseEntity.badRequest().body(
                DispenserOperationResponse(
                    success = false,
                    message = "Dispenser is already filling",
                    dispenserId = dispenserId,
                    status = "FILLING"
                )
            )
        }
        
        // Start mock filling
        activeFillings[dispenserId] = ActiveFillingData(
            startedAt = LocalDateTime.now(),
            pricePerKg = request.pricePerKg,
            currentAmount = 0.0,
            currentVolume = 0.0
        )
        
        logger.info("✅ Filling started on dispenser $dispenserId")
        
        return ResponseEntity.ok(
            DispenserOperationResponse(
                success = true,
                message = "Filling started successfully",
                dispenserId = dispenserId,
                status = "FILLING"
            )
        )
    }
    
    /**
     * Stop filling operation
     */
    @PostMapping("/dispensers/{dispenserId}/stop")
    fun stopFilling(@PathVariable dispenserId: Long): ResponseEntity<DispenserOperationResponse> {
        logger.info("🔴 STOP filling request for dispenser $dispenserId")
        
        val dispenser = dispenserRepository.findById(dispenserId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        
        // Check if filling is active
        if (!activeFillings.containsKey(dispenserId)) {
            return ResponseEntity.badRequest().body(
                DispenserOperationResponse(
                    success = false,
                    message = "No active filling operation",
                    dispenserId = dispenserId,
                    status = "IDLE"
                )
            )
        }
        
        // Stop mock filling
        val fillingData = activeFillings.remove(dispenserId)
        
        logger.info("✅ Filling stopped on dispenser $dispenserId - Total: ${fillingData?.currentAmount} kr")
        
        return ResponseEntity.ok(
            DispenserOperationResponse(
                success = true,
                message = "Filling stopped successfully",
                dispenserId = dispenserId,
                status = "IDLE"
            )
        )
    }
    
    /**
     * Get real-time dispenser status
     */
    @GetMapping("/dispensers/{dispenserId}/status")
    fun getStatus(@PathVariable dispenserId: Long): ResponseEntity<DispenserStatusResponse> {
        val dispenser = dispenserRepository.findById(dispenserId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        
        val fillingData = activeFillings[dispenserId]
        
        // If filling is active, simulate incrementing values
        if (fillingData != null) {
            val secondsElapsed = java.time.Duration.between(fillingData.startedAt, LocalDateTime.now()).seconds
            // Simulate 0.5 kg per second
            val volume = secondsElapsed * 0.5
            val amount = volume * fillingData.pricePerKg
            
            fillingData.currentVolume = volume
            fillingData.currentAmount = amount
        }
        
        val status = if (fillingData != null) "FILLING" else "IDLE"
        
        val response = DispenserStatusResponse(
            dispenserId = dispenserId,
            status = status,
            activeFilling = if (fillingData != null) {
                ActiveFillingResponse(
                    startedAt = fillingData.startedAt,
                    currentAmount = fillingData.currentAmount,
                    currentVolume = fillingData.currentVolume,
                    pricePerKg = fillingData.pricePerKg
                )
            } else null
        )
        
        return ResponseEntity.ok(response)
    }
}

// DTOs
data class StartFillingRequest(
    val pricePerKg: Double,
    val verificationRequired: Boolean,
    val dispenserGroup: String?
)

data class DispenserOperationResponse(
    val success: Boolean,
    val message: String,
    val dispenserId: Long,
    val status: String
)

data class DispenserStatusResponse(
    val dispenserId: Long,
    val status: String,
    val activeFilling: ActiveFillingResponse?
)

data class ActiveFillingResponse(
    val startedAt: LocalDateTime,
    val currentAmount: Double,
    val currentVolume: Double,
    val pricePerKg: Double
)

// Internal data class for tracking active fillings
data class ActiveFillingData(
    val startedAt: LocalDateTime,
    val pricePerKg: Double,
    var currentAmount: Double,
    var currentVolume: Double
)
```

---

## Testing

### 1. Start Backend

```bash
cd backend
mvn clean install
cd backend-api  # eller din API-modul
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Backend kjører på: **http://localhost:8081**

### 2. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend kjører på: **http://localhost:5173**

### 3. Test Grensesnittet

1. Åpne http://localhost:5173
2. Logg inn (hvis påkrevd)
3. Du bør se **Dispenserkontroll Dashboard**
4. Klikk på menyvalg i venstre panel
5. Klikk "💳 Transaksjoner" for å se transaksjonssiden
6. Gå tilbake til dispenserkontroll
7. Klikk **START** - se mengdene øke
8. Klikk **STOPP** - se fyllingen stoppe

### 4. Test med curl

```bash
# Start fylling
curl -X POST http://localhost:8081/api/dispensers/1/start \
  -H "Content-Type: application/json" \
  -d '{
    "pricePerKg": 17.99,
    "verificationRequired": true,
    "dispenserGroup": "Gruppe A"
  }'

# Hent status
curl http://localhost:8081/api/dispensers/1/status

# Stopp fylling
curl -X POST http://localhost:8081/api/dispensers/1/stop
```

---

## Feilsøking

### Frontend kompilerer ikke

**Problem:** `dispensers is not defined`

**Løsning:** Sjekk at du har lagt til `dispensers` i `api.js` og eksportert den i default export.

---

### Backend kompilerer ikke

**Problem:** `DispenserRepository not found`

**Løsning:** Sjekk at du har:
1. `DispenserRepository` interface
2. `Dispenser` entity/model
3. `StationRepository` interface
4. Riktig package-import i `DispenserController.kt`

---

### Status viser alltid OFFLINE

**Problem:** API returnerer ikke riktig status

**Løsning:** 
1. Sjekk at backend kjører
2. Sjekk CORS-konfigurasjon i `DispenserController`
3. Sjekk at `dispensers.getByStation()` returnerer data
4. Se nettverkstrafikk i browser DevTools

---

### Mengdevisninger oppdateres ikke

**Problem:** Polling kjører ikke

**Løsning:**
1. Sjekk browser console for feil
2. Sjekk at `pollDispenserStatus` kjører hvert sekund
3. Sjekk at `selectedDispenser` er satt
4. Se nettverkstrafikk - skal være en request hvert sekund

---

## Tilpasninger

### Endre stasjonskode

I `StationOwnerPage.jsx`, endre:

```javascript
const dispenser = await dispensers.getByStation('S001');  // ← Endre 'S001'
```

### Endre pris

I `StationOwnerPage.jsx`, endre initial pris:

```javascript
const [pricePerKg, setPricePerKg] = useState(17.99);  // ← Endre 17.99
```

### Endre fyllehastighet

I `DispenserController.kt`, endre:

```kotlin
val volume = secondsElapsed * 0.5  // ← Endre 0.5 (kg per sekund)
```

### Endre polling-intervall

I `StationOwnerPage.jsx`, endre:

```javascript
const interval = setInterval(pollDispenserStatus, 1000); // ← Endre 1000 (ms)
```

---

## Fargepalett

### Dispenserkontroll Theme

```css
/* Bakgrunn */
background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);

/* Panel */
background: rgba(30, 30, 46, 0.9);

/* Farger */
--online-green: #2ecc71
--offline-red: #e74c3c
--primary-blue: #3498db
--warning-orange: #f39c12
--menu-green: #4caf50
```

---

## Oppsummering

Du har nå all informasjon for å gjenskape dispenserkontroll-grensesnittet:

1. ✅ **7 filer** med fullstendig kode
2. ✅ **Steg-for-steg** implementering
3. ✅ **Testing**-instruksjoner
4. ✅ **Feilsøking**-tips
5. ✅ **Tilpasninger**-guide

**Estimert tid:** 30-60 minutter for erfaren utvikler

**Hjelpemidler:**
- Denne filen
- Browser DevTools
- Backend logs

**Lykke til!** 🚀
