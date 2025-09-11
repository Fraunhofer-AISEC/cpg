interface ApiResponse<T = any> {
    ok: boolean;
    status: number;
    statusText: string;
    data?: T;
    error?: string;
}

class ApiService {
    constructor(private readonly baseUrl: string = "") {
    }

    private async request<T>(
        url: string,
        options: RequestInit
    ): Promise<ApiResponse<T>> {
        try {
            const response = await fetch(`${this.baseUrl}${url}`, {
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
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
                statusText: "Network Error",
                error: err instanceof Error ? err.message : String(err)
            };
        }
    }

    get<T = any>(url: string, headers: Record<string, string> = {}) {
        return this.request<T>(url, {method: "GET", headers});
    }

    post<T = any>(url: string, body: any, headers: Record<string, string> = {}) {
        return this.request<T>(url, {
            method: "POST",
            headers,
            body: JSON.stringify(body)
        });
    }

    // TODO: Refactor this with the new post api
    async streamPost(url: string, body: any, onChunk: (chunk: string) => void, onError?: (error: string) => void, onComplete?: () => void): Promise<void> {
        try {
            const response = await fetch(`${this.baseUrl}${url}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream',
                    'Cache-Control': 'no-cache'
                },
                body: JSON.stringify(body)
            });

            if (!response.ok) {
                if (onError) {
                    onError(`HTTP ${response.status}: ${response.statusText}`);
                }
                return;
            }

            const reader = response.body?.getReader();
            if (!reader) {
                if (onError) {
                    onError('Failed to get response reader');
                }
                return;
            }

            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                
                if (done) {
                    if (onComplete) {
                        onComplete();
                    }
                    break;
                }

                buffer += decoder.decode(value, { stream: true });
                
                // Process SSE data
                const lines = buffer.split('\n');
                buffer = lines.pop() || ''; // Keep incomplete line in buffer

                for (const line of lines) {
                    if (line.startsWith('data: ')) {
                        const data = line.slice(6); // Remove 'data: ' prefix
                        if (data.trim()) {
                            onChunk(data);
                        }
                    }
                }
            }
        } catch (error) {
            if (onError) {
                onError(error instanceof Error ? error.message : String(error));
            }
        }
    }
}

export default ApiService;