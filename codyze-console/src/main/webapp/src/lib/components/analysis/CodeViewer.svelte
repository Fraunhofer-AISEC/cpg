<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { TranslationUnitJSON, NodeJSON } from '$lib/types';
  import { TabNavigation } from '$lib/components/navigation';
  import { NodeTable, NodeOverlays, FindingOverlay } from '$lib/components/analysis';
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
    '.kt': java,
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
    finding?: string;
    findingKind?: string;
    headerActions?: Snippet;
    nodePanelCollapsed?: boolean;
    onClose?: () => void;
    hideControls?: boolean;
  }

  let { translationUnit, astNodes, overlayNodes, conceptGroups, highlightLine, finding, findingKind, headerActions, nodePanelCollapsed = $bindable(false), onClose, hideControls = false }: Props = $props();

  let activeTab = $state('overlayNodes');
  let nodes = $derived(
    flattenNodes(
      activeTab === 'overlayNodes' ? overlayNodes : astNodes,
      '',
      translationUnit.id
    )
  );
  let tableTitle = $derived(activeTab === 'overlayNodes' ? 'Overlay Nodes' : 'AST Nodes');
  let highlightedNode = $state<NodeJSON | null>(null);
  let codeContainerElement = $state<HTMLDivElement>();

  const tabs = $derived([
    { id: 'overlayNodes', label: 'Overlay Nodes', count: overlayNodes?.length || 0 },
    { id: 'astNodes', label: 'AST Nodes', count: astNodes?.length || 0 }
  ]);

  const lineHeight = 1.5;
  const charWidth = 0.60015625;
  const offsetTop = 1;
  const baseOffsetLeft = 2.4;

  const totalLines = $derived(translationUnit.code.split('\n').length);
  const lineNumberWidth = $derived(Math.ceil(Math.log10(totalLines + 1)));
  const offsetLeft = $derived(baseOffsetLeft + lineNumberWidth * charWidth);

  $effect(() => {
    if (highlightLine && codeContainerElement && typeof window !== 'undefined') {
      setTimeout(() => {
        if (!codeContainerElement) return;
        const computedStyle = window.getComputedStyle(codeContainerElement);
        const lineHeight = parseFloat(computedStyle.lineHeight) || parseFloat(computedStyle.fontSize) * 1.5 || 20;
        codeContainerElement.scrollTo({ top: Math.max(0, (highlightLine - 3) * lineHeight), behavior: 'smooth' });
      }, 300);
    }
  });
</script>

<div class="flex h-full w-full overflow-hidden rounded-[inherit]">
  <!-- Code display -->
  <div class="relative flex-1 overflow-auto" bind:this={codeContainerElement}>
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-4 py-2">
      <div class="font-mono text-xs text-gray-500">{translationUnit.name}</div>
      <div class="flex items-center gap-2">
        {#if headerActions}
          {@render headerActions()}
        {/if}
        {#if onClose}
          <button
            onclick={onClose}
            class="flex items-center justify-center w-8 h-8 rounded-md text-gray-500 transition-colors hover:bg-gray-200 hover:text-gray-700"
            type="button"
            aria-label="Close panel"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        {/if}
        {#if !hideControls}
          <!-- Toggle button for panel -->
          <button
            onclick={() => (nodePanelCollapsed = !nodePanelCollapsed)}
            class="flex items-center justify-center w-8 h-8 rounded-md text-gray-500 transition-colors hover:bg-gray-200 hover:text-gray-700"
            type="button"
            aria-label={nodePanelCollapsed ? 'Show nodes panel' : 'Hide nodes panel'}
          >
            <svg
              class="w-5 h-5 transition-transform duration-300 {nodePanelCollapsed ? 'rotate-180' : ''}"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
            </svg>
          </button>
        {/if}
      </div>
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

      {#if finding && highlightLine}
        <FindingOverlay {finding} kind={findingKind} line={highlightLine} {lineHeight} {offsetTop} />
      {/if}

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
  {#if hideControls}
    <!-- Side-tab mode: header IS the toggle -->
    {#if nodePanelCollapsed}
      <!-- Collapsed: narrow strip with vertical label -->
      <button
        onclick={() => (nodePanelCollapsed = false)}
        class="group flex h-full w-8 shrink-0 flex-col items-center justify-start gap-2 border-l border-gray-200 bg-white pt-4 text-gray-400 transition-all rounded-r-xl hover:bg-blue-50 hover:text-blue-600"
        aria-label="Show nodes panel"
      >
        <svg class="w-3 h-3 transition-transform group-hover:-translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
        <span class="text-[10px] font-semibold tracking-widest uppercase group-hover:text-blue-500" style="writing-mode: vertical-rl;">Nodes</span>
      </button>
    {:else}
      <!-- Expanded: right panel with clickable header + blue right accent bar -->
      <div class="flex h-full w-96 shrink-0 flex-col border-l border-gray-200 bg-white rounded-r-xl overflow-hidden">
        <button
          onclick={() => (nodePanelCollapsed = true)}
          class="group flex w-full shrink-0 items-center justify-between border-b border-gray-200 px-3 py-2 text-left transition-colors hover:bg-gray-50"
          aria-label="Collapse nodes panel"
        >
          <svg class="h-3.5 w-3.5 text-gray-300 transition-transform group-hover:text-gray-500 group-hover:translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
          </svg>
          <div class="flex items-center gap-2">
            <span class="text-[11px] font-semibold uppercase tracking-widest text-gray-500 group-hover:text-gray-700">Nodes</span>
            <div class="h-3.5 w-0.5 rounded-full bg-blue-500"></div>
          </div>
        </button>
        <div class="bg-white">
          <TabNavigation {tabs} {activeTab} onTabChange={(id) => (activeTab = id)} />
        </div>
        <div class="flex-1 overflow-auto p-4">
          <NodeTable
            title={tableTitle}
            {nodes}
            bind:highlightedNode
            nodeClick={(node) => console.log('Node clicked:', node)}
          />
        </div>
      </div>
    {/if}
  {:else}
    {#if !nodePanelCollapsed}
      <div class="w-96 border-l border-gray-200 bg-gray-50" style="animation: slideInPanel 0.3s ease-out">
        <div class="bg-white">
          <TabNavigation {tabs} {activeTab} onTabChange={(id) => (activeTab = id)} />
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
  {/if}
</div>

<style>
  @keyframes slideInPanel {
    from { transform: translateX(100%); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
  }
</style>