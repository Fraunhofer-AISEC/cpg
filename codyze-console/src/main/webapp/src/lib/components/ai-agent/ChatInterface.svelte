<script lang="ts">
  import { untrack } from 'svelte';
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import MessageInput from './MessageInput.svelte';
  import SessionBar from './SessionBar.svelte';
  import CodeItemList, { isCodeItemContent } from './widgets/CodeItemList.svelte';
  import DfgFlowWidget from './widgets/DfgFlowWidget.svelte';
  import ToolResultBlock from './widgets/ToolResultBlock.svelte';
  import { CodeViewer, FileTree } from '$lib/components/analysis';
  import { LoadingSpinner } from '$lib/components/ui';
  import { agentSession } from '$lib/stores/agentSession.svelte';
  import type { NodeJSON, AnalysisResultJSON, TranslationUnitJSON, ChatMessage, ComponentJSON, ConceptSuggestionItem, Model, NodeDetailsJSON, NodeRefJSON } from '$lib/types';

  let selectedNode = $state<NodeJSON | null>(null);
  let selectedTranslationUnit = $state<TranslationUnitJSON | null>(null);
  let selectedComponentName = $state<string | null>(null);
  // The selected unit including its code, which is not part of the units in analysisResult
  let openedUnit = $state.raw<TranslationUnitJSON | null>(null);
  // These can contain tens of thousands of nodes and are only ever replaced as a whole, so they do
  // not need to be deeply reactive
  let overlayNodes = $state.raw<NodeJSON[]>([]);
  let astNodes = $state.raw<NodeJSON[]>([]);
  let fileTreeCollapsed = $state(false);
  let nodesPanelCollapsed = $state(false);

  async function loadUnit(componentName: string, tuId: string) {
    const unit: TranslationUnitJSON | null = await fetch(
      `/api/component/${componentName}/translation-unit/${tuId}`
    ).then(r => (r.ok ? r.json() : null)).catch(() => null);
    // Ignore the response if another unit was selected in the meantime
    if (selectedTranslationUnit?.id === tuId) openedUnit = unit;
  }

  async function loadNodes(componentName: string, tuId: string) {
    const [overlay, ast] = await Promise.all([
      fetch(`/api/component/${componentName}/translation-unit/${tuId}/overlay-nodes`).then(r => r.json()).catch(() => []),
      fetch(`/api/component/${componentName}/translation-unit/${tuId}/ast-nodes`).then(r => r.json()).catch(() => []),
    ]);
    if (selectedTranslationUnit?.id !== tuId) return;
    overlayNodes = overlay;
    astNodes = ast;
  }

  function findTranslationUnit(node: NodeJSON): TranslationUnitJSON | null {
    if (!analysisResult) return null;
    if (node.translationUnitId) {
      for (const component of analysisResult.components) {
        const tu = component.translationUnits.find((tu) => tu.id === node.translationUnitId);
        if (tu) return tu;
      }
    }
    if (node.fileName) {
      for (const component of analysisResult.components) {
        const tu = component.translationUnits.find((tu) => tu.name.includes(node.fileName!));
        if (tu) return tu;
      }
    }
    if ((node as any).componentName) {
      const component = analysisResult.components.find((c) => c.name === (node as any).componentName);
      if (component?.translationUnits.length) return component.translationUnits[0];
    }
    return null;
  }

  function handleNodeClick(node: NodeJSON) {
    const tu = findTranslationUnit(node);
    if (!tu) return;
    selectedNode = node;
    selectedTranslationUnit = tu;
    const comp = findComponentForTu(tu.id);
    selectedComponentName = comp?.name ?? null;
    if (comp) {
      loadUnit(comp.name, tu.id);
      loadNodes(comp.name, tu.id);
    }
  }

  // Opens the file of a node selected in the inspector and inspects the node there
  function handleNavigateToNode(ref: NodeRefJSON) {
    handleNodeClick({
      ...ref,
      code: ref.code,
      astChildren: [],
      prevDFG: [],
      nextDFG: []
    });
  }

  // Questions about the inspected node, which are put into the input so they can be adjusted
  const nodeQuestions: { label: string; question: (where: string) => string }[] = [
    { label: 'Explain', question: (where) => `Explain what ${where} does and why it matters for security.` },
    { label: 'Where does the value come from?', question: (where) => `Where does the value of ${where} come from? Follow the dataflow backwards to its origins (e.g. user input, files, network, constants).` },
    { label: 'Reachable from outside?', question: (where) => `Can ${where} be reached from an entry point of the program? Show the call path.` }
  ];

  function askAboutNode(details: NodeDetailsJSON, question: (where: string) => string) {
    const n = details.node;
    const where = `the ${n.type} \`${n.code || n.name}\` (node ID ${n.id}, ${n.fileName}:${n.startLine})`;
    onMessageChange(question(where));
  }

  function handleFileSelect(unit: TranslationUnitJSON) {
    selectedTranslationUnit = unit;
    selectedNode = null;
    const comp = findComponentForTu(unit.id);
    selectedComponentName = comp?.name ?? null;
    if (comp) {
      loadUnit(comp.name, unit.id);
      loadNodes(comp.name, unit.id);
    } else {
      overlayNodes = [];
      astNodes = [];
    }
  }

  function handleComponentSelect(name: string) {
    const unit = analysisResult?.components.find((c) => c.name === name)?.translationUnits[0];
    if (unit) handleFileSelect(unit);
  }

  function findComponentForTu(tuId: string): ComponentJSON | null {
    if (!analysisResult) return null;
    return analysisResult.components.find((c) =>
      c.translationUnits.some((tu) => tu.id === tuId)
    ) ?? null;
  }

  const selectedComponent: ComponentJSON | null = $derived.by(() => {
    if (!selectedTranslationUnit || !analysisResult) return null;
    return findComponentForTu(selectedTranslationUnit.id);
  });

  interface Props {
    messages: ChatMessage[];
    currentMessage: string;
    isLoading: boolean;
    streamingContent: string;
    isThinking: boolean;
    models?: Model[];
    selectedModel?: Model | null;
    analysisResult?: AnalysisResultJSON | null;
    suggestions?: ConceptSuggestionItem[];
    onApplySuggestions?: (accepted: ConceptSuggestionItem[]) => Promise<void> | void;
    onSendMessage: () => void;
    onReset: () => void;
    onMessageChange: (message: string) => void;
    onModelSelect?: (model: Model) => void;
    onPromptSelect?: (name: string, args: Record<string, string>) => void;
  }

  let {
    messages,
    currentMessage,
    isLoading,
    streamingContent,
    isThinking,
    models = [],
    selectedModel = null,
    analysisResult,
    suggestions = $bindable([]),
    onApplySuggestions,
    onSendMessage,
    onReset,
    onMessageChange,
    onModelSelect,
    onPromptSelect,
  }: Props = $props();

  async function handleApplyAndReload(accepted: ConceptSuggestionItem[]) {
    await onApplySuggestions?.(accepted);
    if (selectedComponentName && selectedTranslationUnit) {
      await loadNodes(selectedComponentName, selectedTranslationUnit.id);
    }
  }

  let chatCollapsed = $state(false);

  // The code is the main view, so a file is open from the start
  $effect(() => {
    if (selectedTranslationUnit || !analysisResult) return;
    const first = analysisResult.components
      .flatMap((c) => c.translationUnits)
      .sort((a, b) => a.path.localeCompare(b.path))[0];
    if (first) handleFileSelect(first);
  });

  // Questions to start with, as long as the chat is empty
  const starterQuestions = [
    'What does this project do, and where are its entry points?',
    'Which functions handle external input (network, files, arguments)?',
    'Where is cryptography used, and how?',
    'Which memory operations could be dangerous (memcpy, strcpy, …)?'
  ];

  function ask(question: string) {
    onMessageChange(question);
    onSendMessage();
  }
  let displayContent = $derived(streamingContent.trim().length > 0 ? streamingContent : '');
  // The nodes referenced by the suggestions, by ID. They can be nested anywhere in a translation
  // unit, so they are not necessarily part of astNodes
  let suggestionNodes = $state.raw<Map<string, NodeJSON>>(new Map());
  const tusWithSuggestions = $derived(
    new Set([...suggestionNodes.values()].flatMap(n => (n.translationUnitId ? [n.translationUnitId] : [])))
  );

  // The node IDs referenced by the suggestions. As a string, this only changes when the IDs change,
  // and not when a suggestion is accepted or rejected (which replaces the suggestion objects)
  const suggestionNodeIdsKey = $derived(
    [...new Set(
      suggestions.flatMap(s => [
        s.suggestion.nodeId,
        ...s.operations.map(o => o.operation.nodeId)
      ])
    )].sort().join(',')
  );

  // When suggestions arrive, load the referenced nodes to know their files and lines
  $effect(() => {
    const key = suggestionNodeIdsKey;
    if (!key || !analysisResult) {
      suggestionNodes = new Map();
      return;
    }
    loadSuggestionNodes(key);
  });

  async function loadSuggestionNodes(key: string) {
    const nodes: NodeJSON[] = await fetch('/api/nodes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(key.split(','))
    }).then(r => (r.ok ? r.json() : [])).catch(() => []);

    // Ignore the response if the suggestions changed in the meantime
    if (key !== suggestionNodeIdsKey) return;
    suggestionNodes = new Map(nodes.map(n => [n.id, n]));
  }

  // Show the first translation unit with suggestions when they arrive, unless the open one has some
  $effect(() => {
    if (tusWithSuggestions.size === 0 || !analysisResult) return;
    // Only react to new suggestions, not to the user opening another file
    const current = untrack(() => selectedTranslationUnit);
    if (current && tusWithSuggestions.has(current.id)) return;
    {
      for (const comp of analysisResult.components) {
        const tu = comp.translationUnits.find(tu => tusWithSuggestions.has(tu.id));
        if (tu) {
          handleFileSelect(tu);
          return;
        }
      }
    }
  });

  // The messages are only rendered while the agent panel is open
  let messagesContainer = $state<HTMLDivElement>();
  let shouldAutoScroll = $state(true);

  function isNearBottom(): boolean {
    if (!messagesContainer) return true;
    const threshold = 150;
    const position = messagesContainer.scrollTop + messagesContainer.clientHeight;
    return messagesContainer.scrollHeight - position < threshold;
  }

  function handleScroll() {
    shouldAutoScroll = isNearBottom();
  }

  function scrollToBottom() {
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  $effect(() => {
    messages.length;
    streamingContent;
    if (shouldAutoScroll) {
      requestAnimationFrame(() => scrollToBottom());
    }
  });

  let expandedReasoning = $state<Set<string>>(new Set());

  function toggleReasoning(id: string) {
    const next = new Set(expandedReasoning);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    expandedReasoning = next;
  }
</script>

<div class="flex h-full min-h-0 gap-2 bg-gray-50 p-2">
  <!-- Files -->
  {#if selectedComponent}
    <div class="flex min-h-0 shrink-0 overflow-hidden rounded-xl border border-gray-200 bg-white">
      <FileTree
        component={selectedComponent}
        allComponents={analysisResult?.components}
        currentUnitId={selectedTranslationUnit?.id}
        onFileSelect={handleFileSelect}
        onComponentSelect={handleComponentSelect}
        bind:collapsed={fileTreeCollapsed}
        conceptSuggestions={tusWithSuggestions}
      />
    </div>
  {/if}

  <!-- Code with inspector: the main view -->
  <div class="flex min-h-0 min-w-0 flex-1 overflow-hidden rounded-xl border border-gray-200 bg-white">
    {#if !analysisResult}
      <div class="flex flex-1 flex-col items-center justify-center gap-2 text-sm text-gray-500">
        <p>No project has been analysed yet.</p>
        <a href="/new-analysis" class="text-blue-600 hover:underline">Start a new analysis</a>
      </div>
    {:else if selectedTranslationUnit && openedUnit?.id === selectedTranslationUnit.id}
      <CodeViewer
        translationUnit={openedUnit}
        astNodes={astNodes}
        overlayNodes={overlayNodes}
        highlightLine={selectedNode?.startLine ?? undefined}
        bind:nodePanelCollapsed={nodesPanelCollapsed}
        bind:suggestions
        {suggestionNodes}
        onApplySuggestions={handleApplyAndReload}
        componentName={selectedComponentName ?? undefined}
        selectedNodeId={selectedNode?.id}
        onNavigateToNode={handleNavigateToNode}
        panelPosition="bottom"
      >
        {#snippet nodeActions(details)}
          {#each nodeQuestions as q (q.label)}
            <button
              type="button"
              class="rounded border border-purple-200 bg-purple-50 px-2 py-0.5 text-[11px] text-purple-700 hover:bg-purple-100 disabled:opacity-50"
              disabled={isLoading || !selectedModel}
              title="Put this question into the agent's input"
              onclick={() => {
                chatCollapsed = false;
                askAboutNode(details, q.question);
              }}
            >
              {q.label}
            </button>
          {/each}
        {/snippet}
      </CodeViewer>
    {:else if selectedTranslationUnit}
      <div class="flex flex-1 items-center justify-center">
        <LoadingSpinner message="Loading {selectedTranslationUnit.name}..." />
      </div>
    {/if}
  </div>

  <!-- Agent -->
  {#if chatCollapsed}
    <button
      type="button"
      onclick={() => (chatCollapsed = false)}
      class="group flex w-8 shrink-0 flex-col items-center gap-2 rounded-xl border border-gray-200 bg-white pt-4 text-gray-400 hover:bg-purple-50 hover:text-purple-600"
      aria-label="Show agent"
    >
      <span class="text-[10px] font-semibold tracking-widest uppercase" style="writing-mode: vertical-rl;">Agent</span>
      {#if isLoading}
        <span class="h-2 w-2 animate-pulse rounded-full bg-purple-500" title="The agent is working"></span>
      {/if}
    </button>
  {:else}
    <div class="flex min-h-0 w-[26rem] shrink-0 flex-col overflow-hidden rounded-xl border border-gray-200 bg-white">
      <div class="flex shrink-0 items-center justify-between border-b border-gray-200 px-3 py-2">
        <span class="text-[11px] font-semibold tracking-widest text-gray-500 uppercase">Agent</span>
        <button
          type="button"
          class="rounded px-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
          onclick={() => (chatCollapsed = true)}
          aria-label="Hide agent"
        >
          »
        </button>
      </div>

      <!-- Messages -->
      <div class="min-h-0 flex-1 overflow-y-auto" style="transform: translateZ(0);" bind:this={messagesContainer} onscroll={handleScroll}>
        {#if messages.length === 0 && !isLoading}
          <div class="p-4">
            <p class="text-sm text-gray-600">
              Ask about the code, or click into it and use the questions in the inspector. The agent
              uses the code property graph to answer.
            </p>
            <div class="mt-3 space-y-2">
              {#each starterQuestions as question (question)}
                <button
                  type="button"
                  class="w-full rounded-lg border border-gray-200 px-3 py-2 text-left text-xs text-gray-700 hover:border-purple-300 hover:bg-purple-50 disabled:opacity-50"
                  disabled={!selectedModel}
                  onclick={() => ask(question)}
                >
                  {question}
                </button>
              {/each}
            </div>
          </div>
        {/if}

        {#each messages as message}
          {#if message.role === 'user'}
            <div class="flex justify-end px-3 py-2">
              <div class="max-w-[85%] rounded-2xl bg-blue-600 px-3 py-2 text-white">
                <div class="whitespace-pre-wrap text-sm leading-relaxed">{message.content}</div>
              </div>
            </div>
          {:else}
            <div class="{message.contentType === 'tool-result' ? 'px-3 py-1' : 'px-3 py-3'}">
              {#if message.reasoning}
                <div class="mb-2 inline-block">
                  <button
                    class="flex items-center gap-1.5 text-xs text-gray-400 transition-colors hover:text-gray-600"
                    onclick={() => toggleReasoning(message.id)}
                  >
                    <svg
                      class="h-3 w-3 transition-transform duration-200 {expandedReasoning.has(message.id) ? 'rotate-90' : ''}"
                      fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                    </svg>
                    <span>Thought process</span>
                  </button>
                  {#if expandedReasoning.has(message.id)}
                    <div class="mt-1.5 ml-4 border-l-2 border-gray-200 pl-3">
                      <p class="whitespace-pre-wrap text-xs italic leading-relaxed text-gray-400">{message.reasoning}</p>
                    </div>
                  {/if}
                </div>
              {/if}
              {#if message.contentType === 'tool-result' && message.toolResult}
                <ToolResultBlock
                  toolResult={message.toolResult}
                  onItemClick={handleNodeClick}
                />
              {:else if message.content}
                <div class="prose prose-sm max-w-none text-gray-800">
                  <MarkdownRenderer content={message.content} />
                </div>
              {/if}
            </div>
          {/if}
        {/each}

        {#if isLoading || displayContent}
          <div class="px-3 py-3">
            {#if displayContent}
              <div class="prose prose-sm max-w-none text-gray-800">
                <MarkdownRenderer content={displayContent} />
              </div>
            {:else}
              <div class="flex gap-1">
                <div class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:0ms]"></div>
                <div class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:150ms]"></div>
                <div class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:300ms]"></div>
              </div>
            {/if}
          </div>
        {/if}
      </div>

      <!-- Input -->
      <div class="shrink-0 border-t border-gray-100 px-3 pt-2 pb-2">
        <MessageInput
          value={currentMessage}
          onSend={onSendMessage}
          onValueChange={onMessageChange}
          placeholder={!selectedModel ? 'No LLM provider configured — check application.conf' : 'Ask about the code...'}
          disabled={isLoading || !selectedModel}
          prompts={agentSession.mcpCapabilities?.prompts}
          onPromptSelect={onPromptSelect}
          onNewChat={onReset}
        />
        <div class="mt-1.5">
          <SessionBar
            {models}
            {selectedModel}
            {onModelSelect}
          />
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  @keyframes slideIn {
    from { transform: translateX(100%); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
  }
</style>
