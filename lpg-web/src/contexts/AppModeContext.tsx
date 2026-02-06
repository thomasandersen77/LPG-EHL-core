import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export type AppMode = 'LAB' | 'KIOSK' | 'LOADING';
export type HardwareMode = 'LAB' | 'FIELD' | 'LOADING';

export type ConnectionKind = 'SOCAT_VIRTUAL' | 'REAL_SERIAL' | null;

interface AppModeContextType {
  mode: AppMode;
  isLab: boolean;
  isKiosk: boolean;
  isLoading: boolean;
  profiles: string[];
  description: string;
  hardwareMode: HardwareMode;
  isRealHardware: boolean;
  hardwareDescription: string;
  serialPort?: string;
  baudRate?: number;
  parity?: string;
  dataBits?: number;
  stopBits?: number;
  connectionKind: ConnectionKind;
}

const AppModeContext = createContext<AppModeContextType | undefined>(undefined);

export function AppModeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<AppMode>('LOADING');
  const [profiles, setProfiles] = useState<string[]>([]);
  const [description, setDescription] = useState<string>('');
  const [hardwareMode, setHardwareMode] = useState<HardwareMode>('LOADING');
  const [isRealHardware, setIsRealHardware] = useState(false);
  const [hardwareDescription, setHardwareDescription] = useState('');
  const [serialPort, setSerialPort] = useState<string | undefined>();
  const [baudRate, setBaudRate] = useState<number | undefined>();
  const [parity, setParity] = useState<string | undefined>();
  const [dataBits, setDataBits] = useState<number | undefined>();
  const [stopBits, setStopBits] = useState<number | undefined>();
  const [connectionKind, setConnectionKind] = useState<ConnectionKind>(null);

  useEffect(() => {
    async function fetchModes() {
      try {
        // Fetch application mode (Spring profile based)
        const appModeResponse = await axios.get(`${API_URL}/config/mode`);
        setMode(appModeResponse.data.mode);
        setProfiles(appModeResponse.data.profiles);
        setDescription(appModeResponse.data.description);
        
        // Fetch hardware mode (emulator vs real hardware)
        try {
          const hwResponse = await axios.get(`${API_URL}/config/hardware-mode`);
          setHardwareMode(hwResponse.data.hardwareMode);
          setIsRealHardware(hwResponse.data.isRealHardware);
          setHardwareDescription(hwResponse.data.description);
          setSerialPort(hwResponse.data.serialPort);
          setBaudRate(hwResponse.data.baudRate);
          setParity(hwResponse.data.parity);
          setDataBits(hwResponse.data.dataBits);
          setStopBits(hwResponse.data.stopBits);
          setConnectionKind(hwResponse.data.connectionKind || null);
        } catch (hwError) {
          console.warn('Failed to fetch hardware mode:', hwError);
          // Default to LAB if hardware-mode endpoint fails
          setHardwareMode('LAB');
          setHardwareDescription('Failed to detect hardware mode, defaulting to LAB');
        }
      } catch (error) {
        console.error('Failed to fetch app mode, defaulting to LAB:', error);
        setMode('LAB');
        setDescription('Failed to detect mode, using LAB as fallback');
        setHardwareMode('LAB');
        setHardwareDescription('Failed to detect mode, using LAB as fallback');
      }
    }

    fetchModes();
  }, []);

  const value: AppModeContextType = {
    mode,
    isLab: mode === 'LAB',
    isKiosk: mode === 'KIOSK',
    isLoading: mode === 'LOADING',
    profiles,
    description,
    hardwareMode,
    isRealHardware,
    hardwareDescription,
    serialPort,
    baudRate,
    parity,
    dataBits,
    stopBits,
    connectionKind,
  };

  return <AppModeContext.Provider value={value}>{children}</AppModeContext.Provider>;
}

export function useAppMode() {
  const context = useContext(AppModeContext);
  if (context === undefined) {
    throw new Error('useAppMode must be used within AppModeProvider');
  }
  return context;
}
