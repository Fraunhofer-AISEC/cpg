<script lang="ts">
  import type { NodeDetailsJSON } from '$lib/types';

  /**
   * A small card with the key facts about the node under the mouse. Clicking the node still opens
   * it in the inspector.
   */
  interface Props {
    details: NodeDetailsJSON;
  }

  let { details }: Props = $props();

  const node = $derived(details.node);
  const showCode = $derived(!!node.code && node.code !== node.name);
</script>

<div
  class="w-72 rounded-md border border-gray-200 bg-white px-2.5 py-2 text-xs text-gray-700 shadow-lg"
>
  <div class="flex min-w-0 items-center gap-1.5">
    <span
      class="shrink-0 rounded bg-gray-100 px-1 py-px font-mono text-[10px] font-semibold text-gray-600"
      >{node.type}</span
    >
    <span class="truncate font-mono font-semibold text-gray-900">{node.name || node.code}</span>
  </div>
  {#if showCode}
    <p class="mt-1 truncate font-mono text-[11px] text-gray-500">{node.code}</p>
  {/if}
  <dl class="mt-1.5 grid grid-cols-[auto_1fr] gap-x-2 gap-y-0.5 text-[11px]">
    {#if details.typeName}
      <dt class="text-gray-400">Type</dt>
      <dd class="truncate font-mono">{details.typeName}</dd>
    {/if}
    {#if details.value != null}
      <dt class="text-gray-400">Value</dt>
      <dd class="truncate font-mono">{details.value}</dd>
    {/if}
    <dt class="text-gray-400">Dataflow</dt>
    <dd>
      <span title="Direct dataflow predecessors">← {details.dataflowFrom.length} in</span>
      <span class="text-gray-300">·</span>
      <span title="Direct dataflow successors">→ {details.dataflowTo.length} out</span>
    </dd>
  </dl>
  {#if details.overlays.length > 0 || details.warnings.length > 0}
    <div class="mt-1.5 flex flex-wrap gap-x-2 text-[11px]">
      {#if details.overlays.length > 0}
        <span class="text-purple-700">◆ {details.overlays.length} concepts</span>
      {/if}
      {#if details.warnings.length > 0}
        <span class="text-red-600" title={details.warnings.join('\n')}
          >⚠ analysis is uncertain here</span
        >
      {/if}
    </div>
  {/if}
</div>
