<script lang="ts">
  interface NodeInfo {
    nodeId: string;
    name: string;
    code?: string | null;
    type?: string | null;
    fileName?: string | null;
    startLine?: number | null;
    endLine?: number | null;
    startColumn?: number | null;
    endColumn?: number | null;
  }

  let { content }: { content: NodeInfo[] } = $props();

  let nodes = $derived(content ?? []);
</script>

{#if nodes.length === 0}
  <div class="text-sm text-gray-400 italic px-1">No data flow found.</div>
{:else}
  <div class="rounded-lg border border-gray-200 bg-gray-50 p-3">
    <div class="mb-2 flex items-center gap-1.5">
      <!-- small arrow-left icon -->
      <svg class="h-3.5 w-3.5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 15.75L3 12m0 0l3.75-3.75M3 12h18" />
      </svg>
      <span class="text-xs font-semibold text-gray-500 uppercase tracking-wide">Data flow (backward)</span>
    </div>

    <div class="flex flex-wrap items-center gap-1.5">
      {#each nodes as node, i}
        {#if i > 0}
          <span class="text-gray-400 font-bold text-sm select-none">←</span>
        {/if}
        <div class="rounded-md border border-gray-200 bg-white px-2.5 py-1.5 shadow-sm">
          <div class="flex items-center gap-1.5">
            <span class="font-mono text-xs text-gray-400">{node.type ?? ''}</span>
            {#if node.startLine != null && node.startLine >= 0}
              <span class="text-gray-300 text-xs">·</span>
              <span class="font-mono text-xs text-gray-400">
                {node.fileName ? `${node.fileName}:${node.startLine}` : `:${node.startLine}`}
              </span>
            {/if}
          </div>
          <div class="font-mono text-sm font-semibold text-gray-900">{node.name ?? '?'}</div>
          {#if node.code && node.code !== node.name}
            <div class="mt-1 rounded bg-gray-100 px-1.5 py-0.5 font-mono text-xs text-gray-600">{node.code.trim()}</div>
          {/if}
        </div>
      {/each}
    </div>
  </div>
{/if}