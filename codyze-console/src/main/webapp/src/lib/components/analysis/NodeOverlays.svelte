<script lang="ts">
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';
  import NodeOverlay from './NodeOverlay.svelte';
  import NodeTooltip from './NodeTooltip.svelte';
  import type { ConceptGroup } from '$lib/concepts';
  import AddConceptDialog from '../forms/AddConceptDialog.svelte';

  interface Props {
    nodes: FlattenedNode[];
    codeLines: string[];
    /** 0-based index of the first visible line; only nodes overlapping the visible lines are rendered */
    startLine: number;
    /** 0-based index after the last visible line */
    endLine: number;
    highlightedNode: NodeJSON | null;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    conceptGroups: ConceptGroup[];
    /** Whether clicking a node opens the dialog to add a concept to it */
    addConceptOnClick?: boolean;
  }

  let {
    nodes,
    codeLines,
    startLine,
    endLine,
    highlightedNode = $bindable(),
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft,
    conceptGroups,
    addConceptOnClick = true
  }: Props = $props();

  // Node lines are 1-based, startLine and endLine are 0-based
  const visibleNodes = $derived(
    nodes.filter((node) => node.startLine - 1 < endLine && node.endLine - 1 >= startLine)
  );

  let showDialog = $state(false);
  let clickedNode = $state<FlattenedNode | null>(null);

  function handleClick(node: FlattenedNode) {
    if (!addConceptOnClick) return;
    showDialog = true;
    clickedNode = node;
  }
</script>

<div class="absolute top-0 left-0 h-full w-full">
  {#each visibleNodes as node (node.id)}
    <NodeOverlay
      {node}
      {codeLines}
      bind:highlightedNode
      {lineHeight}
      {charWidth}
      {offsetTop}
      {offsetLeft}
      onNodeClick={handleClick}
    />
  {/each}

  {#if showDialog && clickedNode}
    <AddConceptDialog bind:showDialog node={clickedNode} {conceptGroups} />
  {/if}

  {#if highlightedNode}
    <NodeTooltip node={highlightedNode} {lineHeight} {charWidth} {offsetTop} {offsetLeft} />
  {/if}
</div>
