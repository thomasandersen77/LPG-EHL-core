import { createContext, useContext, useState, type ReactNode } from 'react';

interface AuthContextType {
  isLoggedIn: boolean;
  stationName: string | null;
  login: (stationName?: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isLoggedIn, setIsLoggedIn] = useState(() => {
    // Check localStorage for persisted login state
    return localStorage.getItem('lpg-ehl-logged-in') === 'true';
  });
  const [stationName, setStationName] = useState<string | null>(() => {
    return localStorage.getItem('lpg-ehl-station-name');
  });

  const login = (name: string = 'NorgesGass Demo Stasjon') => {
    setIsLoggedIn(true);
    setStationName(name);
    localStorage.setItem('lpg-ehl-logged-in', 'true');
    localStorage.setItem('lpg-ehl-station-name', name);
  };

  const logout = () => {
    setIsLoggedIn(false);
    setStationName(null);
    localStorage.removeItem('lpg-ehl-logged-in');
    localStorage.removeItem('lpg-ehl-station-name');
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, stationName, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
