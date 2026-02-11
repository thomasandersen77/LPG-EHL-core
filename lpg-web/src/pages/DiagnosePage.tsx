import { Link } from 'react-router-dom';
import { useAppMode } from '../contexts/AppModeContext';

export function DiagnosePage() {
  const { hardwareMode, hardwareDescription } = useAppMode();
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-800 via-slate-900 to-slate-800 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="mb-8">
          <Link
            to="/station"
            className="inline-flex items-center text-blue-400 hover:text-blue-300 mb-4 transition-colors"
          >
            ← Tilbake til Stasjonseier
          </Link>
          <h1 className="text-3xl font-bold text-white mb-2">Diagnose & Simulering</h1>
          <p className="text-slate-400">
            Verktøy for feilsøking, testing og simulering av LPG-dispenser
          </p>
          <div className="mt-4 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-700/50 border border-slate-600">
            <span className={`w-3 h-3 rounded-full ${hardwareMode === 'LAB' ? 'bg-yellow-400' : 'bg-green-400'}`}></span>
            <span className="text-slate-300 text-sm">
              {hardwareMode === 'LAB' ? '🧪 Lab Mode' : '🏭 Field Mode'}: {hardwareDescription}
            </span>
          </div>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          <Link
            to="/simulator"
            className="group p-6 bg-gradient-to-br from-green-900/40 to-green-800/20 border border-green-700/30 rounded-xl hover:border-green-500/50 transition-all"
          >
            <div className="text-4xl mb-3">⛽</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-green-400 transition-colors">
              Pumpe Simulator
            </h3>
            <p className="text-sm text-slate-400">
              Simuler fylling, test pumpeinteraksjon og se sanntidsdata
            </p>
          </Link>
          <Link
            to="/control"
            className="group p-6 bg-gradient-to-br from-blue-900/40 to-blue-800/20 border border-blue-700/30 rounded-xl hover:border-blue-500/50 transition-all"
          >
            <div className="text-4xl mb-3">🔧</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-blue-400 transition-colors">
              Kontrollpanel
            </h3>
            <p className="text-sm text-slate-400">
              Direkte dispenserkontroll med alle protokollkommandoer
            </p>
          </Link>
          <Link
            to="/fueling"
            className="group p-6 bg-gradient-to-br from-orange-900/40 to-orange-800/20 border border-orange-700/30 rounded-xl hover:border-orange-500/50 transition-all"
          >
            <div className="text-4xl mb-3">⚡</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-orange-400 transition-colors">
              Fyllingsvisning
            </h3>
            <p className="text-sm text-slate-400">
              Sanntidsvisning av fylleprosessen
            </p>
          </Link>

          <Link
            to="/protocol-tester"
            className="group p-6 bg-gradient-to-br from-purple-900/40 to-purple-800/20 border border-purple-700/30 rounded-xl hover:border-purple-500/50 transition-all"
          >
            <div className="text-4xl mb-3">🧪</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-purple-400 transition-colors">
              Protokolltester
            </h3>
            <p className="text-sm text-slate-400">
              Test EHL-protokollkommandoer direkte
            </p>
          </Link>
          <Link
            to="/wire-tester"
            className="group p-6 bg-gradient-to-br from-pink-900/40 to-pink-800/20 border border-pink-700/30 rounded-xl hover:border-pink-500/50 transition-all"
          >
            <div className="text-4xl mb-3">🔬</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-pink-400 transition-colors">
              VB6 Wire Tester
            </h3>
            <p className="text-sm text-slate-400">
              Test VB6-kompatibilitet og wireformat
            </p>
          </Link>
          <Link
            to="/emulator-debug"
            className="group p-6 bg-gradient-to-br from-red-900/40 to-red-800/20 border border-red-700/30 rounded-xl hover:border-red-500/50 transition-all"
          >
            <div className="text-4xl mb-3">🐛</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-red-400 transition-colors">
              Emulator Debug
            </h3>
            <p className="text-sm text-slate-400">
              Debug emulator-tilstand og simuler feil
            </p>
          </Link>
          <Link
            to="/azure-storage"
            className="group p-6 bg-gradient-to-br from-cyan-900/40 to-cyan-800/20 border border-cyan-700/30 rounded-xl hover:border-cyan-500/50 transition-all"
          >
            <div className="text-4xl mb-3">☁️</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-cyan-400 transition-colors">
              Azure Storage
            </h3>
            <p className="text-sm text-slate-400">
              Se transaksjoner i skykø (Azurite emulator)
            </p>
          </Link>

          <Link
            to="/serial-config"
            className="group p-6 bg-gradient-to-br from-slate-700/40 to-slate-600/20 border border-slate-600/30 rounded-xl hover:border-slate-500/50 transition-all"
          >
            <div className="text-4xl mb-3">🔌</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-slate-300 transition-colors">
              Serial Port Config
            </h3>
            <p className="text-sm text-slate-400">
              Skann og konfigurer serieporter
            </p>
          </Link>
          <Link
            to="/diagnose/terminal"
            className="group p-6 bg-gradient-to-br from-emerald-900/40 to-emerald-800/20 border border-emerald-700/30 rounded-xl hover:border-emerald-500/50 transition-all"
          >
            <div className="text-4xl mb-3">💳</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-emerald-400 transition-colors">
              Payment Terminal API
            </h3>
            <p className="text-sm text-slate-400">
              Diagnostikk og API-kall mot betalingsterminal
            </p>
          </Link>
          <Link
            to="/payment-terminal"
            className="group p-6 bg-gradient-to-br from-teal-900/40 to-teal-800/20 border border-teal-700/30 rounded-xl hover:border-teal-500/50 transition-all"
          >
            <div className="text-4xl mb-3">🧾</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-teal-400 transition-colors">
              Betalingsterminal
            </h3>
            <p className="text-sm text-slate-400">
              Test kortbetaling og terminalkommunikasjon
            </p>
          </Link>
          <a
            href="http://localhost:8080/swagger-ui.html"
            target="_blank"
            rel="noopener noreferrer"
            className="group p-6 bg-gradient-to-br from-indigo-900/40 to-indigo-800/20 border border-indigo-700/30 rounded-xl hover:border-indigo-500/50 transition-all"
          >
            <div className="text-4xl mb-3">📚</div>
            <h3 className="text-lg font-bold text-white mb-2 group-hover:text-indigo-400 transition-colors">
              API Dokumentasjon
            </h3>
            <p className="text-sm text-slate-400">
              OpenAPI/Swagger dokumentasjon (åpner i nytt vindu)
            </p>
          </a>
        </div>

        <div className="mt-12 p-6 bg-slate-800/50 rounded-xl border border-slate-700">
          <h2 className="text-xl font-bold text-white mb-4">Hurtiglenker</h2>
          <div className="flex flex-wrap gap-3">
            <Link to="/transactions" className="px-4 py-2 bg-blue-600/20 text-blue-300 rounded-lg hover:bg-blue-600/30 transition-colors">
              📋 Transaksjoner
            </Link>
            <Link to="/reports" className="px-4 py-2 bg-yellow-600/20 text-yellow-300 rounded-lg hover:bg-yellow-600/30 transition-colors">
              📊 Rapporter
            </Link>
            <Link to="/credit" className="px-4 py-2 bg-purple-600/20 text-purple-300 rounded-lg hover:bg-purple-600/30 transition-colors">
              🏪 Stasjonskreditt
            </Link>
            <Link to="/price-admin" className="px-4 py-2 bg-orange-600/20 text-orange-300 rounded-lg hover:bg-orange-600/30 transition-colors">
              💰 Prisadministrasjon
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
