<script lang="ts">
  import {
    Background,
    Controls,
    MarkerType,
    MiniMap,
    SvelteFlow,
    type Edge,
    type Node
  } from '@xyflow/svelte';
  import '@xyflow/svelte/dist/style.css';
  import type { PdgSlice } from '$lib/pdg';
  import type { PdgPanel } from '$lib/stores/pdgPanel.svelte';
  import {
    cardHeight,
    cardWidth,
    edgeColor,
    edgeKey,
    layoutSlice,
    type SliceLayout
  } from './layout';
  import PdgCard, { type PdgCardData } from './PdgCard.svelte';
  import PdgEdge, { type PdgEdgeData } from './PdgEdge.svelte';
  import FitView from './FitView.svelte';

  /**
   * The graph of a slice: a card for each statement and an edge for each dependence, laid out in
   * layers by ELK and shown with Svelte Flow. The selected statement and its edges are emphasized,
   * the others step back.
   */
  interface Props {
    panel: PdgPanel;
  }

  let { panel }: Props = $props();

  const nodeTypes = { card: PdgCard };
  const edgeTypes = { pdg: PdgEdge };

  // The layout of the slice it was computed for. The graph keeps showing the previous slice until
  // the new one is laid out
  let layout = $state.raw<{ slice: PdgSlice; laid: SliceLayout } | null>(null);

  $effect(() => {
    const slice = panel.slice;
    if (!slice) return;
    layoutSlice(slice).then((laid) => {
      if (panel.slice === slice) layout = { slice, laid };
    });
  });

  // The variable whose edges are emphasized while its label is hovered
  let hoveredVariable = $state<string | null>(null);

  // Zoomed far out, the cards show only their line number
  let zoom = $state(1);
  const far = $derived(zoom < 0.6);

  const selectedId = $derived(panel.selectedId);

  const nodes = $derived.by((): Node[] => {
    if (!layout) return [];
    const { slice, laid } = layout;
    const connected = new Set<string>();
    for (const e of slice.edges) {
      if (e.from === selectedId) connected.add(e.to);
      if (e.to === selectedId) connected.add(e.from);
    }
    return slice.nodes.flatMap((pdg) => {
      const position = laid.positions.get(pdg.id);
      if (!position) return [];
      const data: PdgCardData = {
        pdg,
        selected: pdg.id === selectedId,
        faded: selectedId !== null && pdg.id !== selectedId && !connected.has(pdg.id),
        root: pdg.id === slice.root,
        onExpand: (id) => panel.expand(id)
      };
      return [
        {
          id: pdg.id,
          type: 'card',
          position,
          width: cardWidth,
          height: cardHeight,
          draggable: false,
          selectable: false,
          data
        }
      ];
    });
  });

  const edges = $derived.by((): Edge[] => {
    if (!layout) return [];
    const { slice, laid } = layout;
    return slice.edges.flatMap((e) => {
      const route = laid.edges.get(edgeKey(e));
      if (!route) return [];
      const active = selectedId !== null && (e.from === selectedId || e.to === selectedId);
      const marked = e.kind === 'DATA' && hoveredVariable !== null && e.label === hoveredVariable;
      const data: PdgEdgeData = {
        points: route.points,
        label: route.label,
        text: e.label,
        kind: e.kind,
        active,
        faded: selectedId !== null && !active,
        marked,
        onHoverVariable: (name) => (hoveredVariable = name)
      };
      return [
        {
          id: edgeKey(e),
          source: e.from,
          target: e.to,
          type: 'pdg',
          selectable: false,
          focusable: false,
          // The tip has the color of the edge
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: edgeColor(e.kind, active, marked),
            width: 14,
            height: 14
          },
          data
        }
      ];
    });
  });

  function nodeColor(node: Node): string {
    const pdg = (node.data as PdgCardData).pdg;
    if (pdg.id === selectedId) return '#3b82f6';
    return pdg.kind === 'STUB' ? '#d1d5db' : '#94a3b8';
  }
</script>

<div class="group/flow relative h-full min-h-0 w-full {far ? 'pdg-far' : ''}">
  {#if layout}
    <SvelteFlow
      {nodes}
      {edges}
      {nodeTypes}
      {edgeTypes}
      minZoom={0.15}
      maxZoom={1.6}
      nodesDraggable={false}
      nodesConnectable={false}
      elementsSelectable={false}
      deleteKey={null}
      onnodeclick={({ node }) => panel.select(node.id)}
      onmove={(_, viewport) => (zoom = viewport.zoom)}
    >
      <Background gap={16} size={1} bgColor="#fbfbfc" patternColor="#e2e5ea" />
      <Controls showLock={false} position="bottom-left" />
      <MiniMap
        pannable
        zoomable
        {nodeColor}
        maskColor="rgba(241, 245, 249, 0.7)"
        position="bottom-right"
        class="!h-24 !w-36"
      />
      <FitView trigger={layout} />
    </SvelteFlow>
  {/if}

  {#if panel.loading && !layout}
    <div class="absolute inset-0 flex items-center justify-center text-xs text-gray-400">
      Loading the dependence graph…
    </div>
  {:else if panel.error}
    <div
      class="absolute inset-x-4 top-4 rounded border border-red-200 bg-red-50 p-2 text-xs text-red-700"
    >
      {panel.error}
    </div>
  {:else if layout && layout.slice.edges.length === 0}
    <div class="pointer-events-none absolute inset-x-0 bottom-12 text-center text-xs text-gray-500">
      No dependences in this function
    </div>
  {/if}

  {#if panel.slice && panel.more > 0 && panel.hops < 3}
    <!-- More of the slice, one hop further -->
    <button
      type="button"
      class="absolute top-3 right-3 z-10 rounded-full border border-blue-200 bg-white px-3 py-1 text-[11.5px] text-blue-700 shadow-sm hover:bg-blue-50"
      onclick={() => panel.setHops(panel.hops + 1)}
    >
      + {panel.more}
      {panel.more === 1 ? 'node' : 'nodes'}
      {panel.direction === 'BACKWARD' ? 'upstream' : 'downstream'}
    </button>
  {/if}

  {#if panel.slice?.truncated}
    <div
      class="absolute top-3 left-3 z-10 rounded border border-gray-200 bg-white px-2 py-0.5 text-[11px] text-gray-500"
    >
      The slice was cut off, some statements are missing
    </div>
  {/if}
</div>
