import { Link } from 'react-router-dom';
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { terminalDiagApi, type TerminalAction } from '../api/terminalDiag';

export function PaymentTerminalDiagnosticsPage() {
  const [lastAction, setLastAction] = useState<string>('');
  const [lastResponse, setLastResponse] = useState<unknown>(null);
  const [lastError, setLastError] = useState<string>('');

  const [purchaseAmountMinor, setPurchaseAmountMinor] = useState<number>(12500);
  const [purchaseCurrency, setPurchaseCurrency] = useState<string>('NOK');
  const [purchaseOperatorId, setPurchaseOperatorId] = useState<string>('0000');
  const [purchaseOptionalData, setPurchaseOptionalData] = useState<string>('');
  const [purchaseClientRequestId, setPurchaseClientRequestId] = useState<string>('');
  const [purchasePreAvstemming, setPurchasePreAvstemming] = useState<boolean>(false);
  const [purchasePreAvstemmingPassword, setPurchasePreAvstemmingPassword] = useState<string>('0000');
  const [purchasePreAvstemmingTimeout, setPurchasePreAvstemmingTimeout] = useState<number>(300);

  const [refundAmountMinor, setRefundAmountMinor] = useState<number>(12500);
  const [refundOperatorId, setRefundOperatorId] = useState<string>('0000');
  const [refundOptionalData, setRefundOptionalData] = useState<string>('');
  const [refundClientRequestId, setRefundClientRequestId] = useState<string>('');
  const [refundPreAvstemming, setRefundPreAvstemming] = useState<boolean>(false);
  const [refundPreAvstemmingPassword, setRefundPreAvstemmingPassword] = useState<string>('0000');
  const [refundPreAvstemmingTimeout, setRefundPreAvstemmingTimeout] = useState<number>(300);

  const [cashbackPurchaseMinor, setCashbackPurchaseMinor] = useState<number>(10000);
  const [cashbackCashbackMinor, setCashbackCashbackMinor] = useState<number>(2000);
  const [cashbackCurrency, setCashbackCurrency] = useState<string>('NOK');
  const [cashbackOperatorId, setCashbackOperatorId] = useState<string>('4321');
  const [cashbackOptionalData, setCashbackOptionalData] = useState<string>('');
  const [cashbackClientRequestId, setCashbackClientRequestId] = useState<string>('');

  const [adminPassword, setAdminPassword] = useState<string>('0000');
  const [adminCode, setAdminCode] = useState<number>(12598);

  const [eventsSince, setEventsSince] = useState<string>('0');

  const [diagJson, setDiagJson] = useState<string>('');
  const [tldType, setTldType] = useState<string>('D0');
  const [tldData, setTldData] = useState<string>('');
  const [confirmId, setConfirmId] = useState<number>(0);
  const [confirmAllow, setConfirmAllow] = useState<boolean>(true);

  const actionMutation = useMutation({
    mutationFn: async (action: TerminalAction) => {
      const params = action.params
        ? Object.fromEntries(
            Object.entries(action.params).map(([k, v]) => [k, String(v ?? '')])
          ) as Record<string, string>
        : undefined;
      return terminalDiagApi.execute({ ...action, params });
    },
    onSuccess: (data, action) => {
      setLastAction(action.label);
      setLastResponse(data);
      setLastError('');
    },
    onError: (error: any, action) => {
      setLastAction(action.label);
      setLastResponse(null);
      const responseData = error?.response?.data;
      setLastError(responseData ? JSON.stringify(responseData, null, 2) : String(error));
    }
  });

  const triggerAction = (action: TerminalAction) => actionMutation.mutate(action);
  const formatJson = (payload: unknown): string => JSON.stringify(payload, null, 2) ?? '';

  const purchasePayload: Record<string, unknown> = {
    AmountMinor: purchaseAmountMinor,
    Currency: purchaseCurrency,
    OperatorId: purchaseOperatorId
  };
  if (purchaseOptionalData) purchasePayload.OptionalData = purchaseOptionalData;
  if (purchaseClientRequestId) purchasePayload.ClientRequestId = purchaseClientRequestId;
  if (purchasePreAvstemming) {
    purchasePayload.PreAvstemming = {
      Enabled: true,
      Password: purchasePreAvstemmingPassword,
      TimeoutSeconds: purchasePreAvstemmingTimeout
    };
  }

  const refundPayload: Record<string, unknown> = {
    AmountMinor: refundAmountMinor,
    OperatorId: refundOperatorId
  };
  if (refundOptionalData) refundPayload.OptionalData = refundOptionalData;
  if (refundClientRequestId) refundPayload.ClientRequestId = refundClientRequestId;
  if (refundPreAvstemming) {
    refundPayload.PreAvstemming = {
      Enabled: true,
      Password: refundPreAvstemmingPassword,
      TimeoutSeconds: refundPreAvstemmingTimeout
    };
  }

  const cashbackPayload: Record<string, unknown> = {
    PurchaseMinor: cashbackPurchaseMinor,
    CashbackMinor: cashbackCashbackMinor,
    Currency: cashbackCurrency,
    OperatorId: cashbackOperatorId
  };
  if (cashbackOptionalData) cashbackPayload.OptionalData = cashbackOptionalData;
  if (cashbackClientRequestId) cashbackPayload.ClientRequestId = cashbackClientRequestId;

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white p-4">
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="text-center mb-8">
          <Link
            to="/diagnose"
            className="inline-flex items-center text-blue-400 hover:text-blue-300 mb-4 transition-colors"
          >
            ← Tilbake til Diagnose
          </Link>
          <h1 className="text-4xl font-bold mb-2">💳 Payment Terminal API</h1>
          <p className="text-gray-400">Kjør API-kall direkte mot betalingsterminalen</p>
        </div>

        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h2 className="text-xl font-bold mb-4">🔗 Backend proxy</h2>
          <p className="text-gray-400 text-sm">
            API-kall går via backend (payment.terminal.base-url). Ingen direkte kobling fra nettleser.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="space-y-6">
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">❤️ Health</h2>
              <button
                onClick={() => triggerAction({ label: 'Health', method: 'get', path: '/health' })}
                disabled={actionMutation.isPending}
                className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 rounded-lg font-bold transition"
              >
                {actionMutation.isPending ? '⏳ Henter...' : 'Hent Health'}
              </button>
            </div>

            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">🧭 Terminal</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <button
                  onClick={() => triggerAction({ label: 'Terminal Status', method: 'get', path: '/v1/terminal/status' })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Status
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Terminal Open', method: 'post', path: '/v1/terminal/open' })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Open
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Terminal Close', method: 'post', path: '/v1/terminal/close' })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Close
                </button>
              </div>
            </div>

            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">💰 Purchase</h2>
              <div className="grid grid-cols-2 gap-3">
                <input
                  type="number"
                  value={purchaseAmountMinor}
                  onChange={(event) => setPurchaseAmountMinor(Number(event.target.value))}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="AmountMinor"
                />
                <input
                  value={purchaseCurrency}
                  onChange={(event) => setPurchaseCurrency(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="Currency"
                />
                <input
                  value={purchaseOperatorId}
                  onChange={(event) => setPurchaseOperatorId(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="OperatorId"
                />
                <input
                  value={purchaseClientRequestId}
                  onChange={(event) => setPurchaseClientRequestId(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="ClientRequestId"
                />
                <input
                  value={purchaseOptionalData}
                  onChange={(event) => setPurchaseOptionalData(event.target.value)}
                  className="col-span-2 bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="OptionalData"
                />
              </div>
              <div className="mt-3 grid grid-cols-1 md:grid-cols-3 gap-3">
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={purchasePreAvstemming}
                    onChange={(event) => setPurchasePreAvstemming(event.target.checked)}
                    className="w-4 h-4 rounded"
                  />
                  Pre-avstemming
                </label>
                <input
                  value={purchasePreAvstemmingPassword}
                  onChange={(event) => setPurchasePreAvstemmingPassword(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="Password"
                />
                <input
                  type="number"
                  value={purchasePreAvstemmingTimeout}
                  onChange={(event) => setPurchasePreAvstemmingTimeout(Number(event.target.value))}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="TimeoutSeconds"
                />
              </div>
              <button
                onClick={() => triggerAction({ label: 'Purchase', method: 'post', path: '/v1/payments/purchase', body: purchasePayload })}
                disabled={actionMutation.isPending}
                className="mt-4 w-full py-3 bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 rounded-lg font-bold transition"
              >
                Start Purchase
              </button>
            </div>

            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">↩️ Refund</h2>
              <div className="grid grid-cols-2 gap-3">
                <input
                  type="number"
                  value={refundAmountMinor}
                  onChange={(event) => setRefundAmountMinor(Number(event.target.value))}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="AmountMinor"
                />
                <input
                  value={refundOperatorId}
                  onChange={(event) => setRefundOperatorId(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="OperatorId"
                />
                <input
                  value={refundClientRequestId}
                  onChange={(event) => setRefundClientRequestId(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="ClientRequestId"
                />
                <input
                  value={refundOptionalData}
                  onChange={(event) => setRefundOptionalData(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="OptionalData"
                />
              </div>
              <div className="mt-3 grid grid-cols-1 md:grid-cols-3 gap-3">
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={refundPreAvstemming}
                    onChange={(event) => setRefundPreAvstemming(event.target.checked)}
                    className="w-4 h-4 rounded"
                  />
                  Pre-avstemming
                </label>
                <input
                  value={refundPreAvstemmingPassword}
                  onChange={(event) => setRefundPreAvstemmingPassword(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="Password"
                />
                <input
                  type="number"
                  value={refundPreAvstemmingTimeout}
                  onChange={(event) => setRefundPreAvstemmingTimeout(Number(event.target.value))}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="TimeoutSeconds"
                />
              </div>
              <button
                onClick={() => triggerAction({ label: 'Refund', method: 'post', path: '/v1/payments/refund', body: refundPayload })}
                disabled={actionMutation.isPending}
                className="mt-4 w-full py-3 bg-pink-600 hover:bg-pink-700 disabled:bg-gray-600 rounded-lg font-bold transition"
              >
                Start Refund
              </button>
            </div>
          </div>

          <div className="space-y-6">
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">💵 Cashback</h2>
              <div className="grid grid-cols-2 gap-3">
                <input
                  type="number"
                  value={cashbackPurchaseMinor}
                  onChange={(event) => setCashbackPurchaseMinor(Number(event.target.value))}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="PurchaseMinor"
                />
                <input
                  type="number"
                  value={cashbackCashbackMinor}
                  onChange={(event) => setCashbackCashbackMinor(Number(event.target.value))}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="CashbackMinor"
                />
                <input
                  value={cashbackCurrency}
                  onChange={(event) => setCashbackCurrency(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="Currency"
                />
                <input
                  value={cashbackOperatorId}
                  onChange={(event) => setCashbackOperatorId(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="OperatorId"
                />
                <input
                  value={cashbackClientRequestId}
                  onChange={(event) => setCashbackClientRequestId(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="ClientRequestId"
                />
                <input
                  value={cashbackOptionalData}
                  onChange={(event) => setCashbackOptionalData(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="OptionalData"
                />
              </div>
              <button
                onClick={() => triggerAction({ label: 'Cashback', method: 'post', path: '/v1/payments/cashback', body: cashbackPayload })}
                disabled={actionMutation.isPending}
                className="mt-4 w-full py-3 bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-600 rounded-lg font-bold transition"
              >
                Start Cashback
              </button>
            </div>

            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">🛠️ Admin Operations</h2>
              <input
                value={adminPassword}
                onChange={(event) => setAdminPassword(event.target.value)}
                className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white mb-3"
                placeholder="Password"
              />
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <button
                  onClick={() => triggerAction({ label: 'Avstemming', method: 'post', path: '/v1/admin/avstemming', body: { Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-blue-700 hover:bg-blue-800 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Avstemming
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Cancel', method: 'post', path: '/v1/admin/cancel', body: { Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-yellow-600 hover:bg-yellow-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Cancel
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Reversal', method: 'post', path: '/v1/admin/reversal', body: { Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Reversal
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Z-report', method: 'post', path: '/v1/admin/z-report', body: { Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Z-report
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Last Receipt', method: 'post', path: '/v1/admin/last-receipt', body: { Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-teal-600 hover:bg-teal-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Last Receipt
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Software Download', method: 'post', path: '/v1/admin/software', body: { Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-orange-600 hover:bg-orange-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Software
                </button>
                <button
                  onClick={() => triggerAction({ label: 'Dataset Download', method: 'post', path: '/v1/admin/dataset', body: { Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-slate-500 hover:bg-slate-600 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Dataset
                </button>
              </div>
              <div className="mt-4 grid grid-cols-2 gap-3">
                <input
                  type="number"
                  value={adminCode}
                  onChange={(event) => setAdminCode(Number(event.target.value))}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="Admin Code"
                />
                <button
                  onClick={() => triggerAction({ label: 'Admin Code', method: 'post', path: '/v1/admin/code', body: { Code: adminCode, Password: adminPassword } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Send Admin Code
                </button>
              </div>
            </div>

            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">📡 Events</h2>
              <div className="grid grid-cols-2 gap-3">
                <input
                  value={eventsSince}
                  onChange={(event) => setEventsSince(event.target.value)}
                  className="bg-gray-700 rounded-lg px-3 py-2 text-white"
                  placeholder="since=0"
                />
                <button
                  onClick={() => triggerAction({ label: 'Poll Events', method: 'get', path: '/v1/events', params: { since: eventsSince } })}
                  disabled={actionMutation.isPending}
                  className="py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  Poll Events
                </button>
              </div>
              <button
                onClick={() => window.open(terminalDiagApi.getEventsStreamUrl(eventsSince), '_blank')}
                className="mt-3 w-full py-2 bg-cyan-600 hover:bg-cyan-700 rounded-lg font-bold transition"
              >
                Open SSE Stream
              </button>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
            <h2 className="text-xl font-bold mb-4">🧩 Diagnostics Schema</h2>
            <button
              onClick={() => triggerAction({ label: 'Diagnostics Schema', method: 'get', path: '/v1/diag/schema' })}
              disabled={actionMutation.isPending}
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 rounded-lg font-bold transition"
            >
              Get Schema
            </button>
          </div>

          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
            <h2 className="text-xl font-bold mb-4">🧪 Diagnostics JSON</h2>
            <textarea
              value={diagJson}
              onChange={(event) => setDiagJson(event.target.value)}
              placeholder='{"Cmd":"GetConfig"}'
              className="w-full h-32 bg-gray-700 rounded-lg px-3 py-2 text-white font-mono text-sm"
            />
            <button
              onClick={() => triggerAction({ label: 'Send JSON', method: 'post', path: '/v1/diag/sendjson', body: { json: diagJson } })}
              disabled={actionMutation.isPending || !diagJson}
              className="mt-3 w-full py-3 bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 rounded-lg font-bold transition"
            >
              Send JSON
            </button>
          </div>

          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
            <h2 className="text-xl font-bold mb-4">📤 Diagnostics TLD</h2>
            <div className="grid grid-cols-1 gap-3">
              <input
                value={tldType}
                onChange={(event) => setTldType(event.target.value)}
                placeholder="TLD Type (e.g. D0)"
                className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
              />
              <input
                value={tldData}
                onChange={(event) => setTldData(event.target.value)}
                placeholder="Base64 TLD data"
                className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white font-mono"
              />
            </div>
            <button
              onClick={() => triggerAction({ label: 'Send TLD', method: 'post', path: '/v1/diag/sendtld', body: { tldType, tldData } })}
              disabled={actionMutation.isPending || !tldType || !tldData}
              className="mt-3 w-full py-3 bg-teal-600 hover:bg-teal-700 disabled:bg-gray-600 rounded-lg font-bold transition"
            >
              Send TLD
            </button>
          </div>

          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
            <h2 className="text-xl font-bold mb-4">✅ Diagnostics Confirm</h2>
            <div className="grid grid-cols-2 gap-3">
              <input
                type="number"
                min="0"
                value={confirmId}
                onChange={(event) => setConfirmId(Number(event.target.value))}
                className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
              />
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={confirmAllow}
                  onChange={(event) => setConfirmAllow(event.target.checked)}
                  className="w-5 h-5 rounded"
                />
                Allow operation
              </label>
            </div>
            <button
              onClick={() => triggerAction({ label: 'Confirm Diagnostics', method: 'post', path: '/v1/diag/confirm', body: { id: confirmId, allow: confirmAllow } })}
              disabled={actionMutation.isPending}
              className="mt-3 w-full py-3 bg-amber-600 hover:bg-amber-700 disabled:bg-gray-600 rounded-lg font-bold transition"
            >
              Confirm
            </button>
          </div>
        </div>

        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h2 className="text-xl font-bold mb-4">📝 Last Response</h2>
          {actionMutation.isPending && (
            <div className="text-gray-400 mb-2">⏳ Sender {lastAction}...</div>
          )}
          {lastAction && !actionMutation.isPending && (
            <div className="text-gray-400 mb-2">Siste kall: {lastAction}</div>
          )}
          {lastError && (
            <pre className="bg-red-900/40 border border-red-600 rounded-lg p-3 text-xs overflow-auto max-h-64">{lastError}</pre>
          )}
          {lastResponse ? (
            <pre className="bg-gray-700 rounded-lg p-3 text-xs overflow-auto max-h-64">{formatJson(lastResponse)}</pre>
          ) : null}
          {!lastError && !lastResponse && (
            <div className="text-gray-500 text-sm">Ingen respons enda.</div>
          )}
        </div>
      </div>
    </div>
  );
}