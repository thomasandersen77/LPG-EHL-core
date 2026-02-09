import { Outlet, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export function Layout() {
  const location = useLocation();
  const { logout } = useAuth();
  
  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const linkClass = (path: string) => {
    const base = "px-3 py-1.5 rounded-lg transition-colors font-medium text-sm";
    return isActive(path)
      ? `${base} bg-green-600 text-white`
      : `${base} text-gray-700 hover:bg-gray-100`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-slate-100">
      {/* Navigation Bar */}
      <nav className="bg-white shadow-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14">
            {/* Left side - Logo and main nav */}
            <div className="flex items-center space-x-4">
              <Link to="/station" className="flex items-center space-x-2">
                <div className="p-1.5 bg-green-100 rounded-lg">
                  <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </div>
                <span className="text-lg font-bold text-gray-900">LPG EHL</span>
              </Link>
              
              {/* Primary Navigation */}
              <div className="hidden md:flex items-center space-x-1 border-l pl-4 ml-2">
                <Link to="/station" className="px-3 py-1.5 text-sm font-medium text-green-700 bg-green-50 rounded-lg hover:bg-green-100">
                  ⛽ Stasjonseier
                </Link>
                <Link to="/diagnose" className="px-3 py-1.5 text-sm font-medium text-blue-700 bg-blue-50 rounded-lg hover:bg-blue-100">
                  🔧 Diagnose
                </Link>
              </div>
            </div>

            {/* Center - Tool Navigation */}
            <div className="hidden lg:flex items-center space-x-1">
              <Link to="/control" className={linkClass('/control')}>
                🔧 Kontroll
              </Link>
              <Link to="/simulator" className={linkClass('/simulator')}>
                ⛽ Simulator
              </Link>
              <Link to="/transactions" className={linkClass('/transactions')}>
                📋 Transaksjoner
              </Link>
              <Link to="/credit" className={linkClass('/credit')}>
                🏪 Kreditt
              </Link>
              <Link to="/reports" className={linkClass('/reports')}>
                📊 Rapporter
              </Link>
            </div>

            {/* Right side - Logout */}
            <div className="flex items-center space-x-2">
              <button
                onClick={() => {
                  logout();
                  window.location.href = '/';
                }}
                className="px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 rounded-lg transition-colors"
              >
                Logg ut
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Page Content */}
      <main>
        <Outlet />
      </main>
    </div>
  );
}
