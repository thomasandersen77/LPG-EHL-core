import { Outlet, Link, useLocation } from 'react-router-dom';

export function Layout() {
  const location = useLocation();
  
  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const linkClass = (path: string) => {
    const base = "px-4 py-2 rounded-lg transition-colors font-medium";
    return isActive(path)
      ? `${base} bg-green-600 text-white`
      : `${base} text-gray-700 hover:bg-gray-100`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-slate-100">
      {/* Navigation Bar */}
      <nav className="bg-white shadow-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <Link to="/" className="flex items-center space-x-3">
              <div className="p-2 bg-green-100 rounded-lg">
                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <span className="text-xl font-bold text-gray-900">LPG EHL</span>
            </Link>

            <div className="flex items-center space-x-2">
              <Link to="/" className={linkClass('/')}>
                🏠 Hjem
              </Link>
              <Link to="/simulator" className={linkClass('/simulator')}>
                ⛽ Simulator
              </Link>
              <Link to="/protocol-tester" className={linkClass('/protocol-tester')}>
                🧪 Protokoll
              </Link>
              <Link to="/transactions" className={linkClass('/transactions')}>
                📋 Transaksjoner
              </Link>
              <Link to="/payment-terminal" className={linkClass('/payment-terminal')}>
                💳 Terminal
              </Link>
              <Link to="/credit" className={linkClass('/credit')}>
                🏪 Kreditt
              </Link>
              <Link to="/reports" className={linkClass('/reports')}>
                📊 Rapporter
              </Link>
              <Link to="/emulator-debug" className={linkClass('/emulator-debug')}>
                🔧 Debug
              </Link>
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
