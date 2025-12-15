export function TransactionsPage() {
  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Transaksjoner</h1>
      <div className="bg-white rounded-xl shadow p-8">
        <p className="text-slate-600">
          Transaksjoner-siden vil vise en liste over alle transaksjoner med filtrering og søk.
        </p>
        <p className="text-sm text-slate-500 mt-4">
          Backend TransactionController må implementeres først.
        </p>
      </div>
    </div>
  );
}
