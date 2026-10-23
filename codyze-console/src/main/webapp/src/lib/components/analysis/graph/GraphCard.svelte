<script lang="ts" module>
  import type { GraphNode } from '$lib/graph';

  /** What a card of the graph needs to know besides its statement. */
  export interface GraphCardData extends Record<string, unknown> {
    statement: GraphNode;
    /** The statement is selected */
    selected: boolean;
    /** The statement is hovered, in the graph or in the code */
    hovered: boolean;
    /** The statement is not connected to the selected one */
    faded: boolean;
    /** The statement is the root of the slice */
    root: boolean;
    /** The file of the statement, if it is another one than the file of the root */
    otherFile: string | null;
    /** The overlays of the statement are nodes of their own in the graph */
    overlaysShown: boolean;
    /** The statement is not on the path of the agent's answer, which the graph shows */
    offPath: boolean;
    onExpand: (id: string) => void;
  }
</script>

<script lang="ts">
  import { Handle, Position, type NodeProps } from '@xyflow/svelte';
  import { branchName, locationOf } from '$lib/graph';

  let { data }: NodeProps = $props();
  const card = $derived(data as GraphCardData);
  const statement = $derived(card.statement);
  const stub = $derived(statement.kind === 'STUB');
  const branch = $derived(statement.kind === 'BRANCH');
  const overlay = $derived(statement.kind === 'OVERLAY');

  // The concepts and operations of the statement by name, unless they are nodes of their own
  const overlayNames = $derived(
    card.overlaysShown ? [] : [...new Set(statement.overlays.map((o) => o.name || o.type))]
  );

  // The badges of the semantic colors. With a single one its word is shown as well
  const badges = $derived(
    [
      overlayNames.length > 0 && {
        icon: '◆',
        word: overlayNames[0] + (overlayNames.length > 1 ? ` +${overlayNames.length - 1}` : ''),
        title: statement.overlays.map((o) => `${o.label ?? ''} ${o.name || o.type}`).join('\n'),
        cls: 'text-purple-600'
      },
      statement.warning && { icon: '⚠', word: 'warning', cls: 'text-amber-600' },
      statement.external && !stub && { icon: '↗', word: 'external', cls: 'text-amber-600' }
    ].filter((b): b is { icon: string; word: string; title?: string; cls: string } => !!b)
  );
</script>

<!-- A statement: its line, the badges of the analysis and its code. Branches get a tag, stubs a
dashed border. A concept or operation is rounded and tinted, with its name instead of the code, so
that it does not look like code. Far away only the line number is shown, see the zoom class of the
graph. Expanding by double click is a shortcut, the button at the border does the same -->
<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
  class="relative h-14 w-39 px-2.5 py-1.5 text-left transition-opacity {overlay
    ? 'rounded-2xl'
    : 'rounded-lg'} {card.selected
    ? 'border-2 border-blue-500 bg-blue-50 shadow-[0_0_0_3px_rgba(59,130,246,0.15)]'
    : overlay
      ? 'border border-gray-300 bg-white shadow-sm'
      : stub
        ? 'border border-dashed border-gray-400 bg-gray-50'
        : branch
          ? 'border border-gray-300 bg-slate-50 shadow-sm'
          : 'border border-gray-300 bg-white shadow-sm'} {card.hovered && !card.selected
    ? 'ring-2 ring-blue-300'
    : ''} {card.offPath && !card.selected
    ? 'opacity-35'
    : card.faded && !card.selected
      ? 'opacity-60'
      : ''}"
  title="{statement.node.type} · {locationOf(statement)}"
  ondblclick={() => statement.more > 0 && card.onExpand(statement.id)}
>
  <Handle type="target" position={Position.Top} class="statement-handle" />
  <div
    class="flex h-4 items-center gap-1 text-[10.5px] leading-none text-gray-500 group-[.graph-far]/flow:h-full group-[.graph-far]/flow:justify-center group-[.graph-far]/flow:gap-2"
  >
    {#if stub}
      <span class="hidden text-4xl text-gray-500 group-[.graph-far]/flow:inline"
        >{statement.external ? '↗' : '↪'}</span
      >
      <span class="truncate group-[.graph-far]/flow:hidden"
        >{statement.external
          ? 'outside the analysed code'
          : `${locationOf(statement)} · other file`}</span
      >
    {:else if overlay}
      <span class="text-purple-600 group-[.graph-far]/flow:text-4xl">◆</span>
      <span class="truncate group-[.graph-far]/flow:hidden"
        >{statement.node.label ?? 'Overlay'} · L{statement.startLine}</span
      >
    {:else}
      <span
        class="font-medium tabular-nums group-[.graph-far]/flow:text-4xl group-[.graph-far]/flow:font-semibold group-[.graph-far]/flow:text-gray-700"
        >L{statement.startLine}</span
      >
      {#if card.otherFile}
        <span class="truncate text-gray-400 group-[.graph-far]/flow:hidden">{card.otherFile}</span>
      {/if}
      {#if branch}
        <span
          class="rounded bg-gray-200 px-1 text-[9.5px] text-gray-600 group-[.graph-far]/flow:hidden"
          >{branchName(statement)}</span
        >
      {/if}
    {/if}
    {#each badges as badge (badge.word)}
      <span
        class="rounded px-1 text-[10px] group-[.graph-far]/flow:text-3xl {badge.cls}"
        title={badge.title ?? badge.word}
        >{badge.icon}<span class="group-[.graph-far]/flow:hidden"
          >{badges.length === 1 ? ` ${badge.word}` : ''}</span
        ></span
      >
    {/each}
  </div>
  {#if overlay}
    <div
      class="mt-1 truncate text-[12px] font-semibold text-gray-900 group-[.graph-far]/flow:hidden"
    >
      {statement.node.name || statement.node.type}
    </div>
  {:else}
    <div class="mt-1 truncate font-mono text-[10.5px] text-gray-900 group-[.graph-far]/flow:hidden">
      {statement.code}
    </div>
  {/if}
  {#if statement.more > 0}
    <!-- Statements beyond the border of the slice -->
    <button
      type="button"
      class="nodrag nopan absolute -right-2 -bottom-2 rounded-full border border-blue-200 bg-white px-1.5 text-[10px] leading-4 text-blue-700 shadow-sm hover:bg-blue-50"
      title="Show {statement.more} more {statement.more === 1
        ? 'statement'
        : 'statements'} around this one"
      onclick={(e) => {
        e.stopPropagation();
        card.onExpand(statement.id);
      }}
      ondblclick={(e) => e.stopPropagation()}
    >
      +{statement.more}
    </button>
  {/if}
  <Handle type="source" position={Position.Bottom} class="statement-handle" />
</div>

<style>
  :global(.statement-handle) {
    opacity: 0;
    pointer-events: none;
  }
</style>
