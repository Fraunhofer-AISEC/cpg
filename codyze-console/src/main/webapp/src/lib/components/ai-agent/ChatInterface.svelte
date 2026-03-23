<script lang="ts">
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import MessageInput from './MessageInput.svelte';
  import CodeItemList, { isCodeItemContent } from './widgets/CodeItemList.svelte';
  import DfgFlowWidget from './widgets/DfgFlowWidget.svelte';
  import { CodeViewer, FileTree } from '$lib/components/analysis';
  import type { NodeJSON, AnalysisResultJSON, TranslationUnitJSON, ChatMessage, McpCapabilities, ComponentJSON } from '$lib/types';

  let selectedNode = $state<NodeJSON | null>(null);
  let selectedTranslationUnit = $state<TranslationUnitJSON | null>(null);
  let selectedComponentName = $state<string | null>(null);
  let overlayNodes = $state<NodeJSON[]>([]);
  let astNodes = $state<NodeJSON[]>([]);
  let showCodePanel = $derived(selectedTranslationUnit !== null);
  let fileTreeCollapsed = $state(false);
  let nodesPanelCollapsed = $state(false);

  async function loadNodes(componentName: string, tuId: string) {
    const [overlay, ast] = await Promise.all([
      fetch(`/api/component/${componentName}/translation-unit/${tuId}/overlay-nodes`).then(r => r.json()).catch(() => []),
      fetch(`/api/component/${componentName}/translation-unit/${tuId}/ast-nodes`).then(r => r.json()).catch(() => []),
    ]);
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
    if (comp) loadNodes(comp.name, tu.id);
  }

  function handleFileSelect(unit: TranslationUnitJSON) {
    selectedTranslationUnit = unit;
    selectedNode = null;
    const comp = findComponentForTu(unit.id);
    selectedComponentName = comp?.name ?? null;
    if (comp) loadNodes(comp.name, unit.id);
  }

  function closeCodePanel() {
    selectedNode = null;
    selectedTranslationUnit = null;
    selectedComponentName = null;
    overlayNodes = [];
    astNodes = [];
  }

  function findComponentForTu(tuId: string): ComponentJSON | null {
    if (!analysisResult) return null;
    return analysisResult.components.find((c) =>
      c.translationUnits.some((tu) => tu.id === tuId)
    ) ?? null;
  }

  const selectedComponent = $derived((): ComponentJSON | null => {
    if (!selectedTranslationUnit || !analysisResult) return null;
    return findComponentForTu(selectedTranslationUnit.id);
  });

  interface Props {
    messages: ChatMessage[];
    currentMessage: string;
    isLoading: boolean;
    streamingContent: string;
    isThinking: boolean;
    analysisResult?: AnalysisResultJSON | null;
    mcpCapabilities?: McpCapabilities | null;
    onSendMessage: () => void;
    onReset: () => void;
    onMessageChange: (message: string) => void;
    onPromptSelect?: (name: string, args: Record<string, string>) => void;
    onOpenMcpModal?: () => void;
  }

  let {
    messages,
    currentMessage,
    isLoading,
    streamingContent,
    isThinking,
    analysisResult,
    mcpCapabilities,
    onSendMessage,
    onReset,
    onMessageChange,
    onPromptSelect,
    onOpenMcpModal
  }: Props = $props();

  let displayContent = $derived(streamingContent.trim().length > 0 ? streamingContent : '');

  let messagesContainer: HTMLDivElement;
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

<div class="flex h-full bg-gray-50">
  <!-- Chat Container -->
  <div class="flex flex-col transition-all duration-300 {showCodePanel ? 'w-[45%]' : 'w-full'}">
    <!-- Messages Container -->
    <div class="flex-1 overflow-y-auto" bind:this={messagesContainer} onscroll={handleScroll}>
      <div class="mx-auto max-w-6xl">
        {#each messages as message}
          {#if message.role === 'user'}
            <div class="flex justify-end px-6 py-3">
              <div class="max-w-[65%]">
                <div class="rounded-2xl bg-blue-600 px-5 py-3 text-white">
                  <div class="whitespace-pre-wrap text-[15px] leading-relaxed">{message.content}</div>
                </div>
              </div>
            </div>
          {:else}
            <div class="px-6 py-6">
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
                    <svg class="h-3 w-3 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
                    </svg>
                    <span>Thought process</span>
                  </button>
                  {#if expandedReasoning.has(message.id)}
                    <div class="mt-1.5 ml-4 max-w-xl border-l-2 border-gray-200 pl-3">
                      <p class="whitespace-pre-wrap text-xs italic leading-relaxed text-gray-400">{message.reasoning}</p>
                    </div>
                  {/if}
                </div>
              {/if}
              {#if message.contentType === 'tool-result' && message.toolResult}
                {#if message.toolResult.toolName === 'cpg_dfg_backward'}
                  <DfgFlowWidget content={message.toolResult.content} />
                {:else if isCodeItemContent(message.toolResult.content)}
                  <CodeItemList data={message.toolResult} onItemClick={handleNodeClick} />
                {:else}
                  <div class="my-2 overflow-hidden rounded-lg border border-gray-200 bg-white">
                    <div class="flex items-center gap-2 border-b border-gray-200 bg-gray-50 px-4 py-3">
                      {#if message.toolResult.toolName}
                        <span class="font-mono text-sm font-semibold text-gray-700">{message.toolResult.toolName}</span>
                      {/if}
                      {#if message.toolResult.isError}
                        <span class="rounded bg-red-100 px-2 py-1 text-xs font-semibold uppercase text-red-800">Error</span>
                      {/if}
                    </div>
                    <pre class="m-0 overflow-x-auto whitespace-pre-wrap wrap-break-word p-4 font-mono text-sm text-gray-700">{typeof message.toolResult.content === 'string' ? message.toolResult.content : JSON.stringify(message.toolResult.content, null, 2)}</pre>
                  </div>
                {/if}
              {:else if message.content}
                <div class="prose prose-sm max-w-4xl text-gray-800">
                  <MarkdownRenderer content={message.content} />
                </div>
              {/if}
            </div>
          {/if}
        {/each}

        {#if isLoading || displayContent}
          <div class="px-6 py-6">
            {#if displayContent}
              <div class="prose prose-sm max-w-4xl text-gray-800">
                <MarkdownRenderer content={displayContent} />
              </div>
            {:else}
              <div class="flex items-center gap-2">
                <div class="flex gap-1">
                  <div class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:0ms]"></div>
                  <div class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:150ms]"></div>
                  <div class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:300ms]"></div>
                </div>
              </div>
            {/if}
          </div>
        {/if}
      </div>
    </div>

    <!-- Input Area -->
    <div class="shrink-0 px-4 pb-3 pt-3">
      <div class="mx-auto max-w-6xl">
        <MessageInput
          value={currentMessage}
          onSend={onSendMessage}
          onValueChange={onMessageChange}
          placeholder="Ask me about your codebase..."
          disabled={isLoading}
          prompts={mcpCapabilities?.prompts}
          onPromptSelect={onPromptSelect}
          onNewChat={onReset}
        />
        {#if mcpCapabilities && onOpenMcpModal}
          <div class="mt-1.5 flex items-center gap-1.5">
            <button
              class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-white px-2.5 py-1 text-xs font-medium text-gray-500 transition-colors hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700"
              onclick={onOpenMcpModal}
              title="MCP Server"
            >
              <span class="h-1.5 w-1.5 rounded-full bg-green-500"></span>
              {mcpCapabilities.serverName}
            </button>
          </div>
        {/if}
      </div>
    </div>
  </div>

  <!-- Code Panel: FileTree + CodeViewer -->
  {#if showCodePanel && selectedTranslationUnit}
    <div class="flex flex-1 overflow-hidden rounded-xl shadow-lg mx-2 my-2 border border-gray-200 bg-white" style="animation: slideIn 0.3s ease-out">

      {#if selectedComponent()}
        <FileTree
          component={selectedComponent()!}
          currentUnitId={selectedTranslationUnit.id}
          onFileSelect={handleFileSelect}
          bind:collapsed={fileTreeCollapsed}
          hideHeader={true}
        />
      {/if}

      <CodeViewer
        translationUnit={selectedTranslationUnit}
        astNodes={astNodes}
        overlayNodes={overlayNodes}
        highlightLine={selectedNode?.startLine ?? undefined}
        bind:nodePanelCollapsed={nodesPanelCollapsed}
        onClose={closeCodePanel}
        hideControls={true}
      />

    </div>
  {/if}
</div>

<style>
  @keyframes slideIn {
    from { transform: translateX(100%); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
  }
</style>