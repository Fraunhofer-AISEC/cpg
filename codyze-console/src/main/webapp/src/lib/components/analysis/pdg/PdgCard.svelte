<script lang="ts" module>
  import type { PdgNode } from '$lib/pdg';

  /** What a card of the graph needs to know besides its statement. */
  export interface PdgCardData extends Record<string, unknown> {
    pdg: PdgNode;
    /** The statement is selected */
    selected: boolean;
    /** The statement is hovered, in the graph or in the code */
    hovered: boolean;
    /** The statement is not connected to the selected one */
    faded: boolean;
    /** The statement is the root of the slice */
    root: boolean;
    onExpand: (id: string) => void;
  }
</script>

<script lang="ts">
  import { Handle, Position, type NodeProps } from '@xyflow/svelte';
  import { branchName, locationOf } from '$lib/pdg';

  let { data }: NodeProps = $props();
  const card = $derived(data as PdgCardData);
  const pdg = $derived(card.pdg);
  const stub = $derived(pdg.kind === 'STUB');
  const branch = $derived(pdg.kind === 'BRANCH');

  // The badges of the semantic colors. With a single one its word is shown as well
  const badges = $derived(
    [
      pdg.concept && { icon: '◆', word: 'concept', cls: 'bg-purple-50 text-purple-700' },
      pdg.unresolved && { icon: '!', word: 'unresolved', cls: 'bg-red-50 text-red-700' },
      pdg.warning && { icon: '⚠', word: 'warning', cls: 'bg-red-50 text-red-700' },
      pdg.external && !stub && { icon: '↗', word: 'external', cls: 'bg-orange-50 text-orange-700' }
    ].filter((b): b is { icon: string; word: string; cls: string } => !!b)
  );
</script>

<!-- A statement: its line, the badges of the analysis and its code. Branches get a tag, stubs a
dashed border. Far away only the line number is shown, see the zoom class of the graph. Expanding
by double click is a shortcut, the button at the border does the same -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
  class="relative h-14 w-39 rounded-lg px-2.5 py-1.5 text-left transition-opacity {card.selected
    ? 'border-2 border-blue-500 bg-blue-50 shadow-[0_0_0_3px_rgba(59,130,246,0.15)]'
    : stub
      ? 'border border-dashed border-gray-400 bg-gray-50'
      : branch
        ? 'border border-gray-300 bg-slate-50 shadow-sm'
        : 'border border-gray-300 bg-white shadow-sm'} {card.hovered && !card.selected
    ? 'ring-2 ring-blue-300'
    : ''} {card.faded && !card.selected ? 'opacity-60' : ''}"
  title="{pdg.node.type} · {locationOf(pdg)}"
  ondblclick={() => pdg.more > 0 && card.onExpand(pdg.id)}
>
  <Handle type="target" position={Position.Top} class="pdg-handle" />
  <div
    class="flex h-4 items-center gap-1 text-[10.5px] leading-none text-gray-500 group-[.pdg-far]/flow:h-full group-[.pdg-far]/flow:justify-center group-[.pdg-far]/flow:gap-2"
  >
    {#if stub}
      <span class="hidden text-4xl text-gray-500 group-[.pdg-far]/flow:inline"
        >{pdg.external ? '↗' : '↪'}</span
      >
      <span class="truncate group-[.pdg-far]/flow:hidden"
        >{pdg.external ? 'outside the analysed code' : `${locationOf(pdg)} · other file`}</span
      >
    {:else}
      <span
        class="font-medium tabular-nums group-[.pdg-far]/flow:text-4xl group-[.pdg-far]/flow:font-semibold group-[.pdg-far]/flow:text-gray-700"
        >L{pdg.startLine}</span
      >
      {#if branch}
        <span
          class="rounded bg-gray-200 px-1 text-[9.5px] text-gray-600 group-[.pdg-far]/flow:hidden"
          >{branchName(pdg)}</span
        >
      {/if}
    {/if}
    {#each badges as badge (badge.word)}
      <span
        class="rounded px-1 text-[10px] group-[.pdg-far]/flow:text-3xl {badge.cls}"
        title={badge.word}
        >{badge.icon}<span class="group-[.pdg-far]/flow:hidden"
          >{badges.length === 1 ? ` ${badge.word}` : ''}</span
        ></span
      >
    {/each}
  </div>
  <div class="mt-1 truncate font-mono text-[10.5px] text-gray-900 group-[.pdg-far]/flow:hidden">
    {pdg.code}
  </div>
  {#if pdg.more > 0}
    <!-- Statements beyond the border of the slice -->
    <button
      type="button"
      class="nodrag nopan absolute -right-2 -bottom-2 rounded-full border border-blue-200 bg-white px-1.5 text-[10px] leading-4 text-blue-700 shadow-sm hover:bg-blue-50"
      title="Show {pdg.more} more {pdg.more === 1 ? 'statement' : 'statements'} around this one"
      onclick={(e) => {
        e.stopPropagation();
        card.onExpand(pdg.id);
      }}
      ondblclick={(e) => e.stopPropagation()}
    >
      +{pdg.more}
    </button>
  {/if}
  <Handle type="source" position={Position.Bottom} class="pdg-handle" />
</div>

<style>
  :global(.pdg-handle) {
    opacity: 0;
    pointer-events: none;
  }
</style>
