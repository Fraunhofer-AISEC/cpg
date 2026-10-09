import ApiService from './apiService';
import type { LLMMessage } from '$lib/types';

export interface StreamingCallbacks {
  onChunk: (chunk: string) => void;
  onError?: (error: string) => void;
  onComplete?: () => void;
}

class LLMAgent {
  private apiService = new ApiService();

  async chat(
    messages: LLMMessage[],
    client: string,
    model: string,
    sessionId: string,
    callbacks: StreamingCallbacks
  ): Promise<void> {
    await this.apiService.streamPost(
      '/api/chat',
      { messages, client, model, sessionId },
      callbacks.onChunk,
      callbacks.onError,
      callbacks.onComplete
    );
  }
  async createSession(): Promise<string> {
    const response = await fetch('/api/chat/sessions', { method: 'POST' });
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    return (await response.json()).sessionId;
  }
}

export const llmAgent = new LLMAgent();
