import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { StationOwnerPage } from './pages/StationOwnerPage';
import { DiagnosePage } from './pages/DiagnosePage';
import { HomePage } from './pages/HomePage';
import { DispenserSimulator } from './components/DispenserSimulator';
import { ControlPanel } from './components/ControlPanel';
import { ProtocolTester } from './components/ProtocolTester';
import { WireComplianceTester } from './components/WireComplianceTester';
import { TransactionsPage } from './pages/TransactionsPage';
import { CreditAccountsPage } from './pages/CreditAccountsPage';
import { ReportsPage } from './pages/ReportsPage';
import { EmulatorDebugPage } from './pages/EmulatorDebugPage';
import { PaymentTerminalPage } from './pages/PaymentTerminalPage';
import { PaymentTerminalDiagnosticsPage } from './pages/PaymentTerminalDiagnosticsPage';
import { AzureStoragePage } from './pages/AzureStoragePage';
import { FuelingPage } from './pages/FuelingPage';
import { PriceAdminPage } from './pages/PriceAdminPage';
import { SerialPortConfigPage } from './pages/SerialPortConfigPage';
import { Layout } from './components/Layout';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppModeProvider, useAppMode } from './contexts/AppModeContext';
import { AuthProvider, useAuth } from './contexts/AuthContext';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function LabModeBanner() {
  const { hardwareMode, hardwareDescription } = useAppMode();

  if (hardwareMode !== 'LAB') return null;

  return (
    <div className="bg-yellow-500 text-yellow-900 px-4 py-3 text-center font-bold border-b-4 border-yellow-600">
      🧪 LAB MODE - SIMULATED HARDWARE
      <span className="ml-3 text-sm font-normal">{hardwareDescription}</span>
    </div>
  );
}

function FieldModeBanner() {
  const { hardwareMode, hardwareDescription, serialPort, baudRate, parity, dataBits, stopBits, connectionKind } = useAppMode();

  if (hardwareMode !== 'FIELD') return null;

  const isSocat = connectionKind === 'SOCAT_VIRTUAL';
  const parityChar = parity === 'NONE' ? 'N' : parity === 'EVEN' ? 'E' : parity === 'ODD' ? 'O' : '?';
  const uartSummary = baudRate ? `${baudRate} ${dataBits || 8}${parityChar}${stopBits || 1}` : '';

  if (isSocat) {
    return (
      <div className="bg-orange-600 text-white px-4 py-3 text-center font-bold border-b-4 border-orange-800">
        🔧 FIELD MODE (SOCAT)
        <span className="ml-3 text-sm font-normal">
          {hardwareDescription}
          {serialPort && uartSummary ? ` — ${serialPort} @ ${uartSummary}` : ''}
        </span>
      </div>
    );
  }

  return (
    <div className="bg-red-600 text-white px-4 py-3 text-center font-bold border-b-4 border-red-800">
      🏭 PRODUCTION MODE - REAL HARDWARE
      <span className="ml-3 text-sm font-normal">
        {hardwareDescription}
        {serialPort && uartSummary ? ` — ${serialPort} @ ${uartSummary}` : ''}
      </span>
    </div>
  );
}

// Protected Route Component - requires login
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isLoggedIn } = useAuth();
  
  if (!isLoggedIn) {
    return <Navigate to="/" replace />;
  }
  
  return <>{children}</>;
}

function AppContent() {
  const { isLab } = useAppMode();

  return (
    <>
      <Routes>
        {/* Login Page - Public */}
        <Route path="/" element={<LoginPage />} />
        
        {/* Station Owner Page - Main page after login */}
        <Route path="/station" element={
          <ProtectedRoute>
            <LabModeBanner />
            <FieldModeBanner />
            <StationOwnerPage />
          </ProtectedRoute>
        } />
        
        {/* Diagnose Page - Tool overview */}
        <Route path="/diagnose" element={
          <ProtectedRoute>
            <LabModeBanner />
            <FieldModeBanner />
            <DiagnosePage />
          </ProtectedRoute>
        } />
        <Route path="/diagnose/terminal" element={
          <ProtectedRoute>
            <LabModeBanner />
            <FieldModeBanner />
            <PaymentTerminalDiagnosticsPage />
          </ProtectedRoute>
        } />
        
        {/* Legacy Home Page - accessible from diagnose */}
        <Route path="/home" element={
          <ProtectedRoute>
            <LabModeBanner />
            <FieldModeBanner />
            <Layout />
          </ProtectedRoute>
        }>
          <Route index element={<HomePage />} />
        </Route>
        
        {/* All other pages with Layout */}
        <Route element={
          <ProtectedRoute>
            <LabModeBanner />
            <FieldModeBanner />
            <Layout />
          </ProtectedRoute>
        }>
          <Route path="simulator" element={<DispenserSimulator />} />
          <Route path="control" element={<ControlPanel />} />
          <Route path="fueling" element={<FuelingPage />} />
          {isLab && <Route path="protocol-tester" element={<ProtocolTester />} />}
          {isLab && <Route path="wire-tester" element={<WireComplianceTester />} />}
          <Route path="transactions" element={<TransactionsPage />} />
          <Route path="credit" element={<CreditAccountsPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="payment-terminal" element={<PaymentTerminalPage />} />
          <Route path="price-admin" element={<PriceAdminPage />} />
          {isLab && <Route path="emulator-debug" element={<EmulatorDebugPage />} />}
          {isLab && <Route path="azure-storage" element={<AzureStoragePage />} />}
          <Route path="serial-config" element={<SerialPortConfigPage />} />
        </Route>
      </Routes>
    </>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppModeProvider>
        <AuthProvider>
          <BrowserRouter>
            <AppContent />
          </BrowserRouter>
        </AuthProvider>
      </AppModeProvider>
    </QueryClientProvider>
  );
}

export default App;
