import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export type PaymentType = 'CASH' | 'CARD' | 'CREDIT' | 'UNKNOWN';

export interface TransactionDto {
  id: string;
  dispenserAddress: number;
  startedAt: string;
  finishedAt: string | null;
  litres: number;
  amountNok: number;
  pricePerLitreNok: number;
  paymentType: PaymentType;
  customerName?: string;
  customerId?: string;
  includesRoadTax: boolean;
  state: string;
}

export interface TransactionFilter {
  from?: string;
  to?: string;
  paymentType?: PaymentType | 'ALL';
  customerId?: string;
  page?: number;
  size?: number;
}

export interface TransactionsPage {
  content: TransactionDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export async function fetchTransactions(filter: TransactionFilter): Promise<TransactionsPage> {
  const params: Record<string, string | number> = {};
  if (filter.from) params['from'] = filter.from;
  if (filter.to) params['to'] = filter.to;
  if (filter.paymentType && filter.paymentType !== 'ALL') {
    params['paymentType'] = filter.paymentType;
  }
  if (filter.customerId) params['customerId'] = filter.customerId;
  if (filter.page !== undefined) params['page'] = filter.page;
  if (filter.size !== undefined) params['size'] = filter.size;

  const res = await axios.get<TransactionsPage>(`${API_URL}/transactions`, { params });
  return res.data;
}

export async function fetchTransaction(id: string): Promise<TransactionDto> {
  const res = await axios.get<TransactionDto>(`${API_URL}/transactions/${id}`);
  return res.data;
}

export async function fetchTransactionCount(): Promise<number> {
  const res = await axios.get<{ count: number }>(`${API_URL}/transactions/count`);
  return res.data.count;
}
