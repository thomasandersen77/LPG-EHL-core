import axios from 'axios';

// Use relative URLs that work with the backend proxy
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Types
export interface PumpStatus {
  state: string;
  address: number;
  volumeLitres: number;
  amountKr: number;
  pricePerLitreKr: number;
  nozzleLifted: boolean;
  hasPendingTransaction: boolean;
}

export interface CardSwipeRequest {
  maxAmountKr: number;
  triggeredBy?: string;
  paymentMethod?: string;
}

/**
 * Pump API Client - Uses the correct emulator/pump endpoints
 * that communicate with the real hardware via PumpController
 */
export const pumpApi = {
  /**
   * Get pump status
   */
  getStatus: async (address: number = 1): Promise<PumpStatus> => {
    const response = await api.get<PumpStatus>(`/api/v1/emulator/pump/${address}/status`);
    return response.data;
  },

  /**
   * Simulate card swipe (step 1)
   * Creates an authorization and starts the 60-second countdown
   */
  cardSwipe: async (address: number = 1, maxAmountKr: number = 2000): Promise<any> => {
    const response = await api.post(`/api/v1/emulator/pump/${address}/card-swipe`, {
      maxAmountKr,
      triggeredBy: 'STATION_OWNER_PAGE',
      paymentMethod: 'CARD'
    });
    return response.data;
  },

  /**
   * Unblock pump (step 2 - "FRI DISPENSER")
   * Sends UNBLOCK command to physical dispenser
   */
  unblock: async (address: number = 1): Promise<any> => {
    const response = await api.post(`/api/v1/emulator/pump/${address}/unblock`);
    return response.data;
  },

  /**
   * Start pumping simulation (for GUI testing)
   */
  startPumping: async (address: number = 1): Promise<any> => {
    const response = await api.post(`/api/v1/emulator/pump/${address}/start-pumping`);
    return response.data;
  },

  /**
   * Block pump / stop pumping
   * Sends BLOCK command to physical dispenser
   */
  block: async (address: number = 1): Promise<any> => {
    const response = await api.post(`/api/v1/emulator/pump/${address}/block`);
    return response.data;
  },

  /**
   * Confirm payment after pumping
   * @param paymentMethod - Only CARD or CREDIT are valid
   */
  confirmPayment: async (address: number = 1, paymentMethod: 'CARD' | 'CREDIT' = 'CARD'): Promise<any> => {
    const response = await api.post(`/api/v1/emulator/settle/${address}?method=${paymentMethod}`);
    return response.data;
  },

  /**
   * Admin: Cleanup stuck authorizations
   */
  cleanupAuthorizations: async (): Promise<any> => {
    const response = await api.post('/api/v1/admin/cleanup-authorizations');
    return response.data;
  },

  /**
   * Admin: Full reset (mark all paid + cancel authorizations)
   */
  fullReset: async (): Promise<any> => {
    const response = await api.post('/api/v1/admin/full-reset');
    return response.data;
  }
};
