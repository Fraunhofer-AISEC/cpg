<script lang="ts">
  import type { PageProps } from './$types';
  import type { ChatMessage, LLMMessage } from '$lib/types';
  import { WelcomeScreen, ChatInterface } from '$lib/components/ai-agent';
  import { PageHeader } from '$lib/components/navigation';
  import {
    llmAgent,
    type StreamingCallbacks,
  } from '$lib/services/llmAgent';

  let { data }: PageProps = $props();
  const hasError = $derived(!data);
  const analysisResult = $derived(data?.result);

  // Load persisted state from sessionStorage
  function loadPersistedState() {
    if (typeof window === 'undefined') return { messages: [], showWelcome: true };

    const stored = sessionStorage.getItem('codyze-agent-state');
    if (!stored) return { messages: [], showWelcome: true };

    try {
      const parsed = JSON.parse(stored);
      const messages = parsed.messages.map((msg: any) => ({
        ...msg,
        timestamp: new Date(msg.timestamp)
      }));
      return { messages, showWelcome: parsed.showWelcome };
    } catch {
      return { messages: [], showWelcome: true };
    }
  }

  const persisted = loadPersistedState();

  let chatMessages = $state<ChatMessage[]>(persisted.messages);
  let currentMessage = $state('');
  let isLoading = $state(false);
  let streamingContent = $state('');
  let streamingReasoning = $state('');
  let showWelcome = $state(persisted.showWelcome);
  // Track pending tools by name for loading state
  let pendingTools = $state<Map<string, { toolName: string; arguments?: string }>>(new Map());

  // Save state to sessionStorage whenever it changes
  $effect(() => {
    if (typeof window !== 'undefined') {
      sessionStorage.setItem('codyze-agent-state', JSON.stringify({
        messages: chatMessages,
        showWelcome
      }));
    }
  });


  function handleWelcomeMessage(message: string) {
    currentMessage = message;
    showWelcome = false;
    sendMessage();
  }

  function resetChat() {
    showWelcome = true;
    chatMessages = [];
    currentMessage = '';
    streamingContent = '';
    streamingReasoning = '';
    pendingTools = new Map();
  }

  async function sendMessage() {
    if (!currentMessage.trim()) return;

    const userMessage = {
      id: Date.now().toString(),
      role: 'user' as const,
      content: currentMessage,
      timestamp: new Date()
    };

    chatMessages = [...chatMessages, userMessage];
    currentMessage = '';
    isLoading = true;
    streamingContent = '';
    streamingReasoning = '';

    try {
      const llmMessages: LLMMessage[] = chatMessages.map((msg) => ({
        role: msg.role as 'user' | 'assistant',
        content: msg.content
      }));

      const callbacks: StreamingCallbacks = {
        onChunk: (chunk: string) => {
          // Parse JSON event
          try {
            const event = JSON.parse(chunk);

            if (event.type === 'text') {
              // Normal text streaming
              streamingContent += event.content;
            } else if (event.type === 'reasoning') {
              // LLM thinking/reasoning content - accumulate it
              streamingReasoning += event.content;
            } else if (event.type === 'tool_pending') {
              // Tool is starting - add a pending message immediately
              const pendingMessage: ChatMessage = {
                id: `pending-${event.toolName}-${Date.now()}`,
                role: 'assistant' as const,
                content: '',
                contentType: 'tool-pending',
                toolResult: {
                  toolName: event.toolName,
                  content: null,
                  isPending: true
                },
                timestamp: new Date()
              };
              chatMessages = [...chatMessages, pendingMessage];
              // Track this pending tool
              pendingTools.set(event.toolName, { toolName: event.toolName, arguments: event.arguments });
            } else if (event.type === 'tool_result') {
              // Tool completed - replace pending message with result
              console.log('Received tool_result event:', event);
              // Find and update the pending message for this tool
              const pendingIdx = chatMessages.findIndex(
                m => m.contentType === 'tool-pending' && m.toolResult?.toolName === event.toolName
              );
              if (pendingIdx !== -1) {
                // Replace pending with actual result
                const updatedMessages = [...chatMessages];
                updatedMessages[pendingIdx] = {
                  ...updatedMessages[pendingIdx],
                  id: Date.now().toString(),
                  contentType: 'tool-result',
                  toolResult: {
                    toolName: event.toolName,
                    content: event.content,
                    isPending: false
                  }
                };
                chatMessages = updatedMessages;
              } else {
                // No pending message found, add as new
                chatMessages = [...chatMessages, {
                  id: Date.now().toString(),
                  role: 'assistant' as const,
                  content: '',
                  contentType: 'tool-result',
                  toolResult: {
                    toolName: event.toolName,
                    content: event.content,
                    isPending: false
                  },
                  timestamp: new Date()
                }];
              }
              // Remove from pending tracking
              pendingTools.delete(event.toolName);
            }
          } catch (e) {
            // If JSON parsing fails, treat as plain text
            console.log('Failed to parse chunk:', chunk);
            streamingContent += chunk;
          }
        },
        onError: (error: string) => {
          console.error('Streaming error:', error);
          const errorMessage = {
            id: (Date.now() + 1).toString(),
            role: 'assistant' as const,
            content: `Encountered an error: ${error}`,
            timestamp: new Date()
          };
          chatMessages = [...chatMessages, errorMessage];
          isLoading = false;
          streamingContent = '';
        },
        onComplete: () => {
          // Capture reasoning and attach to first tool message if exists
          const reasoning = streamingReasoning || undefined;
          if (reasoning) {
            // Find first tool-result message and attach reasoning
            const firstToolIdx = chatMessages.findIndex(
              m => m.contentType === 'tool-result' || m.contentType === 'tool-pending'
            );
            if (firstToolIdx !== -1 && !chatMessages[firstToolIdx].reasoning) {
              const updatedMessages = [...chatMessages];
              updatedMessages[firstToolIdx] = {
                ...updatedMessages[firstToolIdx],
                reasoning
              };
              chatMessages = updatedMessages;
            }
          }

          // Add text response (LLM summary) if present
          const content = streamingContent;
          if (content && content.trim().length > 0) {
            const hasToolResults = chatMessages.some(
              m => m.contentType === 'tool-result' || m.contentType === 'tool-pending'
            );
            chatMessages = [...chatMessages, {
              id: (Date.now() + 1).toString(),
              role: 'assistant' as const,
              content: content,
              contentType: 'text',
              // Attach reasoning to text only if no tool results
              reasoning: !hasToolResults ? reasoning : undefined,
              timestamp: new Date()
            }];
          }

          // Reset state
          isLoading = false;
          streamingContent = '';
          streamingReasoning = '';
          pendingTools = new Map();
        }
      };

      await llmAgent.chat(llmMessages, callbacks);
    } catch (error) {
      const errorMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant' as const,
        content: `Encountered an error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        timestamp: new Date()
      };
      chatMessages = [...chatMessages, errorMessage];
      isLoading = false;
      streamingContent = '';
    }
  }
</script>

<!-- Page header -->
<PageHeader
  title="CodAIze Agent"
  subtitle="Understand your code better through AI-powered graph analysis."
/>

<!-- Chat interface breaks out and takes remaining height -->
<div class="-mx-6 -mb-6 flex flex-col" style="height: calc(100vh - 180px);">
{#if hasError}
  <div class="flex flex-1 flex-col items-center justify-center">
    <svg
      class="mx-auto mb-4 h-12 w-12 text-gray-400"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"
      />
    </svg>
    <h3 class="mb-2 text-lg font-medium text-gray-900">Service unavailable</h3>
    <p class="max-w-md text-center text-gray-500">
      Could not connect to the analysis service. Please ensure the backend is running.
    </p>
  </div>
{:else if showWelcome}
  <div class="flex-1 overflow-hidden">
    <WelcomeScreen onWelcomeMessage={handleWelcomeMessage} />
  </div>
{:else}
  <ChatInterface
    messages={chatMessages}
    {currentMessage}
    {isLoading}
    {streamingContent}
    isThinking={streamingReasoning.length > 0}
    {analysisResult}
    onSendMessage={sendMessage}
    onReset={resetChat}
    onMessageChange={(message) => (currentMessage = message)}
  />
{/if}
</div>
