<script lang="ts">
  import { FindingOverlay, NodeOverlays, NodeTable } from '$lib/components/analysis';
  import { TabNavigation } from '$lib/components/navigation';
  import { Button } from '$lib/components/ui';
  import { flattenNodes } from '$lib/flatten';
  import { type NodeJSON } from '$lib/types';
  import Highlight, { LineNumbers } from 'svelte-highlight';
  import python from 'svelte-highlight/languages/python';
  import 'svelte-highlight/styles/github.css';
  import { page } from '$app/stores';
  import type { PageProps } from './$types';

  let { data }: PageProps = $props();

  // Get URL parameters reactively from SvelteKit's page store
  const line = $derived($page.url.searchParams.get('line'));
  const finding = $derived($page.url.searchParams.get('finding'));
  const kind = $derived($page.url.searchParams.get('kind'));

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
  let codeContainerElement = $state<HTMLDivElement>();

  // Tab configuration
  const tabs = $derived([
    {
      id: 'overlayNodes',
      label: 'Overlay Nodes',
      count: data.overlayNodes?.length || 0
    },
    {
      id: 'astNodes',
      label: 'AST Nodes',
      count: data.astNodes?.length || 0
    }
  ]);

  function handleTabChange(tabId: string) {
    activeTab = tabId;
  }

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

  // Scroll to highlighted line when line parameter changes
  $effect(() => {
    if (line && codeContainerElement && typeof window !== 'undefined') {
      const lineNumber = parseInt(line);
      console.log(`Scrolling to line ${lineNumber}`);

      // Simple approach: calculate scroll position based on line height
      setTimeout(() => {
        // Type guard to ensure element exists
        if (!codeContainerElement) return;

        // Get line height from CSS or use default
        const computedStyle = window.getComputedStyle(codeContainerElement);
        const lineHeight =
          parseFloat(computedStyle.lineHeight) || parseFloat(computedStyle.fontSize) * 1.5 || 20;

        console.log(`Using line height: ${lineHeight}px`);

        // Calculate scroll position (show target line near top with some context)
        const scrollTop = Math.max(0, (lineNumber - 3) * lineHeight);

        console.log(`Scrolling to position: ${scrollTop}px for line ${lineNumber}`);

        // Scroll the container
        codeContainerElement.scrollTo({
          top: scrollTop,
          behavior: 'smooth'
        });
      }, 300);
    }
  });
</script>

<!-- Code display -->
<div class="flex-1 overflow-auto" bind:this={codeContainerElement}>
  <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-2">
    <div class="text-sm text-gray-700">{data.translationUnit.name}</div>
    <Button variant="primary" size="sm" onclick={exportConcepts}>Export Concepts</Button>
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
  <div class="bg-white">
    <TabNavigation {tabs} {activeTab} onTabChange={handleTabChange} />
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
