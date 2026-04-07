<script lang="ts">
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';

  interface Props {
    title: string;
    nodes: FlattenedNode[];
    highlightedNode: NodeJSON | null;
    nodeClick: (node: NodeJSON) => void;
  }

  let { title, nodes, highlightedNode = $bindable(), nodeClick }: Props = $props();

  function typeColor(type: string): string {
    if (/Decl/i.test(type)) return 'bg-blue-100 text-blue-700';
    if (/Call/i.test(type)) return 'bg-purple-100 text-purple-700';
    if (/Expr|Lit/i.test(type)) return 'bg-green-100 text-green-700';
    if (/Stmt|Block/i.test(type)) return 'bg-orange-100 text-orange-700';
    if (/Ref|Member/i.test(type)) return 'bg-teal-100 text-teal-700';
    return 'bg-gray-100 text-gray-600';
  }
</script>

<div class="flex flex-col h-full text-xs">
  <!-- Header -->
  <div class="sticky top-0 z-10 flex items-center justify-between px-3 py-2 bg-gray-50 border-b border-gray-200">
    <span class="text-[11px] font-semibold uppercase tracking-wide text-gray-500">{title}</span>
    <span class="rounded-full bg-gray-200 px-2 py-0.5 text-[11px] font-semibold text-gray-500">{nodes.length}</span>
  </div>

  {#if nodes.length === 0}
    <p class="p-4 text-center text-gray-400 italic">No nodes</p>
  {:else}
    <ul class="flex-1 overflow-y-auto py-1">
      {#each nodes as node (node.id)}
        <li>
          <button
            class="flex w-full items-center gap-1.5 py-1.5 pr-3 mx-1 rounded cursor-pointer transition-colors min-w-0 text-left
              {highlightedNode?.id === node.id ? 'bg-blue-50' : 'hover:bg-gray-100'}"
            style="padding-left: {node.depth * 10 + 8}px"
            onmouseenter={() => (highlightedNode = node)}
            onmouseleave={() => (highlightedNode = null)}
            onclick={() => nodeClick(node)}
            type="button"
          >
            <span class="shrink-0 rounded px-1 py-0.5 font-mono text-[10px] font-semibold {typeColor(node.type)}">
              {node.type}
            </span>
            <span class="flex-1 truncate text-gray-900">{node.name || '—'}</span>
            <span class="shrink-0 font-mono text-[10px] text-gray-400">L{node.startLine}</span>
          </button>
        </li>
      {/each}
    </ul>
  {/if}
</div>