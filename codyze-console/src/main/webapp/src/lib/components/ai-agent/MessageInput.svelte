<script lang="ts">
  import type { McpPromptInfo } from '$lib/types';

  interface Props {
    value: string;
    onSend: () => void;
    onValueChange: (value: string) => void;
    placeholder?: string;
    disabled?: boolean;
    /** MCP prompts available for slash-command completion (e.g. typing "/suggest") */
    prompts?: McpPromptInfo[];
    onPromptSelect?: (name: string, args: Record<string, string>) => void;
    onNewChat?: () => void;
  }

  let {
    value,
    onSend,
    onValueChange,
    placeholder = 'Ask me anything about your codebase...',
    disabled = false,
    prompts,
    onPromptSelect,
    onNewChat
  }: Props = $props();

  let textareaElement: HTMLTextAreaElement;
  /** Index of the currently highlighted prompt in the slash-command picker */
  let pickerIndex = $state(0);

  /**
   * Returns the text after the leading slash when the input matches "/word" exactly.
   */
  const slashQuery = $derived.by(() => {
    if (!prompts || prompts.length === 0) return null;
    const match = value.match(/^\/(\S*)$/);
    return match ? match[1] : null;
  });

  /** Prompts whose names contain the current slash query (case-insensitive) */
  const filteredPrompts = $derived.by(() => {
    if (slashQuery === null) return [];
    const lower = slashQuery.toLowerCase();
    return prompts!.filter((p) => p.name.toLowerCase().includes(lower));
  });

  const showPicker = $derived(filteredPrompts.length > 0);

  function resetTextareaHeight() {
    if (textareaElement) textareaElement.style.height = 'auto';
  }

  // Handles keyboard navigation for the slash-command prompt picker,
  // and sends the message on Enter.
  function handleKeyDown(e: KeyboardEvent) {
    if (showPicker) {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        pickerIndex = (pickerIndex + 1) % filteredPrompts.length;
        return;
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        pickerIndex = (pickerIndex - 1 + filteredPrompts.length) % filteredPrompts.length;
        return;
      }
      if (e.key === 'Enter') {
        e.preventDefault();
        selectPrompt(filteredPrompts[pickerIndex]);
        return;
      }
      if (e.key === 'Escape') {
        e.preventDefault();
        onValueChange('');
        return;
      }
    }

    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  function handleInput(e: Event) {
    const target = e.target as HTMLTextAreaElement;
    onValueChange(target.value);
    // Auto-grow up to 120px, then scroll
    target.style.height = 'auto';
    target.style.height = Math.min(target.scrollHeight, 120) + 'px';
    pickerIndex = 0; // reset picker selection on every keystroke
  }

  function selectPrompt(prompt: McpPromptInfo) {
    onValueChange('');
    resetTextareaHeight();
    onPromptSelect?.(prompt.name, {});
  }

  function handleSend() {
    if (value.trim() && !disabled) {
      onSend();
      resetTextareaHeight();
    }
  }

  // When the parent clears the value (e.g. after sending), reset textarea height too
  $effect(() => {
    if (value === '') resetTextareaHeight();
  });
</script>

<div class="relative">
  <!-- Slash-command picker: shown when user types "/..." to filter MCP prompts -->
  {#if showPicker}
    <div
      class="absolute bottom-full left-0 right-0 mb-2 overflow-hidden rounded-xl border border-gray-200 bg-white shadow-xl"
    >
      <div class="px-3 py-2 border-b border-gray-100">
        <p class="text-xs font-medium text-gray-500">Available prompts</p>
      </div>
      <ul>
        {#each filteredPrompts as prompt, i}
          <li>
            <button
              type="button"
              class="flex w-full flex-col px-3 py-2.5 text-left transition-colors {i === pickerIndex ? 'bg-blue-50' : 'hover:bg-gray-50'}"
              onmouseenter={() => (pickerIndex = i)}
              onclick={() => selectPrompt(prompt)}
            >
              <span class="font-mono text-sm font-semibold text-gray-800">/{prompt.name}</span>
              {#if prompt.description}
                <span class="mt-0.5 text-xs text-gray-500">{prompt.description}</span>
              {/if}
            </button>
          </li>
        {/each}
      </ul>
    </div>
  {/if}

  <div
    class="flex items-center gap-3 rounded-3xl border-2 border-gray-200 bg-white py-2 pl-2 pr-2 shadow-lg transition-all duration-200 focus-within:border-blue-400 focus-within:shadow-xl focus-within:ring-4 focus-within:ring-blue-100"
  >
    {#if onNewChat}
      <button
        type="button"
        onclick={onNewChat}
        class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-gray-400 transition-all hover:bg-gray-100 hover:text-gray-600 active:scale-95"
        aria-label="Start new chat"
        title="New chat"
      >
        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
        </svg>
      </button>
      <div class="h-5 w-px bg-gray-200"></div>
    {/if}
    <textarea
      bind:this={textareaElement}
      class="max-h-30 min-h-6 flex-1 resize-none border-0 bg-transparent text-base leading-relaxed text-gray-900 placeholder-gray-400 outline-none focus:border-0 focus:outline-none focus:ring-0"
      {placeholder}
      rows="1"
      {value}
      oninput={handleInput}
      onkeydown={handleKeyDown}
    ></textarea>
    <button
      class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-linear-to-r from-blue-600 to-blue-700 text-white shadow-md transition-all duration-200 hover:scale-105 hover:from-blue-700 hover:to-blue-800 hover:shadow-lg active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300 disabled:opacity-50 disabled:hover:scale-100 disabled:hover:shadow-md"
      onclick={handleSend}
      disabled={!value.trim() || disabled}
      aria-label="Send message"
    >
      <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M5 12h14m0 0l-7-7m7 7l-7 7"></path>
      </svg>
    </button>
  </div>
</div>