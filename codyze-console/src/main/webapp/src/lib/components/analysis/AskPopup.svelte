<script lang="ts">
  import { tick } from 'svelte';
  import { modifierLabel } from '$lib/utils/keyboard';

  /**
   * A small floating input next to the selected code, to ask the agent about it. It does not take
   * the focus away until it is clicked or its shortcut is used, so that the text can still be
   * copied.
   */
  interface Props {
    /** The place of the selection, e.g. `main.c:10–12` */
    label: string;
    /** Why asking is not possible right now, if it is not */
    disabledReason?: string | null;
    /** The position in the code area in px. The card is shown above [y] if [above] is set */
    x: number;
    y: number;
    above: boolean;
    /** Incremented to focus the input */
    focusRequest: number;
    onSubmit: (question: string) => void;
    onClose: () => void;
    /** Called when the input gets or loses the focus */
    onFocusChange?: (focused: boolean) => void;
  }

  let {
    label,
    disabledReason = null,
    x,
    y,
    above,
    focusRequest,
    onSubmit,
    onClose,
    onFocusChange
  }: Props = $props();

  let question = $state('');
  let input = $state<HTMLInputElement>();

  $effect(() => {
    if (focusRequest > 0) tick().then(() => input?.focus());
  });

  function submit() {
    const text = question.trim();
    if (text && !disabledReason) onSubmit(text);
  }
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
  class="absolute z-40 w-[22rem] max-w-[calc(100%-1rem)] rounded-md border border-gray-300 bg-white p-1.5 text-xs shadow-lg"
  style:left="{x}px"
  style:top="{y}px"
  style:transform={above ? 'translateY(-100%)' : undefined}
  onmousedown={(e) => e.stopPropagation()}
>
  <div class="mb-1 flex items-center gap-1 text-[11px] text-gray-500">
    <span class="min-w-0 flex-1 truncate font-mono" title="Sent along with the question"
      >📎 {label}</span
    >
    <kbd class="shrink-0 rounded border border-gray-200 px-1 text-[10px] text-gray-400"
      >{modifierLabel()}K</kbd
    >
    <button
      type="button"
      class="shrink-0 rounded px-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
      onclick={onClose}
      aria-label="Close"
      title="Close (Esc)"
    >
      ×
    </button>
  </div>
  <div class="flex items-center gap-1">
    <input
      bind:this={input}
      bind:value={question}
      type="text"
      class="min-w-0 flex-1 rounded border border-gray-200 px-2 py-1 text-xs placeholder:text-gray-400 focus:border-gray-400 focus:outline-none disabled:bg-gray-50"
      placeholder={disabledReason ?? 'Ask the agent about this code…'}
      disabled={!!disabledReason}
      aria-label="Ask the agent about the selected code"
      onfocus={() => onFocusChange?.(true)}
      onblur={() => onFocusChange?.(false)}
      onkeydown={(e) => {
        if (e.key === 'Enter' && !e.isComposing) {
          e.preventDefault();
          submit();
        } else if (e.key === 'Escape') {
          e.preventDefault();
          e.stopPropagation();
          onClose();
        }
      }}
    />
    <button
      type="button"
      class="shrink-0 rounded bg-slate-700 px-2 py-1 text-white hover:bg-slate-800 disabled:bg-gray-200 disabled:text-gray-400"
      disabled={!!disabledReason || !question.trim()}
      onclick={submit}
      title="Ask the agent (Enter)"
    >
      Ask
    </button>
  </div>
</div>
