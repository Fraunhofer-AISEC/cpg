<script lang="ts">
  import { untrack } from 'svelte';
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import MessageInput from './MessageInput.svelte';
  import SessionBar from './SessionBar.svelte';
  import ToolResultBlock from './widgets/ToolResultBlock.svelte';
  import { CodeViewer, ConceptChecklist, FileTree } from '$lib/components/analysis';
  import Outline from '$lib/components/analysis/Outline.svelte';
  import NodeInspector from '$lib/components/analysis/inspector/NodeInspector.svelte';
  import { LoadingSpinner } from '$lib/components/ui';
  import { agentSession } from '$lib/stores/agentSession.svelte';
  import { CodeFocus, type CodeLocation } from '$lib/stores/codeFocus.svelte';
  import { hasModifier, isTyping } from '$lib/utils/keyboard';
  import { EditorTabs } from '$lib/stores/editorTabs.svelte';
  import { relativePath } from '$lib/utils/paths';
  import { clearNodeDetailsCache, getNodeDetails } from '$lib/nodeDetails';
  import { getConceptCounts } from '$lib/annotations';
  import type {
    NodeJSON,
    AnalysisResultJSON,
    TranslationUnitJSON,
    ChatMessage,
    ComponentJSON,
    ConceptSuggestionItem,
    Model,
    NodeDetailsJSON,
    NodeRefJSON,
    FileAnnotationsJSON
  } from '$lib/types';

  let selectedNode = $state<NodeJSON | null>(null);
  let selectedTranslationUnit = $state<TranslationUnitJSON | null>(null);
  let selectedComponentName = $state<string | null>(null);
  // The selected unit including its code, which is not part of the units in analysisResult
  let openedUnit = $state.raw<TranslationUnitJSON | null>(null);

  // The inspected node, shared with the code viewer
  const focus = new CodeFocus();

  // The files opened in tabs, with their code and scroll positions, so switching between tabs is
  // instant and keeps the place in each file. Both are only kept while the tab is open
  const tabs = new EditorTabs();
  // They are caches that are never rendered, so they are not reactive on purpose
  // eslint-disable-next-line svelte/prefer-svelte-reactivity
  const unitCode = new Map<string, TranslationUnitJSON>();
  // eslint-disable-next-line svelte/prefer-svelte-reactivity
  const scrollPositions = new Map<string, number>();

  async function loadUnit(componentName: string, tuId: string) {
    const unit: TranslationUnitJSON | null = await fetch(
      `/api/component/${componentName}/translation-unit/${tuId}`
    )
      .then((r) => (r.ok ? r.json() : null))
      .catch(() => null);
    if (unit && tabs.tabs.some((t) => t.id === tuId)) unitCode.set(tuId, unit);
    // Ignore the response if another unit was selected in the meantime
    if (selectedTranslationUnit?.id === tuId) openedUnit = unit;
  }

  // Shows a file in its tab, opening the tab and loading the code if needed
  function showUnit(unit: TranslationUnitJSON) {
    for (const closed of tabs.open(unit)) forgetUnit(closed.id);
    selectedTranslationUnit = unit;
    const comp = findComponentForTu(unit.id);
    selectedComponentName = comp?.name ?? null;
    const cached = unitCode.get(unit.id);
    if (cached) openedUnit = cached;
    else if (comp) loadUnit(comp.name, unit.id);
  }

  function forgetUnit(unitId: string) {
    unitCode.delete(unitId);
    scrollPositions.delete(unitId);
  }

  function closeTab(unitId: string) {
    const next = tabs.close(unitId);
    forgetUnit(unitId);
    if (unitId !== selectedTranslationUnit?.id) return;
    if (next) {
      handleFileSelect(next);
    } else {
      selectedTranslationUnit = null;
      selectedNode = null;
      openedUnit = null;
    }
  }

  // The name shown in a tab, with the folder if another tab has a file with the same name
  function tabLabel(unit: TranslationUnitJSON): { name: string; folder?: string } {
    const duplicate = tabs.tabs.some((t) => t.id !== unit.id && t.name === unit.name);
    if (!duplicate) return { name: unit.name };
    const parts = unit.path.split('/');
    return { name: unit.name, folder: parts[parts.length - 2] };
  }

  function findTranslationUnit(node: NodeJSON): TranslationUnitJSON | null {
    if (!analysisResult) return null;
    if (node.translationUnitId) {
      for (const component of analysisResult.components) {
        const tu = component.translationUnits.find((tu) => tu.id === node.translationUnitId);
        if (tu) return tu;
      }
    }
    if (node.fileName) {
      for (const component of analysisResult.components) {
        const tu = component.translationUnits.find((tu) => tu.name.includes(node.fileName!));
        if (tu) return tu;
      }
    }
    if ((node as any).componentName) {
      const component = analysisResult.components.find(
        (c) => c.name === (node as any).componentName
      );
      if (component?.translationUnits.length) return component.translationUnits[0];
    }
    return null;
  }

  function handleNodeClick(node: NodeJSON) {
    const tu = findTranslationUnit(node);
    if (!tu) return;
    selectedNode = node;
    showUnit(tu);
  }

  // Opens the file of a node selected in the inspector and inspects the node there
  function handleNavigateToNode(ref: NodeRefJSON) {
    handleNodeClick({
      ...ref,
      code: ref.code,
      astChildren: [],
      prevDFG: [],
      nextDFG: []
    });
  }

  // Questions about the inspected node, which are put into the input so they can be adjusted
  const nodeQuestions: { label: string; question: (where: string) => string }[] = [
    {
      label: 'Explain',
      question: (where) => `Explain what ${where} does and why it matters for security.`
    },
    {
      label: 'Where does the value come from?',
      question: (where) =>
        `Where does the value of ${where} come from? Follow the dataflow backwards to its origins (e.g. user input, files, network, constants).`
    },
    {
      label: 'Reachable from outside?',
      question: (where) =>
        `Can ${where} be reached from an entry point of the program? Show the call path.`
    }
  ];

  // How a node is referred to in questions to the agent
  function describeNode(n: NodeRefJSON): string {
    return `the ${n.type} \`${n.code || n.name}\` (node ID ${n.id}, ${n.fileName}:${n.startLine})`;
  }

  function askAboutNode(details: NodeDetailsJSON, question: (where: string) => string) {
    onMessageChange(question(describeNode(details.node)));
  }

  // Selects a node in the inspector: nodes in the open file are revealed in it, others open their file
  function selectRef(ref: NodeRefJSON) {
    if (ref.translationUnitId && ref.translationUnitId !== selectedTranslationUnit?.id) {
      handleNavigateToNode(ref);
    } else {
      showInspector();
      focus.inspect(() => getNodeDetails(ref.id), true);
    }
  }

  function handleFileSelect(unit: TranslationUnitJSON, record = true) {
    if (record) focus.record({ unitId: unit.id, label: unit.name });
    selectedNode = null;
    showUnit(unit);
  }

  function findUnitById(unitId: string): TranslationUnitJSON | null {
    for (const component of analysisResult?.components ?? []) {
      const unit = component.translationUnits.find((tu) => tu.id === unitId);
      if (unit) return unit;
    }
    return null;
  }

  // Shows a location of the history, without recording it again
  function goTo(location: CodeLocation | null) {
    if (!location) return;
    const unit = findUnitById(location.unitId);
    if (!unit) return;
    if (unit.id !== selectedTranslationUnit?.id) handleFileSelect(unit, false);
    const nodeId = location.nodeId;
    if (nodeId) {
      showInspector();
      focus.inspect(() => getNodeDetails(nodeId), true, undefined, false);
    }
  }

  const goBack = () => goTo(focus.back());
  const goForward = () => goTo(focus.forward());

  function handleKeydown(event: KeyboardEvent) {
    if (isTyping(event)) return;
    if (event.altKey && !hasModifier(event) && event.key === 'ArrowLeft') {
      event.preventDefault();
      goBack();
    } else if (event.altKey && !hasModifier(event) && event.key === 'ArrowRight') {
      event.preventDefault();
      goForward();
    }
  }

  function handleComponentSelect(name: string) {
    const unit = analysisResult?.components.find((c) => c.name === name)?.translationUnits[0];
    if (unit) handleFileSelect(unit);
  }

  function findComponentForTu(tuId: string): ComponentJSON | null {
    if (!analysisResult) return null;
    return (
      analysisResult.components.find((c) => c.translationUnits.some((tu) => tu.id === tuId)) ?? null
    );
  }

  interface Props {
    messages: ChatMessage[];
    currentMessage: string;
    isLoading: boolean;
    streamingContent: string;
    isThinking: boolean;
    models?: Model[];
    selectedModel?: Model | null;
    analysisResult?: AnalysisResultJSON | null;
    suggestions?: ConceptSuggestionItem[];
    onApplySuggestions?: (accepted: ConceptSuggestionItem[]) => Promise<void> | void;
    /** Sends the current message, with a description of the selected node as context, if any */
    onSendMessage: (context?: string) => void;
    onReset: () => void;
    onMessageChange: (message: string) => void;
    onModelSelect?: (model: Model) => void;
    onPromptSelect?: (name: string, args: Record<string, string>) => void;
  }

  const SUGGEST_LLM_CONCEPTS_TOOL = 'cpg_suggest_llm_concepts_and_operations';

  let {
    messages,
    currentMessage,
    isLoading,
    streamingContent,
    isThinking,
    models = [],
    selectedModel = null,
    analysisResult,
    suggestions = $bindable([]),
    onApplySuggestions,
    onSendMessage,
    onReset,
    onMessageChange,
    onModelSelect,
    onPromptSelect
  }: Props = $props();

  // The component of the open file, or of the last open file if all tabs were closed
  const selectedComponent: ComponentJSON | null = $derived(
    analysisResult?.components.find((c) => c.name === selectedComponentName) ?? null
  );

  async function handleApplyAndReload(accepted: ConceptSuggestionItem[]) {
    await onApplySuggestions?.(accepted);
    // The applied concepts change the details of the nodes they are attached to, the annotations
    // and the counts in the file tree
    clearNodeDetailsCache();
    focus.invalidate();
  }

  // The number of concepts per file of the selected component, shown in the file tree
  let conceptCounts = $state.raw<Map<string, number>>(new Map());

  async function loadConceptCounts(component: string) {
    const counts = await getConceptCounts(component);
    if (component === selectedComponentName) conceptCounts = counts;
  }

  $effect(() => {
    void focus.revision;
    if (selectedComponentName) loadConceptCounts(selectedComponentName);
  });

  // Selects and reveals a node referenced by a concept suggestion, opening its file if needed. The
  // agent tab stays open, since the suggestions are shown there
  function revealSuggestedNode(nodeId: string | null) {
    if (!nodeId) return;
    const node = suggestionNodes.get(nodeId);
    if (node && node.translationUnitId !== selectedTranslationUnit?.id) {
      const tu = findTranslationUnit(node);
      if (tu) handleFileSelect(tu);
    }
    if (contextTab !== 'inspector' || contextCollapsed) markInspectorUnseen();
    focus.inspect(() => getNodeDetails(nodeId), true);
  }

  // The concept suggestions are attached to the last tool result that suggested a concept
  const suggestionAnchorIndex = $derived(
    suggestions.length > 0
      ? messages.findLastIndex((m) => m.toolResult?.toolName === SUGGEST_LLM_CONCEPTS_TOOL)
      : -1
  );

  // The context column next to the code, with the inspector and the agent
  type ContextTab = 'inspector' | 'agent';
  let contextTab = $state<ContextTab>('agent');
  let contextCollapsed = $state(false);
  // Whether the inspector shows a node that has not been seen because the agent tab stayed open
  let inspectorUnseen = $state(false);
  // The same while the column is collapsed. This is only shown on the collapsed strip and does not
  // mark the inspector tab after the column was opened again
  let collapsedUnseen = $state(false);
  let sidebarOpen = $state(true);

  // The views of the sidebar, switched in the activity bar
  type SidebarView = 'files' | 'outline';
  let sidebarView = $state<SidebarView>('files');
  const sidebarViews: { id: SidebarView; label: string; icon: string }[] = [
    {
      id: 'files',
      label: 'Files',
      icon: 'M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 01-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 011.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 00-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 01-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 00-3.375-3.375h-1.5a1.125 1.125 0 01-1.125-1.125v-1.5a3.375 3.375 0 00-3.375-3.375H9.75'
    },
    {
      id: 'outline',
      label: 'Outline',
      icon: 'M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z'
    }
  ];

  // Like in VS Code, clicking the active view hides the sidebar
  function toggleSidebar(view: SidebarView) {
    if (sidebarOpen && sidebarView === view) {
      sidebarOpen = false;
    } else {
      sidebarView = view;
      sidebarOpen = true;
    }
  }

  // A file to reveal in the file tree, e.g. after clicking its path in the breadcrumb
  let treeReveal = $state<{ unitId: string; request: number } | null>(null);

  function revealInTree(unitId: string) {
    sidebarView = 'files';
    sidebarOpen = true;
    treeReveal = { unitId, request: (treeReveal?.request ?? 0) + 1 };
  }

  // The segments of the breadcrumb above the code: the folders and the file of the open unit
  const breadcrumbPath = $derived(
    openedUnit ? relativePath(openedUnit, selectedComponent?.topLevel).split('/') : []
  );

  // The function containing the inspected node in the open file, or the node if it is a function
  const breadcrumbFunction = $derived.by((): NodeRefJSON | null => {
    const details = focus.details;
    if (!details || details.node.translationUnitId !== openedUnit?.id) return null;
    return (
      details.enclosingFunction ?? (details.node.type.includes('Function') ? details.node : null)
    );
  });

  // The annotations of the open file, loaded by the code viewer and shown in the outline
  let fileAnnotations = $state.raw<FileAnnotationsJSON | null>(null);

  // Switches to the inspector after the user inspected a node, unless the agent is working: then
  // its tab stays open and the inspector tab is only marked
  function showInspector() {
    if (isLoading && contextTab === 'agent') {
      markInspectorUnseen();
      return;
    }
    openTab('inspector');
  }

  function markInspectorUnseen() {
    if (contextCollapsed) collapsedUnseen = true;
    else inspectorUnseen = true;
  }

  function showAgent() {
    openTab('agent');
  }

  function openTab(tab: ContextTab) {
    contextTab = tab;
    contextCollapsed = false;
    collapsedUnseen = false;
  }

  $effect(() => {
    if (contextTab === 'inspector' && !contextCollapsed) inspectorUnseen = false;
  });

  // The width of the context column in px, resizable and remembered
  const contextWidthKey = 'codyze-agent-context-width';
  let contextWidth = $state(loadContextWidth());

  function loadContextWidth(): number {
    try {
      const stored = Number(localStorage.getItem(contextWidthKey));
      if (stored >= 280) return stored;
    } catch {
      // Storage is not available, e.g. during SSR or in a private window
    }
    return 360;
  }

  function startResize(event: PointerEvent) {
    event.preventDefault();
    const startX = event.clientX;
    const startWidth = contextWidth;
    const move = (e: PointerEvent) => {
      const max = Math.max(320, window.innerWidth * 0.6);
      contextWidth = Math.round(Math.min(max, Math.max(280, startWidth + startX - e.clientX)));
    };
    const stop = () => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', stop);
      document.body.style.removeProperty('cursor');
      try {
        localStorage.setItem(contextWidthKey, String(contextWidth));
      } catch {
        // Not remembering the width is fine
      }
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', stop);
    document.body.style.cursor = 'col-resize';
  }

  // The selected node is attached to questions as context, unless the user removed it
  let dismissedContextId = $state<string | null>(null);
  const contextNode = $derived(
    focus.details && focus.details.node.id !== dismissedContextId ? focus.details.node : null
  );

  function send() {
    showAgent();
    onSendMessage(contextNode ? describeNode(contextNode) : undefined);
  }

  // The code is the main view, so a file is open from the start (but not after closing all tabs)
  let openedInitially = false;
  $effect(() => {
    if (openedInitially || selectedTranslationUnit || !analysisResult) return;
    openedInitially = true;
    const first = analysisResult.components
      .flatMap((c) => c.translationUnits)
      .sort((a, b) => a.path.localeCompare(b.path))[0];
    if (first) handleFileSelect(first);
  });

  // Questions to start with, as long as the chat is empty
  const starterQuestions = [
    'What does this project do, and where are its entry points?',
    'Which functions handle external input (network, files, arguments)?',
    'Where is cryptography used, and how?',
    'Which memory operations could be dangerous (memcpy, strcpy, …)?'
  ];

  function ask(question: string) {
    onMessageChange(question);
    send();
  }
  let displayContent = $derived(streamingContent.trim().length > 0 ? streamingContent : '');
  // The nodes referenced by the suggestions, by ID. They can be nested anywhere in a translation
  // unit, so they are not necessarily part of astNodes
  let suggestionNodes = $state.raw<Map<string, NodeJSON>>(new Map());
  const tusWithSuggestions = $derived(
    new Set(
      [...suggestionNodes.values()].flatMap((n) =>
        n.translationUnitId ? [n.translationUnitId] : []
      )
    )
  );

  // The node IDs referenced by the suggestions. As a string, this only changes when the IDs change,
  // and not when a suggestion is accepted or rejected (which replaces the suggestion objects)
  const suggestionNodeIdsKey = $derived(
    [
      ...new Set(
        suggestions.flatMap((s) => [
          s.suggestion.nodeId,
          ...s.operations.map((o) => o.operation.nodeId)
        ])
      )
    ]
      .sort()
      .join(',')
  );

  // When suggestions arrive, load the referenced nodes to know their files and lines
  $effect(() => {
    const key = suggestionNodeIdsKey;
    if (!key || !analysisResult) {
      suggestionNodes = new Map();
      return;
    }
    loadSuggestionNodes(key);
  });

  async function loadSuggestionNodes(key: string) {
    const nodes: NodeJSON[] = await fetch('/api/nodes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(key.split(','))
    })
      .then((r) => (r.ok ? r.json() : []))
      .catch(() => []);

    // Ignore the response if the suggestions changed in the meantime
    if (key !== suggestionNodeIdsKey) return;
    suggestionNodes = new Map(nodes.map((n) => [n.id, n]));
  }

  // Show the first translation unit with suggestions when they arrive, unless the open one has some
  $effect(() => {
    if (tusWithSuggestions.size === 0 || !analysisResult) return;
    // Only react to new suggestions, not to the user opening another file
    const current = untrack(() => selectedTranslationUnit);
    if (current && tusWithSuggestions.has(current.id)) return;
    {
      for (const comp of analysisResult.components) {
        const tu = comp.translationUnits.find((tu) => tusWithSuggestions.has(tu.id));
        if (tu) {
          handleFileSelect(tu);
          return;
        }
      }
    }
  });

  // The messages are only rendered while the agent panel is open
  let messagesContainer = $state<HTMLDivElement>();
  let shouldAutoScroll = $state(true);

  function isNearBottom(): boolean {
    if (!messagesContainer) return true;
    const threshold = 150;
    const position = messagesContainer.scrollTop + messagesContainer.clientHeight;
    return messagesContainer.scrollHeight - position < threshold;
  }

  function handleScroll() {
    shouldAutoScroll = isNearBottom();
  }

  function scrollToBottom() {
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  $effect(() => {
    messages.length;
    streamingContent;
    if (shouldAutoScroll) {
      requestAnimationFrame(() => scrollToBottom());
    }
  });

  const fileName = (path?: string) => path?.split('/').pop() ?? '';

  let expandedReasoning = $state<Set<string>>(new Set());

  function toggleReasoning(id: string) {
    const next = new Set(expandedReasoning);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    expandedReasoning = next;
  }
</script>

<!-- Questions about the inspected node, which put the question into the agent's input -->
{#snippet nodeActions(details: NodeDetailsJSON)}
  {#each nodeQuestions as q (q.label)}
    <button
      type="button"
      class="rounded border border-purple-200 bg-purple-50 px-2 py-0.5 text-[11px] text-purple-700 hover:bg-purple-100 disabled:opacity-50"
      disabled={isLoading || !selectedModel}
      title="Put this question into the agent's input"
      onclick={() => {
        showAgent();
        askAboutNode(details, q.question);
      }}
    >
      {q.label}
    </button>
  {/each}
{/snippet}

{#snippet workingDot()}
  <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-purple-500" title="The agent is working"
  ></span>
{/snippet}

{#snippet unseenDot()}
  <span class="h-1.5 w-1.5 rounded-full bg-blue-500" title="A new node was inspected"></span>
{/snippet}

<!-- Back and forward through the visited locations, like in an editor -->
{#snippet historyButtons()}
  {@const back = focus.history[focus.historyIndex - 1]}
  {@const forward = focus.history[focus.historyIndex + 1]}
  <div class="flex shrink-0 items-center">
    <button
      type="button"
      class="rounded px-1 text-sm leading-5 text-gray-500 hover:bg-gray-100 hover:text-gray-900 disabled:text-gray-300 disabled:hover:bg-transparent"
      disabled={!focus.canGoBack}
      onclick={goBack}
      aria-label="Go back"
      title={back ? `Go back to ${back.label} (Alt+←)` : 'Go back (Alt+←)'}
    >
      ◀
    </button>
    <button
      type="button"
      class="rounded px-1 text-sm leading-5 text-gray-500 hover:bg-gray-100 hover:text-gray-900 disabled:text-gray-300 disabled:hover:bg-transparent"
      disabled={!focus.canGoForward}
      onclick={goForward}
      aria-label="Go forward"
      title={forward ? `Go forward to ${forward.label} (Alt+→)` : 'Go forward (Alt+→)'}
    >
      ▶
    </button>
  </div>
{/snippet}

<!-- Where the open file is: component › folders › file › function, each part clickable -->
{#snippet breadcrumb()}
  <nav
    class="flex min-w-0 items-center gap-0.5 overflow-hidden text-xs whitespace-nowrap text-gray-500"
    aria-label="Breadcrumb"
  >
    {#if selectedComponent}
      <button
        type="button"
        class="shrink-0 rounded px-1 hover:bg-gray-100 hover:text-gray-900"
        onclick={() => openedUnit && revealInTree(openedUnit.id)}
        title="Component {selectedComponent.name}: show the file in the file tree"
      >
        {selectedComponent.name}
      </button>
    {/if}
    {#each breadcrumbPath as segment, i (i)}
      {@const isFile = i === breadcrumbPath.length - 1}
      <span class="shrink-0 text-gray-300">›</span>
      <button
        type="button"
        class="min-w-0 truncate rounded px-1 font-mono hover:bg-gray-100 hover:text-gray-900 {isFile
          ? 'shrink-0 text-gray-800'
          : ''}"
        onclick={() => openedUnit && revealInTree(openedUnit.id)}
        title="Show {breadcrumbPath.slice(0, i + 1).join('/')} in the file tree"
      >
        {segment}
      </button>
    {/each}
    {#if breadcrumbFunction}
      {@const fn = breadcrumbFunction}
      <span class="shrink-0 text-gray-300">›</span>
      <button
        type="button"
        class="min-w-0 truncate rounded px-1 font-mono text-gray-800 hover:bg-gray-100 hover:text-gray-900"
        onclick={() => selectRef(fn)}
        title="Go to the start of {fn.name} (line {fn.startLine})"
      >
        {fn.name}()
      </button>
    {/if}
  </nav>
{/snippet}

<svelte:window onkeydown={handleKeydown} />

<div class="flex h-full min-h-0 bg-white">
  <!-- Activity bar: switches the view of the sidebar -->
  <div
    class="flex w-10 shrink-0 flex-col items-center gap-1 border-r border-gray-200 bg-gray-50 py-1.5"
  >
    {#each sidebarViews as view (view.id)}
      {@const active = sidebarOpen && sidebarView === view.id}
      <button
        type="button"
        class="relative flex h-8 w-8 items-center justify-center rounded {active
          ? 'text-gray-900'
          : 'text-gray-400 hover:text-gray-700'}"
        onclick={() => toggleSidebar(view.id)}
        aria-label={active ? `Hide ${view.label}` : `Show ${view.label}`}
        aria-pressed={active}
        title={view.label}
      >
        {#if active}
          <span class="absolute top-1 bottom-1 -left-1 w-0.5 bg-gray-900"></span>
        {/if}
        <svg
          class="h-5 w-5"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          stroke-width="1.5"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d={view.icon} />
        </svg>
      </button>
    {/each}
  </div>

  <!-- Sidebar -->
  {#if sidebarOpen && selectedComponent}
    <div class="flex min-h-0 w-60 shrink-0 flex-col border-r border-gray-200">
      {#if sidebarView === 'files'}
        <FileTree
          component={selectedComponent}
          allComponents={analysisResult?.components}
          currentUnitId={selectedTranslationUnit?.id}
          onFileSelect={handleFileSelect}
          onComponentSelect={handleComponentSelect}
          conceptSuggestions={tusWithSuggestions}
          {conceptCounts}
          revealUnit={treeReveal}
          embedded
        />
      {:else}
        <Outline
          annotations={openedUnit?.id === selectedTranslationUnit?.id ? fileAnnotations : null}
          fileName={selectedTranslationUnit?.name}
          selectionLine={focus.details?.node.translationUnitId === selectedTranslationUnit?.id
            ? focus.details?.node.startLine
            : undefined}
          onSelect={selectRef}
        />
      {/if}
    </div>
  {/if}

  <!-- Code: the main view -->
  <div class="flex min-h-0 min-w-0 flex-1 flex-col">
    {#if tabs.tabs.length > 0}
      <!-- Editor tabs -->
      <div
        class="flex h-8 shrink-0 items-stretch overflow-x-auto border-b border-gray-200 bg-gray-50"
        role="tablist"
        aria-label="Open files"
      >
        {#each tabs.tabs as unit (unit.id)}
          {@const active = unit.id === selectedTranslationUnit?.id}
          {@const label = tabLabel(unit)}
          <div
            class="group relative flex max-w-56 shrink-0 items-center border-r border-gray-200 {active
              ? 'bg-white text-gray-900'
              : 'text-gray-500 hover:bg-gray-100 hover:text-gray-800'}"
            title={unit.path}
          >
            {#if active}
              <span class="absolute top-0 right-0 left-0 h-0.5 bg-blue-500"></span>
            {/if}
            <button
              type="button"
              role="tab"
              aria-selected={active}
              class="flex min-w-0 items-center gap-1.5 py-1 pr-1 pl-3 font-mono text-xs"
              onclick={() => !active && handleFileSelect(unit)}
              onauxclick={(e) => {
                if (e.button === 1) {
                  e.preventDefault();
                  closeTab(unit.id);
                }
              }}
            >
              <span class="truncate">{label.name}</span>
              {#if label.folder}
                <span class="shrink-0 font-sans text-[10px] text-gray-400">{label.folder}</span>
              {/if}
            </button>
            <button
              type="button"
              class="mr-1 rounded px-1 text-xs leading-4 text-gray-400 hover:bg-gray-200 hover:text-gray-800 {active
                ? ''
                : 'opacity-0 group-hover:opacity-100 focus:opacity-100'}"
              onclick={() => closeTab(unit.id)}
              aria-label="Close {unit.name}"
              title="Close (middle-click on the tab)"
            >
              ×
            </button>
          </div>
        {/each}
      </div>
    {/if}
    <div class="flex min-h-0 flex-1">
      {#if !analysisResult}
        <div class="flex flex-1 flex-col items-center justify-center gap-2 text-sm text-gray-500">
          <p>No project has been analysed yet.</p>
          <a href="/new-analysis" class="text-blue-600 hover:underline">Start a new analysis</a>
        </div>
      {:else if selectedTranslationUnit && openedUnit?.id === selectedTranslationUnit.id}
        <CodeViewer
          translationUnit={openedUnit}
          highlightLine={selectedNode?.startLine ?? undefined}
          componentName={selectedComponentName ?? undefined}
          selectedNodeId={selectedNode?.id}
          onNavigateToNode={handleNavigateToNode}
          {focus}
          bind:annotations={fileAnnotations}
          {scrollPositions}
          externalInspector
          onInspect={showInspector}
        >
          {#snippet headerStart()}
            {@render historyButtons()}
            {@render breadcrumb()}
          {/snippet}
        </CodeViewer>
      {:else if selectedTranslationUnit}
        <div class="flex flex-1 items-center justify-center">
          <LoadingSpinner message="Loading {selectedTranslationUnit.name}..." />
        </div>
      {:else}
        <div class="flex flex-1 items-center justify-center text-xs text-gray-400">
          No file is open. Open one in the file tree.
        </div>
      {/if}
    </div>
  </div>

  <!-- Context column: inspector and agent -->
  {#if contextCollapsed}
    <div
      class="flex w-9 shrink-0 flex-col items-center gap-3 border-l border-gray-200 bg-gray-50 pt-3"
    >
      {#each [{ id: 'inspector', label: 'Inspector' }, { id: 'agent', label: 'Agent' }] as tab (tab.id)}
        <button
          type="button"
          class="flex flex-col items-center gap-1.5 text-gray-500 hover:text-gray-900"
          onclick={() => openTab(tab.id as ContextTab)}
          aria-label="Show {tab.label}"
        >
          <span
            class="text-[10px] font-semibold tracking-widest uppercase"
            style="writing-mode: vertical-rl;">{tab.label}</span
          >
          {#if tab.id === 'agent' && isLoading}
            {@render workingDot()}
          {:else if tab.id === 'inspector' && (collapsedUnseen || inspectorUnseen)}
            {@render unseenDot()}
          {/if}
        </button>
      {/each}
    </div>
  {:else}
    <!-- Resize handle -->
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
      class="w-1 shrink-0 cursor-col-resize border-l border-gray-200 hover:bg-blue-200"
      onpointerdown={startResize}
      title="Drag to resize"
    ></div>
    <div class="flex min-h-0 shrink-0 flex-col" style:width="{contextWidth}px">
      <!-- Tabs -->
      <div class="flex h-9 shrink-0 items-stretch border-b border-gray-200 px-1" role="tablist">
        {#each [{ id: 'inspector', label: 'Inspector' }, { id: 'agent', label: 'Agent' }] as tab (tab.id)}
          <button
            type="button"
            role="tab"
            aria-selected={contextTab === tab.id}
            class="relative flex items-center gap-1.5 px-3 text-[11px] font-semibold tracking-wider uppercase {contextTab ===
            tab.id
              ? 'text-gray-900'
              : 'text-gray-400 hover:text-gray-700'}"
            onclick={() => openTab(tab.id as ContextTab)}
          >
            {tab.label}
            {#if tab.id === 'agent' && isLoading}
              {@render workingDot()}
            {:else if tab.id === 'inspector' && inspectorUnseen}
              {@render unseenDot()}
            {/if}
            {#if contextTab === tab.id}
              <span class="absolute right-2 -bottom-px left-2 h-0.5 bg-gray-900"></span>
            {/if}
          </button>
        {/each}
        <button
          type="button"
          class="ml-auto self-center rounded px-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
          onclick={() => (contextCollapsed = true)}
          aria-label="Hide panel"
          title="Hide panel"
        >
          »
        </button>
      </div>

      {#if contextTab === 'inspector'}
        <div class="min-h-0 flex-1 overflow-y-auto">
          <NodeInspector
            details={focus.details}
            loading={focus.loading}
            error={focus.error}
            revealSection={focus.revealSection}
            onSelect={selectRef}
          >
            {#snippet actions()}
              {#if focus.details}
                {@render nodeActions(focus.details)}
              {/if}
            {/snippet}
          </NodeInspector>
        </div>
      {:else}
        <!-- Messages -->
        <div
          class="min-h-0 flex-1 overflow-y-auto"
          style="transform: translateZ(0);"
          bind:this={messagesContainer}
          onscroll={handleScroll}
        >
          {#if messages.length === 0 && !isLoading}
            <div class="p-4">
              <p class="text-sm text-gray-600">
                Ask about the code, or click into it and use the questions in the inspector. The
                agent uses the code property graph to answer.
              </p>
              <div class="mt-3 space-y-2">
                {#each starterQuestions as question (question)}
                  <button
                    type="button"
                    class="w-full rounded-lg border border-gray-200 px-3 py-2 text-left text-xs text-gray-700 hover:border-purple-300 hover:bg-purple-50 disabled:opacity-50"
                    disabled={!selectedModel}
                    onclick={() => ask(question)}
                  >
                    {question}
                  </button>
                {/each}
              </div>
            </div>
          {/if}

          {#each messages as message, i (i)}
            {#if message.role === 'user'}
              <div class="flex flex-col items-end gap-1 px-3 py-2">
                {#if message.context}
                  <span
                    class="max-w-[85%] truncate rounded bg-gray-100 px-1.5 py-0.5 font-mono text-[10px] text-gray-500"
                    title={message.context}>📎 {message.context}</span
                  >
                {/if}
                <div class="max-w-[85%] rounded-2xl bg-blue-600 px-3 py-2 text-white">
                  <div class="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</div>
                </div>
              </div>
            {:else}
              <div class={message.contentType === 'tool-result' ? 'px-3 py-1' : 'px-3 py-3'}>
                {#if message.reasoning}
                  <div class="mb-2 inline-block">
                    <button
                      class="flex items-center gap-1.5 text-xs text-gray-400 transition-colors hover:text-gray-600"
                      onclick={() => toggleReasoning(message.id)}
                    >
                      <svg
                        class="h-3 w-3 transition-transform duration-200 {expandedReasoning.has(
                          message.id
                        )
                          ? 'rotate-90'
                          : ''}"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                      </svg>
                      <span>Thought process</span>
                    </button>
                    {#if expandedReasoning.has(message.id)}
                      <div class="mt-1.5 ml-4 border-l-2 border-gray-200 pl-3">
                        <p class="text-xs leading-relaxed whitespace-pre-wrap text-gray-400 italic">
                          {message.reasoning}
                        </p>
                      </div>
                    {/if}
                  </div>
                {/if}
                {#if message.contentType === 'tool-result' && message.toolResult}
                  <ToolResultBlock toolResult={message.toolResult} onItemClick={handleNodeClick} />
                  {#if i === suggestionAnchorIndex}
                    <!-- The pending concept suggestions of the agent, to accept or reject -->
                    <div class="my-1 overflow-hidden rounded border border-gray-200">
                      <ConceptChecklist
                        bind:items={suggestions}
                        onApplySuggestions={handleApplyAndReload}
                        onHighlightNode={revealSuggestedNode}
                      />
                    </div>
                  {/if}
                {:else if message.content}
                  <div class="prose prose-sm max-w-none text-gray-800">
                    <MarkdownRenderer content={message.content} />
                  </div>
                {/if}
              </div>
            {/if}
          {/each}

          {#if isLoading || displayContent}
            <div class="px-3 py-3">
              {#if displayContent}
                <div class="prose prose-sm max-w-none text-gray-800">
                  <MarkdownRenderer content={displayContent} />
                </div>
              {:else}
                <div class="flex gap-1">
                  <div
                    class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:0ms]"
                  ></div>
                  <div
                    class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:150ms]"
                  ></div>
                  <div
                    class="h-2 w-2 animate-bounce rounded-full bg-gray-400 [animation-delay:300ms]"
                  ></div>
                </div>
              {/if}
            </div>
          {/if}
        </div>

        <!-- Input -->
        <div class="shrink-0 border-t border-gray-200 px-3 pt-2 pb-2">
          {#if contextNode}
            <!-- The selected node, which is sent along with the question -->
            <div class="mb-1.5 flex">
              <span
                class="flex max-w-full min-w-0 items-center gap-1 rounded border border-gray-200 bg-gray-50 py-0.5 pr-0.5 pl-1.5 text-[11px] text-gray-600"
                title="Sent as context: {describeNode(contextNode)}"
              >
                <span class="min-w-0 truncate"
                  >📎 <span class="font-mono">{contextNode.name || contextNode.code}</span
                  >{#if contextNode.fileName}
                    · {fileName(contextNode.fileName)}:{contextNode.startLine}{/if}</span
                >
                <button
                  type="button"
                  class="shrink-0 rounded px-1 text-gray-400 hover:bg-gray-200 hover:text-gray-700"
                  onclick={() => (dismissedContextId = contextNode.id)}
                  aria-label="Do not send the selected node as context"
                  title="Remove"
                >
                  ×
                </button>
              </span>
            </div>
          {/if}
          <MessageInput
            value={currentMessage}
            onSend={send}
            onValueChange={onMessageChange}
            placeholder={!selectedModel
              ? 'No LLM provider configured — check application.conf'
              : 'Ask about the code...'}
            disabled={isLoading || !selectedModel}
            prompts={agentSession.mcpCapabilities?.prompts}
            onPromptSelect={(name, args) => {
              showAgent();
              onPromptSelect?.(name, args);
            }}
            onNewChat={onReset}
          />
          <div class="mt-1.5">
            <SessionBar {models} {selectedModel} {onModelSelect} />
          </div>
        </div>
      {/if}
    </div>
  {/if}
</div>
