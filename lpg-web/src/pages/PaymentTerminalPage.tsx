import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchTransactions } from '../api/transactions';
import { dispenserApi } from '../api/dispenser';

export function PaymentTerminalPage() {
  const [lastSettlement, setLastSettlement] = useState<any>(null);
  const queryClient = useQueryClient();

  // Fetch latest transactions to show pending amounts
  const { data: transactionsPage } = useQuery({
    queryKey: ['transactions', 'payment-terminal'],
    queryFn: () => fetchTransactions({ page: 0, size: 1 }),
    refetchInterval: 2000,
  });

  const settlementMutation = useMutation({
    mutationFn: async (method: 'CARD' | 'CREDIT') => {
      // Check if we have a pending transaction to pay
      if (!latestTransaction) {
        throw new Error('No pending transaction to pay');
      }
      
      // Only check transactions with PENDING status
      if (latestTransaction.paymentStatus !== 'PENDING') {
        throw new Error(`Transaction already ${latestTransaction.paymentStatus}`);
      }
      
      console.log('💳 Paying transaction via Terminal:', latestTransaction.transactionId, '-', latestTransaction.amountKr, 'kr');
      
      // IMPORTANT: Call settle endpoint to release the dispenser
      // This updates payment status AND resets dispenser state to IDLE
      console.log('🔓 Calling settle endpoint to release dispenser...');
      const settleResult = await dispenserApi.settle(1, method);
      console.log('✅ Dispenser released:', settleResult);
      
      return {
        status: 'settled',
        method: method,
        source: 'terminal-payment',
        dispenserReleased: true,
        transaction: {
          dispenserId: latestTransaction.dispenserAddress,
          liters: latestTransaction.volumeLiters,
          amountNok: latestTransaction.amountKr,
          unitPrice: latestTransaction.pricePerLiter,
          finishedAt: latestTransaction.timestamp,
          idempotencyKey: latestTransaction.transactionId
        }
      };
    },
    onSuccess: (data: any) => {
      setLastSettlement(data);
      // Invalidate transactions to refresh the list
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      // Also invalidate dispenser state so simulator updates
      queryClient.invalidateQueries({ queryKey: ['dispenser-state'] });
      
      // Show success message
      if (data.transaction) {
        console.log(`✅ Payment settled via ${data.source}:`, data.transaction);
        if (data.dispenserReleased) {
          console.log('🚀 Dispenser state reset to IDLE - ready for next fueling');
        }
      }
    },
    onError: (error) => {
      console.error('❌ Payment failed:', error);
      setLastSettlement({
        status: 'error',
        method: '',
        message: String(error)
      });
    },
  });

  // Clear settlement message after 5 seconds
  useEffect(() => {
    if (lastSettlement) {
      const timer = setTimeout(() => setLastSettlement(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [lastSettlement]);

  const handleCardPayment = () => {
    settlementMutation.mutate('CARD');
  };

  const handleCreditPayment = () => {
    settlementMutation.mutate('CREDIT');
  };

  // Get the latest transaction (most recent pending)
  const latestTransaction = transactionsPage?.content?.[0] ?? null;
  const hasPendingAmount = latestTransaction && latestTransaction.amountKr > 0;

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-slate-900 mb-2">💳 Betalingsterminal (Simulering)</h1>
        <p className="text-slate-600">Simulerer Nets-terminal for nullstilling av dispenser</p>
      </div>

      {/* Payment Terminal UI */}
      <div className="bg-gradient-to-br from-slate-800 to-slate-900 rounded-2xl shadow-2xl p-8 mb-6">
        <div className="bg-white rounded-xl p-6 mb-6">
          {/* Display */}
          <div className="text-center mb-6">
            <div className="text-sm text-slate-500 mb-2">Beløp å betale</div>
            {hasPendingAmount ? (
              <>
                <div className="text-5xl font-bold text-slate-900 mb-2">
                  {latestTransaction.amountKr.toFixed(2)} kr
                </div>
                <div className="text-lg text-slate-600">
                  {latestTransaction.volumeLiters.toFixed(2)} L @ {latestTransaction.pricePerLiter.toFixed(2)} kr/L
                </div>
                <div className="text-xs text-slate-400 mt-2">
                  Pumpe #{latestTransaction.dispenserAddress} • {new Date(latestTransaction.timestamp).toLocaleTimeString('nb-NO')}
                </div>
              </>
            ) : (
              <>
                <div className="text-5xl font-bold text-slate-300 mb-2">0.00 kr</div>
                <div className="text-lg text-slate-400">Ingen pending transaksjon</div>
              </>
            )}
          </div>

          {lastSettlement && lastSettlement.status === 'settled' && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
              <div className="flex items-center text-green-800">
                <span className="text-2xl mr-2">✅</span>
                <div>
                  <div className="font-bold">Betaling godkjent!</div>
                  <div className="text-sm text-green-600">
                    {lastSettlement.transaction?.amountNok?.toFixed(2)} kr betalt med {lastSettlement.method}
                  </div>
                  {lastSettlement.windowsBroadcastSent && (
                    <div className="text-xs text-green-500 mt-1">
                      📢 Windows Dispenserkontroll nullstilt
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {lastSettlement && lastSettlement.status === 'no_pending_transaction' && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
              <div className="flex items-center text-yellow-800">
                <span className="text-2xl mr-2">⚠️</span>
                <div>
                  <div className="font-bold">Ingen transaksjon</div>
                  <div className="text-sm text-yellow-600">{lastSettlement.message}</div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Payment Buttons */}
        <div className="grid grid-cols-1 gap-4">
          <button
            onClick={handleCardPayment}
            disabled={settlementMutation.isPending || !hasPendingAmount}
            className="bg-gradient-to-r from-blue-500 to-blue-600 text-white font-bold py-6 rounded-xl hover:from-blue-600 hover:to-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
          >
            <div className="text-3xl mb-2">💳</div>
            <div className="text-xl">Kortbetaling</div>
            <div className="text-sm opacity-80">Simuler Nets terminal</div>
          </button>

          <button
            onClick={handleCreditPayment}
            disabled={settlementMutation.isPending || !hasPendingAmount}
            className="bg-gradient-to-r from-purple-500 to-purple-600 text-white font-bold py-6 rounded-xl hover:from-purple-600 hover:to-purple-700 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
          >
            <div className="text-3xl mb-2">🏢</div>
            <div className="text-xl">Bedriftskreditt</div>
            <div className="text-sm opacity-80">Stasjonskreditt</div>
          </button>
        </div>

        {settlementMutation.isPending && (
          <div className="mt-4 text-center text-white">
            <div className="animate-pulse">⏳ Behandler betaling...</div>
          </div>
        )}
      </div>

      {/* Info Box */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-6">
        <h3 className="font-bold text-blue-900 mb-2">ℹ️ Om betalingssimulatoren</h3>
        <ul className="text-sm text-blue-800 space-y-1">
          <li>✅ Henter transaksjoner fra lpg-ehl database</li>
          <li>✅ Kvitterer betaling via lpg-ehl API (port 8080)</li>
          <li>✅ Oppdaterer paymentStatus fra PENDING til PAID</li>
          <li>⚠️ Kun transaksjoner med status PENDING kan betales</li>
        </ul>
      </div>

      {/* Debug Info */}
      {lastSettlement && (
        <details className="mt-6 bg-slate-50 rounded-xl p-4">
          <summary className="cursor-pointer font-mono text-sm text-slate-600">
            🔍 Debug info
          </summary>
          <pre className="mt-4 text-xs bg-slate-100 p-4 rounded overflow-x-auto">
            {JSON.stringify(lastSettlement, null, 2)}
          </pre>
        </details>
      )}
    </div>
  );
}
