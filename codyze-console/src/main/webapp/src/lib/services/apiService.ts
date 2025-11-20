interface ApiResponse<T = any> {
  ok: boolean;
  status: number;
  statusText: string;
  data?: T;
  error?: string;
}

class ApiService {
  constructor(private readonly baseUrl: string = '') {}

  private async request<T>(url: string, options: RequestInit): Promise<ApiResponse<T>> {
    try {
      const response = await fetch(`${this.baseUrl}${url}`, {
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          ...(options.headers ?? {})
        },
        ...options
      });

      const data = response.ok ? await response.json() : undefined;

      return {
        ok: response.ok,
        status: response.status,
        statusText: response.statusText,
        data,
        error: response.ok ? undefined : `HTTP ${response.status}: ${response.statusText}`
      };
    } catch (err) {
      return {
        ok: false,
        status: 0,
        statusText: 'Network Error',
        error: err instanceof Error ? err.message : String(err)
      };
    }
  }

  get<T = any>(url: string, headers: Record<string, string> = {}) {
    return this.request<T>(url, { method: 'GET', headers });
  }

  post<T = any>(url: string, body: any, headers: Record<string, string> = {}) {
    return this.request<T>(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(body)
    });
  }

  async streamPost(
    url: string,
    body: any,
    onChunk: (chunk: string) => void,
    onError?: (error: string) => void,
    onComplete?: () => void
  ): Promise<void> {
    try {
      const response = await fetch(`${this.baseUrl}${url}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache'
        },
        body: JSON.stringify(body)
      });

      if (!response.ok) {
        onError?.(`HTTP ${response.status}: ${response.statusText}`);
        return;
      }

      const reader = response.body?.getReader();
      if (!reader) {
        onError?.('Failed to get response reader');
        return;
      }

      const decoder = new TextDecoder();
      let buffer = '';
      let eventDataLines: string[] = []; // Persist across reads

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          onComplete?.();
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        // SSE spec: collect all "data:" lines until empty line, then emit as one event
        for (let raw of lines) {
          const line = raw.replace(/\r?$/, '');

          // Skip comments
          if (line.startsWith(':')) continue;

          // Empty line = end of event
          if (line === '') {
            if (eventDataLines.length > 0) {
              // Join all data lines with \n and emit
              const eventData = eventDataLines.join('\n');
              eventDataLines = [];

              if (eventData === '[DONE]') {
                onComplete?.();
                return;
              }

              if (eventData.startsWith('ERROR:')) {
                onError?.(eventData.substring('ERROR:'.length).trim());
                continue;
              }

              onChunk(eventData);
            }
            continue;
          }

          // Collect data lines
          if (line.startsWith('data:')) {
            const data = line.startsWith('data: ') ? line.slice(6) : line.slice(5).trimStart();
            eventDataLines.push(data);
          }
        }
      }
    } catch (error) {
      onError?.(error instanceof Error ? error.message : String(error));
    }
  }
}

export default ApiService;