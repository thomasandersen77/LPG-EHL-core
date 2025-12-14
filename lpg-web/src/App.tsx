import { DispenserSimulator } from './components/DispenserSimulator';
import { useState } from 'react';

type Page = 'home' | 'simulator';

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('home');

  if (currentPage === 'simulator') {
    return (
      <div>
        <button
          onClick={() => setCurrentPage('home')}
          className="fixed top-4 left-4 z-50 px-4 py-2 bg-white/90 backdrop-blur-sm text-gray-800 rounded-lg hover:bg-white transition shadow-lg flex items-center gap-2"
        >
          <span>← Tilbake til hovedside</span>
        </button>
        <DispenserSimulator />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-slate-100">
      <div className="w-full max-w-4xl bg-white shadow-2xl rounded-3xl p-12 space-y-8">
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

        <footer className="text-center space-y-6 pt-8 border-t">
          <div className="mb-6">
            <h3 className="text-lg font-bold text-slate-900 mb-4">Demo & Testing</h3>
            <p className="text-sm text-slate-600 mb-4">
              Test pumpefunksjonaliteten med vår interaktive simulator
            </p>
            <button
              onClick={() => setCurrentPage('simulator')}
              className="px-8 py-4 bg-green-600 text-white rounded-xl hover:bg-green-700 transition font-bold text-lg shadow-lg hover:shadow-xl"
            >
              ⛽ Åpne Pumpe Simulator
            </button>
          </div>
          
          <div className="flex justify-center gap-4 flex-wrap">
            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noopener noreferrer"
              className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-semibold"
            >
              📚 API Documentation
            </a>
            <a
              href="http://localhost:8080/actuator/health"
              target="_blank"
              rel="noopener noreferrer"
              className="px-6 py-3 bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition font-semibold"
            >
              ✓ Health Check
            </a>
          </div>
          <div className="text-sm text-slate-600 space-y-2">
            <p className="font-semibold">System Features:</p>
            <ul className="text-left max-w-2xl mx-auto space-y-1">
              <li>• <strong>Real-time pumpe-kontroll:</strong> Start, stopp og nullstill drivstoffleveringer</li>
              <li>• <strong>Live data-oppdatering:</strong> Se liter og beløp oppdateres i sanntid (0.5s polling)</li>
              <li>• <strong>EHL Protocol:</strong> Komplett implementasjon av EHL for LPG-dispensere</li>
              <li>• <strong>Database persistence:</strong> Alle transaksjoner lagres i PostgreSQL</li>
              <li>• <strong>Azure sync:</strong> Automatisk synkronisering til sky (emulert lokalt)</li>
            </ul>
          </div>
        </footer>
      </div>
    </div>
  );
}

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

export default App;
