import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || '/api/v1';

export type DispenserState =
  | 'IDLE'
  | 'READY'
  | 'DELIVERING'
  | 'FINISHED'
  | 'ERROR';

export interface DispenserDto {
  state: DispenserState;
  amountToPay: number; // NOK
  litres: number;
  pricePerLitre: number; // NOK
  includeRoadTax: boolean;
  cardModeActive: boolean;
  dayMode: boolean;
  stationCreditActive: boolean;
  connected: boolean;
}

export async function fetchDispenserState(): Promise<DispenserDto> {
  const res = await axios.get<DispenserDto>(`${API_URL}/dispenser/state`);
  return res.data;
}

export async function unblockDispenser(): Promise<DispenserDto> {
  const res = await axios.post<DispenserDto>(`${API_URL}/dispenser/unblock`);
  return res.data;
}

export async function stopDispenser(): Promise<DispenserDto> {
  const res = await axios.post<DispenserDto>(`${API_URL}/dispenser/stop`);
  return res.data;
}
