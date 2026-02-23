class ApiService {
  constructor(private readonly baseUrl: string = '') {}

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