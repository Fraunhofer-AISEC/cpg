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
            messages: messages.map(msg => ({
                role: msg.role,
                content: msg.content
            }))
        };

        await this.apiService.streamPost('/api/chat', requestData, callbacks.onChunk, callbacks.onError, callbacks.onComplete);
    }
}

export const llmAgent = new LLMAgent();

