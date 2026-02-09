import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useEffect } from 'react';

export function LoginPage() {
  const navigate = useNavigate();
  const { isLoggedIn, login } = useAuth();

  // Redirect to station page if already logged in
  useEffect(() => {
    if (isLoggedIn) {
      navigate('/station');
    }
  }, [isLoggedIn, navigate]);

  const handleLogin = () => {
    login('NorgesGass Demo Stasjon');
    navigate('/station');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      <div className="w-full max-w-md p-8 space-y-8">
        {/* Logo Section */}
        <div className="text-center space-y-4">
          <div className="inline-block p-6 bg-gradient-to-br from-green-400 to-green-600 rounded-full shadow-2xl mb-4">
            <svg className="w-16 h-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <h1 className="text-4xl font-bold text-white tracking-tight">NorgesGass</h1>
          <p className="text-blue-200 text-lg">Stasjonseier Portal</p>
        </div>

        {/* Login Card */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-8 shadow-2xl border border-white/20">
          <div className="space-y-6">
            <div className="text-center">
              <h2 className="text-xl font-semibold text-white mb-2">Velkommen tilbake</h2>
              <p className="text-blue-200 text-sm">
                Logg inn for å administrere din LPG-stasjon
              </p>
            </div>

            {/* Demo Notice */}
            <div className="bg-yellow-500/20 border border-yellow-400/30 rounded-lg p-4">
              <div className="flex items-start">
                <span className="text-yellow-300 mr-2">🧪</span>
                <div className="text-sm text-yellow-200">
                  <strong>Demo-modus:</strong> Klikk på knappen nedenfor for å simulere innlogging. Ingen brukernavn eller passord kreves.
                </div>
              </div>
            </div>

            {/* Login Button */}
            <button
              onClick={handleLogin}
              className="w-full py-4 px-6 bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 text-white font-bold text-lg rounded-xl shadow-lg transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98]"
            >
              🔐 Logg inn som stasjonseier
            </button>

            {/* Additional Info */}
            <div className="pt-4 border-t border-white/10">
              <p className="text-center text-blue-300 text-xs">
                LPG EHL Dispenserkontroll System v1.0
              </p>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="text-center text-blue-300/60 text-sm">
          © 2026 NorgesGass AS • Alle rettigheter reservert
        </div>
      </div>
    </div>
  );
}
