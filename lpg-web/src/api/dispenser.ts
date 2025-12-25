import axios from 'axios';
import type { DispenserStateDto, ProtocolResponse, VolumeResponse, TankResponse, PriceResponse, DispenserErrorResponse } from '../types/api';

// Use emulator API directly (port 8090) for settlement
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const EMULATOR_BASE_URL = 'http://localhost:8090';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Demo Dispenser API Client
 */
export const dispenserApi = {
  /**
   * Get current dispenser state
   */
  getState: async (): Promise<DispenserStateDto> => {
    const response = await api.get<DispenserStateDto>('/api/v1/dispenser/state');
    return response.data;
  },

  /**
   * Start fuel delivery (unblock)
   */
  unblock: async (paymentType: string = 'CASH'): Promise<DispenserStateDto> => {
    const response = await api.post<DispenserStateDto>(`/api/v1/dispenser/unblock?paymentType=${paymentType}`);
    return response.data;
  },

  /**
   * Stop fuel delivery
   */
  stop: async (): Promise<DispenserStateDto> => {
    const response = await api.post<DispenserStateDto>('/api/v1/dispenser/stop');
    return response.data;
  },

  /**
   * Reset dispenser to IDLE
   */
  reset: async (): Promise<DispenserStateDto> => {
    const response = await api.post<DispenserStateDto>('/api/v1/dispenser/reset');
    return response.data;
  },

  /**
   * Settle payment and reset dispenser (calls emulator directly)
   * This simulates successful card/credit payment and resets Windows Dispenserkontroll
   */
  settle: async (dispenserId: number = 1, method: 'CARD' | 'CREDIT' = 'CARD'): Promise<void> => {
    const response = await axios.post(`${EMULATOR_BASE_URL}/api/v1/emulator/settle/${dispenserId}?method=${method}`);
    return response.data;
  },

  /**
   * VB6-compatible protocol commands
   */
  
  // Product selection (VB6 PRODUCT_SELECT command 195)
  selectProduct: async (address: number = 1, product: string = '0x30'): Promise<ProtocolResponse> => {
    const response = await api.post<ProtocolResponse>('/api/v1/dispenser/product-select', {
      address,
      product
    });
    return response.data;
  },

  // Price programming (VB6 PROG_PRC command 169)
  programPrice: async (address: number = 1, priceKrPerLiter: string): Promise<ProtocolResponse> => {
    const response = await api.post<ProtocolResponse>('/api/v1/dispenser/program-price', {
      address,
      priceKrPerLiter
    });
    return response.data;
  },

  // Amount preset (VB6 PROG_AMOUNT command 170)
  programAmount: async (address: number = 1, amountOre: number): Promise<ProtocolResponse> => {
    const response = await api.post<ProtocolResponse>('/api/v1/dispenser/program-amount', {
      address,
      amountOre
    });
    return response.data;
  },

  // Volume preset (VB6 PROG_VOLUME command 171)
  programVolume: async (address: number = 1, volumeLiters: number): Promise<ProtocolResponse> => {
    const response = await api.post<ProtocolResponse>('/api/v1/dispenser/program-volume', {
      address,
      volumeLiters
    });
    return response.data;
  },

  // Get current volume (VB6 VOLUME command 77)
  getCurrentVolume: async (address: number = 1): Promise<VolumeResponse> => {
    const response = await api.get<VolumeResponse>(`/api/v1/dispenser/volume?address=${address}`);
    return response.data;
  },

  // Get tank status (VB6 TANK command 78)
  getTankStatus: async (address: number = 1): Promise<TankResponse> => {
    const response = await api.get<TankResponse>(`/api/v1/dispenser/tank?address=${address}`);
    return response.data;
  },

  // Get price (VB6 PRICE command 79)
  getCurrentPrice: async (address: number = 1): Promise<PriceResponse> => {
    const response = await api.get<PriceResponse>(`/api/v1/dispenser/price?address=${address}`);
    return response.data;
  },

  // Line test (VB6 LINETEST command 80)
  lineTest: async (address: number = 1): Promise<ProtocolResponse> => {
    const response = await api.post<ProtocolResponse>(`/api/v1/dispenser/linetest?address=${address}`);
    return response.data;
  },

  // Get error status (VB6 ERROR_QUERY command 76)
  getErrorStatus: async (address: number = 1): Promise<DispenserErrorResponse> => {
    const response = await api.get<DispenserErrorResponse>(`/api/v1/dispenser/error?address=${address}`);
    return response.data;
  },
};
