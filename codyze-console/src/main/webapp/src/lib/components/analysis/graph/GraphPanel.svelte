<script lang="ts">
  import type { NodeRefJSON } from '$lib/types';
  import {
    describeDependences,
    locationOf,
    type DependenceGraph,
    type GraphNode
  } from '$lib/graph';
  import type { GraphPanel } from '$lib/stores/graphPanel.svelte';
  import { isTyping } from '$lib/utils/keyboard';
  import GraphCanvas from './GraphCanvas.svelte';

  /**
   * The panel next to the code with the program dependence graph of a statement: what affects it
   * (backward) or what it affects (forward). It shows the graph with a header to choose how far to
   * look, and the details of the selected statement under it.
   */
  interface Props {
    panel: GraphPanel;
    /** Inspects the node of a statement, opening its file if it is in another one */
    onInspect: (node: NodeRefJSON) => void;
    /** Asks the agent about the node of a statement */
    onAsk: (node: NodeRefJSON) => void;
    /** Called when a statement is clicked in the graph, to show it in the code */
    onSelectNode?: (node: GraphNode) => void;
  }

  let { panel, onInspect, onAsk, onSelectNode }: Props = $props();

  const slice = $derived(panel.slice);
  const root = $derived(panel.root);
  const selected = $derived(panel.selected);
  const backward = $derived(panel.direction === 'BACKWARD');

  // Paths (e.g. found by the agent) are shown instead of a slice, without its controls
  const paths = $derived(panel.paths);

  const title = $derived.by(() => {
    if (paths) return `Agent: ${paths.description}`;
    const where = root && root.startLine > 0 ? `line ${root.startLine}` : 'this statement';
    return backward ? `What affects ${where}?` : `What does ${where} affect?`;
  });
  const subtitle = $derived(
    paths
      ? `${paths.count} ${paths.count === 1 ? 'path' : 'paths'} · ${paths.kind}`
      : `${panel.graph} · ${backward ? 'backward' : 'forward'} · ${root?.code ?? ''}${
          panel.scope === 'intraprocedural' && slice?.function ? ` · in ${slice.function.name}` : ''
        }`
  );

  // The tones of the notes about a statement, in the colors of the analysis
  const tones = {
    red: 'border-red-200 bg-red-50 text-red-800',
    purple: 'border-purple-200 bg-purple-50 text-purple-800',
    orange: 'border-orange-200 bg-orange-50 text-orange-800',
    gray: 'border-gray-200 bg-gray-50 text-gray-600'
  };

  function notesOf(node: GraphNode): { tone: keyof typeof tones; text: string }[] {
    const notes: { tone: keyof typeof tones; text: string }[] = [];
    if (node.kind === 'STUB') {
      notes.push(
        node.external
          ? {
              tone: 'orange',
              text: 'The dependence leaves the analysed code here. Everything after this point is invisible to the analysis.'
            }
          : { tone: 'gray', text: 'This statement is in another file. Opening it shows it there.' }
      );
      return notes;
    }
    if (node.unresolved) {
      notes.push({
        tone: 'red',
        text: 'A call in this statement could not be resolved: dependences through it may be incomplete.'
      });
    }
    if (node.warning) {
      notes.push({ tone: 'red', text: 'The analysis could not handle some of this code.' });
    }
    if (node.external) {
      notes.push({
        tone: 'orange',
        text: 'A call in this statement goes to code that is not part of the analysis.'
      });
    }
    if (node.concept) {
      notes.push({ tone: 'purple', text: 'A concept or operation is attached to this statement.' });
    }
    return notes;
  }

  const notes = $derived(selected ? notesOf(selected) : []);
  const dependences = $derived(slice && selected ? describeDependences(slice, selected.id) : '');
  const canOpen = $derived(!!selected && selected.node.startLine >= 1);

  // Escape closes the panel, unless something else (e.g. an input or a popup) used it
  function handleKeydown(event: KeyboardEvent) {
    if (event.key !== 'Escape' || event.defaultPrevented || isTyping(event)) return;
    event.preventDefault();
    panel.close();
  }

  const hopOptions = [1, 2, 3];

  // The dependences a slice can follow, see [DependenceGraph]
  const graphOptions: { graph: DependenceGraph; title: string }[] = [
    { graph: 'PDG', title: 'Data and control dependences (program dependence graph)' },
    { graph: 'DFG', title: 'Only data dependences: where values come from or go to' },
    { graph: 'CDG', title: 'Only control dependences: which conditions decide whether code runs' }
  ];
</script>

<svelte:window onkeydown={handleKeydown} />

<aside class="flex h-full min-h-0 w-full flex-col bg-gray-50" aria-label="Dependence graph">
  <!-- Header: the question, how far to look and the buttons of the panel -->
  <header class="flex h-13 shrink-0 items-center gap-2 border-b border-gray-200 bg-white pr-2 pl-3">
    {#if panel.history.length > 0}
      <button
        type="button"
        class="shrink-0 rounded px-1 text-gray-500 hover:bg-gray-100 hover:text-gray-900"
        onclick={() => panel.back()}
        aria-label="Back to the previous slice"
        title="Back to the previous slice"
      >
        ←
      </button>
    {/if}
    <div class="min-w-0 flex-1">
      <div class="truncate text-[13px] font-semibold text-gray-900">{title}</div>
      <div class="truncate font-mono text-[11px] text-gray-500" title={subtitle}>{subtitle}</div>
    </div>
    {#if !paths}
      <div class="flex gap-0.5 rounded-md bg-gray-100 p-0.5" role="group" aria-label="Graph">
        {#each graphOptions as option (option.graph)}
          <button
            type="button"
            class="h-6 rounded px-1.5 text-[11px] {panel.graph === option.graph
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-500 hover:text-gray-800'}"
            aria-pressed={panel.graph === option.graph}
            title={option.title}
            onclick={() => panel.setGraph(option.graph)}
          >
            {option.graph}
          </button>
        {/each}
      </div>
      <button
        type="button"
        class="h-6 rounded-md px-1.5 text-[11px] {panel.scope === 'intraprocedural'
          ? 'bg-gray-200 text-gray-900'
          : 'text-gray-500 hover:bg-gray-100 hover:text-gray-800'}"
        aria-pressed={panel.scope === 'intraprocedural'}
        title={panel.scope === 'intraprocedural'
          ? 'Intraprocedural: only the function of the statement. Click to follow the dependences into other functions'
          : 'Interprocedural: across functions. Click to stay in the function of the statement'}
        onclick={() =>
          panel.setScope(panel.scope === 'intraprocedural' ? 'interprocedural' : 'intraprocedural')}
      >
        intraprocedural
      </button>
      <span class="text-[11px] text-gray-500">Hops</span>
      <div class="flex gap-0.5 rounded-md bg-gray-100 p-0.5" role="group" aria-label="Hops">
        {#each hopOptions as hops (hops)}
          <button
            type="button"
            class="h-6 w-6 rounded text-xs {panel.hops === hops
              ? 'bg-white text-gray-900 shadow-sm'
              : 'text-gray-500 hover:text-gray-800'}"
            aria-pressed={panel.hops === hops}
            onclick={() => panel.setHops(hops)}
          >
            {hops}
          </button>
        {/each}
      </div>
      <button
        type="button"
        class="flex h-7 w-7 items-center justify-center rounded text-gray-500 hover:bg-gray-100 hover:text-gray-900 {panel.pinned
          ? 'bg-gray-200 text-gray-900'
          : ''}"
        aria-pressed={panel.pinned}
        onclick={() => (panel.pinned = !panel.pinned)}
        aria-label="Pin the graph"
        title={panel.pinned
          ? 'Pinned: the graph stays when another node is inspected'
          : 'Pin the graph, so that it stays when another node is inspected'}
      >
        <svg
          class="h-4 w-4"
          viewBox="0 0 24 24"
          fill={panel.pinned ? 'currentColor' : 'none'}
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M9 4h6l-1 6 4 3H6l4-3zM12 13v7" />
        </svg>
      </button>
    {/if}
    <button
      type="button"
      class="flex h-7 w-7 items-center justify-center rounded text-gray-500 hover:bg-gray-100 hover:text-gray-900"
      onclick={() => panel.close()}
      aria-label="Close the graph"
      title="Close (Esc)"
    >
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" d="M6 6l12 12M18 6L6 18" />
      </svg>
    </button>
  </header>

  <div class="relative min-h-0 flex-1">
    <GraphCanvas {panel} {onSelectNode} />
  </div>

  <!-- The selected statement -->
  <footer
    class="max-h-[45%] shrink-0 space-y-2 overflow-y-auto border-t border-gray-200 bg-white px-3 py-2.5"
  >
    <div class="flex flex-wrap items-center gap-x-3.5 gap-y-1 text-[11px] text-gray-500">
      {#if paths || panel.graph !== 'CDG'}
        <span class="flex items-center gap-1.5">
          <span class="h-0.5 w-5.5 bg-slate-600"></span>data dependence
        </span>
      {/if}
      {#if !paths && panel.graph !== 'DFG'}
        <span class="flex items-center gap-1.5">
          <span class="w-5.5 border-t-2 border-dashed border-slate-400"></span>control dependence
        </span>
      {/if}
      <span class="flex items-center gap-1.5">
        <span class="h-2.5 w-3.5 rounded-sm border border-dashed border-gray-400"></span>
        leaves the function
      </span>
    </div>
    {#if selected}
      <div class="flex items-center gap-2">
        <span
          class="rounded bg-gray-100 px-1.5 py-px font-mono text-[10.5px] font-semibold text-gray-700"
          >{selected.node.type}</span
        >
        <span class="min-w-0 truncate font-mono text-[11.5px] text-gray-500"
          >{locationOf(selected)}</span
        >
      </div>
      <div class="font-mono text-[12.5px] break-words text-gray-900">{selected.code}</div>
      <div class="text-xs text-gray-600">{dependences}</div>
      {#each notes as note (note.text)}
        <div class="rounded-md border px-2 py-1 text-xs {tones[note.tone]}">{note.text}</div>
      {/each}
      <div class="flex flex-wrap gap-1.5 pt-0.5">
        <button
          type="button"
          class="h-7 rounded-md border border-gray-300 bg-white px-2.5 text-xs text-gray-700 hover:bg-gray-50 disabled:text-gray-300"
          disabled={!canOpen}
          onclick={() => onInspect(selected.node)}
        >
          {selected.kind === 'STUB' && canOpen
            ? `Open ${selected.node.fileName ?? 'file'}`
            : 'Inspect'}
        </button>
        {#if selected.kind !== 'STUB' && !paths}
          <button
            type="button"
            class="h-7 rounded-md border border-blue-200 bg-blue-50 px-2.5 text-xs text-blue-700 hover:bg-blue-100"
            onclick={() => panel.reroot(selected.id, backward ? 'FORWARD' : 'BACKWARD')}
          >
            {backward ? 'Show what this affects →' : '← Show what affects this'}
          </button>
        {/if}
        <button
          type="button"
          class="h-7 rounded-md border border-gray-300 bg-white px-2.5 text-xs text-gray-700 hover:bg-gray-50 disabled:text-gray-300"
          disabled={!canOpen}
          onclick={() => onAsk(selected.node)}
        >
          Ask agent about this
        </button>
      </div>
    {:else}
      <div class="text-xs text-gray-400">Select a statement to see its details.</div>
    {/if}
  </footer>
</aside>
