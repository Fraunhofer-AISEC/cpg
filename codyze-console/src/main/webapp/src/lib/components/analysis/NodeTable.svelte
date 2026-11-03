<script lang="ts">
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';
  import { ScrollViewport } from '$lib/scroll-viewport.svelte';

  interface Props {
    nodes: FlattenedNode[];
    highlightedNode: NodeJSON | null;
    nodeClick: (node: NodeJSON) => void;
  }

  let { nodes, highlightedNode = $bindable(), nodeClick }: Props = $props();

  // Rows have a fixed height (h-7), so only the visible ones need to be rendered
  const ROW_HEIGHT = 28;

  let listElement = $state<HTMLDivElement>();
  const viewport = new ScrollViewport();
  $effect(() => {
    if (listElement) return viewport.track(listElement);
  });
  const visible = $derived(viewport.range(ROW_HEIGHT, nodes.length, 20));
  const visibleNodes = $derived(nodes.slice(visible.start, visible.end));

  function typeColor(type: string): string {
    if (/Decl/i.test(type)) return 'bg-blue-100 text-blue-700';
    if (/Call/i.test(type)) return 'bg-purple-100 text-purple-700';
    if (/Expr|Lit/i.test(type)) return 'bg-green-100 text-green-700';
    if (/Stmt|Block/i.test(type)) return 'bg-orange-100 text-orange-700';
    if (/Ref|Member/i.test(type)) return 'bg-teal-100 text-teal-700';
    return 'bg-gray-100 text-gray-600';
  }
</script>

<div class="h-full overflow-y-auto text-xs" bind:this={listElement}>
  {#if nodes.length === 0}
    <p class="p-4 text-center text-gray-400 italic">No nodes</p>
  {:else}
    <ul class="relative" style:height="{nodes.length * ROW_HEIGHT}px">
      {#each visibleNodes as node, i (node.id)}
        <li class="absolute right-0 left-0" style:top="{(visible.start + i) * ROW_HEIGHT}px">
          <button
            class="flex h-7 w-full min-w-0 cursor-pointer items-center gap-1.5 rounded pr-3 text-left transition-colors
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