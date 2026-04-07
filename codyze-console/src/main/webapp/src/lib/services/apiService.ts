const DONE_SIGNAL = '[DONE]';
const ERROR_PREFIX = 'ERROR:';

class ApiService {
  constructor(private readonly baseUrl: string = '') {}

  async streamPost(
    url: string,
    body: Record<string, unknown>,
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
      let eventDataLines: string[] = [];

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          onComplete?.();
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        for (const raw of lines) {
          const line = raw.replace(/\r$/, '');

          if (line.startsWith(':')) continue;

          if (line === '') {
            if (eventDataLines.length > 0) {
              const eventData = eventDataLines.join('\n');
              eventDataLines = [];

              if (eventData === DONE_SIGNAL) {
                onComplete?.();
                return;
              }

              if (eventData.startsWith(ERROR_PREFIX)) {
                onError?.(eventData.slice(ERROR_PREFIX.length).trim());
                continue;
              }

              onChunk(eventData);
            }
            continue;
          }

          if (line.startsWith('data:')) {
            eventDataLines.push(line.slice(5).trimStart());
          }
        }
      }
    } catch (error) {
      onError?.(error instanceof Error ? error.message : String(error));
    }
  }
}

export default ApiService;