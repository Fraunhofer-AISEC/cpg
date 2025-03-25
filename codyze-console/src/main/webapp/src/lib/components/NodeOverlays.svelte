<script lang="ts">
  import type { FlattenedNode } from '$lib/flatten';
  import type { NodeJSON } from '$lib/types';
  import NodeOverlay from '$lib/components/NodeOverlay.svelte';
  import NodeTooltip from '$lib/components/NodeTooltip.svelte';
  import type { ConceptGroup } from '$lib/concepts';
  import AddConceptDialog from './AddConceptDialog.svelte';

  interface Props {
    nodes: FlattenedNode[];
    codeLines: string[];
    highlightedNode: NodeJSON | null;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    conceptGroups: ConceptGroup[];
  }

  let {
    nodes,
    codeLines,
    highlightedNode = $bindable(),
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft,
    conceptGroups
  }: Props = $props();

  let showDialog = $state(false);
  let clickedNode = $state<FlattenedNode | null>(null);

  function handleClick(node: FlattenedNode) {
    console.log('Clicked node:', node);
    showDialog = true;
    clickedNode = node;
  }
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
