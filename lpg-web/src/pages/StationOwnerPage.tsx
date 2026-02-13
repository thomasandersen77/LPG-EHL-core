import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { pumpApi, type PumpStatus } from '../api/pump';
import '../styles/DispenserControl.css';

type ActivePanel = 'status' | 'reports' | 'history' | 'pricing';
type ReportTab = 'omsetning' | 'veibruksavgift' | 'uttak' | 'kvitteringer';

export function StationOwnerPage() {
  const navigate = useNavigate();
  const { isLoggedIn, stationName, logout } = useAuth();

  // State management
  const [pumpStatus, setPumpStatus] = useState<PumpStatus | null>(null);
  const [activePanel, setActivePanel] = useState<ActivePanel>('status');
  const [reportTab, setReportTab] = useState<ReportTab>('omsetning');
  const [dateFrom, setDateFrom] = useState(() => {
    const d = new Date();
    d.setDate(1);
    return d.toISOString().slice(0, 10);
  });
  const [dateTo, setDateTo] = useState(() => new Date().toISOString().slice(0, 10));
  const [pricePerLiter, setPricePerLiter] = useState(17.99);
  const [withRoadTax, setWithRoadTax] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState<'CARD' | 'CREDIT'>('CARD');
  const [amountInput, setAmountInput] = useState('');
  const [litersInput, setLitersInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null);

  // Redirect if not logged in
  useEffect(() => {
    if (!isLoggedIn) {
      navigate('/');
    }
  }, [isLoggedIn, navigate]);

  // Poll pump status
  useEffect(() => {
    const pollStatus = async () => {
      try {
        const status = await pumpApi.getStatus(1);
        setPumpStatus(status);
        if (status.pricePerLitreKr > 0) {
          setPricePerLiter(status.pricePerLitreKr);
        }
      } catch (err) {
        // Silently fail - don't spam errors
      }
    };

    pollStatus();
    // Poll faster during pumping
    const interval = setInterval(pollStatus, 
      pumpStatus?.state === 'PUMPING' ? 500 : 1000
    );
    return () => clearInterval(interval);
  }, [pumpStatus?.state]);

  // 60-second countdown for READY_TO_PUMP
  useEffect(() => {
    if (pumpStatus?.state === 'READY_TO_PUMP') {
      setCountdown(60);
      const interval = setInterval(() => {
        setCountdown(prev => {
          if (prev === null || prev <= 1) {
            clearInterval(interval);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      return () => clearInterval(interval);
    } else {
      setCountdown(null);
    }
  }, [pumpStatus?.state]);

  // Step 1: Release dispenser (FRI DISPENSER)
  const handleReleaseDispenser = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.releaseDispenser(1);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke frigjøre pumpe: ' + message);
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Start pumping (for GUI simulation)
  const handleStartPumping = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.startPumping(1);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke starte pumping: ' + message);
    } finally {
      setLoading(false);
    }
  };

  // Step 4: Stop pumping
  const handleStop = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.block(1);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke stoppe fylling: ' + message);
    } finally {
      setLoading(false);
    }
  };

  // Step 5: Confirm payment
  const handleConfirmPayment = async (method: 'CARD' | 'CREDIT') => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.confirmPayment(1, method);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke bekrefte betaling: ' + message);
    } finally {
      setLoading(false);
    }
  };

  // Admin: Full reset
  const handleReset = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.fullReset();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke nullstille: ' + message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  // Determine status based on pump state
  const state = pumpStatus?.state || 'OFFLINE';
  const canRelease = (state === 'IDLE' || state === 'AUTHORIZED_WAITING') && !pumpStatus?.hasPendingTransaction;
  const isReadyToPump = state === 'READY_TO_PUMP';
  const isPumping = state === 'PUMPING';
  const isPaymentPending = state === 'PAYMENT_PENDING' && pumpStatus?.hasPendingTransaction;

  // Connected = choices locked (tax, payment, amount/liters shown from pump)
  const isConnected = isReadyToPump || isPumping || isPaymentPending;
  const isOnline = pumpStatus != null && state !== 'OFFLINE';

  // Format display values (when connected use live data, else use inputs or 0)
  const displayAmount = isConnected
    ? (pumpStatus?.amountKr?.toFixed(2) ?? '0.00')
    : (amountInput || '0,00').replace('.', ',');
  const displayVolume = isConnected
    ? (pumpStatus?.volumeLitres?.toFixed(2) ?? '0.00')
    : (litersInput || '0,00').replace('.', ',');
  const statusText = state;

  return (
    <div className="dispenser-control">
      {/* Header Bar */}
      <div className="station-header">
        <div className="station-info">
          <span className="station-icon">⛽</span>
          <span className="station-name">{stationName || 'Min Stasjon'}</span>
        </div>
        <div className="header-actions">
          <Link to="/diagnose" className="diagnose-link">
            🔧 Diagnose / Simulér
          </Link>
          <button onClick={handleLogout} className="logout-btn">
            Logg ut
          </button>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="error-banner">
          {error}
          <button onClick={() => setError(null)} className="error-close">✕</button>
        </div>
      )}

      <div className="control-container">
        {/* Left Panel - Menu and Overview */}
        <div className="left-panel">
          {/* STATUS chip (ONLINE/OFFLINE) - matches dashboard image */}
          <div className="left-panel-status-row">
            <span className="left-panel-status-label">STATUS</span>
            <span className={`left-panel-status-chip ${isOnline ? 'online' : 'offline'}`}>
              {isOnline ? 'ONLINE' : 'OFFLINE'}
            </span>
          </div>

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
            <Link to="/credit" className="menu-item link">
              🏪 Stasjonskreditt
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
                <div className="status-details">
                  <div className="detail-row">
                    <span>Tilstand:</span>
                    <span className={`state-badge ${state.toLowerCase()}`}>
                      {statusText}
                    </span>
                  </div>
                  <div className="detail-row">
                    <span>Dyse løftet:</span>
                    <span>{pumpStatus?.nozzleLifted ? 'Ja' : 'Nei'}</span>
                  </div>
                  <div className="detail-row">
                    <span>Ventende betaling:</span>
                    <span>{pumpStatus?.hasPendingTransaction ? 'Ja' : 'Nei'}</span>
                  </div>
                </div>
              </div>
            )}

            {activePanel === 'reports' && (
              <div className="info-section">
                <h3>Rapporter</h3>
                <div className="report-tabs">
                  {(
                    [
                      ['omsetning', 'Omsetningsrapport'],
                      ['veibruksavgift', 'Veibruksavgift'],
                      ['uttak', 'Uttaksrapport'],
                      ['kvitteringer', 'Kvitteringer'],
                    ] as const
                  ).map(([key, label]) => (
                    <button
                      key={key}
                      type="button"
                      className={`report-tab ${reportTab === key ? 'active' : ''}`}
                      onClick={() => setReportTab(key)}
                    >
                      {label}
                    </button>
                  ))}
                </div>
                <div className="report-date-range">
                  <label className="report-date-label">
                    FRA DATO
                    <input
                      type="date"
                      className="report-date-input"
                      value={dateFrom}
                      onChange={(e) => setDateFrom(e.target.value)}
                    />
                  </label>
                  <label className="report-date-label">
                    TIL DATO
                    <input
                      type="date"
                      className="report-date-input"
                      value={dateTo}
                      onChange={(e) => setDateTo(e.target.value)}
                    />
                  </label>
                </div>
                <Link
                  to={reportTab === 'kvitteringer' ? `/transactions?from=${dateFrom}&to=${dateTo}` : '/reports'}
                  className="secondary-btn report-fetch-btn"
                >
                  Hent rapporter
                </Link>
              </div>
            )}

            {activePanel === 'history' && (
              <div className="info-section">
                <h3>Historikk og kvitteringer</h3>
                <p>Hent dokumentasjon for valgt periode. Denne delen er kun informativ.</p>
                <div className="report-date-range">
                  <label className="report-date-label">
                    FRA DATO
                    <input
                      type="date"
                      className="report-date-input"
                      value={dateFrom}
                      onChange={(e) => setDateFrom(e.target.value)}
                    />
                  </label>
                  <label className="report-date-label">
                    TIL DATO
                    <input
                      type="date"
                      className="report-date-input"
                      value={dateTo}
                      onChange={(e) => setDateTo(e.target.value)}
                    />
                  </label>
                </div>
                <Link
                  to={`/transactions?from=${dateFrom}&to=${dateTo}`}
                  className="secondary-btn report-fetch-btn"
                >
                  Hent kvitteringer
                </Link>
              </div>
            )}

            {activePanel === 'pricing' && (
              <div className="info-section">
                <h3>Prisadministrasjon</h3>
                <div className="price-control">
                  <label>
                    Pris per liter:
                    <input
                      type="number"
                      step="0.01"
                      value={pricePerLiter}
                      onChange={(e) => setPricePerLiter(parseFloat(e.target.value))}
                      className="price-input"
                    />
                  </label>
                </div>
                <Link to="/price-admin" className="secondary-btn">
                  💰 Åpne prisadministrasjon
                </Link>
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
            <div className={`status-indicator ${pumpStatus ? 'online' : 'offline'} ${state.toLowerCase()}`}>
              <span className="status-dot"></span>
              <span className="status-text">{statusText}</span>
            </div>
            {countdown !== null && countdown > 0 && (
              <div className="countdown-display">
                <span className="countdown-value">{countdown}s</span>
                <span className="countdown-label">Tid igjen</span>
              </div>
            )}
          </div>

          {/* Control Options */}
          <div className="control-options">
            <div className="option-group">
              <label>Dispensergruppe</label>
              <select className="option-select">
                <option>Pumpe 1</option>
              </select>
            </div>

            <div className="option-group">
              <label>Produkt</label>
              <select className="option-select">
                <option>LPG Propan</option>
              </select>
            </div>
          </div>

          {/* PRISVISNING - tax, amount/liter, price (matches dashboard image) */}
          <div className="prisvisning-section">
            <h3 className="prisvisning-title">PRISVISNING</h3>
            <div className="prisvisning-tax">
              <label className="radio-label">
                <input
                  type="radio"
                  name="roadTax"
                  checked={withRoadTax}
                  onChange={() => setWithRoadTax(true)}
                  disabled={isConnected}
                />
                <span>Med veibruksavgift</span>
              </label>
              <label className="radio-label">
                <input
                  type="radio"
                  name="roadTax"
                  checked={!withRoadTax}
                  onChange={() => setWithRoadTax(false)}
                  disabled={isConnected}
                />
                <span>Uten avgift</span>
              </label>
            </div>
            <div className="displays">
              <div className="display-box">
                <div className="display-label">Beløp å betale</div>
                {isConnected ? (
                  <div className="display-value">{displayAmount}</div>
                ) : (
                  <input
                    type="text"
                    inputMode="decimal"
                    className="option-input display-input"
                    placeholder="0000,00"
                    value={amountInput}
                    onChange={(e) => setAmountInput(e.target.value)}
                  />
                )}
              </div>
              <div className="display-box">
                <div className="display-label">Antall liter</div>
                {isConnected ? (
                  <div className="display-value">{displayVolume}</div>
                ) : (
                  <input
                    type="text"
                    inputMode="decimal"
                    className="option-input display-input"
                    placeholder="0000,00"
                    value={litersInput}
                    onChange={(e) => setLitersInput(e.target.value)}
                  />
                )}
              </div>
            </div>
            <div className="price-display">
              <div className="price-label">PRIS (kr/l)</div>
              <div className="price-value">{pricePerLiter.toFixed(2)}</div>
            </div>
            {/* Velg betaling - kredittavtale / bankterminal */}
            <div className="payment-choice-section">
              <span className="payment-choice-label">Velg betaling:</span>
              <div className="payment-choice-radios">
                <label className="radio-label">
                  <input
                    type="radio"
                    name="paymentMethod"
                    checked={paymentMethod === 'CREDIT'}
                    onChange={() => setPaymentMethod('CREDIT')}
                    disabled={isConnected}
                  />
                  <span>Kredittavtale</span>
                </label>
                <label className="radio-label">
                  <input
                    type="radio"
                    name="paymentMethod"
                    checked={paymentMethod === 'CARD'}
                    onChange={() => setPaymentMethod('CARD')}
                    disabled={isConnected}
                  />
                  <span>Bankterminal</span>
                </label>
              </div>
            </div>
            {isConnected && (
              <p className="prisvisning-lock-msg">
                Avgiftsvalg, pris og betalingsmåte låses når kunden kobler til dispenser.
              </p>
            )}
          </div>

          {!isConnected && (
            <p className="instruction-hint">
              Avgiftsvalg, pris og betalingsmåte låses når kunden kobler til dispenser.
            </p>
          )}

          {/* Koble til dispenser / START / STOPP */}
          <div className="control-buttons">
            {/* IDLE/VENTER: Koble til dispenser (same as FRI DISPENSER) */}
            {canRelease && (
              <button
                className="control-btn start-btn"
                onClick={handleReleaseDispenser}
                disabled={loading}
                title="Koble til dispenser – aktiverer betalingsflyt og reserverer dispenser"
              >
                {loading ? '⏳...' : 'Koble til dispenser'}
              </button>
            )}

            {/* READY_TO_PUMP: START - start fylling innen gyldighetstiden */}
            {isReadyToPump && (
              <button
                className="control-btn start-btn"
                onClick={handleStartPumping}
                disabled={loading || countdown === 0}
              >
                {loading ? '⏳...' : 'START'}
              </button>
            )}

            {/* PUMPING: STOPP */}
            {isPumping && (
              <button
                className="control-btn stop-btn"
                onClick={handleStop}
                disabled={loading}
              >
                {loading ? '⏳ Stopper...' : 'STOPP'}
              </button>
            )}

            {/* PAYMENT_PENDING: Betaling */}
            {isPaymentPending && (
              <>
                <button
                  className="control-btn start-btn"
                  onClick={() => handleConfirmPayment('CARD')}
                  disabled={loading}
                >
                  {loading ? '⏳...' : '💳 BETAL'}
                </button>
                <button
                  className="control-btn credit-btn"
                  onClick={() => handleConfirmPayment('CREDIT')}
                  disabled={loading}
                >
                  {loading ? '⏳...' : '🏪 KREDITT'}
                </button>
              </>
            )}
          </div>

          {/* Instruksjoner: START/STOPP skjer fysisk på dispenseren */}
          <div className="control-instructions">
            {isReadyToPump && countdown !== null && countdown > 0 && (
              <p className="instruction-time">
                Du har <strong>{countdown}</strong> sekunder på å starte fylling. Hvis fylling ikke startes innen tiden, frigjøres dispenser automatisk.
              </p>
            )}
            <p className="instruction-main">
              Hold START inne for å fylle. Slipp START for å stoppe. START/STOPP styres fysisk på dispenseren.
            </p>
          </div>

          {/* Reset Button (for errors or stuck states) */}
          {(state === 'ERROR' || pumpStatus?.hasPendingTransaction) && !isPaymentPending && (
            <button
              className="action-btn reset-btn"
              onClick={handleReset}
              disabled={loading}
            >
              🔄 Nullstill system
            </button>
          )}

          {/* Status Messages */}
          {isReadyToPump && (
            <div className="info-status">
              ✅ Pumpe frigjort - kunden kan starte fylling
            </div>
          )}

          {isPumping && (
            <div className="filling-status">
              ⛽ Fylling pågår...
            </div>
          )}

          {isPaymentPending && (
            <div className="finished-status">
              💰 Fylling fullført - bekreft betaling
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
