export function CreditAccountsPage() {
  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Stasjonskreditt</h1>
      <div className="bg-white rounded-xl shadow p-8">
        <p className="text-slate-600">
          Stasjonskreditt-siden vil vise kredittkontoer og transaksjoner per kunde.
        </p>
        <p className="text-sm text-slate-500 mt-4">
          Backend CreditController og database-tabeller må opprettes først.
        </p>
      </div>
    </div>
  );
}
