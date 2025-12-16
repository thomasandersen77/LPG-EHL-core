import axios from 'axios';
import type { TransactionDto } from './transactions';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export interface CreditAccountDto {
  id: string;
  customerName: string;
  customerNumber: string;
  balanceNok: number;
  lastActivityAt?: string;
}

export interface CreateCreditAccountRequest {
  customerName: string;
  customerNumber: string;
  initialBalanceNok?: number;
}

export async function fetchCreditAccounts(): Promise<CreditAccountDto[]> {
  const res = await axios.get<CreditAccountDto[]>(`${API_URL}/credit/accounts`);
  return res.data;
}

export async function fetchCreditAccount(id: string): Promise<CreditAccountDto> {
  const res = await axios.get<CreditAccountDto>(`${API_URL}/credit/accounts/${id}`);
  return res.data;
}

export async function createCreditAccount(request: CreateCreditAccountRequest): Promise<CreditAccountDto> {
  const res = await axios.post<CreditAccountDto>(`${API_URL}/credit/accounts`, request);
  return res.data;
}

export async function fetchCreditAccountTransactions(accountId: string): Promise<TransactionDto[]> {
  const res = await axios.get<TransactionDto[]>(`${API_URL}/credit/accounts/${accountId}/transactions`);
  return res.data;
}
