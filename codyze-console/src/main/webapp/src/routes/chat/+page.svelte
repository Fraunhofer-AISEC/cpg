<script lang="ts">
  import type { PageProps } from './$types';
  import type { ChatMessage, LLMMessage, Model } from '$lib/types';
  import { WelcomeScreen, ChatInterface, McpCapabilitiesModal, NotConfigured } from '$lib/components/ai-agent';
  import { PageHeader } from '$lib/components/navigation';
  import { llmAgent, type StreamingCallbacks } from '$lib/services/llmAgent';

  let { data }: PageProps = $props();
  const analysisResult = $derived(data?.result);
  const mcpCapabilities = $derived(data?.mcpCapabilities ?? null);
  const providers = $derived(data?.providers ?? []);
  const models = $derived.by((): Model[] =>
    providers.flatMap((provider) =>
      provider.models.map((model) => ({
        client: provider.name,
        model
      }))
    )
  );

  let showMcpModal = $state(false);

  // Load persisted state from sessionStorage
  function loadPersistedState() {
    if (typeof window === 'undefined') {
      return { messages: [], showWelcome: true, selectedClient: null, selectedModel: null };
    }
    const stored = sessionStorage.getItem('codyze-agent-state');
    if (!stored) return { messages: [], showWelcome: true, selectedClient: null, selectedModel: null };
    try {
      const parsed = JSON.parse(stored);
      const messages = parsed.messages.map((msg: any) => ({
        ...msg,
        timestamp: new Date(msg.timestamp)
      }));
      return {
        messages,
        showWelcome: parsed.showWelcome,
        selectedClient: parsed.selectedClient ?? null,
        selectedModel: parsed.selectedModel ?? null
      };
    } catch {
      return { messages: [], showWelcome: true, selectedClient: null, selectedModel: null };
    }
  }

  const persisted = loadPersistedState();

  let chatMessages = $state<ChatMessage[]>(persisted.messages);
  let currentMessage = $state('');
  let isLoading = $state(false);
  let streamingContent = $state('');
  let streamingReasoning = $state('');
  let showWelcome = $state(persisted.showWelcome);
  let selectedClient = $state<string | null>(persisted.selectedClient);
  let selectedModelName = $state<string | null>(persisted.selectedModel);
  let abortController: AbortController | null = null;

  const selectedModel = $derived.by((): Model | null => {
    return models.find((model) => model.client === selectedClient && model.model === selectedModelName) ?? null;
  });

  $effect(() => {
    const currentModel = selectedModel;
    if (currentModel) return;

    const firstModel = models[0];
    if (firstModel) {
      selectedClient = firstModel.client;
      selectedModelName = firstModel.model;
    } else {
      selectedClient = null;
      selectedModelName = null;
    }
  });

  $effect(() => {
    if (typeof window !== 'undefined') {
      sessionStorage.setItem('codyze-agent-state', JSON.stringify({
        messages: chatMessages,
        showWelcome,
        selectedClient,
        selectedModel: selectedModelName
      }));
    }
  });

  function selectModel(model: Model) {
    selectedClient = model.client;
    selectedModelName = model.model;
  }

  function handleWelcomeMessage(message: string) {
    currentMessage = message;
    showWelcome = false;
    sendMessage();
  }

  function resetChat() {
    if (abortController) {
      abortController.abort();
      abortController = null;
    }
    showWelcome = true;
    chatMessages = [];
    currentMessage = '';
    streamingContent = '';
    streamingReasoning = '';
    isLoading = false;
  }

  function makeStreamingCallbacks(): StreamingCallbacks {
    return {
      onChunk: (chunk: string) => {
        try {
          const event = JSON.parse(chunk);
          if (event.type === 'text') {
            streamingContent += event.content;
          } else if (event.type === 'reasoning') {
            streamingReasoning += event.content;
          } else if (event.type === 'tool_result') {
            chatMessages = [...chatMessages, {
              id: Date.now().toString(),
              role: 'assistant' as const,
              content: '',
              contentType: 'tool-result',
              toolResult: { toolName: event.toolName, content: event.content },
              timestamp: new Date()
            }];
          }
        } catch {
          streamingContent += chunk;
        }
      },
      onError: (error: string) => {
        chatMessages = [...chatMessages, {
          id: Date.now().toString(),
          role: 'assistant' as const,
          content: `Encountered an error: ${error}`,
          timestamp: new Date()
        }];
        isLoading = false;
        streamingContent = '';
      },
      onComplete: () => {
        const reasoning = streamingReasoning || undefined;
        if (reasoning) {
          const firstToolIdx = chatMessages.findIndex((m) => m.contentType === 'tool-result');
          if (firstToolIdx !== -1 && !chatMessages[firstToolIdx].reasoning) {
            const updated = [...chatMessages];
            updated[firstToolIdx] = { ...updated[firstToolIdx], reasoning };
            chatMessages = updated;
          }
        }
        const content = streamingContent;
        if (content && content.trim().length > 0) {
          const hasToolResults = chatMessages.some((m) => m.contentType === 'tool-result');
          chatMessages = [...chatMessages, {
            id: (Date.now() + 1).toString(),
            role: 'assistant' as const,
            content,
            contentType: 'text',
            reasoning: !hasToolResults ? reasoning : undefined,
            timestamp: new Date()
          }];
        }
        isLoading = false;
        streamingContent = '';
        streamingReasoning = '';
      }
    };
  }

  async function handlePromptSelect(name: string, args: Record<string, string>) {
    if (!selectedModel) return;

    try {
      const res = await fetch(`/api/chat/mcp/prompts/${encodeURIComponent(name)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(args)
      });
      if (!res.ok) return;
      const resolvedMessages: { role: string; content: string }[] = await res.json();

      const userMsg = resolvedMessages.find((m) => m.role === 'user');
      if (!userMsg) return;

      showWelcome = false;
      chatMessages = [...chatMessages, {
        id: Date.now().toString(),
        role: 'user' as const,
        content: userMsg.content,
        timestamp: new Date()
      }];
      isLoading = true;
      streamingContent = '';
      streamingReasoning = '';

      const llmMessages: LLMMessage[] = resolvedMessages.map((m) => ({
        role: m.role as 'user' | 'assistant',
        content: m.content
      }));

      await llmAgent.chat(
        llmMessages,
        selectedModel.client,
        selectedModel.model,
        makeStreamingCallbacks()
      );
    } catch (error) {
      chatMessages = [...chatMessages, {
        id: Date.now().toString(),
        role: 'assistant' as const,
        content: `Error resolving prompt: ${error instanceof Error ? error.message : 'Unknown error'}`,
        timestamp: new Date()
      }];
      isLoading = false;
    }
  }

  async function sendMessage() {
    if (!currentMessage.trim() || !selectedModel) return;

    chatMessages = [...chatMessages, {
      id: Date.now().toString(),
      role: 'user' as const,
      content: currentMessage,
      timestamp: new Date()
    }];
    currentMessage = '';
    isLoading = true;
    streamingContent = '';
    streamingReasoning = '';

    try {
      const llmMessages: LLMMessage[] = chatMessages.map((msg) => ({
        role: msg.role as 'user' | 'assistant',
        content: msg.contentType === 'tool-result' && msg.toolResult
          ? `[Tool: ${msg.toolResult.toolName}]\n${typeof msg.toolResult.content === 'string' ? msg.toolResult.content : JSON.stringify(msg.toolResult.content)}`
          : msg.content
      }));

      await llmAgent.chat(
        llmMessages,
        selectedModel.client,
        selectedModel.model,
        makeStreamingCallbacks()
      );
    } catch (error) {
      chatMessages = [...chatMessages, {
        id: (Date.now() + 1).toString(),
        role: 'assistant' as const,
        content: `Encountered an error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        timestamp: new Date()
      }];
      isLoading = false;
      streamingContent = '';
    }
  }
</script>

<PageHeader
  title="CodAIze Agent"
  subtitle="Understand your code better through AI-powered analysis."
/>

<div class="-mx-6 -mb-6 flex flex-1 flex-col">
  {#if mcpCapabilities === null}
    <NotConfigured />
  {:else if showWelcome}
    <div class="flex-1 overflow-hidden">
      <WelcomeScreen
        onWelcomeMessage={handleWelcomeMessage}
        {models}
        {selectedModel}
        onModelSelect={selectModel}
        {mcpCapabilities}
        onOpenMcpModal={() => (showMcpModal = true)}
        onPromptSelect={handlePromptSelect}
      />
    </div>
  {:else}
      <ChatInterface
        messages={chatMessages}
        {currentMessage}
        {isLoading}
        {streamingContent}
        isThinking={streamingReasoning.length > 0}
        {models}
        {selectedModel}
        {analysisResult}
        {mcpCapabilities}
        onSendMessage={sendMessage}
        onReset={resetChat}
        onMessageChange={(message) => (currentMessage = message)}
        onModelSelect={selectModel}
        onPromptSelect={handlePromptSelect}
        onOpenMcpModal={() => (showMcpModal = true)}
      />
  {/if}
</div>

{#if showMcpModal && mcpCapabilities}
  <McpCapabilitiesModal
    capabilities={mcpCapabilities}
    onClose={() => (showMcpModal = false)}
  />
{/if}
