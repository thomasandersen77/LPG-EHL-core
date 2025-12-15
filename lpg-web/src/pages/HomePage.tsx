import { Link } from 'react-router-dom';

type CardProps = {
  title: string;
  description: string;
  endpoint: string;
  status: string;
};

function Card({ title, description, endpoint, status }: CardProps) {
  return (
    <div className="p-6 bg-slate-50 rounded-xl border border-slate-200 hover:shadow-lg transition">
      <h3 className="text-xl font-bold text-slate-900 mb-2">{title}</h3>
      <p className="text-slate-600 mb-4 text-sm">{description}</p>
      <div className="flex justify-between items-center">
        <span className="text-xs font-mono text-slate-500">{endpoint}</span>
        <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
          {status}
        </span>
      </div>
    </div>
  );
}

export function HomePage() {
  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4">
      <div className="w-full max-w-6xl bg-white shadow-2xl rounded-3xl p-12 space-y-8">
        <header className="text-center space-y-4">
          <div className="inline-block p-4 bg-green-100 rounded-full mb-4">
            <svg className="w-16 h-16 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <h1 className="text-5xl font-bold text-slate-900">LPG EHL System</h1>
          <p className="text-xl text-slate-600">
            EHL Protocol Implementation for LPG Dispensers
          </p>
        </header>

        <div className="grid md:grid-cols-2 gap-6 py-8">
          <Card
            title="🚀 Emulator"
            description="TCP server simulating EHL protocol dispenser behavior"
            endpoint="Port 9000"
            status="Running"
          />
          <Card
            title="🌐 API Server"
            description="REST API for transaction management and dispenser control"
            endpoint="Port 8080"
            status="Running"
          />
          <Card
            title="💾 PostgreSQL"
            description="Transaction and dispenser status database"
            endpoint="Port 5432"
            status="Healthy"
          />
          <Card
            title="☁️ Azure Storage"
            description="Queue storage for cloud sync (Azurite emulator)"
            endpoint="Port 10001"
            status="Running"
          />
        </div>

        {/* Navigation Section */}
        <section className="space-y-6 pt-8 border-t">
          <h3 className="text-2xl font-bold text-slate-900 text-center mb-6">Systemmoduler</h3>
          <div className="grid md:grid-cols-3 gap-4">
            <Link
              to="/simulator"
              className="p-6 bg-green-50 hover:bg-green-100 border-2 border-green-200 rounded-xl transition text-center group"
            >
              <div className="text-4xl mb-3">⛽</div>
              <h4 className="text-lg font-bold text-slate-900 mb-2">Pumpe Simulator</h4>
              <p className="text-sm text-slate-600">Test pumpekontroll og drivstoffleveringer</p>
            </Link>

            <Link
              to="/transactions"
              className="p-6 bg-blue-50 hover:bg-blue-100 border-2 border-blue-200 rounded-xl transition text-center group"
            >
              <div className="text-4xl mb-3">📋</div>
              <h4 className="text-lg font-bold text-slate-900 mb-2">Transaksjoner</h4>
              <p className="text-sm text-slate-600">Søk og filtrer transaksjonshistorikk</p>
            </Link>

            <Link
              to="/credit"
              className="p-6 bg-purple-50 hover:bg-purple-100 border-2 border-purple-200 rounded-xl transition text-center group"
            >
              <div className="text-4xl mb-3">🏪</div>
              <h4 className="text-lg font-bold text-slate-900 mb-2">Stasjonskreditt</h4>
              <p className="text-sm text-slate-600">Administrer kredittkunder og saldo</p>
            </Link>

            <Link
              to="/reports"
              className="p-6 bg-yellow-50 hover:bg-yellow-100 border-2 border-yellow-200 rounded-xl transition text-center group"
            >
              <div className="text-4xl mb-3">📊</div>
              <h4 className="text-lg font-bold text-slate-900 mb-2">Rapporter</h4>
              <p className="text-sm text-slate-600">Daglige og periodiske salgsrapporter</p>
            </Link>

            <Link
              to="/emulator-debug"
              className="p-6 bg-red-50 hover:bg-red-100 border-2 border-red-200 rounded-xl transition text-center group"
            >
              <div className="text-4xl mb-3">🔧</div>
              <h4 className="text-lg font-bold text-slate-900 mb-2">Emulator Debug</h4>
              <p className="text-sm text-slate-600">Test feilscenarier og debugging</p>
            </Link>

            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noopener noreferrer"
              className="p-6 bg-slate-50 hover:bg-slate-100 border-2 border-slate-200 rounded-xl transition text-center group"
            >
              <div className="text-4xl mb-3">📚</div>
              <h4 className="text-lg font-bold text-slate-900 mb-2">API Docs</h4>
              <p className="text-sm text-slate-600">OpenAPI/Swagger dokumentasjon</p>
            </a>
          </div>
        </section>

        <footer className="text-center text-sm text-slate-600 space-y-2 pt-8 border-t">
          <p className="font-semibold">System Features:</p>
          <ul className="text-left max-w-2xl mx-auto space-y-1">
            <li>• <strong>Real-time pumpe-kontroll:</strong> Start, stopp og nullstill drivstoffleveringer</li>
            <li>• <strong>Live data-oppdatering:</strong> Se liter og beløp oppdateres i sanntid (0.5s polling)</li>
            <li>• <strong>EHL Protocol:</strong> Komplett implementasjon av EHL for LPG-dispensere</li>
            <li>• <strong>Database persistence:</strong> Alle transaksjoner lagres i PostgreSQL</li>
            <li>• <strong>Azure sync:</strong> Automatisk synkronisering til sky (emulert lokalt)</li>
            <li>• <strong>Simulert betaling:</strong> Test kortbetaling, kontant og stasjonskreditt</li>
          </ul>
        </footer>
      </div>
    </div>
  );
}
