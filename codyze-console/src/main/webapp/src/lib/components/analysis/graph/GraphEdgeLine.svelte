<script lang="ts" module>
  import type { Point } from './layout';

  /** What an edge of the graph needs to know besides its route. */
  export interface GraphEdgeData extends Record<string, unknown> {
    points: Point[];
    label: Point | null;
    text: string | null;
    kind: 'DATA' | 'CONTROL' | 'OVERLAY';
    /** Shown in the selection color, because it belongs to the selected statement */
    active: boolean;
    /** Shown in the background, because it does not belong to the selected statement */
    faded: boolean;
    /** Emphasized, because it is dependence on the hovered variable */
    marked: boolean;
    /** On the path the agent's answer leads along */
    agent: boolean;
    onHoverVariable: (name: string | null) => void;
  }
</script>

<script lang="ts">
  import { BaseEdge, EdgeLabel, type EdgeProps } from '@xyflow/svelte';
  import { edgeColor, roundedPath } from './layout';

  let { id, data, markerEnd }: EdgeProps = $props();
  const edge = $derived(data as GraphEdgeData);
  const path = $derived(roundedPath(edge.points));

  // Data dependences are solid and dark, control dependences dashed and light, and the selected
  // statement's edges are blue and thicker. The link of an overlay to its statement is dotted
  const stroke = $derived(edgeColor(edge.kind, edge.active, edge.marked, edge.agent));
  const width = $derived(
    edge.active || edge.marked || edge.agent ? 3 : edge.kind === 'OVERLAY' ? 2.5 : 2
  );
  const dashes: Record<GraphEdgeData['kind'], string> = {
    DATA: '',
    CONTROL: 'stroke-dasharray: 5 4;',
    OVERLAY: 'stroke-dasharray: 0.5 5; stroke-linecap: round;'
  };
  // The link of an overlay stays visible in the background, it is short and thin anyway
  const fadedOpacity = $derived(edge.kind === 'OVERLAY' ? 0.6 : 0.25);
</script>

<BaseEdge
  {id}
  {path}
  {markerEnd}
  style="stroke: {stroke}; stroke-width: {width}px; {dashes[edge.kind]} opacity: {edge.faded &&
  !edge.marked
    ? fadedOpacity
    : 1}; transition: opacity 150ms;"
  interactionWidth={0}
/>
{#if edge.text && edge.label}
  <EdgeLabel x={edge.label.x} y={edge.label.y} transparent>
    <button
      type="button"
      class="rounded-sm px-1 text-[10.5px] leading-4 group-[.graph-far]/flow:hidden {edge.agent &&
      !edge.active
        ? 'bg-teal-50 text-teal-700 ring-1 ring-teal-200'
        : 'bg-gray-50'} {edge.kind === 'DATA' ? 'font-mono' : 'italic'} {edge.agent
        ? ''
        : edge.kind === 'DATA'
          ? 'text-slate-600'
          : 'text-gray-500'} {edge.faded && !edge.marked ? 'opacity-40' : ''} {edge.active
        ? 'text-blue-700'
        : ''}"
      title={edge.agent ? "The agent's answer relies on this dependence" : undefined}
      onmouseenter={() => edge.kind === 'DATA' && edge.onHoverVariable(edge.text)}
      onmouseleave={() => edge.onHoverVariable(null)}
    >
      {edge.text}
    </button>
  </EdgeLabel>
{/if}
