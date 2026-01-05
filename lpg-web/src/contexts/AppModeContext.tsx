import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export type AppMode = 'LAB' | 'KIOSK' | 'LOADING';

interface AppModeContextType {
  mode: AppMode;
  isLab: boolean;
  isKiosk: boolean;
  isLoading: boolean;
  profiles: string[];
  description: string;
}

const AppModeContext = createContext<AppModeContextType | undefined>(undefined);

export function AppModeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<AppMode>('LOADING');
  const [profiles, setProfiles] = useState<string[]>([]);
  const [description, setDescription] = useState<string>('');

  useEffect(() => {
    async function fetchMode() {
      try {
        const response = await axios.get(`${API_URL}/config/mode`);
        setMode(response.data.mode);
        setProfiles(response.data.profiles);
        setDescription(response.data.description);
      } catch (error) {
        console.error('Failed to fetch app mode, defaulting to LAB:', error);
        setMode('LAB');
        setDescription('Failed to detect mode, using LAB as fallback');
      }
    }

    fetchMode();
  }, []);

  const value: AppModeContextType = {
    mode,
    isLab: mode === 'LAB',
    isKiosk: mode === 'KIOSK',
    isLoading: mode === 'LOADING',
    profiles,
    description,
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
