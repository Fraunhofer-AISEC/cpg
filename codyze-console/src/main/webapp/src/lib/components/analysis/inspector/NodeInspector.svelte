<script lang="ts">
  import { tick, type Snippet } from 'svelte';
  import type { InspectorSection } from '$lib/stores/codeFocus.svelte';
  import type { NodeDetailsJSON, NodeRefJSON } from '$lib/types';
  import NodeRefItem from './NodeRefItem.svelte';
  import DataflowTree from './DataflowTree.svelte';

  interface Props {
    details: NodeDetailsJSON | null;
    loading?: boolean;
    error?: string | null;
    onSelect: (ref: NodeRefJSON) => void;
    /** Additional actions for the inspected node, e.g. adding a concept */
    actions?: Snippet;
    /** A section to expand and scroll into view, e.g. after clicking the code lens of a function */
    revealSection?: { section: InspectorSection; request: number } | null;
  }

  let {
    details,
    loading = false,
    error = null,
    onSelect,
    actions,
    revealSection = null
  }: Props = $props();

  // The elements of the sections that can be revealed
  const sectionElements: Partial<Record<InspectorSection, HTMLDetailsElement>> = $state({});

  $effect(() => {
    const target = revealSection;
    if (!target) return;
    tick().then(() => {
      const element = sectionElements[target.section];
      if (!element) return;
      element.open = true;
      element.scrollIntoView({ block: 'start', behavior: 'smooth' });
    });
  });

  const calls = $derived(
    details
      ? [
          {
            id: 'callTargets' as const,
            title: 'Calls',
            refs: details.callTargets,
            hint: 'Functions this call invokes'
          },
          {
            id: 'callers' as const,
            title: 'Called by',
            refs: details.callers,
            hint: 'Calls of this function'
          },
          {
            id: 'callees' as const,
            title: 'Calls in body',
            refs: details.callees,
            hint: 'Calls made by this function'
          }
        ].filter((group) => group.refs.length > 0)
      : []
  );
</script>

{#if loading && !details}
  <p class="p-4 text-center text-xs text-gray-400">Loading node…</p>
{:else if error}
  <p class="p-4 text-center text-xs text-red-600">{error}</p>
{:else if !details}
  <div class="p-6 text-center text-xs text-gray-500">
    <p class="font-medium text-gray-700">No node selected</p>
    <p class="mt-1">Click anywhere in the code to inspect the node at that position.</p>
  </div>
{:else}
  <div class="space-y-3 p-3 text-xs" class:opacity-60={loading}>
    <!-- Header -->
    <div>
      <div class="flex items-center gap-1.5">
        <span
          class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-gray-700"
          >{details.node.type}</span
        >
        <span class="truncate text-sm font-semibold text-gray-900">{details.node.name || '—'}</span>
      </div>
      <p class="mt-1 truncate font-mono text-[11px] text-gray-500" title={details.node.code}>
        {details.node.code}
      </p>
      <dl class="mt-2 grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5 text-[11px]">
        <dt class="text-gray-400">Location</dt>
        <dd class="truncate font-mono text-gray-700">
          {details.node.fileName}:{details.node.startLine}
        </dd>
        {#if details.typeName}
          <dt class="text-gray-400">Type</dt>
          <dd class="truncate font-mono text-gray-700">{details.typeName}</dd>
        {/if}
        {#if details.value != null}
          <dt class="text-gray-400">Value</dt>
          <dd
            class="truncate font-mono text-gray-700"
            title="Constant value computed by the analysis"
          >
            {details.value}
          </dd>
        {/if}
        {#if details.isImplicit}
          <dt class="text-gray-400">Implicit</dt>
          <dd class="text-gray-700">yes (not written in the code)</dd>
        {/if}
      </dl>
      {#if details.enclosingFunction && details.enclosingFunction.id !== details.node.id}
        <div class="mt-2">
          <p class="mb-0.5 text-[10px] font-semibold tracking-wide text-gray-400 uppercase">
            In function
          </p>
          <NodeRefItem ref={details.enclosingFunction} {onSelect} />
        </div>
      {/if}
      {#if actions}
        <div class="mt-2 flex flex-wrap gap-1.5">{@render actions()}</div>
      {/if}
    </div>

    <!-- Analysis status -->
    {#if details.warnings.length > 0}
      <div class="rounded-md border border-amber-200 bg-amber-50 p-2">
        <p class="mb-1 text-[10px] font-semibold tracking-wide text-amber-800 uppercase">
          Analysis is uncertain here
        </p>
        <ul class="list-disc space-y-0.5 pl-4 text-[11px] text-amber-900">
          {#each details.warnings as warning (warning)}
            <li>{warning}</li>
          {/each}
        </ul>
      </div>
    {/if}

    <!-- Concepts & operations -->
    {#if details.overlays.length > 0}
      <details open>
        <summary
          class="cursor-pointer text-[10px] font-semibold tracking-wide text-gray-500 uppercase"
        >
          Concepts & operations ({details.overlays.length})
        </summary>
        <div class="mt-1">
          {#each details.overlays as overlay (overlay.id)}
            <NodeRefItem ref={overlay} {onSelect} />
          {/each}
        </div>
      </details>
    {/if}

    <!-- Calls -->
    {#each calls as group (group.id)}
      <details open bind:this={sectionElements[group.id]}>
        <summary
          class="cursor-pointer text-[10px] font-semibold tracking-wide text-gray-500 uppercase"
          title={group.hint}
        >
          {group.title} ({group.refs.length})
        </summary>
        <div class="mt-1 max-h-64 overflow-y-auto">
          {#each group.refs as ref (ref.id)}
            <NodeRefItem {ref} {onSelect} />
          {/each}
        </div>
      </details>
    {/each}

    <!-- Dataflow -->
    <details open>
      <summary
        class="cursor-pointer text-[10px] font-semibold tracking-wide text-gray-500 uppercase"
        title="Direct dataflow predecessors; expand to follow them further back"
      >
        Where does the value come from? ({details.dataflowFrom.length})
      </summary>
      <div class="mt-1">
        {#key details.node.id}
          <DataflowTree
            refs={details.dataflowFrom}
            direction="from"
            {onSelect}
            ancestors={[details.node.id]}
          />
        {/key}
      </div>
    </details>
    <details open>
      <summary
        class="cursor-pointer text-[10px] font-semibold tracking-wide text-gray-500 uppercase"
        title="Direct dataflow successors; expand to follow them further"
      >
        Where does the value go? ({details.dataflowTo.length})
      </summary>
      <div class="mt-1">
        {#key details.node.id}
          <DataflowTree
            refs={details.dataflowTo}
            direction="to"
            {onSelect}
            ancestors={[details.node.id]}
          />
        {/key}
      </div>
    </details>
  </div>
{/if}
