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
  import type { GraphEdge, GraphNode, GraphSlice } from '$lib/graph';
  import type { GraphPanel } from '$lib/stores/graphPanel.svelte';
  import {
    cardHeight,
    cardWidth,
    edgeColor,
    edgeKey,
    layoutSlice,
    type SliceLayout
  } from './layout';
  import GraphCard, { type GraphCardData } from './GraphCard.svelte';
  import GraphEdgeLine, { type GraphEdgeData } from './GraphEdgeLine.svelte';
  import FitView from './FitView.svelte';

  /**
   * The graph of a slice: a card for each statement and an edge for each dependence, laid out in
   * layers by ELK and shown with Svelte Flow. The selected statement and its edges are emphasized,
   * the others step back. If the inspected node is a part of the selected statement, e.g. an
   * argument of a call, only its own edges are emphasized.
   */
  interface Props {
    panel: GraphPanel;
    /** The inspected node, which may be a part of a statement */
    inspectedId?: string | null;
    /** Called when a statement is clicked, after it was selected */
    onSelectNode?: (node: GraphNode) => void;
  }

  let { panel, inspectedId = null, onSelectNode }: Props = $props();

  const nodeTypes = { card: GraphCard };
  const edgeTypes = { dependence: GraphEdgeLine };

  // The layout of the slice it was computed for. The graph keeps showing the previous slice until
  // the new one is laid out
  let layout = $state.raw<{ slice: GraphSlice; laid: SliceLayout } | null>(null);

  $effect(() => {
    const slice = panel.shown;
    if (!slice) return;
    layoutSlice(slice).then((laid) => {
      if (panel.shown === slice) layout = { slice, laid };
    });
  });

  // The variable whose edges are emphasized while its label is hovered
  let hoveredVariable = $state<string | null>(null);

  // Zoomed far out, the cards show only their line number
  let zoom = $state(1);
  const far = $derived(zoom < 0.4);

  const selectedId = $derived(panel.selectedId);
  // The evidence of the agent stays visible as a whole: there, what is not connected to the
  // selection is not faded, only what the answer does not cite
  const fadeBySelection = $derived(!panel.evidence);

  // The edges of the inspected node, if it is a part of the selected statement
  const focusEdges = $derived.by((): GraphEdge[] => {
    if (!layout || !inspectedId || !selectedId) return [];
    return layout.slice.edges.filter(
      (e) =>
        (e.to === selectedId && e.toPart?.id === inspectedId) ||
        (e.from === selectedId && e.fromPart?.id === inspectedId)
    );
  });
  const focused = $derived(new Set(focusEdges.map(edgeKey)));

  const nodes = $derived.by((): Node[] => {
    if (!layout) return [];
    const { slice, laid } = layout;
    const connected = new Set<string>();
    for (const e of focusEdges.length > 0 ? focusEdges : slice.edges) {
      if (e.from === selectedId) connected.add(e.to);
      if (e.to === selectedId) connected.add(e.from);
    }
    const rootFile = slice.nodes.find((n) => n.id === slice.root)?.node.fileName;
    // The statements whose overlays are shown as nodes of their own
    const withOverlay = new Set(slice.edges.filter((e) => e.kind === 'OVERLAY').map((e) => e.from));
    const path = panel.evidence?.path;
    return slice.nodes.flatMap((statement) => {
      const position = laid.positions.get(statement.id);
      if (!position) return [];
      const data: GraphCardData = {
        statement,
        selected: statement.id === selectedId,
        hovered: statement.id === panel.hoveredId,
        faded:
          fadeBySelection &&
          selectedId !== null &&
          statement.id !== selectedId &&
          !connected.has(statement.id),
        root: statement.id === slice.root,
        otherFile:
          statement.node.fileName && statement.node.fileName !== rootFile
            ? statement.node.fileName
            : null,
        overlaysShown: withOverlay.has(statement.id),
        offPath: !!path && !path.has(statement.id),
        onExpand: (id) => panel.expand(id)
      };
      return [
        {
          id: statement.id,
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
    const path = panel.evidence?.path;
    return slice.edges.flatMap((e) => {
      const route = laid.edges.get(edgeKey(e));
      if (!route) return [];
      const overlay = e.kind === 'OVERLAY';
      // A dependence between two statements of the path the agent's answer leads along
      const agent = !overlay && !!path && path.has(e.from) && path.has(e.to);
      // With the edges of the inspected node, only they belong to the selection
      const active =
        !overlay &&
        selectedId !== null &&
        (focused.size > 0 ? focused.has(edgeKey(e)) : e.from === selectedId || e.to === selectedId);
      const marked = e.kind === 'DATA' && hoveredVariable !== null && e.label === hoveredVariable;
      const data: GraphEdgeData = {
        points: route.points,
        label: route.label,
        text: e.label,
        kind: e.kind,
        active,
        faded:
          (fadeBySelection && selectedId !== null && !active) || (!!path && !agent && !overlay),
        marked,
        agent,
        onHoverVariable: (name) => (hoveredVariable = name)
      };
      return [
        {
          id: edgeKey(e),
          source: e.from,
          target: e.to,
          type: 'dependence',
          selectable: false,
          focusable: false,
          // The tip has the color of the edge. The link of an overlay has no direction
          markerEnd: overlay
            ? undefined
            : {
                type: MarkerType.ArrowClosed,
                color: edgeColor(e.kind, active, marked, agent),
                width: 14,
                height: 14
              },
          data
        }
      ];
    });
  });

  function nodeColor(node: Node): string {
    const statement = (node.data as GraphCardData).statement;
    if (statement.id === selectedId) return '#3b82f6';
    if (statement.kind === 'OVERLAY') return '#c4b5fd';
    return statement.kind === 'STUB' ? '#d1d5db' : '#94a3b8';
  }
</script>

<div class="group/flow relative h-full min-h-0 w-full {far ? 'graph-far' : ''}">
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
      proOptions={{ hideAttribution: true }}
      onnodeclick={({ node }) => {
        panel.select(node.id);
        const statement = layout?.slice.nodes.find((n) => n.id === node.id);
        if (statement) onSelectNode?.(statement);
      }}
      onnodepointerenter={({ node }) => (panel.hoveredId = node.id)}
      onnodepointerleave={() => (panel.hoveredId = null)}
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
      class="absolute top-3 right-3 z-10 rounded-full border border-blue-200 bg-white px-2 py-0.5 text-[11px] text-blue-700 shadow-sm hover:bg-blue-50"
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
