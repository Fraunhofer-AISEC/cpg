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

<!-- Code display -->
<div class="flex-1 overflow-auto">
  <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-2">
    <div class="text-sm text-gray-700">{data.translationUnit.name}</div>
    <button
      onclick={exportConcepts}
      class="rounded bg-blue-600 px-3 py-1 text-xs text-white hover:bg-blue-700"
    >
      Export Concepts
    </button>
  </div>
  
  <div class="relative">
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
      conceptGroups={data.conceptGroups || []}
    />
  </div>
</div>

<!-- Node information panel -->
<div class="w-96 border-l border-gray-200 bg-gray-50">
  <div class="flex border-b border-gray-200 bg-white">
    <button
      class="flex-1 px-4 py-2 text-sm font-medium {activeTab === 'overlayNodes' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500 hover:text-gray-700'}"
      onclick={() => activeTab = 'overlayNodes'}
    >
      Overlay Nodes
    </button>
    <button
      class="flex-1 px-4 py-2 text-sm font-medium {activeTab === 'astNodes' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500 hover:text-gray-700'}"
      onclick={() => activeTab = 'astNodes'}
    >
      AST Nodes
    </button>
  </div>
  
  <div class="h-full overflow-auto p-4">
    <NodeTable 
      title={tableTitle} 
      {nodes} 
      bind:highlightedNode 
      nodeClick={(node) => console.log('Node clicked:', node)}
    />
  </div>
</div>
