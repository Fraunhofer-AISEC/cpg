<script lang="ts">
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import MessageInput from './MessageInput.svelte';
  import ApiService from '$lib/services/apiService';

  const apiService = new ApiService();

  interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    timestamp: Date;
  }

  interface Props {
    messages: ChatMessage[];
    currentMessage: string;
    isLoading: boolean;
    streamingContent: string;
    onSendMessage: () => void;
    onReset: () => void;
    onMessageChange: (message: string) => void;
  }

  let {
    messages,
    currentMessage,
    isLoading,
    streamingContent,
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

<div class="flex h-full flex-col bg-gray-50">
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
          <!-- AI Message - Text with max width for readability -->
          <div class="px-6 py-6">
            <div class="prose prose-sm max-w-4xl text-gray-800">
              <MarkdownRenderer content={message.content} />
            </div>
          </div>
        {/if}
      {/each}

      {#if isLoading || displayContent}
        <!-- Streaming AI Message -->
        <div class="px-6 py-6">
          {#if displayContent}
            <div class="prose prose-sm max-w-4xl text-gray-800">
              <div class="whitespace-pre-wrap text-[15px] leading-relaxed">
                {displayContent}
              </div>
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
