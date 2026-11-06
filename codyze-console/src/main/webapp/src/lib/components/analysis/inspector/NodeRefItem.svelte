<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { NodeJSON } from '$lib/types';

  interface Props {
    ref: NodeJSON;
    onSelect: (ref: NodeJSON) => void;
    /** How the node relates to the listed node, e.g. the granularity of a dataflow */
    label?: string | null;
    /** Rendered before the item, e.g. an expand toggle */
    leading?: Snippet;
  }

  let { ref, onSelect, label, leading }: Props = $props();

  // A concept or operation is named after its class, so its kind (see overlayLabel) is shown
  // instead of the class
  const overlay = $derived(label === 'Concept' || !!label?.startsWith('Operation'));

  const location = $derived(
    ref.startLine >= 0 ? `${ref.fileName ?? ''}:${ref.startLine}` : (ref.fileName ?? 'no location')
  );
</script>

<div class="group flex min-w-0 items-start gap-1">
  {#if leading}{@render leading()}{/if}
  <button
    type="button"
    class="flex min-w-0 flex-1 flex-col rounded px-1.5 py-1 text-left hover:bg-gray-100"
    onclick={() => onSelect(ref)}
    title={ref.code}
  >
    <span class="flex min-w-0 items-center gap-1.5">
      <span class="shrink-0 rounded bg-gray-100 px-1 font-mono text-[10px] text-gray-600">{overlay ? label : ref.type}</span>
      <span class="truncate text-xs font-medium text-gray-900">{ref.name || ref.code || '—'}</span>
      {#if ref.isInferred}
        <span class="shrink-0 rounded border border-amber-200 px-1 text-[10px] text-amber-700" title="Inferred: not part of the analysed code">inferred</span>
      {/if}
      {#if label && !overlay}
        <span class="shrink-0 rounded bg-gray-100 px-1 text-[10px] text-gray-600">{label}</span>
      {/if}
    </span>
    <span class="flex min-w-0 gap-2 font-mono text-[10px] text-gray-400">
      <span class="truncate">{ref.code}</span>
      <span class="ml-auto shrink-0">{location}</span>
    </span>
  </button>
</div>
