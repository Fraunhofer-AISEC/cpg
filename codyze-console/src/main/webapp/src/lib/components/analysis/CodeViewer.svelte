<script lang="ts">
  import type { TranslationUnitJSON, NodeJSON } from '$lib/types';
  import { TabNavigation } from '$lib/components/navigation';
  import { NodeTable, NodeOverlays } from '$lib/components/analysis';
  import { flattenNodes } from '$lib/flatten';
  import Highlight, { LineNumbers } from 'svelte-highlight';
  import python from 'svelte-highlight/languages/python';
  import 'svelte-highlight/styles/github.css';

  interface Props {
    translationUnit: TranslationUnitJSON;
    astNodes: NodeJSON[];
    overlayNodes: NodeJSON[];
    conceptGroups?: any[];
    highlightLine?: number;
  }

  let { translationUnit, astNodes, overlayNodes, conceptGroups, highlightLine }: Props = $props();

  let activeTab = $state('overlayNodes');
  let nodes = $derived(
    flattenNodes(
      activeTab === 'overlayNodes' ? overlayNodes : astNodes,
      '', // component name not needed here
      translationUnit.id
    )
  );
  let tableTitle = $derived(activeTab === 'overlayNodes' ? 'Overlay Nodes' : 'AST Nodes');
  let highlightedNode = $state<NodeJSON | null>(null);
  let codeContainerElement = $state<HTMLDivElement>();

  const tabs = $derived([
    {
      id: 'overlayNodes',
      label: 'Overlay Nodes',
      count: overlayNodes?.length || 0
    },
    {
      id: 'astNodes',
      label: 'AST Nodes',
      count: astNodes?.length || 0
    }
  ]);

  function handleTabChange(tabId: string) {
    activeTab = tabId;
  }

  const lineHeight = 1.5;
  const charWidth = 0.60015625;
  const offsetTop = 1;
  const baseOffsetLeft = 2.4;

  const totalLines = translationUnit.code.split('\n').length;
  const lineNumberWidth = Math.ceil(Math.log10(totalLines + 1));
  const offsetLeft = baseOffsetLeft + lineNumberWidth * charWidth;

  // Scroll to highlighted line
  $effect(() => {
    if (highlightLine && codeContainerElement && typeof window !== 'undefined') {
      setTimeout(() => {
        if (!codeContainerElement) return;

        const computedStyle = window.getComputedStyle(codeContainerElement);
        const lineHeight =
          parseFloat(computedStyle.lineHeight) || parseFloat(computedStyle.fontSize) * 1.5 || 20;

        const scrollTop = Math.max(0, (highlightLine - 3) * lineHeight);
        codeContainerElement.scrollTo({
          top: scrollTop,
          behavior: 'smooth'
        });
      }, 300);
    }
  });
</script>

<div class="h-full flex">
  <!-- Code display -->
  <div class="flex-1 overflow-auto" bind:this={codeContainerElement}>
    <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-2">
      <div class="text-sm text-gray-700">{translationUnit.name}</div>
    </div>

    <div class="relative">
      <div class="font-mono">
        <Highlight language={python} code={translationUnit.code} let:highlighted>
          <LineNumbers
            {highlighted}
            highlightedLines={highlightLine ? [highlightLine - 1] : []}
            --line-number-color="gray"
            --padding-right={0}
            hideBorder
          />
        </Highlight>
      </div>

      <NodeOverlays
        {nodes}
        codeLines={translationUnit.code.split('\n')}
        bind:highlightedNode
        {lineHeight}
        {charWidth}
        {offsetTop}
        {offsetLeft}
        conceptGroups={conceptGroups || []}
      />
    </div>
  </div>

  <!-- Node information panel -->
  <div class="w-80 border-l border-gray-200 bg-gray-50">
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
</div>