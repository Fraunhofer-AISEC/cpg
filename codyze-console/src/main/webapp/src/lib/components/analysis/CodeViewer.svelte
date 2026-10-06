<script lang="ts">
  import type { Snippet } from 'svelte';
  import type { TranslationUnitJSON, NodeJSON, ConceptSuggestionItem } from '$lib/types';
  import { TabNavigation } from '$lib/components/navigation';
  import { CollapsiblePanel } from '$lib/components/ui';
  import { NodeTable, NodeOverlays, FindingOverlay, ConceptChecklist } from '$lib/components/analysis';
  import NodeInspector from './inspector/NodeInspector.svelte';
  import CodeAnnotations from './CodeAnnotations.svelte';
  import { type Lens, lenses, getAnnotations } from '$lib/annotations';
  import type { FileAnnotationsJSON } from '$lib/types';
  import AddConceptDialog from '../forms/AddConceptDialog.svelte';
  import type { NodeDetailsJSON, NodeRefJSON } from '$lib/types';
  import type { FlattenedNode } from '$lib/flatten';
  import { getNodeDetails, getNodeDetailsAt, clearNodeDetailsCache } from '$lib/nodeDetails';
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
    /** The component of the unit; enables selecting nodes by clicking into the code */
    componentName?: string;
    /** The node to inspect initially, e.g. from a deep link */
    selectedNodeId?: string;
    /** Called when a node in another file is selected in the inspector */
    onNavigateToNode?: (ref: NodeRefJSON) => void;
    /** Additional actions for the inspected node, e.g. asking the agent about it */
    nodeActions?: Snippet<[NodeDetailsJSON]>;
    /** Where the panel with the inspector and the node lists is shown */
    panelPosition?: 'right' | 'bottom';
  }

  let { translationUnit, astNodes, overlayNodes, conceptGroups, highlightLine, finding, findingKind, headerActions, nodePanelCollapsed = $bindable(false), onClose, suggestions = $bindable([]), suggestionNodes, onApplySuggestions, componentName, selectedNodeId, onNavigateToNode, nodeActions, panelPosition = 'right' }: Props = $props();

  // The inspector is only available with a component, which never changes for a viewer
  // svelte-ignore state_referenced_locally
  let activeTab = $state(componentName ? 'inspector' : 'astNodes');
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
    ...(componentName ? [{ id: 'inspector', label: 'Inspector' }] : []),
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

  // The node shown in the inspector
  let inspected = $state<NodeDetailsJSON | null>(null);
  let inspectorLoading = $state(false);
  let inspectorError = $state<string | null>(null);
  let inspectRequest = 0;

  async function inspect(load: () => Promise<NodeDetailsJSON | null>, scroll = false) {
    const request = ++inspectRequest;
    inspectorLoading = true;
    inspectorError = null;
    activeTab = 'inspector';
    nodePanelCollapsed = false;
    try {
      const details = await load();
      if (request !== inspectRequest) return;
      if (details) {
        inspected = details;
        if (scroll && details.node.translationUnitId === translationUnit.id) {
          scrollToLine(details.node.startLine);
        }
      }
    } catch (e) {
      if (request === inspectRequest) inspectorError = e instanceof Error ? e.message : String(e);
    } finally {
      if (request === inspectRequest) inspectorLoading = false;
    }
  }

  function selectRef(ref: NodeRefJSON) {
    if (ref.translationUnitId && ref.translationUnitId !== translationUnit.id && onNavigateToNode) {
      onNavigateToNode(ref);
    } else {
      inspect(() => getNodeDetails(ref.id), true);
    }
  }

  // Inspect the initially selected node, and again whenever a deep link selects another one
  $effect(() => {
    const id = selectedNodeId;
    if (componentName && id) inspect(() => getNodeDetails(id));
  });

  function handleCodeClick(event: MouseEvent) {
    if (!componentName) return;
    // Do not interfere with selecting text
    if (window.getSelection()?.toString()) return;
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const rem = remInPx();
    const line = Math.floor(((event.clientY - rect.top) / rem - offsetTop) / lineHeight) + 1;
    const column = Math.floor(((event.clientX - rect.left) / rem - offsetLeft) / charWidth);
    if (line < 1 || line > totalLines || column < 0) return;
    const component = componentName;
    inspect(() => getNodeDetailsAt(component, translationUnit.id, line, column));
  }

  // Annotations of the file (function key figures, call status, concepts), loaded per unit
  let annotations = $state.raw<FileAnnotationsJSON | null>(null);
  let lens = $state<Lens>('callers');

  async function loadAnnotations(component: string, unitId: string) {
    const result = await getAnnotations(component, unitId);
    if (unitId === translationUnit.id) annotations = result;
  }

  $effect(() => {
    annotations = null;
    if (componentName) loadAnnotations(componentName, translationUnit.id);
  });

  // The summary of the file in the header
  const fileSummary = $derived.by(() => {
    if (!annotations) return null;
    const external = annotations.calls.filter((c) => c.status === 'EXTERNAL');
    const counts = new Map<string, number>();
    for (const call of external) counts.set(call.name, (counts.get(call.name) ?? 0) + 1);
    const topExternal = [...counts.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 4)
      .map(([name]) => name);
    return {
      functions: annotations.functions.length,
      external: external.length,
      topExternal,
      unresolved: annotations.calls.filter((c) => c.status === 'UNRESOLVED').length,
      concepts: annotations.concepts.length
    };
  });

  // Box around the inspected node, if it is in this file
  const selectionBox = $derived.by(() => {
    const node = inspected?.node;
    if (!node || node.translationUnitId !== translationUnit.id || node.startLine < 1) return null;
    const singleLine = node.startLine === node.endLine;
    const width = singleLine
      ? (node.endColumn - node.startColumn) * charWidth
      : codeLines
          .slice(node.startLine - 1, node.endLine)
          .reduce((max, line) => Math.max(max, line.length), 0) * charWidth;
    return {
      top: (node.startLine - 1) * lineHeight + offsetTop,
      left: (singleLine ? node.startColumn * charWidth : 0) + offsetLeft,
      width: Math.max(width, 0.5),
      height: (node.endLine - node.startLine + 1) * lineHeight
    };
  });

  // Adding a concept to the inspected node
  let showConceptDialog = $state(false);
  const conceptTarget = $derived<FlattenedNode | null>(
    inspected && componentName
      ? {
          id: inspected.node.id,
          type: inspected.node.type,
          name: inspected.node.name,
          code: inspected.node.code,
          startLine: inspected.node.startLine,
          startColumn: inspected.node.startColumn,
          endLine: inspected.node.endLine,
          endColumn: inspected.node.endColumn,
          astChildren: [],
          prevDFG: [],
          nextDFG: [],
          depth: 0,
          componentName,
          unitId: translationUnit.id
        }
      : null
  );
  let conceptDialogWasOpen = false;
  $effect(() => {
    // Concepts change the inspected node, so reload it once the dialog is closed
    if (conceptDialogWasOpen && !showConceptDialog && inspected) {
      clearNodeDetailsCache();
      const id = inspected.node.id;
      inspect(() => getNodeDetails(id));
      if (componentName) loadAnnotations(componentName, translationUnit.id);
    }
    conceptDialogWasOpen = showConceptDialog;
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

  const code = $derived(translationUnit.code ?? '');
  const codeLines = $derived(code.split('\n'));
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

<div class="flex h-full w-full overflow-hidden rounded-[inherit]" class:flex-col={panelPosition === 'bottom'}>
  <!-- Code display -->
  <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
    <div class="flex shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4 py-2">
      <div class="flex min-w-0 items-center gap-3">
        <div class="shrink-0 font-mono text-xs text-gray-500">{translationUnit.name}</div>
        {#if fileSummary}
          <div class="flex min-w-0 items-center gap-2 truncate text-[11px] text-gray-500">
            <span>{fileSummary.functions} functions</span>
            {#if fileSummary.external > 0}
              <span
                class="text-orange-600"
                title="Calls to functions that are not part of the analysed code"
              >· {fileSummary.external} external calls ({fileSummary.topExternal.join(', ')})</span>
            {/if}
            {#if fileSummary.unresolved > 0}
              <span class="text-red-600" title="Calls whose target the analysis could not determine">
                · ⚠ {fileSummary.unresolved} unresolved
              </span>
            {/if}
            {#if fileSummary.concepts > 0}
              <span class="text-purple-600">· {fileSummary.concepts} concepts</span>
            {/if}
          </div>
        {/if}
      </div>
      <div class="flex shrink-0 items-center gap-2">
        {#if annotations}
          <label class="flex items-center gap-1 text-[11px] text-gray-500" title="Color the functions by">
            Lens
            <select
              bind:value={lens}
              class="rounded border border-gray-300 bg-white py-0.5 pr-6 pl-1.5 text-[11px] text-gray-700"
            >
              {#each lenses as option (option.id)}
                <option value={option.id}>{option.label}</option>
              {/each}
            </select>
          </label>
        {/if}
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
      <!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
      <div
        class="relative inline-block min-w-full w-max align-top"
        class:cursor-pointer={!!componentName}
        onclick={handleCodeClick}
      >
        <div class="font-mono">
          <Highlight language={getLanguage(translationUnit.name)} {code} let:highlighted>
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

        {#if annotations}
          <CodeAnnotations
            {annotations}
            {lens}
            {codeLines}
            startLine={visibleLines.start}
            endLine={visibleLines.end}
            {lineHeight}
            {charWidth}
            {offsetTop}
            {offsetLeft}
            onInspect={(id) => inspect(() => getNodeDetails(id), true)}
          />
        {/if}

        {#if selectionBox}
          <div
            class="pointer-events-none absolute z-20 rounded-sm border-2 border-blue-600 bg-blue-500/10"
            style:top="{selectionBox.top}rem"
            style:left="{selectionBox.left}rem"
            style:width="{selectionBox.width}rem"
            style:height="{selectionBox.height}rem"
          ></div>
        {/if}

        {#if activeTab === 'astNodes' || activeTab === 'overlayNodes'}
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
            addConceptOnClick={!componentName}
          />
        {/if}
      </div>
    </div>
  </div>

  <!-- Node information panel -->
  {#snippet panelContent()}
    {#if activeTab === 'inspector'}
      <div class="min-h-0 flex-1 overflow-y-auto">
        <NodeInspector
          details={inspected}
          loading={inspectorLoading}
          error={inspectorError}
          onSelect={selectRef}
        >
          {#snippet actions()}
            {#if inspected && nodeActions}
              {@render nodeActions(inspected)}
            {/if}
            {#if conceptTarget && conceptGroups?.length}
              <button
                type="button"
                class="rounded border border-gray-300 px-2 py-0.5 text-[11px] text-gray-700 hover:bg-gray-50"
                onclick={() => (showConceptDialog = true)}
              >
                Add concept…
              </button>
            {/if}
          {/snippet}
        </NodeInspector>
      </div>
    {:else if activeTab === 'suggestions'}
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
          nodeClick={(node) =>
            componentName ? inspect(() => getNodeDetails(node.id), true) : scrollToLine(node.startLine)}
        />
      </div>
    {/if}
  {/snippet}

  {#if panelPosition === 'bottom'}
    <div
      class="flex min-h-0 shrink-0 flex-col border-t border-gray-200 bg-white"
      style:height={nodePanelCollapsed ? undefined : '42%'}
    >
      <div class="flex shrink-0 items-center">
        <div class="min-w-0 flex-1">
          <TabNavigation
            {tabs}
            {activeTab}
            onTabChange={(id) => {
              activeTab = id;
              nodePanelCollapsed = false;
            }}
          />
        </div>
        <button
          type="button"
          class="mr-2 rounded px-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
          onclick={() => (nodePanelCollapsed = !nodePanelCollapsed)}
          aria-label={nodePanelCollapsed ? 'Show panel' : 'Hide panel'}
        >
          {nodePanelCollapsed ? '▴' : '▾'}
        </button>
      </div>
      {#if !nodePanelCollapsed}
        {@render panelContent()}
      {/if}
    </div>
  {:else}
    <CollapsiblePanel title="Nodes" side="right" bind:collapsed={nodePanelCollapsed}>
      <div class="flex h-full flex-col overflow-hidden">
        <div class="shrink-0 bg-white">
          <TabNavigation {tabs} {activeTab} onTabChange={(id) => (activeTab = id)} />
        </div>
        {@render panelContent()}
      </div>
    </CollapsiblePanel>
  {/if}
</div>

{#if showConceptDialog && conceptTarget}
  <AddConceptDialog bind:showDialog={showConceptDialog} node={conceptTarget} conceptGroups={conceptGroups || []} />
{/if}