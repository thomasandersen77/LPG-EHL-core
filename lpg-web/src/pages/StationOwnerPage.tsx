import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { pumpApi, type PumpStatus } from '../api/pump';
import '../styles/DispenserControl.css';

type ActivePanel = 'status' | 'reports' | 'history' | 'pricing';

export function StationOwnerPage() {
  const navigate = useNavigate();
  const { isLoggedIn, stationName, logout } = useAuth();

  // State management
  const [pumpStatus, setPumpStatus] = useState<PumpStatus | null>(null);
  const [activePanel, setActivePanel] = useState<ActivePanel>('status');
  const [pricePerLiter, setPricePerLiter] = useState(17.99);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [maxAmount, setMaxAmount] = useState(2000);

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

  // 60-second countdown for AUTHORIZED_WAITING and READY_TO_PUMP
  useEffect(() => {
    if (pumpStatus?.state === 'AUTHORIZED_WAITING' || pumpStatus?.state === 'READY_TO_PUMP') {
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

  // Step 1: Card swipe (authorize)
  const handleCardSwipe = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.cardSwipe(1, maxAmount);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke registrere kort: ' + message);
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Unblock (FRI DISPENSER)
  const handleUnblock = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.unblock(1);
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
  const isIdle = state === 'IDLE' && !pumpStatus?.hasPendingTransaction;
  const isAuthorizedWaiting = state === 'AUTHORIZED_WAITING';
  const isReadyToPump = state === 'READY_TO_PUMP';
  const isPumping = state === 'PUMPING';
  const isPaymentPending = state === 'PAYMENT_PENDING' && pumpStatus?.hasPendingTransaction;

  // Format display values
  const displayAmount = pumpStatus?.amountKr?.toFixed(2) || '0.00';
  const displayVolume = pumpStatus?.volumeLitres?.toFixed(2) || '0.00';
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
                <h3>Rapportering</h3>
                <p>Generer rapporter for fylleoperasjoner</p>
                <Link to="/reports" className="secondary-btn">
                  📊 Åpne rapporter
                </Link>
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
              <label>Maks beløp</label>
              <input
                type="number"
                value={maxAmount}
                onChange={(e) => setMaxAmount(Number(e.target.value))}
                className="option-input"
                disabled={!isIdle}
              />
            </div>

            <div className="option-group">
              <label>Produkt</label>
              <select className="option-select">
                <option>LPG Propan</option>
              </select>
            </div>
          </div>

          {/* Amount Displays */}
          <div className="displays">
            <div className="display-box">
              <div className="display-label">Beløp (kr)</div>
              <div className="display-value">{displayAmount}</div>
            </div>

            <div className="display-box">
              <div className="display-label">Volum (liter)</div>
              <div className="display-value">{displayVolume}</div>
            </div>
          </div>

          {/* Price Display */}
          <div className="price-display">
            <div className="price-label">PRIS KR/L</div>
            <div className="price-value">{pricePerLiter.toFixed(2)}</div>
          </div>

          {/* Control Buttons - State-based */}
          <div className="control-buttons">
            {/* IDLE: Show card swipe button */}
            {isIdle && (
              <button
                className="control-btn start-btn"
                onClick={handleCardSwipe}
                disabled={loading}
              >
                {loading ? '⏳...' : '💳 REGISTRER KORT'}
              </button>
            )}

            {/* AUTHORIZED_WAITING: Card registered, show FRI DISPENSER */}
            {isAuthorizedWaiting && (
              <button
                className="control-btn start-btn"
                onClick={handleUnblock}
                disabled={loading || countdown === 0}
              >
                {loading ? '⏳...' : '🔓 FRI DISPENSER'}
              </button>
            )}

            {/* READY_TO_PUMP: Pump unblocked, show START PUMPING */}
            {isReadyToPump && (
              <button
                className="control-btn start-btn"
                onClick={handleStartPumping}
                disabled={loading || countdown === 0}
              >
                {loading ? '⏳...' : '⛽ START FYLLING'}
              </button>
            )}

            {/* PUMPING: Show STOP button */}
            {isPumping && (
              <button
                className="control-btn stop-btn"
                onClick={handleStop}
                disabled={loading}
              >
                {loading ? '⏳ Stopper...' : '🛑 STOPP'}
              </button>
            )}

            {/* PAYMENT_PENDING: Show payment method choice */}
            {isPaymentPending && (
              <>
                <button
                  className="control-btn start-btn"
                  onClick={() => handleConfirmPayment('CARD')}
                  disabled={loading}
                >
                  {loading ? '⏳...' : '💳 BETAL MED KORT'}
                </button>
                <button
                  className="control-btn credit-btn"
                  onClick={() => handleConfirmPayment('CREDIT')}
                  disabled={loading}
                >
                  {loading ? '⏳...' : '🏪 BETAL MED KREDITT'}
                </button>
              </>
            )}
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
          {isAuthorizedWaiting && (
            <div className="info-status">
              💳 Kort registrert - trykk FRI DISPENSER for å starte
            </div>
          )}

          {isReadyToPump && (
            <div className="info-status">
              ✅ Pumpe frigjort - trykk START FYLLING
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
