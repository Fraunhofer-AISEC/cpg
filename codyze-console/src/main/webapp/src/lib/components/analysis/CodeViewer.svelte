<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { TranslationUnitJSON, NodeJSON, ConceptSuggestionItem } from '$lib/types';
  import { TabNavigation } from '$lib/components/navigation';
  import { CollapsiblePanel } from '$lib/components/ui';
  import { NodeTable, NodeOverlays, FindingOverlay, ConceptChecklist } from '$lib/components/analysis';
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
    suggestions?: ConceptSuggestionItem[];
    onApplySuggestions?: (accepted: ConceptSuggestionItem[]) => void;
  }

  let { translationUnit, astNodes, overlayNodes, conceptGroups, highlightLine, finding, findingKind, headerActions, nodePanelCollapsed = $bindable(false), onClose, suggestions = $bindable([]), onApplySuggestions }: Props = $props();

  let activeTab = $state('astNodes');
  let nodes = $derived(
    flattenNodes(
      activeTab === 'overlayNodes' ? overlayNodes : astNodes,
      '',
      translationUnit.id
    )
  );
  let highlightedNode = $state<NodeJSON | null>(null);
  let codeContainerElement = $state<HTMLDivElement>();

  const tabs = $derived([
    { id: 'astNodes', label: 'AST Nodes', count: astNodes?.length || 0 },
    { id: 'overlayNodes', label: 'Overlay Nodes', count: overlayNodes?.length || 0 },
    ...(suggestions.length > 0
      ? [{ id: 'suggestions', label: 'Suggestions', count: suggestions.length }]
      : [])
  ]);

  let hoveredSuggestionNodeId = $state<string | null>(null);

  // Resolve a nodeId to its line range via astNodes
  function linesForNodeId(nodeId: string): number[] {
    const node = astNodes.find(n => n.id === nodeId);
    if (!node) return [];
    const lines: number[] = [];
    for (let l = node.startLine; l <= node.endLine; l++) lines.push(l - 1); // 0-based
    return lines;
  }

  const suggestionHighlightLines = $derived.by(() => {
    if (activeTab !== 'suggestions' || suggestions.length === 0 || astNodes.length === 0) return [];
    const nodeIds = new Set(
      suggestions.flatMap(s => [
        s.suggestion.nodeId,
        ...s.operations.map(o => o.operation.nodeId)
      ])
    );
    const lines = new Set<number>();
    for (const node of astNodes) {
      if (nodeIds.has(node.id)) {
        for (let l = node.startLine; l <= node.endLine; l++) lines.add(l - 1);
      }
    }
    return [...lines];
  });

  // Lines to highlight when hovering a specific suggestion node
  const hoveredLines = $derived.by(() => {
    if (!hoveredSuggestionNodeId) return [];
    return linesForNodeId(hoveredSuggestionNodeId);
  });

  // Combined highlight lines for the code viewer
  const allHighlightLines = $derived.by(() => {
    if (activeTab === 'suggestions') {
      const lines = new Set([...suggestionHighlightLines, ...hoveredLines]);
      return [...lines];
    }
    return highlightLine ? [highlightLine - 1] : [];
  });

  const lineHeight = 1.5;
  const charWidth = 0.60015625;
  const offsetTop = 1;
  const baseOffsetLeft = 2.4;

  const totalLines = $derived(translationUnit.code.split('\n').length);
  const lineNumberWidth = $derived(Math.ceil(Math.log10(totalLines + 1)));
  const offsetLeft = $derived(baseOffsetLeft + lineNumberWidth * charWidth);

  // Scroll to hovered suggestion node
  $effect(() => {
    if (hoveredSuggestionNodeId && codeContainerElement) {
      const node = astNodes.find(n => n.id === hoveredSuggestionNodeId);
      if (node) {
        const computedStyle = window.getComputedStyle(codeContainerElement);
        const lh = parseFloat(computedStyle.lineHeight) || parseFloat(computedStyle.fontSize) * 1.5 || 20;
        codeContainerElement.scrollTo({ top: Math.max(0, (node.startLine - 3) * lh), behavior: 'smooth' });
      }
    }
  });

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
      </div>
    </div>

    <div class="relative">
      <div class="font-mono">
        <Highlight language={getLanguage(translationUnit.name)} code={translationUnit.code} let:highlighted>
          <LineNumbers
            {highlighted}
            highlightedLines={allHighlightLines}
            --line-number-color="gray"
            --padding-right={0}
            hideBorder
          />
        </Highlight>
      </div>

      {#if finding && highlightLine}
        <FindingOverlay {finding} kind={findingKind} line={highlightLine} {lineHeight} {offsetTop} />
      {/if}

      {#if activeTab !== 'suggestions'}
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
      {/if}
    </div>

  </div>

  <!-- Node information panel -->
  <CollapsiblePanel title="Nodes" side="right" bind:collapsed={nodePanelCollapsed}>
    <div class="bg-white">
      <TabNavigation {tabs} {activeTab} onTabChange={(id) => (activeTab = id)} />
    </div>
    <div class="flex-1 overflow-auto p-4">
      {#if activeTab === 'suggestions'}
        <ConceptChecklist
          bind:items={suggestions}
          {onApplySuggestions}
          onHighlightNode={(nodeId) => hoveredSuggestionNodeId = nodeId}
        />
      {:else}
        <NodeTable
          {nodes}
          bind:highlightedNode
          nodeClick={(node) => console.log('Node clicked:', node)}
        />
      {/if}
    </div>
  </CollapsiblePanel>
</div>