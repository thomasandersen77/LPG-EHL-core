import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081/api/v1';

export interface DailySummaryResponse {
  summaryDate: string;
  dispenserAddress: number;
  transactionCount: number;
  totalVolumeLiters: number;
  totalAmountKr: number;
  averagePricePerLiter: number;
}

export interface PeriodSummaryResponse {
  fromDate: string;
  toDate: string;
  dispenserAddress?: number;
  totalTransactions: number;
  totalVolumeLiters: number;
  totalAmountKr: number;
  averagePricePerLiter: number;
  dailySummaries: DailySummaryResponse[];
}

export async function fetchDailySummary(date?: string): Promise<DailySummaryResponse[]> {
  const res = await axios.get<DailySummaryResponse[]>(`${API_URL}/reports/daily`, {
    params: { date }
  });
  return res.data;
}

export async function fetchPeriodSummary(from: string, to: string, dispenserAddress?: number): Promise<PeriodSummaryResponse> {
  const res = await axios.get<PeriodSummaryResponse>(`${API_URL}/reports/period`, {
    params: { from, to, dispenserAddress }
  });
  return res.data;
}
