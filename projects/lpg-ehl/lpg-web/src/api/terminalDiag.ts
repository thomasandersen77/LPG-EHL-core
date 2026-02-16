import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

export type TerminalAction = {
  label: string;
  method: 'get' | 'post';
  path: string;
  body?: Record<string, unknown>;
  params?: Record<string, unknown>;
};

/**
 * Payment Terminal Diagnostics API Client.
 * All calls go via backend proxy – no direct browser-to-terminal calls.
 */
export const terminalDiagApi = {
  /**
   * Execute any terminal action via backend.
   */
  execute: async (action: TerminalAction): Promise<unknown> => {
    const { method, path, body, params } = action;

    // Dedicated endpoints for diagnostics (service methods)
    const base = '/api/v1/terminal-diag';
    if (path === '/health' && method === 'get') {
      const res = await api.get(`${base}/health`);
      return res.data;
    }
    if (path === '/v1/terminal/status' && method === 'get') {
      const res = await api.get(`${base}/terminal/status`);
      return res.data;
    }
    if (path === '/v1/terminal/open' && method === 'post') {
      const res = await api.post(`${base}/terminal/open`);
      return res.data;
    }
    if (path === '/v1/terminal/close' && method === 'post') {
      const res = await api.post(`${base}/terminal/close`);
      return res.data;
    }
    if (path === '/v1/diag/schema' && method === 'get') {
      const res = await api.get(`${base}/diag/schema`);
      return res.data;
    }
    if (path === '/v1/diag/sendjson' && method === 'post' && body?.json) {
      const res = await api.post(`${base}/diag/sendjson`, { json: body.json });
      return res.data;
    }
    if (path === '/v1/diag/sendtld' && method === 'post' && body?.tldType && body?.tldData) {
      const res = await api.post(`${base}/diag/sendtld`, {
        tldType: body.tldType,
        tldData: body.tldData,
      });
      return res.data;
    }
    if (path === '/v1/diag/confirm' && method === 'post' && body?.id !== undefined && body?.allow !== undefined) {
      const res = await api.post(`${base}/diag/confirm`, { id: body.id, allow: body.allow });
      return res.data;
    }

    // Proxy for Purchase, Refund, Cashback, Admin, Events, etc.
    const res = await api.post(`${base}/proxy`, {
      method: method.toUpperCase(),
      path,
      body: body ?? null,
      params: params ?? null,
    });
    return res.data;
  },

  /** URL for SSE events stream (opens in new window) */
  getEventsStreamUrl: (since: string): string => {
    const base = API_BASE || '';
    const params = new URLSearchParams({ since });
    return `${base}/api/v1/terminal-diag/events/stream?${params}`;
  },
};
