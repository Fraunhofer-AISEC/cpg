<script lang="ts">
  import type { PageProps } from './$types';
  import type { ChatMessage, LLMMessage, ConceptSuggestionItem, LLMConcept, Model } from '$lib/types';
  import { WelcomeScreen, ChatInterface, McpCapabilitiesModal, SkillsModal, NotConfigured } from '$lib/components/ai-agent';
  import { PageHeader } from '$lib/components/navigation';
  import { llmAgent, type StreamingCallbacks } from '$lib/services/llmAgent';
  import { agentSession } from '$lib/stores/agentSession.svelte';

  const SUGGEST_LLM_CONCEPTS_TOOL = 'cpg_suggest_llm_concepts_and_operations';
  const ADD_LLM_CONCEPTS_TOOL = 'cpg_add_llm_concept_and_operations';

  let { data }: PageProps = $props();
  const analysisResult = $derived(data?.result);

  const providers = $derived(data?.providers ?? []);

  const models = $derived.by((): Model[] =>
    providers.flatMap((provider) =>
      provider.models.map((model) => ({
        client: provider.name,
        model
      }))
    )
  );

  $effect(() => {
    agentSession.init(data?.mcpCapabilities ?? null, data?.skills ?? []);
  });

  function hasMcpToolAvailable(toolName: string): boolean {
    return agentSession.mcpCapabilities?.tools.some(t => t.name === toolName) ?? false;
  }

  let suggestions = $state<ConceptSuggestionItem[]>([]);

  function isConceptSuggestion(toolName: string | undefined, content: any): content is LLMConcept {
    return toolName === SUGGEST_LLM_CONCEPTS_TOOL
      && content != null
      && typeof content === 'object'
      && 'operations' in content
      && 'properties' in content;
  }

  function addSuggestion(concept: LLMConcept) {
    const item: ConceptSuggestionItem = {
      suggestion: concept,
      status: 'pending',
      operations: concept.operations.map(op => ({ operation: op, status: 'pending' })),
    };
    suggestions = [...suggestions, item];
  }

  async function handleApplySuggestions(accepted: ConceptSuggestionItem[]) {
    if (!hasMcpToolAvailable(ADD_LLM_CONCEPTS_TOOL)) {
      chatMessages = [...chatMessages, {
        id: Date.now().toString(),
        role: 'assistant' as const,
        content: `Tool "${ADD_LLM_CONCEPTS_TOOL}" is not available. Make sure the MCP server provides this tool.`,
        timestamp: new Date()
      }];
      return;
    }
    const concepts = accepted.map(item => item.suggestion);
    try {
      const res = await fetch(`/api/chat/mcp/tools/${ADD_LLM_CONCEPTS_TOOL}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ concepts }),
      });
      const result = await res.json();
      chatMessages = [...chatMessages, {
        id: Date.now().toString(),
        role: 'assistant' as const,
        content: '',
        contentType: 'tool-result',
        toolResult: { toolName: ADD_LLM_CONCEPTS_TOOL, content: result },
        timestamp: new Date()
      }];
      suggestions = [];
    } catch (error) {
      chatMessages = [...chatMessages, {
        id: Date.now().toString(),
        role: 'assistant' as const,
        content: `Failed to apply concepts (called tool: "${ADD_LLM_CONCEPTS_TOOL}"): ${error instanceof Error ? error.message : 'Unknown error'}`,
        timestamp: new Date()
      }];
    }
  }

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
    suggestions = [];
    isLoading = false;
    selectedClient = null;
    selectedModelName = null;
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
            if (isConceptSuggestion(event.toolName, event.content)) {
              addSuggestion(event.content);
            } else {
              chatMessages = [...chatMessages, {
                id: Date.now().toString(),
                role: 'assistant' as const,
                content: '',
                contentType: 'tool-result',
                toolResult: { toolName: event.toolName, content: event.content },
                timestamp: new Date()
              }];
            }
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

<div class="-mx-6 -mb-6 flex flex-col" style="height: calc(100vh - 120px);">
  {#if agentSession.mcpCapabilities === null}
    <NotConfigured />
  {:else if showWelcome}
    <div class="flex-1 overflow-hidden">
      <WelcomeScreen
        onWelcomeMessage={handleWelcomeMessage}
        {models}
        {selectedModel}
        onModelSelect={selectModel}
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
      bind:suggestions
      onApplySuggestions={handleApplySuggestions}
      onSendMessage={sendMessage}
      onReset={resetChat}
      onMessageChange={(message) => (currentMessage = message)}
      onModelSelect={selectModel}
      onPromptSelect={handlePromptSelect}
    />
  {/if}
</div>

{#if agentSession.showMcpModal && agentSession.mcpCapabilities}
  <McpCapabilitiesModal
    capabilities={agentSession.mcpCapabilities}
    onClose={agentSession.closeMcpModal}
  />
{/if}

{#if agentSession.showSkillsModal}
  <SkillsModal
    skills={agentSession.skills}
    onClose={agentSession.closeSkillsModal}
  />
{/if}
