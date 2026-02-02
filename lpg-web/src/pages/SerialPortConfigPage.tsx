import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import axios from 'axios';

// API configuration
const API_BASE = import.meta.env.VITE_API_URL?.replace('/api/v1', '') || 'http://localhost:8080';

// Types based on backend models
interface AvailablePort {
  path: string;
  description: string;
  location: string;
  vendorId: number;
  productId: number;
}

interface WorkingConfiguration {
  port: AvailablePort;
  baudRate: number;
  parity: string;
  dispenserAddress: number;
  confidence: number;
}

interface RespondingAddress {
  address: number;
  description: string;
  responseTimeMs: number;
}

interface AddressScanResult {
  portPath: string;
  addressRange: string;
  baudRate: number;
  parity: string;
  respondingAddresses: RespondingAddress[];
  testedCount: number;
}

interface SerialStatus {
  connected: boolean;
  transportType: string;
}

interface SerialHealthStatus {
  healthy: boolean;
  responseTimeMs: number;
  lastError?: string;
  parityMode?: string;
  baudRate?: number;
}

// Configuration options
const BAUD_RATES = [9600, 19200, 38400, 57600, 115200];
const PARITY_MODES = ['NONE', 'EVEN', 'ODD'];
const DATA_BITS = [7, 8];
const STOP_BITS = [1, 2];

// API functions
const serialApi = {
  listPorts: async (): Promise<AvailablePort[]> => {
    const res = await axios.get(`${API_BASE}/api/debug/serial/ports`);
    return res.data;
  },
  getStatus: async (): Promise<SerialStatus> => {
    const res = await axios.get(`${API_BASE}/api/debug/serial/status`);
    return res.data;
  },
  healthCheck: async (address: number): Promise<SerialHealthStatus> => {
    const res = await axios.get(`${API_BASE}/api/debug/serial/health`, {
      params: { address }
    });
    return res.data;
  },
  smartScan: async (timeoutMs: number, stopOnFirst: boolean): Promise<WorkingConfiguration[]> => {
    const res = await axios.post(`${API_BASE}/api/debug/serial/smart-scan`, null, {
      params: { timeoutMs, stopOnFirst }
    });
    return res.data;
  },
  scanAddresses: async (
    port: string,
    start: number,
    end: number,
    baud: number,
    parity: string,
    timeoutMs: number
  ): Promise<AddressScanResult> => {
    const res = await axios.post(`${API_BASE}/api/debug/serial/scan-addresses`, null, {
      params: { port, start, end, baud, parity, timeoutMs }
    });
    return res.data;
  },
  autoDetectParity: async (port: string, address: number): Promise<{ detected: boolean; parityMode?: string; description?: string; error?: string }> => {
    const res = await axios.post(`${API_BASE}/api/debug/serial/auto-detect`, null, {
      params: { port, address }
    });
    return res.data;
  }
};

export function SerialPortConfigPage() {
  // State for configuration form
  const [selectedPort, setSelectedPort] = useState<string>('');
  const [baudRate, setBaudRate] = useState<number>(9600);
  const [parity, setParity] = useState<string>('NONE');
  const [dataBits, setDataBits] = useState<number>(8);
  const [stopBits, setStopBits] = useState<number>(1);
  const [testAddress, setTestAddress] = useState<number>(1);
  
  // State for address scan
  const [scanStartAddr, setScanStartAddr] = useState<number>(1);
  const [scanEndAddr, setScanEndAddr] = useState<number>(40);
  const [scanTimeout, setScanTimeout] = useState<number>(500);
  
  // State for smart scan
  const [smartScanTimeout, setSmartScanTimeout] = useState<number>(1000);
  const [stopOnFirstMatch, setStopOnFirstMatch] = useState<boolean>(true);

  // Queries
  const portsQuery = useQuery({
    queryKey: ['serial-ports'],
    queryFn: serialApi.listPorts,
    refetchInterval: 10000 // Refresh every 10s to detect new ports
  });

  const statusQuery = useQuery({
    queryKey: ['serial-status'],
    queryFn: serialApi.getStatus,
    refetchInterval: 5000
  });

  // Mutations
  const healthCheckMutation = useMutation({
    mutationFn: () => serialApi.healthCheck(testAddress)
  });

  const smartScanMutation = useMutation({
    mutationFn: () => serialApi.smartScan(smartScanTimeout, stopOnFirstMatch)
  });

  const addressScanMutation = useMutation({
    mutationFn: () => serialApi.scanAddresses(
      selectedPort,
      scanStartAddr,
      scanEndAddr,
      baudRate,
      parity,
      scanTimeout
    )
  });

  const parityDetectMutation = useMutation({
    mutationFn: () => serialApi.autoDetectParity(selectedPort, testAddress)
  });

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white p-4">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold mb-2">🔌 Serial Port Configuration</h1>
          <p className="text-gray-400">Scan, test and configure serial port connections</p>
        </div>

        {/* Connection Status */}
        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold">📡 Connection Status</h2>
            <div className="flex items-center gap-4">
              <div className={`flex items-center gap-2 px-4 py-2 rounded-full ${
                statusQuery.data?.connected ? 'bg-green-600' : 'bg-red-600'
              }`}>
                <div className={`w-3 h-3 rounded-full ${
                  statusQuery.data?.connected ? 'bg-green-300 animate-pulse' : 'bg-red-300'
                }`} />
                <span className="font-semibold">
                  {statusQuery.data?.connected ? 'Connected' : 'Disconnected'}
                </span>
              </div>
              {statusQuery.data?.transportType && (
                <span className="text-gray-400 text-sm">
                  Transport: {statusQuery.data.transportType}
                </span>
              )}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left Column: Port Selection & Configuration */}
          <div className="space-y-6">
            {/* Available Ports */}
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold">🔍 Available Ports</h2>
                <button
                  onClick={() => portsQuery.refetch()}
                  className="px-3 py-1 bg-blue-600 hover:bg-blue-700 rounded text-sm"
                >
                  🔄 Refresh
                </button>
              </div>

              {portsQuery.isLoading ? (
                <div className="text-gray-400 text-center py-4">Scanning ports...</div>
              ) : portsQuery.data?.length === 0 ? (
                <div className="text-yellow-400 text-center py-4">
                  ⚠️ No serial ports found
                </div>
              ) : (
                <div className="space-y-2">
                  {portsQuery.data?.map((port) => (
                    <button
                      key={port.path}
                      onClick={() => setSelectedPort(port.path)}
                      className={`w-full p-3 rounded-lg border text-left transition ${
                        selectedPort === port.path
                          ? 'bg-blue-600 border-blue-400'
                          : 'bg-gray-700 border-gray-600 hover:bg-gray-600'
                      }`}
                    >
                      <div className="font-mono font-bold">{port.path}</div>
                      <div className="text-sm text-gray-300">{port.description}</div>
                      {port.vendorId !== 0 && (
                        <div className="text-xs text-gray-400 mt-1">
                          VID: {port.vendorId.toString(16).toUpperCase()} | 
                          PID: {port.productId.toString(16).toUpperCase()}
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Configuration Parameters */}
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">⚙️ Configuration</h2>
              
              <div className="grid grid-cols-2 gap-4">
                {/* Baud Rate */}
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Baud Rate</label>
                  <select
                    value={baudRate}
                    onChange={(e) => setBaudRate(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  >
                    {BAUD_RATES.map((rate) => (
                      <option key={rate} value={rate}>{rate}</option>
                    ))}
                  </select>
                </div>

                {/* Parity */}
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Parity</label>
                  <select
                    value={parity}
                    onChange={(e) => setParity(e.target.value)}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  >
                    {PARITY_MODES.map((mode) => (
                      <option key={mode} value={mode}>{mode}</option>
                    ))}
                  </select>
                </div>

                {/* Data Bits */}
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Data Bits</label>
                  <select
                    value={dataBits}
                    onChange={(e) => setDataBits(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  >
                    {DATA_BITS.map((bits) => (
                      <option key={bits} value={bits}>{bits}</option>
                    ))}
                  </select>
                </div>

                {/* Stop Bits */}
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Stop Bits</label>
                  <select
                    value={stopBits}
                    onChange={(e) => setStopBits(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  >
                    {STOP_BITS.map((bits) => (
                      <option key={bits} value={bits}>{bits}</option>
                    ))}
                  </select>
                </div>

                {/* Test Address */}
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Test Address</label>
                  <input
                    type="number"
                    min="1"
                    max="255"
                    value={testAddress}
                    onChange={(e) => setTestAddress(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  />
                </div>
              </div>

              {/* Config summary */}
              <div className="mt-4 p-3 bg-gray-700 rounded-lg text-center font-mono">
                {dataBits}{parity.charAt(0)}{stopBits} @ {baudRate} baud
              </div>

              {/* Quick actions */}
              <div className="mt-4 grid grid-cols-2 gap-2">
                <button
                  onClick={() => healthCheckMutation.mutate()}
                  disabled={healthCheckMutation.isPending || !selectedPort}
                  className="py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  {healthCheckMutation.isPending ? '⏳ Testing...' : '💓 Health Check'}
                </button>
                <button
                  onClick={() => parityDetectMutation.mutate()}
                  disabled={parityDetectMutation.isPending || !selectedPort}
                  className="py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 rounded-lg font-bold transition"
                >
                  {parityDetectMutation.isPending ? '⏳ Detecting...' : '🔎 Auto-detect Parity'}
                </button>
              </div>

              {/* Health check result */}
              {healthCheckMutation.data && (
                <div className={`mt-4 p-4 rounded-lg ${
                  healthCheckMutation.data.healthy ? 'bg-green-900/30 border border-green-500' : 'bg-red-900/30 border border-red-500'
                }`}>
                  <div className="font-bold mb-2">
                    {healthCheckMutation.data.healthy ? '✅ Communication OK' : '❌ Communication Failed'}
                  </div>
                  {healthCheckMutation.data.responseTimeMs && (
                    <div className="text-sm">Response time: {healthCheckMutation.data.responseTimeMs}ms</div>
                  )}
                  {healthCheckMutation.data.lastError && (
                    <div className="text-sm text-red-400">{healthCheckMutation.data.lastError}</div>
                  )}
                </div>
              )}

              {/* Parity detection result */}
              {parityDetectMutation.data && (
                <div className={`mt-4 p-4 rounded-lg ${
                  parityDetectMutation.data.detected ? 'bg-purple-900/30 border border-purple-500' : 'bg-red-900/30 border border-red-500'
                }`}>
                  {parityDetectMutation.data.detected ? (
                    <>
                      <div className="font-bold mb-2">✅ Parity Detected: {parityDetectMutation.data.parityMode}</div>
                      <div className="text-sm">{parityDetectMutation.data.description}</div>
                      <button
                        onClick={() => setParity(parityDetectMutation.data?.parityMode || 'NONE')}
                        className="mt-2 px-3 py-1 bg-purple-600 hover:bg-purple-700 rounded text-sm"
                      >
                        Apply this setting
                      </button>
                    </>
                  ) : (
                    <div className="text-red-400">❌ {parityDetectMutation.data.error}</div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Scanning Tools */}
          <div className="space-y-6">
            {/* Smart Scan */}
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">🤖 Smart Auto-Scan</h2>
              <p className="text-gray-400 text-sm mb-4">
                Automatically scans all ports with different configurations to find a working setup.
              </p>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Timeout (ms)</label>
                  <input
                    type="number"
                    min="100"
                    max="5000"
                    value={smartScanTimeout}
                    onChange={(e) => setSmartScanTimeout(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  />
                </div>
                <div className="flex items-end">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={stopOnFirstMatch}
                      onChange={(e) => setStopOnFirstMatch(e.target.checked)}
                      className="w-5 h-5 rounded"
                    />
                    <span className="text-sm">Stop on first match</span>
                  </label>
                </div>
              </div>

              <button
                onClick={() => smartScanMutation.mutate()}
                disabled={smartScanMutation.isPending}
                className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 rounded-lg font-bold text-lg transition"
              >
                {smartScanMutation.isPending ? '⏳ Scanning all ports...' : '🔍 Start Smart Scan'}
              </button>

              {/* Smart scan results */}
              {smartScanMutation.data && (
                <div className="mt-4 space-y-2">
                  {smartScanMutation.data.length === 0 ? (
                    <div className="p-4 bg-yellow-900/30 border border-yellow-500 rounded-lg text-center">
                      ⚠️ No working configuration found
                    </div>
                  ) : (
                    smartScanMutation.data.map((config, idx) => (
                      <div key={idx} className="p-4 bg-green-900/30 border border-green-500 rounded-lg">
                        <div className="flex justify-between items-start">
                          <div>
                            <div className="font-bold text-green-400">✅ {config.port.path}</div>
                            <div className="text-sm mt-1">
                              {config.baudRate} baud, {config.parity} parity, Address {config.dispenserAddress}
                            </div>
                          </div>
                          <div className="text-right">
                            <div className="text-xs text-gray-400">Confidence</div>
                            <div className="text-2xl font-bold text-green-400">{config.confidence}%</div>
                          </div>
                        </div>
                        <button
                          onClick={() => {
                            setSelectedPort(config.port.path);
                            setBaudRate(config.baudRate);
                            setParity(config.parity);
                            setTestAddress(config.dispenserAddress);
                          }}
                          className="mt-2 px-3 py-1 bg-green-600 hover:bg-green-700 rounded text-sm"
                        >
                          Apply this configuration
                        </button>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>

            {/* Address Scan */}
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">📍 Address Scanner</h2>
              <p className="text-gray-400 text-sm mb-4">
                Scan a range of addresses on the selected port to find responding dispensers.
              </p>

              <div className="grid grid-cols-3 gap-4 mb-4">
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Start Address</label>
                  <input
                    type="number"
                    min="1"
                    max="255"
                    value={scanStartAddr}
                    onChange={(e) => setScanStartAddr(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">End Address</label>
                  <input
                    type="number"
                    min="1"
                    max="255"
                    value={scanEndAddr}
                    onChange={(e) => setScanEndAddr(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  />
                </div>
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Timeout (ms)</label>
                  <input
                    type="number"
                    min="100"
                    max="5000"
                    value={scanTimeout}
                    onChange={(e) => setScanTimeout(Number(e.target.value))}
                    className="w-full bg-gray-700 rounded-lg px-3 py-2 text-white"
                  />
                </div>
              </div>

              <button
                onClick={() => addressScanMutation.mutate()}
                disabled={addressScanMutation.isPending || !selectedPort}
                className="w-full py-3 bg-orange-600 hover:bg-orange-700 disabled:bg-gray-600 rounded-lg font-bold text-lg transition"
              >
                {addressScanMutation.isPending ? '⏳ Scanning addresses...' : '📡 Scan Address Range'}
              </button>

              {!selectedPort && (
                <div className="mt-2 text-yellow-400 text-sm text-center">
                  ⚠️ Select a port first
                </div>
              )}

              {/* Address scan results */}
              {addressScanMutation.data && (
                <div className="mt-4 p-4 bg-gray-700 rounded-lg">
                  <div className="flex justify-between text-sm text-gray-400 mb-3">
                    <span>Port: {addressScanMutation.data.portPath}</span>
                    <span>Tested: {addressScanMutation.data.testedCount} addresses</span>
                  </div>
                  
                  {addressScanMutation.data.respondingAddresses.length === 0 ? (
                    <div className="text-center text-yellow-400 py-4">
                      ⚠️ No responding addresses found in range {addressScanMutation.data.addressRange}
                    </div>
                  ) : (
                    <div className="space-y-2">
                      <div className="font-bold text-green-400 mb-2">
                        ✅ Found {addressScanMutation.data.respondingAddresses.length} responding address(es):
                      </div>
                      {addressScanMutation.data.respondingAddresses.map((addr) => (
                        <div
                          key={addr.address}
                          className="flex justify-between items-center p-2 bg-green-900/30 border border-green-600 rounded"
                        >
                          <div>
                            <span className="font-mono font-bold">Address {addr.address}</span>
                            {addr.description && (
                              <span className="ml-2 text-sm text-gray-400">{addr.description}</span>
                            )}
                          </div>
                          <button
                            onClick={() => setTestAddress(addr.address)}
                            className="px-2 py-1 bg-green-600 hover:bg-green-700 rounded text-xs"
                          >
                            Use this
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Quick presets */}
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <h2 className="text-xl font-bold mb-4">⚡ Quick Presets</h2>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => {
                    setBaudRate(9600);
                    setParity('NONE');
                    setDataBits(8);
                    setStopBits(1);
                  }}
                  className="p-3 bg-gray-700 hover:bg-gray-600 rounded-lg text-left"
                >
                  <div className="font-bold">8N1 @ 9600</div>
                  <div className="text-sm text-gray-400">Simulator/Python</div>
                </button>
                <button
                  onClick={() => {
                    setBaudRate(9600);
                    setParity('EVEN');
                    setDataBits(8);
                    setStopBits(1);
                  }}
                  className="p-3 bg-gray-700 hover:bg-gray-600 rounded-lg text-left"
                >
                  <div className="font-bold">8E1 @ 9600</div>
                  <div className="text-sm text-gray-400">Standard EHL</div>
                </button>
                <button
                  onClick={() => {
                    setBaudRate(19200);
                    setParity('NONE');
                    setDataBits(8);
                    setStopBits(1);
                  }}
                  className="p-3 bg-gray-700 hover:bg-gray-600 rounded-lg text-left"
                >
                  <div className="font-bold">8N1 @ 19200</div>
                  <div className="text-sm text-gray-400">High speed</div>
                </button>
                <button
                  onClick={() => {
                    setTestAddress(33);
                    setScanStartAddr(32);
                    setScanEndAddr(40);
                  }}
                  className="p-3 bg-gray-700 hover:bg-gray-600 rounded-lg text-left"
                >
                  <div className="font-bold">Legacy 32+n</div>
                  <div className="text-sm text-gray-400">Real hardware</div>
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Help section */}
        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h3 className="text-xl font-bold mb-4">📖 Help</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-gray-300">
            <div>
              <h4 className="font-bold text-blue-400 mb-2">🔌 Port Detection</h4>
              <p className="text-sm">
                The system automatically detects hardware serial ports (USB adapters) and 
                virtual ports created by socat (/tmp/vserial*).
              </p>
            </div>
            <div>
              <h4 className="font-bold text-green-400 mb-2">🤖 Smart Scan</h4>
              <p className="text-sm">
                Tests all ports with common baud rates (9600, 19200, 115200) and parity modes 
                (NONE, EVEN, ODD) to find a working configuration automatically.
              </p>
            </div>
            <div>
              <h4 className="font-bold text-orange-400 mb-2">📍 Address Scan</h4>
              <p className="text-sm">
                Some pumps use address 1-8 (standard), while others use 32+n format 
                (e.g., pump 1 = address 33). The scanner helps find the right address.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
