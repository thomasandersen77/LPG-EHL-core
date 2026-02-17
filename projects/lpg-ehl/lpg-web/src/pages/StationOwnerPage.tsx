import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { pumpApi, type PumpStatus } from '../api/pump';
import { useSmoothCounter } from '../hooks/useSmoothCounter';
import { useQuery } from '@tanstack/react-query';
import { fetchPeriodSummary } from '../api/reports';
import { fetchTransactions, type TransactionDto } from '../api/transactions';
import axios from 'axios';
import '../styles/DispenserControl.css';

const API_URL = import.meta.env.VITE_API_URL || '/api/v1';
const WS_BASE_URL = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_EMULATOR_BASE_URL || window.location.origin).replace(/^http/, 'ws');

// View types for the dashboard
type ActiveView = 'dashboard' | 'reports' | 'receipts' | 'price' | 'customers' | 'klippekort';
type ReportType = 'omsetning' | 'veibruksavgift' | 'uttak';

// Mock data for customers (will be replaced with real API later)
const mockCustomers = [
  { id: '1', name: 'Ola Nordmann', type: 'Privatkunde', email: 'ola@example.com', phone: '99887766', klippekort: 3, kreditt: null },
  { id: '2', name: 'Norsk Transport AS', type: 'Bedrift', email: 'post@norsktransport.no', phone: '22334455', klippekort: null, kreditt: { limit: 50000, used: 12500 } },
  { id: '3', name: 'Kari Hansen', type: 'Privatkunde', email: 'kari@example.com', phone: '91234567', klippekort: 5, kreditt: null },
  { id: '4', name: 'Byggmester Bygg AS', type: 'Bedrift', email: 'faktura@byggmester.no', phone: '55667788', klippekort: null, kreditt: { limit: 100000, used: 45000 } },
];


export function StationOwnerPage() {
  const navigate = useNavigate();
  const { isLoggedIn, logout } = useAuth();

  // State management
  const [pumpStatus, setPumpStatus] = useState<PumpStatus | null>(null);
  const [activeView, setActiveView] = useState<ActiveView>('dashboard');
  const [reportType, setReportType] = useState<ReportType>('omsetning');
  const [dateFrom, setDateFrom] = useState(() => {
    const d = new Date();
    d.setDate(1);
    return d.toISOString().slice(0, 10);
  });
  const [dateTo, setDateTo] = useState(() => new Date().toISOString().slice(0, 10));
  const [withRoadTax, setWithRoadTax] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null);

  // Price state
  const [priceWithTax, setPriceWithTax] = useState(17.99);
  const [priceWithoutTax, setPriceWithoutTax] = useState(11.50);
  const [showPriceModal, setShowPriceModal] = useState(false);
  const [newPriceWithTax, setNewPriceWithTax] = useState('');
  const [newPriceWithoutTax, setNewPriceWithoutTax] = useState('');

  // Klippekort state
  const [klippekortSize, setKlippekortSize] = useState(6);

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
        const status = await pumpApi.getStatus();
        setPumpStatus(status);
        if (status.pricePerLitreKr > 0) {
          setPriceWithTax(status.pricePerLitreKr);
        }
      } catch (err) {
        // Silently fail - don't spam errors
      }
    };

    pollStatus();
    const interval = setInterval(pollStatus,
      pumpStatus?.state === 'PUMPING' ? 5000 : 10000 // Relax polling when WebSocket is used
    );
    return () => clearInterval(interval);
  }, [pumpStatus?.state]);

  // WebSocket connection for real-time updates
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const ws = new WebSocket(`${WS_BASE_URL}/ws/logs`);
    wsRef.current = ws;

    ws.onopen = () => {
      // Subscribe to necessary channels
      ws.send(JSON.stringify({
        action: 'subscribe',
        channels: ['api', 'service', 'protocol']
      }));
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);

        // Handle pump updates
        if (data.type === 'pump_update' || data.type === 'fueling_update' || data.eventType === 'FUELING_UPDATE') {
          const volumeLitres = Number(data.volumeLitres ?? data.volumeLiters ?? data.liters ?? 0);
          const amountKr = Number(data.amountKr ?? data.amount ?? 0);
          const pricePerLitreKr = Number(data.pricePerLitreKr ?? data.pricePerLiterKr ?? data.pricePerLiter ?? 0);
          setPumpStatus(prev => ({
            ...prev,
            state: data.state || prev?.state,
            address: data.address || prev?.address,
            volumeLitres,
            amountKr,
            pricePerLitreKr,
            nozzleLifted: data.nozzleLifted ?? prev?.nozzleLifted,
            hasPendingTransaction: data.hasPendingTransaction ?? prev?.hasPendingTransaction
          } as PumpStatus));

          if (pricePerLitreKr > 0) {
            setPriceWithTax(pricePerLitreKr);
          }
        }

        // Handle price updates
        if (data.type === 'price_update') {
          if (data.pricePerLiterKr > 0) {
            setPriceWithTax(data.pricePerLiterKr);
          }
        }
      } catch (e) {
        console.error('Error parsing WebSocket message:', e);
      }
    };

    return () => {
      ws.close();
    };
  }, []);

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

  // Fetch period summary for reports
  const { data: periodSummary, isLoading: isLoadingReport } = useQuery({
    queryKey: ['period-summary', dateFrom, dateTo],
    queryFn: () => fetchPeriodSummary(dateFrom, dateTo),
    enabled: activeView === 'reports',
  });
  const {
    data: receiptTransactions,
    isFetching: isLoadingReceipts,
    refetch: refetchReceipts,
  } = useQuery({
    queryKey: ['receipt-transactions', dateFrom, dateTo],
    queryFn: () =>
      fetchTransactions({
        from: `${dateFrom}T00:00:00`,
        to: `${dateTo}T23:59:59`,
        page: 0,
        size: 200,
      }),
    enabled: false,
  });

  // Pump control functions
  const handleReleaseDispenser = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.releaseDispenser();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke frigjøre pumpe: ' + message);
    } finally {
      setLoading(false);
    }
  };

  const handleStartPumping = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.startPumping();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke starte pumping: ' + message);
    } finally {
      setLoading(false);
    }
  };

  const handleStop = async () => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.block();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke stoppe fylling: ' + message);
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmPayment = async (method: 'CARD' | 'CREDIT') => {
    if (loading) return;
    setLoading(true);
    setError(null);

    try {
      await pumpApi.confirmPayment(method);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ukjent feil';
      setError('Kunne ikke bekrefte betaling: ' + message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleSavePrice = async () => {
    const withTax = parseFloat(newPriceWithTax);
    const withoutTax = parseFloat(newPriceWithoutTax);

    if (!isNaN(withTax) && withTax > 0) {
      setPriceWithTax(withTax);
    }
    if (!isNaN(withoutTax) && withoutTax > 0) {
      setPriceWithoutTax(withoutTax);
    }

    // Call API to update price if available
    try {
      await axios.post(`${API_URL}/prices/update`, {
        pricePerLiter: withTax || priceWithTax,
      });
      // PART 4: Backend will broadcast price_update, queryClient invalidation handled in WS listener
    } catch (err) {
      // Price API might not be available, that's ok
    }

    setShowPriceModal(false);
    setNewPriceWithTax('');
    setNewPriceWithoutTax('');
  };

  const handleFetchReceipts = async () => {
    setActiveView('receipts');
    await refetchReceipts();
  };

  const getPaymentMethodLabel = (transaction: TransactionDto) => {
    switch (transaction.paymentType) {
      case 'CARD':
        return 'Kort';
      case 'CREDIT':
        return 'Kreditt';
      default:
        return transaction.paymentType || 'Ukjent';
    }
  };

  const formatReceiptDate = (timestamp: string) =>
    new Date(timestamp).toLocaleDateString('nb-NO');

  const formatReceiptTime = (timestamp: string) =>
    new Date(timestamp).toLocaleTimeString('nb-NO', { hour: '2-digit', minute: '2-digit' });

  // Determine pump state
  const state = pumpStatus?.state || 'OFFLINE';
  const canRelease = (state === 'IDLE' || state === 'AUTHORIZED_WAITING') && !pumpStatus?.hasPendingTransaction;
  const isReadyToPump = state === 'READY_TO_PUMP';
  const isPumping = state === 'PUMPING';
  const isPaymentPending = state === 'PAYMENT_PENDING' && pumpStatus?.hasPendingTransaction;
  const isConnected = isReadyToPump || isPumping || isPaymentPending;
  const isOnline = pumpStatus != null && state !== 'OFFLINE';

  // Smooth counters for real-time feel
  const smoothAmount = useSmoothCounter(pumpStatus?.amountKr || 0, isPumping);
  const smoothVolume = useSmoothCounter(pumpStatus?.volumeLitres || 0, isPumping);

  // Display values
  const displayAmount = isConnected
    ? smoothAmount.toFixed(2)
    : '0000,00';
  const displayVolume = isConnected
    ? smoothVolume.toFixed(2)
    : '0000,00';
  const currentPrice = withRoadTax ? priceWithTax : priceWithoutTax;

  return (
    <div className="dispenser-control">
      {/* Header Bar */}
      <div className="station-header-new">
        <div className="station-info-new">
          <div className="station-logo-icon">⛽</div>
          <span className="station-logo-text">Min LPG</span>
        </div>
        <div className="header-right">
          <span className="header-section-label">Dispenserkontroll</span>
          <div className="header-actions-new">
            <button onClick={handleLogout} className="logout-btn-new">
              Logg ut
            </button>
          </div>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="error-banner">
          {error}
          <button onClick={() => setError(null)} className="error-close">✕</button>
        </div>
      )}

      <div className="control-container-v2">
        {/* Left Panel - Overview, Reports, History */}
        <div className="left-panel-v2">
          {/* Status Section */}
          <div className="status-row-v2">
            <span className="status-label-v2">STATUS</span>
            <span className={`status-badge-v2 ${isOnline ? 'online' : 'offline'}`}>
              {isOnline ? 'ONLINE' : 'OFFLINE'}
            </span>
          </div>

          {/* RAPPORTER Section Box */}
          <div className="rapporter-box-v2">
            <div className="rapporter-header-v2">RAPPORTER</div>
            <div className="rapporter-tabs-v2">
              <button
                className={`rapporter-tab-v2 ${reportType === 'omsetning' ? 'active' : ''}`}
                onClick={() => { setReportType('omsetning'); setActiveView('reports'); }}
              >
                Omsetningsrapport
              </button>
              <button
                className={`rapporter-tab-v2 ${reportType === 'veibruksavgift' ? 'active' : ''}`}
                onClick={() => { setReportType('veibruksavgift'); setActiveView('reports'); }}
              >
                Veibruksavgift
              </button>
              <button
                className={`rapporter-tab-v2 ${reportType === 'uttak' ? 'active' : ''}`}
                onClick={() => { setReportType('uttak'); setActiveView('reports'); }}
              >
                Uttaksrapport
              </button>
            </div>
          </div>

          {/* Action Buttons - ENDRE PRIS / KVITTERINGER */}
          <div className="action-row-v2">
            <button
              className="action-btn-v2"
              onClick={() => setShowPriceModal(true)}
            >
              ENDRE PRIS
            </button>
            <button
              className="action-btn-v2"
              onClick={() => setActiveView('receipts')}
            >
              KVITTERINGER
            </button>
          </div>

          {/* Date Range */}
          <div className="date-row-v2">
            <div className="date-field-v2">
              <label>FRA DATO</label>
              <input
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
              />
            </div>
            <div className="date-field-v2">
              <label>TIL DATO</label>
              <input
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
              />
            </div>
          </div>

          {/* Hent kvitteringer button */}
          <button
            className="hent-kvitteringer-btn-v2"
            onClick={() => void handleFetchReceipts()}
          >
            Hent kvitteringer
          </button>

          {/* Additional Navigation - Hidden on main view for cleaner UI */}
          {activeView === 'dashboard' && (
            <div className="extra-nav-v2">
              <button
                className="extra-nav-btn-v2"
                onClick={() => setActiveView('customers')}
              >
                👥 Mine kunder
              </button>
              <button
                className="extra-nav-btn-v2"
                onClick={() => setActiveView('klippekort')}
              >
                🎫 Klippekort
              </button>
            </div>
          )}
        </div>

        {/* Right Panel - Dispenser Control */}
        <div className="right-panel-v2">
          {/* Show different content based on activeView */}
          {activeView === 'dashboard' && (
            <>
              {/* PRISVISNING Section */}
              <div className="prisvisning-box-v2">
                <div className="prisvisning-label-v2">PRISVISNING</div>
                <div className="tax-options-v2">
                  <label className="tax-radio-v2">
                    <input
                      type="radio"
                      name="taxOption"
                      checked={withRoadTax}
                      onChange={() => setWithRoadTax(true)}
                      disabled={isConnected}
                    />
                    <span>Med veibruksavgift</span>
                  </label>
                  <label className="tax-radio-v2">
                    <input
                      type="radio"
                      name="taxOption"
                      checked={!withRoadTax}
                      onChange={() => setWithRoadTax(false)}
                      disabled={isConnected}
                    />
                    <span>Uten avgift</span>
                  </label>
                </div>

                <button className={`tax-btn-v2 ${withRoadTax ? 'with-tax' : 'without-tax'}`}>
                  {withRoadTax ? 'MED VEIBRUKSAVGIFT' : 'UTEN VEIBRUKSAVGIFT'}
                </button>

                {!withRoadTax && (
                  <div className="tax-warning-v2">
                    DU HAR ÅPNET FOR AT TANKING SKJER UTEN VEIBRUKSAVGIFT. VED Å FORTSETTE AKSEPTERER DU LOVVERKET RUNDT DETTE. <a href="#" className="warning-link-v2">LES MER</a>
                  </div>
                )}
              </div>

              {/* Beløp å betale */}
              <div className="display-field-v2">
                <div className="display-label-v2">Beløp å betale, KR</div>
                <div className="display-box-v2">
                  {isConnected ? displayAmount.replace('.', ',') : '0000,00'}
                </div>
              </div>

              {/* Antall liter */}
              <div className="display-field-v2">
                <div className="display-label-v2">Antall liter</div>
                <div className="display-box-v2">
                  {isConnected ? displayVolume.replace('.', ',') : '0000,00'}
                </div>
              </div>

              {/* Pris, KR/L */}
              <div className="display-field-v2">
                <div className="display-label-v2">Pris, KR/L</div>
                <div className="display-box-v2 price">
                  {currentPrice.toFixed(2).replace('.', ',')}
                </div>
              </div>

              {/* Control Buttons - Always show START and STOPP stacked */}
              <div className="control-btns-v2">
                <button
                  className={`ctrl-btn-v2 start ${(!canRelease && !isReadyToPump) ? 'disabled' : ''}`}
                  onClick={canRelease ? handleReleaseDispenser : (isReadyToPump ? handleStartPumping : undefined)}
                  disabled={loading || (!canRelease && !isReadyToPump)}
                >
                  {loading && (canRelease || isReadyToPump) ? '⏳...' : 'START'}
                </button>
                <button
                  className={`ctrl-btn-v2 stop ${!isPumping ? 'disabled' : ''}`}
                  onClick={isPumping ? handleStop : undefined}
                  disabled={loading || !isPumping}
                >
                  {loading && isPumping ? '⏳...' : 'STOPP'}
                </button>
              </div>

              {/* Payment buttons when payment is pending */}
              {isPaymentPending && (
                <div className="payment-btns-v2">
                  <button
                    className="pay-btn-v2 card"
                    onClick={() => handleConfirmPayment('CARD')}
                    disabled={loading}
                  >
                    💳 BETAL MED KORT
                  </button>
                  <button
                    className="pay-btn-v2 credit"
                    onClick={() => handleConfirmPayment('CREDIT')}
                    disabled={loading}
                  >
                    🏪 KREDITT
                  </button>
                </div>
              )}

              {/* Status Messages */}
              {countdown !== null && countdown > 0 && (
                <div className="status-msg-v2 countdown">
                  Du har <strong>{countdown}</strong> sekunder på å starte fylling.
                </div>
              )}

              {isPumping && (
                <div className="status-msg-v2 pumping">
                  ⛽ Fylling pågår...
                </div>
              )}

              {isPaymentPending && (
                <div className="status-msg-v2 payment">
                  💰 Fylling fullført - bekreft betaling
                </div>
              )}
            </>
          )}

          {activeView === 'reports' && (
            <div className="subview-content-v2">
              <h2>{reportType === 'omsetning' && 'Omsetningsrapport'}{reportType === 'veibruksavgift' && 'Veibruksavgift'}{reportType === 'uttak' && 'Uttaksrapport'}</h2>
              <p className="subview-period">Periode: {dateFrom} - {dateTo}</p>

              {isLoadingReport ? (
                <div className="loading-msg-v2">Laster rapport...</div>
              ) : periodSummary ? (
                <div className="report-stats-v2">
                  {reportType === 'omsetning' && (
                    <>
                      <div className="stat-row-v2"><span>Totalt salg</span><span>{periodSummary.totalAmountKr.toFixed(2)} kr</span></div>
                      <div className="stat-row-v2"><span>Antall transaksjoner</span><span>{periodSummary.totalTransactions}</span></div>
                      <div className="stat-row-v2"><span>Totalt volum</span><span>{periodSummary.totalVolumeLiters.toFixed(2)} L</span></div>
                    </>
                  )}
                  {reportType === 'veibruksavgift' && (
                    <>
                      <div className="stat-row-v2"><span>Avgiftspliktig volum</span><span>{periodSummary.totalVolumeLiters.toFixed(2)} L</span></div>
                      <div className="stat-row-v2"><span>Estimert avgift</span><span>{(periodSummary.totalVolumeLiters * 2.0).toFixed(2)} kr</span></div>
                    </>
                  )}
                  {reportType === 'uttak' && (
                    <>
                      <div className="stat-row-v2"><span>Totalt LPG-uttak</span><span>{periodSummary.totalVolumeLiters.toFixed(2)} L</span></div>
                      <div className="stat-row-v2"><span>Antall fyllinger</span><span>{periodSummary.totalTransactions}</span></div>
                    </>
                  )}
                </div>
              ) : (
                <div className="no-data-v2">Ingen data for valgt periode</div>
              )}
              <button className="back-btn-v2" onClick={() => setActiveView('dashboard')}>← Tilbake</button>
            </div>
          )}

          {activeView === 'receipts' && (
            <div className="subview-content-v2">
              <h2>Kvitteringer</h2>
              <div className="date-row-v2" style={{ marginBottom: '1rem' }}>
                <div className="date-field-v2"><label>FRA DATO</label><input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} /></div>
                <div className="date-field-v2"><label>TIL DATO</label><input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} /></div>
              </div>
              <button className="hent-kvitteringer-btn-v2" style={{ marginBottom: '1rem' }} onClick={() => void handleFetchReceipts()}>
                Hent kvitteringer
              </button>
              <div className="receipts-table-v2">
                <div className="receipts-hdr-v2"><span>DATO</span><span>TID</span><span>BELØP</span><span>BETALING</span><span>KVITTERING</span></div>
                {isLoadingReceipts ? (
                  <div className="loading-msg-v2">Laster kvitteringer...</div>
                ) : (receiptTransactions?.content.length ?? 0) === 0 ? (
                  <div className="no-data-v2">Ingen transaksjoner i valgt periode</div>
                ) : (
                  receiptTransactions?.content.map((transaction) => (
                    <div key={transaction.transactionId} className="receipts-row-v2">
                      <span>{formatReceiptDate(transaction.timestamp)}</span>
                      <span>{formatReceiptTime(transaction.timestamp)}</span>
                      <span>{transaction.amountKr.toFixed(2)} kr</span>
                      <span>{getPaymentMethodLabel(transaction)}</span>
                      <a
                        href={`${API_URL}/transactions/${transaction.transactionId}/receipt.pdf`}
                        className="pdf-link-v2"
                        target="_blank"
                        rel="noreferrer"
                      >
                        Last ned (PDF)
                      </a>
                    </div>
                  ))
                )}
              </div>
              <p className="note-v2">Kvitteringer hentes fra transaksjoner i valgt periode.</p>
              <button className="back-btn-v2" onClick={() => setActiveView('dashboard')}>← Tilbake</button>
            </div>
          )}

          {activeView === 'customers' && (
            <div className="subview-content-v2">
              <h2>Mine kunder</h2>
              <p className="note-v2">Oversikt over alle kunder knyttet til stasjonen.</p>
              <div className="customers-tbl-v2">
                <div className="customers-hdr-v2"><span>NAVN</span><span>TYPE</span><span>KONTAKT</span><span>KLIPPEKORT</span><span>KREDITT</span></div>
                {mockCustomers.map((c) => (
                  <div key={c.id} className="customers-row-v2">
                    <span>{c.name}</span>
                    <span className={c.type === 'Bedrift' ? 'type-business' : 'type-private'}>{c.type}</span>
                    <span>{c.email}</span>
                    <span>{c.klippekort !== null ? `${c.klippekort}/${klippekortSize}` : '-'}</span>
                    <span>{c.kreditt ? `${c.kreditt.used.toLocaleString()}/${c.kreditt.limit.toLocaleString()} kr` : '-'}</span>
                  </div>
                ))}
              </div>
              <button className="back-btn-v2" onClick={() => setActiveView('dashboard')}>← Tilbake</button>
            </div>
          )}

          {activeView === 'klippekort' && (
            <div className="subview-content-v2">
              <h2>Klippekort</h2>
              <p className="note-v2">Stasjonsbasert lojalitetsfunksjon for propanflasker.</p>
              <div className="info-box-v2">Klippekort gjelder kun kjøp/fylling av propanflasker og kan ikke benyttes ved LPG-dispenserfylling.</div>
              <div className="setting-row-v2">
                <label>Antall fyllinger for gratis:</label>
                <input type="number" min="3" max="10" value={klippekortSize} onChange={(e) => setKlippekortSize(parseInt(e.target.value) || 6)} />
                <button className="save-btn-v2">Lagre</button>
              </div>
              <div className="rule-box-v2"><strong>Gjeldende regel:</strong> {klippekortSize} kjøp → 1 gratis propanflaske</div>
              <button className="back-btn-v2" onClick={() => setActiveView('dashboard')}>← Tilbake</button>
            </div>
          )}
        </div>
      </div>

      {/* Price Modal */}
      {showPriceModal && (
        <div className="modal-overlay" onClick={() => setShowPriceModal(false)}>
          <div className="price-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Endre pris</h3>
            <p className="modal-description">
              Sett ny pris for denne dispenseren. Når API-et er koblet til vil disse verdiene sendes videre.
            </p>

            <div className="price-input-group">
              <label>MED VEIBRUKSAVGIFT (KR/L)</label>
              <input
                type="number"
                step="0.01"
                value={newPriceWithTax}
                onChange={(e) => setNewPriceWithTax(e.target.value)}
                placeholder={priceWithTax.toString()}
                className="price-modal-input"
              />
            </div>

            <div className="price-input-group">
              <label>UTEN VEIBRUKSAVGIFT (KR/L)</label>
              <input
                type="number"
                step="0.01"
                value={newPriceWithoutTax}
                onChange={(e) => setNewPriceWithoutTax(e.target.value)}
                placeholder={priceWithoutTax.toString()}
                className="price-modal-input"
              />
            </div>

            <div className="modal-buttons">
              <button className="modal-btn save" onClick={handleSavePrice}>
                Lagre
              </button>
              <button className="modal-btn cancel" onClick={() => setShowPriceModal(false)}>
                Avbryt
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
