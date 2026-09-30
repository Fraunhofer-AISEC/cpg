<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { TranslationUnitJSON, NodeJSON, ConceptSuggestionItem } from '$lib/types';
  import { TabNavigation } from '$lib/components/navigation';
  import { CollapsiblePanel } from '$lib/components/ui';
  import { NodeTable, NodeOverlays, FindingOverlay, ConceptChecklist } from '$lib/components/analysis';
  import { flattenNodes } from '$lib/flatten';
  import { ScrollViewport, remInPx } from '$lib/scroll-viewport.svelte';
  import CodeLines from './CodeLines.svelte';
  import Highlight from 'svelte-highlight';
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
    /** The nodes referenced by the suggestions, by ID, which may be nested anywhere in the unit */
    suggestionNodes?: Map<string, NodeJSON>;
    onApplySuggestions?: (accepted: ConceptSuggestionItem[]) => void;
  }

  let { translationUnit, astNodes, overlayNodes, conceptGroups, highlightLine, finding, findingKind, headerActions, nodePanelCollapsed = $bindable(false), onClose, suggestions = $bindable([]), suggestionNodes, onApplySuggestions }: Props = $props();

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

  let activeSuggestionNodeId = $state<string | null>(null);

  $effect(() => {
    if (!tabs.some(t => t.id === activeTab)) {
      activeTab = tabs[0]?.id ?? 'astNodes';
    }
  });

  // Auto-switch to suggestions tab on transition from 0 -> >0 suggestions
  let prevSuggestionCount = 0;
  $effect(() => {
    const count = suggestions.length;
    if (prevSuggestionCount === 0 && count > 0) {
      activeTab = 'suggestions';
    }
    prevSuggestionCount = count;
  });

  function scrollToLine(line: number, behavior: ScrollBehavior = 'smooth') {
    if (!codeContainerElement || typeof window === 'undefined') return;
    const top = (offsetTop + (line - 3) * lineHeight) * remInPx();
    codeContainerElement.scrollTo({ top: Math.max(0, top), behavior });
  }

  // Resolve a nodeId to its line range via suggestionNodes, astNodes or overlayNodes
  function findNodeById(nodeId: string): NodeJSON | undefined {
    // A suggested node may be located in another file than the one that is shown
    const suggestionNode = suggestionNodes?.get(nodeId);
    return (
      (suggestionNode?.translationUnitId === translationUnit.id ? suggestionNode : undefined) ??
      astNodes.find(n => n.id === nodeId) ??
      overlayNodes.find(n => n.id === nodeId)
    );
  }

  function linesForNodeId(nodeId: string): number[] {
    const node = findNodeById(nodeId);
    if (!node) return [];
    const lines: number[] = [];
    for (let l = node.startLine; l <= node.endLine; l++) lines.push(l - 1); // 0-based
    return lines;
  }

  // Lines to highlight for the currently active suggestion node (click-focused)
  const activeNodeLines = $derived.by(() => {
    if (!activeSuggestionNodeId) return [];
    return linesForNodeId(activeSuggestionNodeId);
  });

  // Combined highlight lines for the code viewer
  const allHighlightLines = $derived.by(() => {
    if (activeTab === 'suggestions') {
      return activeNodeLines;
    }
    return highlightLine ? [highlightLine - 1] : [];
  });

  const lineHeight = 1.5;
  const charWidth = 0.60015625;
  const offsetTop = 1;
  const baseOffsetLeft = 2.4;

  const codeLines = $derived(translationUnit.code.split('\n'));
  const totalLines = $derived(codeLines.length);
  const lineNumberWidth = $derived(Math.ceil(Math.log10(totalLines + 1)));
  const offsetLeft = $derived(baseOffsetLeft + lineNumberWidth * charWidth);
  const maxColumns = $derived.by(() => {
    let max = 0;
    for (const line of codeLines) {
      // Tabs are rendered with a width of 8 characters
      const tabs = line.split('\t').length - 1;
      max = Math.max(max, line.length + tabs * 7);
    }
    return max;
  });

  // Only the lines (and overlays) around the visible part of the file are rendered
  const codeViewport = new ScrollViewport();
  $effect(() => {
    if (codeContainerElement) return codeViewport.track(codeContainerElement);
  });
  const visibleLines = $derived.by(() => {
    const remPx = remInPx();
    return codeViewport.range(lineHeight * remPx, totalLines, 40, offsetTop * remPx);
  });

  // Scroll to focused suggestion node
  $effect(() => {
    if (activeSuggestionNodeId && codeContainerElement) {
      const node = findNodeById(activeSuggestionNodeId);
      if (node) scrollToLine(node.startLine);
    }
  });

  $effect(() => {
    if (highlightLine && codeContainerElement) {
      const line = highlightLine;
      setTimeout(() => scrollToLine(line, 'auto'), 300);
    }
  });
</script>

<div class="flex h-full w-full overflow-hidden rounded-[inherit]">
  <!-- Code display -->
  <div class="flex flex-1 flex-col overflow-hidden">
    <div class="flex shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4 py-2">
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

    <div class="relative flex-1 overflow-auto" style="transform: translateZ(0);" bind:this={codeContainerElement}>
      <div class="relative inline-block min-w-full w-max align-top">
        <div class="font-mono">
          <Highlight language={getLanguage(translationUnit.name)} code={translationUnit.code} let:highlighted>
            <CodeLines
              {highlighted}
              start={visibleLines.start}
              end={visibleLines.end}
              highlightedLines={allHighlightLines}
              {maxColumns}
              {lineHeight}
              {charWidth}
              {offsetTop}
              {offsetLeft}
            />
          </Highlight>
        </div>

        {#if finding && highlightLine}
          <FindingOverlay {finding} kind={findingKind} line={highlightLine} {lineHeight} {offsetTop} />
        {/if}

        {#if activeTab !== 'suggestions'}
          <NodeOverlays
            {nodes}
            {codeLines}
            startLine={visibleLines.start}
            endLine={visibleLines.end}
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
  </div>

  <!-- Node information panel -->
  <CollapsiblePanel title="Nodes" side="right" bind:collapsed={nodePanelCollapsed}>
    <div class="flex h-full flex-col overflow-hidden">
      <div class="shrink-0 bg-white">
        <TabNavigation {tabs} {activeTab} onTabChange={(id) => (activeTab = id)} />
      </div>
      {#if activeTab === 'suggestions'}
        <div class="flex-1 overflow-auto p-4">
          <ConceptChecklist
            bind:items={suggestions}
            {onApplySuggestions}
            onHighlightNode={(nodeId) => (activeSuggestionNodeId = nodeId)}
          />
        </div>
      {:else}
        <!-- NodeTable is virtualized and needs to be its own scroll container -->
        <div class="min-h-0 flex-1 px-2">
          <NodeTable
            {nodes}
            bind:highlightedNode
            nodeClick={(node) => scrollToLine(node.startLine)}
          />
        </div>
      {/if}
    </div>
  </CollapsiblePanel>
</div>