<script lang="ts">
  import type { PageProps } from './$types';
  import type {
    ChatMessage,
    LLMMessage,
    ConceptSuggestionItem,
    LLMConcept,
    Model,
    ToolActivity
  } from '$lib/types';
  import {
    ChatInterface,
    McpCapabilitiesModal,
    SkillsModal,
    NotConfigured
  } from '$lib/components/ai-agent';
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
    return agentSession.mcpCapabilities?.tools.some((t) => t.name === toolName) ?? false;
  }

  function isConceptSuggestion(toolName: string | undefined, content: any): content is LLMConcept {
    return (
      toolName === SUGGEST_LLM_CONCEPTS_TOOL &&
      content != null &&
      typeof content === 'object' &&
      'operations' in content &&
      'properties' in content
    );
  }

  function addSuggestion(concept: LLMConcept) {
    const item: ConceptSuggestionItem = {
      suggestion: concept,
      status: 'pending',
      operations: concept.operations.map((op) => ({ operation: op, status: 'pending' }))
    };
    suggestions = [...suggestions, item];
  }

  async function handleApplySuggestions(accepted: ConceptSuggestionItem[]) {
    if (!hasMcpToolAvailable(ADD_LLM_CONCEPTS_TOOL)) {
      chatMessages = [
        ...chatMessages,
        {
          id: Date.now().toString(),
          role: 'assistant' as const,
          content: `Tool "${ADD_LLM_CONCEPTS_TOOL}" is not available. Make sure the MCP server provides this tool.`,
          timestamp: new Date()
        }
      ];
      return;
    }
    const concepts = accepted.map((item) => item.suggestion);
    try {
      const res = await fetch(`/api/chat/mcp/tools/${ADD_LLM_CONCEPTS_TOOL}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ concepts })
      });
      const result = await res.json();
      chatMessages = [
        ...chatMessages,
        {
          id: Date.now().toString(),
          role: 'assistant' as const,
          content: '',
          contentType: 'tool-result',
          toolResult: { toolName: ADD_LLM_CONCEPTS_TOOL, content: result },
          timestamp: new Date()
        }
      ];
      suggestions = [];
    } catch (error) {
      chatMessages = [
        ...chatMessages,
        {
          id: Date.now().toString(),
          role: 'assistant' as const,
          content: `Failed to apply concepts (called tool: "${ADD_LLM_CONCEPTS_TOOL}"): ${error instanceof Error ? error.message : 'Unknown error'}`,
          timestamp: new Date()
        }
      ];
    }
  }

  // Load persisted state from sessionStorage
  function loadPersistedState() {
    if (typeof window === 'undefined') {
      return { messages: [], suggestions: [], selectedClient: null, selectedModel: null };
    }
    const stored = sessionStorage.getItem('codyze-agent-state');
    if (!stored)
      return { messages: [], suggestions: [], selectedClient: null, selectedModel: null };
    try {
      const parsed = JSON.parse(stored);
      const messages = parsed.messages.map((msg: any) => ({
        ...msg,
        timestamp: new Date(msg.timestamp)
      }));
      return {
        messages,
        suggestions: parsed.suggestions ?? [],
        selectedClient: parsed.selectedClient ?? null,
        selectedModel: parsed.selectedModel ?? null
      };
    } catch {
      return { messages: [], suggestions: [], selectedClient: null, selectedModel: null };
    }
  }

  const persisted = loadPersistedState();

  let chatMessages = $state<ChatMessage[]>(persisted.messages);
  // The concept suggestions of the agent that have not been applied yet. They are shown in the
  // timeline at the last tool result that suggested a concept
  let suggestions = $state<ConceptSuggestionItem[]>(persisted.suggestions);
  let currentMessage = $state('');
  let isLoading = $state(false);
  let streamingContent = $state('');
  let streamingReasoning = $state('');
  // The tool call the agent is running, until its result arrives
  let activity = $state<ToolActivity | null>(null);
  let selectedClient = $state<string | null>(persisted.selectedClient);
  let selectedModelName = $state<string | null>(persisted.selectedModel);
  let abortController: AbortController | null = null;

  const selectedModel = $derived.by((): Model | null => {
    return (
      models.find(
        (model) => model.client === selectedClient && model.model === selectedModelName
      ) ?? null
    );
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
      sessionStorage.setItem(
        'codyze-agent-state',
        JSON.stringify({
          messages: chatMessages,
          suggestions,
          selectedClient,
          selectedModel: selectedModelName
        })
      );
    }
  });

  function selectModel(model: Model) {
    selectedClient = model.client;
    selectedModelName = model.model;
  }

  function resetChat() {
    if (abortController) {
      abortController.abort();
      abortController = null;
    }
    chatMessages = [];
    currentMessage = '';
    streamingContent = '';
    streamingReasoning = '';
    suggestions = [];
    activity = null;
    isLoading = false;
  }

  // Starts a request to the agent, which a new chat stops
  function startRequest(): AbortSignal {
    abortController?.abort();
    abortController = new AbortController();
    return abortController.signal;
  }

  // The events of a request that was stopped are ignored, e.g. the ones that already arrived
  function makeStreamingCallbacks(signal: AbortSignal): StreamingCallbacks {
    return {
      onChunk: (chunk: string) => {
        if (signal.aborted) return;
        try {
          const event = JSON.parse(chunk);
          if (event.type === 'text') {
            streamingContent += event.content;
          } else if (event.type === 'reasoning') {
            streamingReasoning += event.content;
          } else if (event.type === 'tool_call') {
            keepNarration();
            activity = { toolName: event.toolName, arguments: event.arguments ?? {} };
          } else if (event.type === 'tool_result') {
            if (isConceptSuggestion(event.toolName, event.content)) {
              addSuggestion(event.content);
            }
            // Without the event of the call, e.g. from an older server, the text before the call is
            // kept when its result arrives
            keepNarration();
            const args = activity?.toolName === event.toolName ? activity?.arguments : undefined;
            activity = null;
            chatMessages = [
              ...chatMessages,
              {
                id: Date.now().toString(),
                role: 'assistant' as const,
                content: '',
                contentType: 'tool-result',
                toolResult: { toolName: event.toolName, content: event.content, arguments: args },
                timestamp: new Date()
              }
            ];
          }
        } catch {
          streamingContent += chunk;
        }
      },
      onError: (error: string) => {
        if (signal.aborted) return;
        chatMessages = [
          ...chatMessages,
          {
            id: Date.now().toString(),
            role: 'assistant' as const,
            content: `Encountered an error: ${error}`,
            timestamp: new Date()
          }
        ];
        activity = null;
        isLoading = false;
        streamingContent = '';
      },
      onComplete: () => {
        if (signal.aborted) return;
        finishAnswer();
      }
    };
  }

  // What the model wrote before a tool call belongs to its steps, not to its answer
  function keepNarration() {
    const narration = streamingContent.trim();
    streamingContent = '';
    if (!narration) return;
    chatMessages = [
      ...chatMessages,
      {
        id: `${Date.now()}-narration`,
        role: 'assistant' as const,
        content: narration,
        contentType: 'narration' as const,
        timestamp: new Date()
      }
    ];
  }

  // Keeps what the agent wrote for the current question as its answer, with the reasoning of the
  // model at its first tool call or else at the answer
  function finishAnswer() {
    activity = null;
    const reasoning = streamingReasoning || undefined;
    const start = chatMessages.findLastIndex((m) => m.role === 'user') + 1;
    const firstToolIdx = chatMessages.findIndex(
      (m, i) => i >= start && m.contentType === 'tool-result'
    );
    if (reasoning && firstToolIdx !== -1 && !chatMessages[firstToolIdx].reasoning) {
      const updated = [...chatMessages];
      updated[firstToolIdx] = { ...updated[firstToolIdx], reasoning };
      chatMessages = updated;
    }
    const content = streamingContent;
    if (content && content.trim().length > 0) {
      chatMessages = [
        ...chatMessages,
        {
          id: (Date.now() + 1).toString(),
          role: 'assistant' as const,
          content,
          contentType: 'text',
          reasoning: firstToolIdx === -1 ? reasoning : undefined,
          timestamp: new Date()
        }
      ];
    }
    isLoading = false;
    streamingContent = '';
    streamingReasoning = '';
  }

  // Stops the running question, keeping its steps and what the agent wrote so far
  function stopAnswer() {
    if (!isLoading) return;
    abortController?.abort();
    abortController = null;
    finishAnswer();
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

      chatMessages = [
        ...chatMessages,
        {
          id: Date.now().toString(),
          role: 'user' as const,
          content: userMsg.content,
          timestamp: new Date()
        }
      ];
      isLoading = true;
      streamingContent = '';
      streamingReasoning = '';

      const llmMessages: LLMMessage[] = resolvedMessages.map((m) => ({
        role: m.role as 'user' | 'assistant',
        content: m.content
      }));

      const signal = startRequest();
      await llmAgent.chat(
        llmMessages,
        selectedModel.client,
        selectedModel.model,
        makeStreamingCallbacks(signal),
        signal
      );
    } catch (error) {
      chatMessages = [
        ...chatMessages,
        {
          id: Date.now().toString(),
          role: 'assistant' as const,
          content: `Error resolving prompt: ${error instanceof Error ? error.message : 'Unknown error'}`,
          timestamp: new Date()
        }
      ];
      isLoading = false;
    }
  }

  async function sendMessage(context?: string, contextLabel?: string) {
    if (!currentMessage.trim() || !selectedModel) return;

    chatMessages = [
      ...chatMessages,
      {
        id: Date.now().toString(),
        role: 'user' as const,
        content: currentMessage,
        context,
        contextLabel,
        timestamp: new Date()
      }
    ];
    currentMessage = '';
    isLoading = true;
    streamingContent = '';
    streamingReasoning = '';

    try {
      // The earlier questions and answers, without the steps that led to them: the results of the
      // tool calls, sent as text, are taken by the model for its own words, and it writes tool
      // calls as text instead of calling the tools. The answers cite the nodes they rely on
      const llmMessages: LLMMessage[] = chatMessages
        .filter((msg) => msg.contentType !== 'tool-result' && msg.contentType !== 'narration')
        .map((msg) => ({
          role: msg.role as 'user' | 'assistant',
          content: msg.context
            ? `${msg.content}\n\n(This refers to the code selected by the user: ${msg.context})`
            : msg.content
        }));

      const signal = startRequest();
      await llmAgent.chat(
        llmMessages,
        selectedModel.client,
        selectedModel.model,
        makeStreamingCallbacks(signal),
        signal
      );
    } catch (error) {
      chatMessages = [
        ...chatMessages,
        {
          id: (Date.now() + 1).toString(),
          role: 'assistant' as const,
          content: `Encountered an error: ${error instanceof Error ? error.message : 'Unknown error'}`,
          timestamp: new Date()
        }
      ];
      isLoading = false;
      streamingContent = '';
    }
  }
</script>

<!-- The agent page is an editor-like workspace that uses the whole height of the window -->
<div class="-m-6 flex h-screen min-h-0 flex-col">
  {#if agentSession.mcpCapabilities === null}
    <NotConfigured />
  {:else}
    <ChatInterface
      messages={chatMessages}
      {currentMessage}
      {isLoading}
      {streamingContent}
      isThinking={streamingReasoning.length > 0}
      {activity}
      {models}
      {selectedModel}
      {analysisResult}
      bind:suggestions
      onApplySuggestions={handleApplySuggestions}
      onSendMessage={sendMessage}
      onReset={resetChat}
      onStop={stopAnswer}
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
  <SkillsModal skills={agentSession.skills} onClose={agentSession.closeSkillsModal} />
{/if}
