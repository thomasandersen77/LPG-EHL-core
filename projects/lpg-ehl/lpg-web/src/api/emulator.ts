import axios from 'axios';

const EMULATOR_URL = import.meta.env.VITE_EMULATOR_URL || '/api/v1';

export type EmulatorScenario = 'NORMAL' | 'TIMEOUT' | 'CHECKSUM_ERROR' | 'NO_CONNECTION';

export interface EmulatorStatus {
  dispenserAddress: number;
  scenario: EmulatorScenario;
  lastMessage: string | null;
  lastError: string | null;
  connected: boolean;
}

export async function setEmulatorScenario(address: number, scenario: EmulatorScenario): Promise<EmulatorStatus> {
  const res = await axios.post<EmulatorStatus>(`${EMULATOR_URL}/emulator/scenario`, {
    dispenserAddress: address,
    scenario
  });
  return res.data;
}

export async function resetEmulator(address: number): Promise<EmulatorStatus> {
  const res = await axios.post<EmulatorStatus>(`${EMULATOR_URL}/emulator/reset/${address}`);
  return res.data;
}

export async function getEmulatorStatus(address: number): Promise<EmulatorStatus> {
  const res = await axios.get<EmulatorStatus>(`${EMULATOR_URL}/emulator/status/${address}`);
  return res.data;
}

export interface SettlementResponse {
  status: string;
  method: string;
  windowsBroadcastSent?: boolean;
  transaction?: {
    dispenserId: number;
    liters: number;
    amountNok: number;
    unitPrice: number;
    finishedAt: string;
    idempotencyKey: string;
  };
  message?: string;
}

export async function settlePayment(dispenserId: number, method: 'CARD' | 'CREDIT' = 'CARD'): Promise<SettlementResponse> {
  const res = await axios.post<SettlementResponse>(
    `${EMULATOR_URL}/emulator/settle/${dispenserId}`,
    null,
    { params: { method } }
  );
  return res.data;
}

export interface ConfirmPaymentResponse {
  success: boolean;
  message: string;
  authorization?: {
    authorizationId: string;
    status: string;
    actualVolumeLiters: number;
    actualAmountKr: number;
    completedAt: string;
  };
  error?: string;
}

export async function confirmPayment(
  address: number, 
  paymentMethod: 'CARD' | 'CREDIT' = 'CARD'
): Promise<ConfirmPaymentResponse> {
  const res = await axios.post<ConfirmPaymentResponse>(
    `${EMULATOR_URL}/emulator/pump/${address}/confirm-payment`,
    { paymentMethod },
    { headers: { 'Content-Type': 'application/json' } }
  );
  return res.data;
}
