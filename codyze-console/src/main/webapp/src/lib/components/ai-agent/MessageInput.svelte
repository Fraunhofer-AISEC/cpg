<script lang="ts">
  interface Props {
    value: string;
    onSend: () => void;
    onValueChange: (value: string) => void;
    placeholder?: string;
    disabled?: boolean;
  }

  let { value, onSend, onValueChange, placeholder = 'Ask me anything about your codebase...', disabled = false }: Props = $props();

  let textareaElement: HTMLTextAreaElement;

  function handleKeyDown(e: KeyboardEvent) {
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
  }

  function handleSend() {
    if (value.trim() && !disabled) {
      onSend();
      // Reset textarea height after sending
      if (textareaElement) {
        textareaElement.style.height = 'auto';
      }
    }
  }

  // Reset textarea height when value becomes empty
  $effect(() => {
    if (value === '' && textareaElement) {
      textareaElement.style.height = 'auto';
    }
  });
</script>

<div class="flex items-center gap-3 rounded-3xl border-2 border-gray-200 bg-white py-2 pl-5 pr-2 shadow-lg transition-all duration-200 focus-within:border-blue-400 focus-within:shadow-xl focus-within:ring-4 focus-within:ring-blue-100">
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