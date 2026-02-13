const API_URL = import.meta.env.VITE_API_URL || '/api/v1';

export interface SyncStatusResponse {
  pendingCount: number;
  syncedCount: number;
  failedCount: number;
  lastSyncTime: string | null;
}

export async function fetchSyncStatus(): Promise<SyncStatusResponse> {
  const response = await fetch(`${API_URL}/sync/status`);
  if (!response.ok) {
    throw new Error('Failed to fetch sync status');
  }
  return response.json();
}

export async function triggerManualSync(): Promise<void> {
  const response = await fetch(`${API_URL}/sync/trigger`, {
    method: 'POST',
  });
  if (!response.ok) {
    throw new Error('Failed to trigger sync');
  }
}

export interface AzureQueueMessage {
  messageId: string;
  insertionTime: string;
  expirationTime: string;
  dequeueCount: number;
  entityType: string | null;
  entityId: string | null;
  status: string | null;
  retryCount: number;
  transaction: {
    dispenserAddress: number | null;
    volumeLiters: number | null;
    amountKr: number | null;
    pricePerLiter: number | null;
    paymentType: string | null;
    paymentStatus: string | null;
    timestamp: string | null;
  } | null;
}

export interface AzureQueueByDateResponse {
  dates: Record<string, AzureQueueMessage[]>;
  totalMessages: number;
}

export async function fetchQueueMessages(maxMessages: number = 32): Promise<AzureQueueMessage[]> {
  const response = await fetch(`${API_URL}/sync/queue/messages?maxMessages=${maxMessages}`);
  if (!response.ok) {
    throw new Error('Failed to fetch queue messages');
  }
  return response.json();
}

export async function fetchQueueMessagesByDate(): Promise<AzureQueueByDateResponse> {
  const response = await fetch(`${API_URL}/sync/queue/by-date`);
  if (!response.ok) {
    throw new Error('Failed to fetch queue messages by date');
  }
  return response.json();
}
