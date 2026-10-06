<script lang="ts" module>
  import type { Point } from './layout';

  /** What an edge of the graph needs to know besides its route. */
  export interface PdgEdgeData extends Record<string, unknown> {
    points: Point[];
    label: Point | null;
    text: string | null;
    kind: 'DATA' | 'CONTROL';
    /** Shown in the selection color, because it belongs to the selected statement */
    active: boolean;
    /** Shown in the background, because it does not belong to the selected statement */
    faded: boolean;
    /** Emphasized, because it is dependence on the hovered variable */
    marked: boolean;
    onHoverVariable: (name: string | null) => void;
  }
</script>

<script lang="ts">
  import { BaseEdge, EdgeLabel, type EdgeProps } from '@xyflow/svelte';
  import { edgeColor, roundedPath } from './layout';

  let { id, data, markerEnd }: EdgeProps = $props();
  const edge = $derived(data as PdgEdgeData);
  const path = $derived(roundedPath(edge.points));

  // Data dependences are solid and dark, control dependences dashed and light, and the selected
  // statement's edges are blue and thicker
  const stroke = $derived(edgeColor(edge.kind, edge.active, edge.marked));
  const width = $derived(edge.active || edge.marked ? 3 : 2);
</script>

<BaseEdge
  {id}
  {path}
  {markerEnd}
  style="stroke: {stroke}; stroke-width: {width}px; {edge.kind === 'CONTROL'
    ? 'stroke-dasharray: 5 4;'
    : ''} opacity: {edge.faded && !edge.marked ? 0.25 : 1}; transition: opacity 150ms;"
  interactionWidth={0}
/>
{#if edge.text && edge.label}
  <EdgeLabel x={edge.label.x} y={edge.label.y} transparent>
    <button
      type="button"
      class="rounded-sm bg-gray-50 px-1 text-[10.5px] leading-4 group-[.pdg-far]/flow:hidden {edge.kind ===
      'DATA'
        ? 'font-mono text-slate-600'
        : 'text-gray-500 italic'} {edge.faded && !edge.marked ? 'opacity-40' : ''} {edge.active
        ? 'text-blue-700'
        : ''}"
      onmouseenter={() => edge.kind === 'DATA' && edge.onHoverVariable(edge.text)}
      onmouseleave={() => edge.onHoverVariable(null)}
    >
      {edge.text}
    </button>
  </EdgeLabel>
{/if}
