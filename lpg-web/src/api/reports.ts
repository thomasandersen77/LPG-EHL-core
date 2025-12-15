import axios from 'axios';
import type { PaymentType } from './transactions';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export interface PaymentTypeSummary {
  litres: number;
  amountNok: number;
  count: number;
}

export interface DailyReport {
  date: string;
  totalLitres: number;
  totalAmountNok: number;
  transactionCount: number;
  byPaymentType: Record<PaymentType, PaymentTypeSummary>;
}

export interface RangeReport {
  from: string;
  to: string;
  totalLitres: number;
  totalAmountNok: number;
  transactionCount: number;
  dailyBreakdown: DailyReport[];
}

export async function fetchDailyReport(date: string): Promise<DailyReport> {
  const res = await axios.get<DailyReport>(`${API_URL}/reports/daily`, {
    params: { date }
  });
  return res.data;
}

export async function fetchRangeReport(from: string, to: string): Promise<RangeReport> {
  const res = await axios.get<RangeReport>(`${API_URL}/reports/range`, {
    params: { from, to }
  });
  return res.data;
}
