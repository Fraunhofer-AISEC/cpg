<script lang="ts">
  import { tick, untrack } from 'svelte';
  import { useSvelteFlow } from '@xyflow/svelte';
  import { cardHeight, cardWidth } from './layout';

  /**
   * Fits the graph into the view whenever [trigger] changes, e.g. after a new layout. A graph that
   * would only fit at a size where its cards cannot be read is shown from its top at a readable
   * size instead, e.g. from the start of the evidence of the agent; the rest is reached by moving
   * the graph, or fitted with the controls.
   */
  interface Props {
    trigger: unknown;
    /** The positions of the cards, to know the size of the graph */
    positions?: Map<string, { x: number; y: number }>;
    /** The size of the view in px */
    width?: number;
    height?: number;
  }

  let { trigger, positions, width = 0, height = 0 }: Props = $props();

  const { fitView, setViewport } = useSvelteFlow();

  // The smallest zoom at which the code of the cards can be read, and the margin of the graph
  const readable = 0.75;
  const margin = 24;

  $effect(() => {
    void trigger;
    untrack(() => tick().then(show));
  });

  function show() {
    const points = [...(positions?.values() ?? [])];
    if (points.length === 0 || !width || !height) {
      fitView({ padding: 0.12, maxZoom: 1, duration: 200 });
      return;
    }
    const minX = Math.min(...points.map((p) => p.x));
    const maxX = Math.max(...points.map((p) => p.x)) + cardWidth;
    const minY = Math.min(...points.map((p) => p.y));
    const maxY = Math.max(...points.map((p) => p.y)) + cardHeight;
    const fit = Math.min(
      (width - 2 * margin) / (maxX - minX),
      (height - 2 * margin) / (maxY - minY)
    );
    if (fit >= readable) {
      fitView({ padding: 0.12, maxZoom: 1, duration: 200 });
      return;
    }
    // The top of the graph: centered if it is narrow enough, else around its first card
    const top = points.reduce((a, b) => (b.y < a.y || (b.y === a.y && b.x < a.x) ? b : a));
    const x =
      (maxX - minX) * readable <= width - 2 * margin
        ? (width - (maxX - minX) * readable) / 2 - minX * readable
        : width / 2 - (top.x + cardWidth / 2) * readable;
    setViewport({ x, y: margin - minY * readable, zoom: readable }, { duration: 200 });
  }
</script>
