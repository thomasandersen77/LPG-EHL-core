export function ReportsPage() {
  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Rapporter</h1>
      <div className="bg-white rounded-xl shadow p-8">
        <p className="text-slate-600">
          Rapport-siden vil vise daglige og periodiske salgsrapporter aggregert per betalingstype.
        </p>
        <p className="text-sm text-slate-500 mt-4">
          Backend ReportController må implementeres først.
        </p>
      </div>
    </div>
  );
}
