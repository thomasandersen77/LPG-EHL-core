/**
 * API Type Definitions for LPG EHL
 */

export interface DispenserStateDto {
  state: 'IDLE' | 'READY' | 'DELIVERING' | 'FINISHED' | 'ERROR';
  amountToPay: number;
  litres: number;
  pricePerLitre: number;
  includeRoadTax: boolean;
  cardModeActive: boolean;
  dayMode: boolean;
  stationCreditActive: boolean;
  connected: boolean;
}

export interface TransactionResponse {
  transactionId: string;
  dispenserAddress: number;
  nozzleNumber: number;
  productCode: string | null;
  volumeLiters: number;
  amountKr: number;
  pricePerLiter: number | null;
  timestamp: string;
  decodedData: Record<string, any> | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface DispenserStatusResponse {
  dispenserAddress: number;
  lastTransactionId: string | null;
  totalTransactions: number;
  totalVolumeLiters: number;
  totalAmountKr: number;
  lastSeen: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
