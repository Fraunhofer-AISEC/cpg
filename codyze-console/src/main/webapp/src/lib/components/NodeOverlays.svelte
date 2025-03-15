<script lang="ts">
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';
  import NodeOverlay from '$lib/components/NodeOverlay.svelte';

  interface Props {
    nodes: FlattenedNode[];
    codeLines: string[];
    highlightedNode: NodeJSON | null;
    setHighlightedNode: (node: NodeJSON | null) => void;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    highlightLine: number | null;
    findingText?: string;
    kind?: string;
  }

  let {
    nodes,
    codeLines,
    highlightedNode = $bindable(),
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft,
    highlightLine,
    findingText,
    kind
  }: Props = $props();
</script>

<div class="absolute top-0 left-0 h-full w-full">
  {#each nodes as node (node.id)}
    <NodeOverlay
      {node}
      {codeLines}
      bind:highlightedNode
      {lineHeight}
      {charWidth}
      {offsetTop}
      {offsetLeft}
    />
  {/each}
</div>
