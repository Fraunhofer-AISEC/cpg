<script lang="ts">
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import MessageInput from './MessageInput.svelte';
  import CodeItemList, { isCodeItemContent } from './widgets/CodeItemList.svelte';
  import DfgFlowWidget from './widgets/DfgFlowWidget.svelte';
  import CodePreview from './CodePreview.svelte';
  import ApiService from '$lib/services/apiService';
  import type { NodeJSON, AnalysisResultJSON, TranslationUnitJSON, ChatMessage, McpCapabilities } from '$lib/types';

  // State for code preview split-view
  let selectedNode = $state<NodeJSON | null>(null);
  let showCodePreview = $derived(selectedNode !== null);
  let showRightPanel = $derived(showCodePreview);


  function handleNodeClick(node: NodeJSON) {
    selectedNode = node;
  }

  function closeCodePreview() {
    selectedNode = null;
  }

  function findTranslationUnit(node: NodeJSON): TranslationUnitJSON | null {
    if (!analysisResult) return null;

    // Try to find by translationUnitId
    if (node.translationUnitId) {
      for (const component of analysisResult.components) {
        const tu = component.translationUnits.find((tu) => tu.id === node.translationUnitId);
        if (tu) return tu;
      }
    }

    // Fallback: Try to find by fileName
    if (node.fileName) {
      for (const component of analysisResult.components) {
        const tu = component.translationUnits.find((tu) => tu.name.includes(node.fileName!));
        if (tu) return tu;
      }
    }

    return null;
  }

  // Helper to create a fallback TranslationUnit if not found
  function getTranslationUnitForNode(node: NodeJSON): TranslationUnitJSON {
    const found = findTranslationUnit(node);
    if (found) return found;

    // Fallback: Create minimal TranslationUnit with node's code
    return {
      id: node.translationUnitId || 'fallback-tu',
      name: node.fileName || 'unknown',
      path: `file:///${node.fileName || 'unknown'}`,
      code: node.code || '// Code not available',
      findings: []
    };
  }

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
    onOpenMcp?: () => void;
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
    onOpenMcp
  }: Props = $props();

  // Only show content when there's actual visible text
  let displayContent = $derived(streamingContent.trim().length > 0 ? streamingContent : '');

  // Auto-scroll to bottom
  let messagesContainer: HTMLDivElement;
  let shouldAutoScroll = $state(true);

  // Check if user is near the bottom of the scroll container
  function isNearBottom(): boolean {
    if (!messagesContainer) return true;
    const threshold = 150; // pixels from bottom
    const position = messagesContainer.scrollTop + messagesContainer.clientHeight;
    const bottom = messagesContainer.scrollHeight;
    return bottom - position < threshold;
  }

  // Handle manual scrolling by user
  function handleScroll() {
    if (messagesContainer) {
      shouldAutoScroll = isNearBottom();
    }
  }

  // Scroll to bottom function
  function scrollToBottom() {
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  // Auto-scroll when messages change
  $effect(() => {
    messages.length;
    if (shouldAutoScroll) {
      requestAnimationFrame(() => scrollToBottom());
    }
  });

  // Auto-scroll when streaming content updates
  $effect(() => {
    streamingContent;
    if (shouldAutoScroll) {
      requestAnimationFrame(() => scrollToBottom());
    }
  });
</script>

<div class="flex h-full bg-gray-50">
  <!-- Chat Container - Dynamic Width with Transition -->
  <div
    class="chat-container"
    class:chat-full={!showRightPanel}
    class:chat-split={showRightPanel}
  >
    <!-- New Chat button bar -->
    <div class="flex flex-shrink-0 items-center gap-2 px-6 py-3">
      <button
        class="flex items-center gap-2 rounded-full bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-md transition-all hover:bg-gray-100 hover:shadow-lg active:scale-95"
        onclick={onReset}
        aria-label="Start new chat"
      >
        <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
        </svg>
        New Chat
      </button>
    </div>

  <!-- Messages Container - This scrolls -->
  <div class="flex-1 overflow-y-auto" bind:this={messagesContainer} onscroll={handleScroll}>
    <div class="mx-auto max-w-6xl">
      {#each messages as message}
        {#if message.role === 'user'}
          <!-- User Message - Right aligned bubble -->
          <div class="flex justify-end px-6 py-3">
            <div class="max-w-[65%]">
              <div class="rounded-2xl bg-blue-600 px-5 py-3 text-white">
                <div class="whitespace-pre-wrap text-[15px] leading-relaxed">{message.content}</div>
              </div>
            </div>
          </div>
        {:else}
          <!-- AI Message -->
          <div class="px-6 py-6">
            <!-- Show saved reasoning if present (collapsed by default, inline style) -->
            {#if message.reasoning}
              {@const messageId = message.id}
              <div class="mb-2 inline-block">
                <button
                  class="flex items-center gap-1.5 text-xs text-gray-400 hover:text-gray-600 transition-colors"
                  onclick={() => {
                    const el = document.getElementById(`reasoning-${messageId}`);
                    if (el) el.classList.toggle('hidden');
                    const chevron = document.getElementById(`chevron-${messageId}`);
                    if (chevron) chevron.classList.toggle('rotate-90');
                  }}
                >
                  <svg
                    id="chevron-{messageId}"
                    class="h-3 w-3 transition-transform duration-200"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                  <svg class="h-3 w-3 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
                  </svg>
                  <span>Thought process</span>
                </button>
                <div id="reasoning-{messageId}" class="hidden mt-1.5 ml-4 pl-3 border-l-2 border-gray-200 max-w-xl">
                  <p class="whitespace-pre-wrap text-xs italic text-gray-400 leading-relaxed">
                    {message.reasoning}
                  </p>
                </div>
              </div>
            {/if}
            {#if message.contentType === 'tool-result' && message.toolResult}
              {#if message.toolResult.toolName === 'cpg_dfg_backward'}
                <DfgFlowWidget content={message.toolResult.content} />
              {:else if isCodeItemContent(message.toolResult.content)}
                <CodeItemList data={message.toolResult} onItemClick={handleNodeClick} />
              {:else}
                <!-- Fallback: plain text/JSON -->
                <div class="my-2 bg-white border border-gray-200 rounded-lg overflow-hidden">
                  <div class="flex items-center gap-2 px-4 py-3 bg-gray-50 border-b border-gray-200">
                    {#if message.toolResult.toolName}
                      <span class="text-sm font-semibold text-gray-700 font-mono">{message.toolResult.toolName}</span>
                    {/if}
                    {#if message.toolResult.isError}
                      <span class="px-2 py-1 bg-red-100 text-red-800 text-xs font-semibold rounded uppercase">Error</span>
                    {/if}
                  </div>
                  <pre class="p-4 m-0 font-mono text-sm text-gray-700 overflow-x-auto whitespace-pre-wrap break-words">{typeof message.toolResult.content === 'string' ? message.toolResult.content : JSON.stringify(message.toolResult.content, null, 2)}</pre>
                </div>
              {/if}
            {:else if message.content}
              <!-- Regular Text or markdown table -->
              <div class="prose prose-sm max-w-4xl text-gray-800">
                <MarkdownRenderer content={message.content} />
              </div>
            {/if}
          </div>
        {/if}
      {/each}

      {#if isLoading || displayContent}
        <!-- Streaming AI Message -->
        <div class="px-6 py-6">
          <!-- Main content -->
          {#if displayContent}
            <div class="prose prose-sm max-w-4xl text-gray-800">
              <MarkdownRenderer content={displayContent} />
            </div>
          {:else}
            <!-- Typing indicator while loading -->
            <div class="flex items-center gap-2">
              <div class="flex gap-1">
                <div class="h-2 w-2 rounded-full bg-gray-400 animate-bounce" style="animation-delay: 0ms;"></div>
                <div class="h-2 w-2 rounded-full bg-gray-400 animate-bounce" style="animation-delay: 150ms;"></div>
                <div class="h-2 w-2 rounded-full bg-gray-400 animate-bounce" style="animation-delay: 300ms;"></div>
              </div>
            </div>
          {/if}
        </div>
      {/if}
    </div>
  </div>

  <!-- Input Area - Fixed at bottom -->
  <div class="flex-shrink-0 px-4 pb-0 pt-3">
    <div class="mx-auto max-w-6xl">
      <MessageInput
        value={currentMessage}
        onSend={onSendMessage}
        onValueChange={onMessageChange}
        placeholder="Ask me about your codebase..."
        disabled={isLoading}
        prompts={mcpCapabilities?.prompts}
        onPromptSelect={onPromptSelect}
      />
      {#if mcpCapabilities && onOpenMcp}
        <div class="mt-2 flex items-center">
          <button
            class="flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            onclick={onOpenMcp}
            title="MCP Server"
          >
            <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 011.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.559.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.894.149c-.424.07-.764.383-.929.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 01-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.398.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 01-.12-1.45l.527-.737c.25-.35.272-.806.108-1.204-.165-.397-.506-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.108-1.204l-.526-.738a1.125 1.125 0 01.12-1.45l.773-.773a1.125 1.125 0 011.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            {mcpCapabilities.serverName}
          </button>
        </div>
      {/if}
    </div>
  </div>
  </div>

  <!-- Code Preview Panel - Slides in from right -->
  {#if showCodePreview && selectedNode}
    <div class="code-preview-panel">
      <CodePreview
        node={selectedNode}
        translationUnit={getTranslationUnitForNode(selectedNode)}
        astNodes={[selectedNode]}
        overlayNodes={[]}
        onClose={closeCodePreview}
      />
    </div>
  {/if}
</div>


<style>
  .chat-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    transition: width 0.3s ease;
  }

  .chat-full {
    width: 100%;
  }

  .chat-split {
    width: 50%;
  }

  .code-preview-panel {
    width: 50%;
    height: 100%;
    animation: slideIn 0.3s ease-out;
  }

  @keyframes slideIn {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }

  /* Responsive: Stack on mobile */
  @media (max-width: 768px) {
    .chat-split {
      width: 0;
      display: none;
    }

    .code-preview-panel {
      width: 100%;
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 50;
      background: white;
    }
  }
</style>
