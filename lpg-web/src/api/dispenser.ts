import axios from 'axios';
import type { DispenserStateDto } from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

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
  unblock: async (): Promise<DispenserStateDto> => {
    const response = await api.post<DispenserStateDto>('/api/v1/dispenser/unblock');
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
};
