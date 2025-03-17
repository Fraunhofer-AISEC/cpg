<script lang="ts">
  import NodeTable from '$lib/components/NodeTable.svelte';
  import { flattenNodes } from '$lib/flatten';
  import { type NodeJSON } from '$lib/types';
  import Highlight, { LineNumbers } from 'svelte-highlight';
  import python from 'svelte-highlight/languages/python';
  import github from 'svelte-highlight/styles/github';
  import type { PageProps } from './$types';
  import NodeOverlays from '$lib/components/NodeOverlays.svelte';
  import NodeTooltip from '$lib/components/NodeTooltip.svelte';
  import { getFindingStyle } from '$lib/colors';
  import FindingOverlay from '$lib/components/FindingOverlay.svelte';

  let { data }: PageProps = $props();

  const urlParams = new URLSearchParams(window.location.search);
  const line = urlParams.get('line');
  const finding = urlParams.get('finding');
  const kind = urlParams.get('kind');

  let activeTab = $state('overlayNodes');
  let nodes = $derived(
    flattenNodes(activeTab === 'overlayNodes' ? data.overlayNodes : data.astNodes)
  );
  let tableTitle = $derived(activeTab === 'overlayNodes' ? 'Overlay Nodes' : 'AST Nodes');
  let highlightedNode = $state<NodeJSON | null>(null);

  const lineHeight = 1.5;
  const charWidth = 0.625;
  const offsetTop = 1;
  const baseOffsetLeft = 2.25;

  const totalLines = data.translationUnit.code.split('\n').length;
  const lineNumberWidth = Math.ceil(Math.log10(totalLines + 1));
  const offsetLeft = baseOffsetLeft + lineNumberWidth * charWidth;
</script>

<svelte:head>
  {@html github}
</svelte:head>

<div class="container mx-auto p-4">
  <div class="mb-4">
    <a href={`/component/${data.component.name}`} class="text-blue-600 hover:underline">
      Back to Component
    </a>
  </div>
  <h1 class="mb-6 text-2xl font-bold">{data.translationUnit.name}</h1>
  <p class="mb-4 text-gray-500">{data.translationUnit.path}</p>

  <div class="relative mb-6 rounded bg-white p-2 shadow-md">
    <div class="relative">
      <Highlight language={python} code={data.translationUnit.code} let:highlighted>
        <LineNumbers
          {highlighted}
          highlightedLines={line ? [parseInt(line) - 1] : []}
          --line-number-color="gray"
          --padding-right={0}
          hideBorder
        />
        {#if finding && line}
          <FindingOverlay
            finding={finding}
            kind={kind}
            line={parseInt(line)}
            lineHeight={lineHeight}
            offsetTop={offsetTop}
          />
        {/if}
      </Highlight>

      <NodeOverlays
        {nodes}
        codeLines={data.translationUnit.code.split('\n')}
        bind:highlightedNode
        {lineHeight}
        {charWidth}
        {offsetTop}
        {offsetLeft}
      />
    </div>
  </div>

  <div class="rounded bg-white p-6 shadow-md">
    <div class="mb-4">
      <button
        class={`ml-2 cursor-pointer px-4 py-2 ${
          activeTab === 'overlayNodes' ? 'bg-blue-600 text-white' : 'bg-gray-200'
        }`}
        onclick={() => (activeTab = 'overlayNodes')}
      >
        Overlay Nodes
      </button>
      <button
        class={`cursor-pointer px-4 py-2 ${activeTab === 'astNodes' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
        onclick={() => (activeTab = 'astNodes')}
      >
        AST Nodes
      </button>
    </div>

    <NodeTable
      title={tableTitle}
      {nodes}
      bind:highlightedNode
      nodeClick={(node) => console.log(node)}
    />
  </div>
</div>
