<script lang="ts">
  import type { NodeRefJSON } from '$lib/types';
  import { getNodeDetails } from '$lib/nodeDetails';
  import NodeRefItem from './NodeRefItem.svelte';
  import DataflowTree from './DataflowTree.svelte';

  interface Props {
    refs: NodeRefJSON[];
    /** Backward ("where does it come from") or forward ("where does it go to") */
    direction: 'from' | 'to';
    onSelect: (ref: NodeRefJSON) => void;
    /** IDs of the nodes on the path to this level, to detect cycles */
    ancestors?: string[];
  }

  let { refs, direction, onSelect, ancestors = [] }: Props = $props();

  // The children of expanded items, by node ID
  let expanded = $state<Record<string, NodeRefJSON[] | 'loading'>>({});

  async function toggle(ref: NodeRefJSON) {
    if (expanded[ref.id]) {
      delete expanded[ref.id];
      return;
    }
    expanded[ref.id] = 'loading';
    const details = await getNodeDetails(ref.id).catch(() => null);
    expanded[ref.id] = (direction === 'from' ? details?.dataflowFrom : details?.dataflowTo) ?? [];
  }
</script>

{#if refs.length === 0}
  <p class="py-1 pl-6 text-[11px] text-gray-400 italic">
    {direction === 'from' ? 'No incoming dataflow' : 'No outgoing dataflow'}
  </p>
{:else}
  <ul>
    {#each refs as ref (ref.id + (ref.label ?? ''))}
      {@const isCycle = ancestors.includes(ref.id)}
      {@const children = expanded[ref.id]}
      <li>
        <NodeRefItem {ref} {onSelect}>
          {#snippet leading()}
            {#if isCycle}
              <span class="mt-1 w-5 shrink-0 text-center text-xs text-gray-400" title="Already on this path (cycle)">↻</span>
            {:else}
              <button
                type="button"
                class="mt-1 w-5 shrink-0 rounded text-xs text-gray-500 hover:bg-gray-200"
                onclick={() => toggle(ref)}
                aria-label={children ? 'Collapse' : 'Expand'}
              >
                {children ? '▾' : '▸'}
              </button>
            {/if}
          {/snippet}
        </NodeRefItem>
        {#if children === 'loading'}
          <p class="py-1 pl-8 text-[11px] text-gray-400">Loading…</p>
        {:else if children}
          <div class="ml-2.5 border-l border-gray-200 pl-1">
            <DataflowTree refs={children} {direction} {onSelect} ancestors={[...ancestors, ref.id]} />
          </div>
        {/if}
      </li>
    {/each}
  </ul>
{/if}