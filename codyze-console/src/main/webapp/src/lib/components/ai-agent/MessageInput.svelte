<script lang="ts">
  import type { McpPromptInfo } from '$lib/types';

  interface Props {
    value: string;
    onSend: () => void;
    onValueChange: (value: string) => void;
    placeholder?: string;
    disabled?: boolean;
    prompts?: McpPromptInfo[];
    onPromptSelect?: (name: string, args: Record<string, string>) => void;
  }

  let {
    value,
    onSend,
    onValueChange,
    placeholder = 'Ask me anything about your codebase...',
    disabled = false,
    prompts,
    onPromptSelect
  }: Props = $props();

  let textareaElement: HTMLTextAreaElement;
  let pickerIndex = $state(0);

  const slashQuery = $derived(() => {
    if (!prompts || prompts.length === 0) return null;
    const match = value.match(/^\/(\S*)$/);
    return match ? match[1] : null;
  });

  const filteredPrompts = $derived(() => {
    const q = slashQuery();
    if (q === null) return [];
    const lower = q.toLowerCase();
    return prompts!.filter((p) => p.name.toLowerCase().includes(lower));
  });

  const showPicker = $derived(filteredPrompts().length > 0);

  function handleKeyDown(e: KeyboardEvent) {
    if (showPicker) {
      const fp = filteredPrompts();
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        pickerIndex = (pickerIndex + 1) % fp.length;
        return;
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        pickerIndex = (pickerIndex - 1 + fp.length) % fp.length;
        return;
      }
      if (e.key === 'Enter') {
        e.preventDefault();
        selectPrompt(fp[pickerIndex]);
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
    target.style.height = 'auto';
    target.style.height = Math.min(target.scrollHeight, 120) + 'px';
    pickerIndex = 0;
  }

  function selectPrompt(prompt: McpPromptInfo) {
    onValueChange('');
    if (textareaElement) textareaElement.style.height = 'auto';
    if (onPromptSelect) {
      onPromptSelect(prompt.name, {});
    }
  }

  function handleSend() {
    if (value.trim() && !disabled) {
      onSend();
      if (textareaElement) {
        textareaElement.style.height = 'auto';
      }
    }
  }

  $effect(() => {
    if (value === '' && textareaElement) {
      textareaElement.style.height = 'auto';
    }
  });
</script>

<div class="relative">
  <!-- Slash-command picker for mcp prompts -->
  {#if showPicker}
    {@const fp = filteredPrompts()}
    <div
      class="absolute bottom-full left-0 right-0 mb-2 overflow-hidden rounded-xl border border-gray-200 bg-white shadow-xl"
    >
      <div class="px-3 py-2 border-b border-gray-100">
        <p class="text-xs font-medium text-gray-500">Available prompts</p>
      </div>
      <ul>
        {#each fp as prompt, i}
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
    class="flex items-center gap-3 rounded-3xl border-2 border-gray-200 bg-white py-2 pl-5 pr-2 shadow-lg transition-all duration-200 focus-within:border-blue-400 focus-within:shadow-xl focus-within:ring-4 focus-within:ring-blue-100"
  >
    <textarea
      bind:this={textareaElement}
      class="max-h-[120px] min-h-[24px] flex-1 resize-none border-0 bg-transparent text-base leading-relaxed text-gray-900 placeholder-gray-400 outline-none focus:border-0 focus:outline-none focus:ring-0"
      {placeholder}
      rows="1"
      {value}
      oninput={handleInput}
      onkeydown={handleKeyDown}
    ></textarea>
    <button
      class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-r from-blue-600 to-blue-700 text-white shadow-md transition-all duration-200 hover:scale-105 hover:from-blue-700 hover:to-blue-800 hover:shadow-lg active:scale-95 disabled:cursor-not-allowed disabled:bg-gray-300 disabled:opacity-50 disabled:hover:scale-100 disabled:hover:shadow-md"
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
