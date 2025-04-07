<script lang="ts">
  import FindingOverlay from '$lib/components/FindingOverlay.svelte';
  import NodeOverlays from '$lib/components/NodeOverlays.svelte';
  import NodeTable from '$lib/components/NodeTable.svelte';
  import { flattenNodes } from '$lib/flatten';
  import { type NodeJSON } from '$lib/types';
  import Highlight, { LineNumbers } from 'svelte-highlight';
  import python from 'svelte-highlight/languages/python';
  import 'svelte-highlight/styles/github.css';
  import type { PageProps } from './$types';

  let { data }: PageProps = $props();

  const urlParams = new URLSearchParams(window.location.search);
  const line = urlParams.get('line');
  const finding = urlParams.get('finding');
  const kind = urlParams.get('kind');

  let activeTab = $state('overlayNodes');
  let nodes = $derived(
    flattenNodes(
      activeTab === 'overlayNodes' ? data.overlayNodes : data.astNodes,
      data.component.name,
      data.translationUnit.id
    )
  );
  let tableTitle = $derived(activeTab === 'overlayNodes' ? 'Overlay Nodes' : 'AST Nodes');
  let highlightedNode = $state<NodeJSON | null>(null);

  const lineHeight = 1.5; // 24px / 16
  const charWidth = 0.60015625; // 9.6025px / 16
  const offsetTop = 1;
  const baseOffsetLeft = 2.4;

  const totalLines = data.translationUnit.code.split('\n').length;
  const lineNumberWidth = Math.ceil(Math.log10(totalLines + 1));
  const offsetLeft = baseOffsetLeft + lineNumberWidth * charWidth;

  /**
   * Export the added concepts to a YAML file.
   * This function fetches the new concepts from the server and creates a downloadable file.
   */
  async function exportConcepts() {
    const res = await fetch('/api/export-concepts');
    if (!res.ok) {
      console.error('Error exporting concepts');
      return;
    }
    const content = await res.text();
    const blob = new Blob([content], { type: 'text/yaml' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'concepts.yaml';
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }
</script>

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
      <!-- 
      Make sure that really everything inside the code area is monospaced,
      otherwise we have a slight offset between browsers.
      -->
      <div class="font-mono">
        <Highlight language={python} code={data.translationUnit.code} let:highlighted>
          <LineNumbers
            {highlighted}
            highlightedLines={line ? [parseInt(line) - 1] : []}
            --line-number-color="gray"
            --padding-right={0}
            hideBorder
          />
        </Highlight>
      </div>

      {#if finding && line}
        <FindingOverlay {finding} {kind} line={parseInt(line)} {lineHeight} {offsetTop} />
      {/if}

      <NodeOverlays
        {nodes}
        codeLines={data.translationUnit.code.split('\n')}
        bind:highlightedNode
        {lineHeight}
        {charWidth}
        {offsetTop}
        {offsetLeft}
        conceptGroups={data.conceptGroups}
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
      {#if activeTab === 'overlayNodes'}<button
          class="ml-2 cursor-pointer bg-gray-200 px-4 py-2 text-black"
          onclick={() => exportConcepts()}
          ><!--- TODO: spacing. make nice. --->
          Export Added Concepts (.yaml)
        </button>{/if}
    </div>

    <NodeTable
      title={tableTitle}
      {nodes}
      bind:highlightedNode
      nodeClick={(node) => console.log(node)}
    />
  </div>
</div>
