import ApiService from './apiService';
import type { LLMMessage } from '$lib/types';

export interface StreamingCallbacks {
  onChunk: (chunk: string) => void;
  onError?: (error: string) => void;
  onComplete?: () => void;
}

class LLMAgent {
  private apiService = new ApiService();

  async chat(messages: LLMMessage[], callbacks: StreamingCallbacks): Promise<void> {
    await this.apiService.streamPost(
      '/api/chat',
      { messages },
      callbacks.onChunk,
      callbacks.onError,
      callbacks.onComplete
    );
  }
}

export const llmAgent = new LLMAgent();