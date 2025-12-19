<script lang="ts">
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import MessageInput from './MessageInput.svelte';
  import ToolResultWidget from './widgets/ToolResultWidget.svelte';
  import CodePreview from './CodePreview.svelte';
  import ApiService from '$lib/services/apiService';
  import type { NodeJSON, AnalysisResultJSON, TranslationUnitJSON, ChatMessage } from '$lib/types';

  const apiService = new ApiService();


  // State for code preview split-view
  let selectedNode = $state<NodeJSON | null>(null);
  let showCodePreview = $derived(selectedNode !== null);

  function handleNodeClick(node: NodeJSON) {
    selectedNode = node;
  }

  function closeCodePreview() {
    selectedNode = null;
  }

  // Helper to find the TranslationUnit for a node
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
    analysisResult?: AnalysisResultJSON | null;
    onSendMessage: () => void;
    onReset: () => void;
    onMessageChange: (message: string) => void;
  }

  let {
    messages,
    currentMessage,
    isLoading,
    streamingContent,
    analysisResult,
    onSendMessage,
    onReset,
    onMessageChange
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
    class:chat-full={!showCodePreview}
    class:chat-split={showCodePreview}
  >
    <!-- New Chat button bar -->
    <div class="flex-shrink-0 px-6 py-3">
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
            {#if message.contentType === 'tool-result' && message.toolResult}
              <!-- Tool Result Widget -->
              <ToolResultWidget data={message.toolResult} onItemClick={handleNodeClick} />
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
          {#if displayContent}
            <div class="prose prose-sm max-w-4xl text-gray-800">
              <MarkdownRenderer content={displayContent} />
            </div>
          {:else}
            <!-- Typing indicator -->
            <div class="flex items-center space-x-2">
              <div class="flex space-x-1">
                <div class="h-2 w-2 animate-pulse rounded-full bg-gray-400"></div>
                <div
                  class="h-2 w-2 animate-pulse rounded-full bg-gray-400"
                  style="animation-delay: 0.2s"
                ></div>
                <div
                  class="h-2 w-2 animate-pulse rounded-full bg-gray-400"
                  style="animation-delay: 0.4s"
                ></div>
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
      />
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
