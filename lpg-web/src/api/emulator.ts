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

export async function setEmulatorScenario(scenario: EmulatorScenario): Promise<EmulatorStatus> {
  const res = await axios.post<EmulatorStatus>(`${EMULATOR_URL}/emulator/scenario`, {
    scenario
  });
  return res.data;
}

export async function resetEmulator(): Promise<EmulatorStatus> {
  const res = await axios.post<EmulatorStatus>(`${EMULATOR_URL}/emulator/reset`);
  return res.data;
}

export async function getEmulatorStatus(): Promise<EmulatorStatus> {
  const res = await axios.get<EmulatorStatus>(`${EMULATOR_URL}/emulator/status`);
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

export async function settlePayment(method: 'CARD' | 'CREDIT' = 'CARD'): Promise<SettlementResponse> {
  const res = await axios.post<SettlementResponse>(
    `${EMULATOR_URL}/emulator/settle`,
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
  paymentMethod: 'CARD' | 'CREDIT' = 'CARD'
): Promise<ConfirmPaymentResponse> {
  const res = await axios.post<ConfirmPaymentResponse>(
    `${EMULATOR_URL}/emulator/pump/confirm-payment`,
    { paymentMethod },
    { headers: { 'Content-Type': 'application/json' } }
  );
  return res.data;
}
