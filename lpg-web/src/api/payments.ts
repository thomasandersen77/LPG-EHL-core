import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || '';

export type PaymentMethod = 'CARD' | 'CREDIT';
export type PaymentStatus = 'PENDING' | 'APPROVED' | 'DECLINED' | 'CANCELLED';

export interface PaymentRequest {
  amountCents: number;
  method: PaymentMethod;
  reference: string;
  metadata?: Record<string, string>;
}

export interface Payment {
  id: string;
  requestedAt: string;
  completedAt: string | null;
  amountCents: number;
  method: PaymentMethod;
  status: PaymentStatus;
  reference: string;
  metadata?: Record<string, string>;
}

export async function startPayment(request: PaymentRequest): Promise<Payment> {
  const res = await axios.post<Payment>(`${API_URL}/api/v1/payments`, request);
  return res.data;
}

export async function getPayment(id: string): Promise<Payment> {
  const res = await axios.get<Payment>(`${API_URL}/api/v1/payments/${id}`);
  return res.data;
}
