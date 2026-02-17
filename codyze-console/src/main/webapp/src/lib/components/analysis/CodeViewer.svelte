<script lang="ts">
  import type { TranslationUnitJSON, NodeJSON } from '$lib/types';
  import { TabNavigation } from '$lib/components/navigation';
  import { NodeTable, NodeOverlays } from '$lib/components/analysis';
  import { flattenNodes } from '$lib/flatten';
  import Highlight, { LineNumbers } from 'svelte-highlight';
  import python from 'svelte-highlight/languages/python';
  import java from 'svelte-highlight/languages/java';
  import cpp from 'svelte-highlight/languages/cpp';
  import csharp from 'svelte-highlight/languages/csharp';
  import javascript from 'svelte-highlight/languages/javascript';
  import typescript from 'svelte-highlight/languages/typescript';
  import go from 'svelte-highlight/languages/go';
  import rust from 'svelte-highlight/languages/rust';
  import ruby from 'svelte-highlight/languages/ruby';
  import plaintext from 'svelte-highlight/languages/plaintext';
  import 'svelte-highlight/styles/github.css';

  const languageMap: Record<string, any> = {
    '.py': python,
    '.java': java,
    '.kt': java, // Kotlin uses Java highlighting as fallback
    '.c': cpp,
    '.cpp': cpp,
    '.cc': cpp,
    '.cxx': cpp,
    '.h': cpp,
    '.hpp': cpp,
    '.cs': csharp,
    '.js': javascript,
    '.jsx': javascript,
    '.ts': typescript,
    '.tsx': typescript,
    '.go': go,
    '.rs': rust,
    '.rb': ruby,
  };

  function getLanguage(fileName: string) {
    const ext = fileName.substring(fileName.lastIndexOf('.'));
    return languageMap[ext] || plaintext;
  }

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

  // Collapsible panel state
  let isPanelCollapsed = $state(false);

  function togglePanel() {
    isPanelCollapsed = !isPanelCollapsed;
  }

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

<div class="flex h-full">
  <!-- Code display -->
  <div class="flex-1 overflow-auto" bind:this={codeContainerElement}>
    <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-4 py-2">
      <div class="text-sm text-gray-700">{translationUnit.name}</div>

      <!-- Toggle button for panel -->
      <button
        onclick={togglePanel}
        class="toggle-panel-btn"
        type="button"
        aria-label={isPanelCollapsed ? 'Show nodes panel' : 'Hide nodes panel'}
      >
        <svg
          class="toggle-icon"
          class:rotate-180={isPanelCollapsed}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M13 5l7 7-7 7M5 5l7 7-7 7"
          />
        </svg>
      </button>
    </div>

    <div class="relative">
      <div class="font-mono">
        <Highlight language={getLanguage(translationUnit.name)} code={translationUnit.code} let:highlighted>
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

  <!-- Node information panel - Collapsible -->
  {#if !isPanelCollapsed}
    <div class="nodes-panel">
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
  {/if}
</div>

<style>
  .toggle-panel-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2rem;
    height: 2rem;
    border-radius: 0.375rem;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: all 0.15s ease;
    color: #6b7280;
  }

  .toggle-panel-btn:hover {
    background: #e5e7eb;
    color: #374151;
  }

  .toggle-icon {
    width: 1.25rem;
    height: 1.25rem;
    transition: transform 0.3s ease;
  }

  .rotate-180 {
    transform: rotate(180deg);
  }

  .nodes-panel {
    width: 20rem;
    border-left: 1px solid #e5e7eb;
    background: #f9fafb;
    animation: slideInPanel 0.3s ease-out;
  }

  @keyframes slideInPanel {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }
</style>