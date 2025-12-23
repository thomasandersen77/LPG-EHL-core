import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { HomePage } from './pages/HomePage';
import { DispenserSimulator } from './components/DispenserSimulator';
import { ProtocolTester } from './components/ProtocolTester';
import { TransactionsPage } from './pages/TransactionsPage';
import { CreditAccountsPage } from './pages/CreditAccountsPage';
import { ReportsPage } from './pages/ReportsPage';
import { EmulatorDebugPage } from './pages/EmulatorDebugPage';
import { PaymentTerminalPage } from './pages/PaymentTerminalPage';
import { Layout } from './components/Layout';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="simulator" element={<DispenserSimulator />} />
            <Route path="protocol-tester" element={<ProtocolTester />} />
            <Route path="transactions" element={<TransactionsPage />} />
            <Route path="credit" element={<CreditAccountsPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="payment-terminal" element={<PaymentTerminalPage />} />
            <Route path="emulator-debug" element={<EmulatorDebugPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
