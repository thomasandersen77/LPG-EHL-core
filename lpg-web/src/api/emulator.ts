import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export type EmulatorScenario = 'NORMAL' | 'TIMEOUT' | 'CHECKSUM_ERROR' | 'NO_CONNECTION';

export interface EmulatorStatus {
  dispenserAddress: number;
  scenario: EmulatorScenario;
  lastMessage: string | null;
  lastError: string | null;
  connected: boolean;
}

export async function setEmulatorScenario(address: number, scenario: EmulatorScenario): Promise<EmulatorStatus> {
  const res = await axios.post<EmulatorStatus>(`${API_URL}/emulator/scenario`, {
    dispenserAddress: address,
    scenario
  });
  return res.data;
}

export async function resetEmulator(address: number): Promise<EmulatorStatus> {
  const res = await axios.post<EmulatorStatus>(`${API_URL}/emulator/reset/${address}`);
  return res.data;
}

export async function getEmulatorStatus(address: number): Promise<EmulatorStatus> {
  const res = await axios.get<EmulatorStatus>(`${API_URL}/emulator/status/${address}`);
  return res.data;
}
