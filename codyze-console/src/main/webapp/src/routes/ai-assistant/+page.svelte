<script lang="ts">
  import type { PageProps } from './$types';
  import { WelcomeScreen, ChatInterface } from '$lib/components/ai-assistant';
  import { PageHeader } from '$lib/components/navigation';
  import { llmAgent, type LLMMessage, type StreamingCallbacks } from '$lib/services/llmAgent';
  import type { ComponentJSON } from '$lib/types';

  let { data }: PageProps = $props();
  const components = $derived(data?.components || []);
  const hasError = $derived(!data || !data.components);

  let chatMessages = $state<Array<{id: string, role: 'user' | 'assistant', content: string, timestamp: Date}>>([]);
  let currentMessage = $state('');
  let isLoading = $state(false);
  let streamingContent = $state('');
  let selectedComponent = $state<ComponentJSON | null>(null);
  let showWelcome = $state(true);

  function selectComponent(component: ComponentJSON) {
    selectedComponent = component;
    showWelcome = false;
    chatMessages = [{
      id: '1',
      role: 'assistant',
      content: `Hi! I'm analyzing the **${component.name}** component. What would you like to know about it?`,
      timestamp: new Date()
    }];
  }

  function handleWelcomeMessage(message: string) {
    currentMessage = message;
    showWelcome = false;
    // Need to select the first available component when using welcome message
    if (components.length > 0) {
      selectedComponent = components[0];
    } else {
      // TODO: Not sure whether we need the component handling
      selectedComponent = {
        name: "No Components",
        translationUnits: [],
        topLevel: null
      };
    }
    sendMessage();
  }

  function resetChat() {
    showWelcome = true;
    selectedComponent = null;
    chatMessages = [];
    currentMessage = '';
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

    try {
      const llmMessages: LLMMessage[] = chatMessages.map(msg => ({
        role: msg.role as 'user' | 'assistant',
        content: msg.content
      }));

      const callbacks: StreamingCallbacks = {
        onChunk: (chunk: string) => {
          streamingContent += chunk;
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
          // When streaming is complete, add the full message to chat history
          if (streamingContent) {
            const assistantMessage = {
              id: (Date.now() + 1).toString(),
              role: 'assistant' as const,
              content: streamingContent,
              timestamp: new Date()
            };
            chatMessages = [...chatMessages, assistantMessage];
          }
          isLoading = false;
          streamingContent = '';
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

<div class="h-screen flex flex-col">
  <PageHeader title="AI Assistant" subtitle="Understand your code better through AI-powered graph analysis." />

  <div class="flex-1 overflow-hidden">
    {#if hasError}
      <div class="flex flex-col items-center justify-center h-full">
        <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
        <h3 class="text-lg font-medium text-gray-900 mb-2">No components available</h3>
        <p class="text-gray-500 text-center max-w-md">
          Could not load analysis components. Please ensure your project has been analyzed and the backend service is running.
        </p>
      </div>
    {:else}
      {#if showWelcome}
        <WelcomeScreen
          {components}
          onComponentSelect={selectComponent}
          onWelcomeMessage={handleWelcomeMessage}
        />
      {:else if selectedComponent}
        <ChatInterface
          {selectedComponent}
          messages={chatMessages}
          {currentMessage}
          {isLoading}
          {streamingContent}
          onSendMessage={sendMessage}
          onReset={resetChat}
          onMessageChange={(message) => currentMessage = message}
        />
      {/if}
    {/if}
  </div>
</div>
