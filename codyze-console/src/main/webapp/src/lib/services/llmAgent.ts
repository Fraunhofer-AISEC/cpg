import ApiService from './apiService';

export interface LLMMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface StreamingCallbacks {
  onChunk: (chunk: string) => void;
  onError?: (error: string) => void;
  onComplete?: () => void;
}

class LLMAgent {
  private apiService = new ApiService();

  async chat(messages: LLMMessage[], callbacks: StreamingCallbacks): Promise<void> {
    const requestData = {
      messages: messages.map((msg) => ({
        role: msg.role,
        content: msg.content
      }))
    };

    await this.apiService.streamPost(
      '/api/chat',
      requestData,
      callbacks.onChunk,
      callbacks.onError,
      callbacks.onComplete
    );
  }

  // Custom MCP client endpoint - prototype
  async chatCustom(messages: LLMMessage[], callbacks: StreamingCallbacks): Promise<void> {
    console.log('[LLMAgent] Using custom MCP client endpoint');
    console.log('[LLMAgent] Messages:', messages);

    const requestData = {
      messages: messages.map((msg) => ({
        role: msg.role,
        content: msg.content
      }))
    };

    console.log('[LLMAgent] Request data:', requestData);

    await this.apiService.streamPost(
      '/api/chat-custom',
      requestData,
      (chunk) => {
        console.log('[LLMAgent] Received chunk:', chunk);
        callbacks.onChunk(chunk);
      },
      (error) => {
        console.error('[LLMAgent] Error:', error);
        callbacks.onError?.(error);
      },
      () => {
        console.log('[LLMAgent] Stream complete');
        callbacks.onComplete?.();
      }
    );
  }
}

export const llmAgent = new LLMAgent();
