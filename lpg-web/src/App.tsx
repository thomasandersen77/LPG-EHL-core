import { BrowserRouter, Routes, Route } from 'react-router-dom';
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
import { AzureStoragePage } from './pages/AzureStoragePage';
import { FuelingPage } from './pages/FuelingPage';
import { PriceAdminPage } from './pages/PriceAdminPage';
import { Layout } from './components/Layout';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppModeProvider, useAppMode } from './contexts/AppModeContext';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function LabModeBanner() {
  const { isLab, description } = useAppMode();

  if (!isLab) return null;

  return (
    <div className="bg-yellow-500 text-yellow-900 px-4 py-3 text-center font-bold border-b-4 border-yellow-600">
      ⚠️ LAB MODE - SIMULATED HARDWARE
      <span className="ml-3 text-sm font-normal">{description}</span>
    </div>
  );
}

function AppContent() {
  const { isLab } = useAppMode();

  return (
    <>
      <LabModeBanner />
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
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
        </Route>
      </Routes>
    </>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppModeProvider>
        <BrowserRouter>
          <AppContent />
        </BrowserRouter>
      </AppModeProvider>
    </QueryClientProvider>
  );
}

export default App;
