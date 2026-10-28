<script lang="ts">
  import { untrack } from 'svelte';
  import { fade, slide } from 'svelte/transition';
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import MessageInput from './MessageInput.svelte';
  import SessionBar from './SessionBar.svelte';
  import ToolStep from './ToolStep.svelte';
  import { CodeViewer, ConceptChecklist, FileTree } from '$lib/components/analysis';
  import Outline from '$lib/components/analysis/Outline.svelte';
  import FindingsView from '$lib/components/analysis/FindingsView.svelte';
  import StepBar from '$lib/components/analysis/StepBar.svelte';
  import NodeInspector from '$lib/components/analysis/inspector/NodeInspector.svelte';
  import { LoadingSpinner } from '$lib/components/ui';
  import { agentSession } from '$lib/stores/agentSession.svelte';
  import { CodeFocus, type CodeLocation } from '$lib/stores/codeFocus.svelte';
  import { hasModifier, isTyping, modifierLabel } from '$lib/utils/keyboard';
  import { layers, layerInfos } from '$lib/stores/layers.svelte';
  import { navigation } from '$lib/stores/navigation.svelte';
  import type { InspectorSection } from '$lib/stores/codeFocus.svelte';
  import { EditorTabs } from '$lib/stores/editorTabs.svelte';
  import { relativePath } from '$lib/utils/paths';
  import {
    checkTrust,
    citationLabel,
    citationName,
    extractCitations,
    extractNodeIds,
    extractToolPaths,
    extractToolSlice,
    pathNodeIds,
    resolveNodes
  } from '$lib/agentEvidence';
  import type { ThreadNode } from '$lib/stores/codeFocus.svelte';
  import CommandPalette, { type PaletteCommand } from './CommandPalette.svelte';
  import { clearNodeDetailsCache, getNodeDetails, getNodeDetailsAt } from '$lib/nodeDetails';
  import { GraphPanel } from '$lib/stores/graphPanel.svelte';
  import {
    clearGraphCache,
    evidencePath,
    getEvidenceGraph,
    getPathsGraph,
    getSliceCounts,
    type EvidenceStep,
    type SliceCounts,
    type GraphNode
  } from '$lib/graph';
  import GraphPanelView from '$lib/components/analysis/graph/GraphPanel.svelte';
  import CodeContextMenu, {
    type ContextMenuItem
  } from '$lib/components/analysis/CodeContextMenu.svelte';
  import { getConceptCounts } from '$lib/annotations';
  import type {
    FindingsJSON,
    NodeJSON,
    NodePathsJSON,
    NodeSliceJSON,
    RequirementJSON,
    AnalysisResultJSON,
    TranslationUnitJSON,
    ChatMessage,
    ComponentJSON,
    ConceptSuggestionItem,
    Model,
    NodeDetailsJSON,
    NodeRefJSON,
    FileAnnotationsJSON,
    TrustIssueJSON,
    ToolActivity,
    CodeSelection
  } from '$lib/types';
  import { toolCallLabel, toolNodeIds, type ToolCallLabel } from '$lib/toolLabels';

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
    },
    {
      label: 'How does it depend on its function?',
      question: (where) =>
        `How does ${where} depend on the rest of its function? Look at its data and control dependences: what affects it, and what does it affect?`
    }
  ];

  // How a node is referred to in questions to the agent
  function describeNode(n: NodeRefJSON): string {
    return `the ${n.type} \`${n.code || n.name}\` (node ID ${n.id}, ${n.fileName}:${n.startLine})`;
  }

  function askAboutNode(details: NodeDetailsJSON, question: (where: string) => string) {
    onMessageChange(question(describeNode(details.node)));
  }

  const fileName = (path?: string) => path?.split('/').pop() ?? '';

  // How the context of a question is shown with it, e.g. `log_debug main.c:10`
  function nodeLabel(n: NodeRefJSON): string {
    const where = n.fileName ? ` ${fileName(n.fileName)}:${n.startLine}` : '';
    return `${n.name || n.code}${where}`;
  }

  // The input next to selected code asks right away. The selection is the context of the question,
  // instead of the inspected node
  const maxSelectionLength = 2000;

  function selectionLines(s: CodeSelection): string {
    return s.startLine === s.endLine ? `${s.startLine}` : `${s.startLine}-${s.endLine}`;
  }

  function describeSelection(s: CodeSelection): string {
    const lines = selectionLines(s);
    const text =
      s.text.length > maxSelectionLength ? `${s.text.slice(0, maxSelectionLength)}…` : s.text;
    const node = s.node ? `; the innermost node containing it is ${describeNode(s.node)}` : '';
    return `${s.fileName}:${lines}, selected code \`${text}\`${node}`;
  }

  function askAboutSelection(question: string, selection: CodeSelection) {
    if (isLoading) return;
    // The draft in the input stays where it is
    const draft = currentMessage;
    showAgent();
    chosenBlock = null;
    onMessageChange(question);
    onSendMessage(
      describeSelection(selection),
      `${fileName(selection.fileName)}:${selectionLines(selection)}`
    );
    onMessageChange(draft);
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

  // Goes to a step of the dataflow path, opening its file if needed
  function goToStep(index: number) {
    const ref = focus.path[index];
    if (ref) selectRef(ref);
  }

  function stepPath(delta: number) {
    const ref = focus.pathStep(delta);
    if (ref) selectRef(ref);
  }

  // The steps of the path in the bar above the code
  const pathSteps = $derived(
    focus.path.map((ref) => {
      const code = ref.code.length <= 28 ? ref.code : '';
      return {
        label: code || ref.name || ref.code,
        title: `${ref.type} ${ref.code} · ${ref.startLine >= 1 ? `${ref.fileName}:${ref.startLine}` : 'no location'}`
      };
    })
  );

  function handleKeydown(event: KeyboardEvent) {
    // The palette is also opened from an input, e.g. the question to the agent. It is a shortcut
    // with a modifier, so it does not get in the way of typing
    if (hasModifier(event) && !event.altKey && event.key.toLowerCase() === 'p') {
      // Replaces printing the page
      event.preventDefault();
      paletteQuery = event.shiftKey ? '>' : '';
      return;
    }
    if (isTyping(event)) return;
    if (event.key === 'Escape' && !graph.open) {
      // Deselects, unless something else used the key (e.g. closing a popup). That is only known
      // once all listeners have run
      setTimeout(() => {
        if (!event.defaultPrevented) focus.clear();
      });
      return;
    }
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
    /** The tool call the agent is running, until its result arrives */
    activity?: ToolActivity | null;
    models?: Model[];
    selectedModel?: Model | null;
    analysisResult?: AnalysisResultJSON | null;
    suggestions?: ConceptSuggestionItem[];
    onApplySuggestions?: (accepted: ConceptSuggestionItem[]) => Promise<void> | void;
    /**
     * Sends the current message, with a description of the selected node as context, if any, and
     * a short name of it to show with the question
     */
    onSendMessage: (context?: string, contextLabel?: string) => void;
    /** Stops the agent, keeping what it found so far */
    onStop?: () => void;
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
    activity = null,
    models = [],
    selectedModel = null,
    analysisResult,
    suggestions = $bindable([]),
    onApplySuggestions,
    onSendMessage,
    onStop,
    onReset,
    onMessageChange,
    onModelSelect,
    onPromptSelect
  }: Props = $props();

  // The component of the open file, or of the last open file if all tabs were closed
  const selectedComponent: ComponentJSON | null = $derived(
    analysisResult?.components.find((c) => c.name === selectedComponentName) ?? null
  );

  // The query of the quick open and command palette, null while it is closed
  let paletteQuery = $state<string | null>(null);

  // The files of the open component for the quick open, with their relative paths
  const paletteFiles = $derived(
    (selectedComponent?.translationUnits ?? []).map((unit) => ({
      unit,
      path: relativePath(selectedComponent, unit)
    }))
  );

  // The files of the history, most recent first, which the quick open lists first
  const recentUnitIds = $derived([...new Set(focus.history.map((l) => l.unitId).reverse())]);

  // Inspects the selected node again, revealing a section of the inspector, e.g. its callers
  function showSection(section: InspectorSection) {
    const node = focus.details?.node;
    if (!node) return;
    showInspector();
    focus.inspect(() => getNodeDetails(node.id), true, section, false);
  }

  // All actions of the page, for the command palette
  const paletteCommands = $derived.by((): PaletteCommand[] => {
    const mod = modifierLabel();
    const details = focus.details;
    const noSelection = details ? undefined : 'No node is selected';
    return [
      {
        id: 'open-file',
        category: 'Go',
        label: 'Open file…',
        shortcut: `${mod}P`,
        run: () => (paletteQuery = '')
      },
      {
        id: 'ask',
        category: 'Selection',
        label: 'Ask the agent about the selected code',
        shortcut: `${mod}K`,
        disabledReason: openedUnit ? undefined : 'No file is open',
        run: () => askRequest++
      },
      {
        id: 'back',
        category: 'Go',
        label: 'Back',
        shortcut: 'Alt+←',
        disabledReason: focus.canGoBack ? undefined : 'Nothing to go back to',
        run: goBack
      },
      {
        id: 'forward',
        category: 'Go',
        label: 'Forward',
        shortcut: 'Alt+→',
        disabledReason: focus.canGoForward ? undefined : 'Nothing to go forward to',
        run: goForward
      },
      ...layerInfos.map((layer) => ({
        id: `layer-${layer.id}`,
        category: 'Layers',
        label: `${layers.visible[layer.id] ? 'Hide' : 'Show'} ${layer.icon} ${layer.label}`,
        run: () => layers.toggle(layer.id)
      })),
      {
        id: 'graph-backward',
        category: 'Graph',
        label: `PDG: What affects this?${slicePreview(sliceCounts?.backward)}`,
        disabledReason: noSelection ?? graphDisabledReason(sliceCounts?.backward),
        run: () => details && graph.show(details.node.id, 'BACKWARD', undefined, undefined, 'PDG')
      },
      {
        id: 'graph-forward',
        category: 'Graph',
        label: `PDG: What does this affect?${slicePreview(sliceCounts?.forward)}`,
        disabledReason: noSelection ?? graphDisabledReason(sliceCounts?.forward),
        run: () => details && graph.show(details.node.id, 'FORWARD', undefined, undefined, 'PDG')
      },
      {
        id: 'graph-close',
        category: 'Graph',
        label: 'Close the dependence graph',
        disabledReason: graph.open ? undefined : 'No graph is shown',
        run: () => graph.close()
      },
      {
        id: 'path-next',
        category: 'Path',
        label: 'Next step of the dataflow path',
        disabledReason: focus.pathStep(1) ? undefined : 'There is no next step',
        run: () => stepPath(1)
      },
      {
        id: 'path-previous',
        category: 'Path',
        label: 'Previous step of the dataflow path',
        disabledReason: focus.pathStep(-1) ? undefined : 'There is no previous step',
        run: () => stepPath(-1)
      },
      {
        id: 'path-clear',
        category: 'Path',
        label: 'Close the dataflow path',
        disabledReason: focus.path.length > 0 ? undefined : 'No path is shown',
        run: () => focus.clearPath()
      },
      {
        id: 'callers',
        category: 'Selection',
        label: 'Show callers',
        disabledReason:
          noSelection ?? (details!.callers.length > 0 ? undefined : 'The selection has no callers'),
        run: () => showSection('callers')
      },
      {
        id: 'callees',
        category: 'Selection',
        label: 'Show callees',
        disabledReason:
          noSelection ??
          (details!.callTargets.length > 0 || details!.callees.length > 0
            ? undefined
            : 'The selection calls nothing'),
        run: () => showSection(details!.callTargets.length > 0 ? 'callTargets' : 'callees')
      },
      {
        id: 'ask-node',
        category: 'Selection',
        label: 'Ask the agent about it',
        disabledReason:
          noSelection ?? (selectedModel ? undefined : 'No LLM provider is configured'),
        run: () => {
          showAgent();
          askAboutNode(details!, nodeQuestions[0].question);
        }
      },
      {
        id: 'reveal',
        category: 'Selection',
        label: 'Reveal the file in the file tree',
        disabledReason: openedUnit ? undefined : 'No file is open',
        run: () => openedUnit && revealInTree(openedUnit.id)
      },
      {
        id: 'sidebar',
        category: 'View',
        label: sidebarOpen ? 'Hide the sidebar' : 'Show the sidebar',
        run: () => (sidebarOpen = !sidebarOpen)
      },
      {
        id: 'files',
        category: 'View',
        label: 'Show the files',
        run: () => {
          sidebarView = 'files';
          sidebarOpen = true;
        }
      },
      {
        id: 'outline',
        category: 'View',
        label: 'Show the outline',
        run: () => {
          sidebarView = 'outline';
          sidebarOpen = true;
        }
      },
      {
        id: 'context',
        category: 'View',
        label: contextCollapsed ? 'Show the right column' : 'Hide the right column',
        run: () => (contextCollapsed ? openTab(contextTab) : (contextCollapsed = true))
      },
      {
        id: 'inspector',
        category: 'View',
        label: 'Show the inspector',
        run: () => openTab('inspector')
      },
      { id: 'agent', category: 'View', label: 'Show the agent', run: showAgent },
      {
        id: 'commands',
        category: 'View',
        label: 'Show all commands',
        shortcut: `${mod}Shift+P`,
        run: () => (paletteQuery = '>')
      }
    ];
  });

  async function handleApplyAndReload(accepted: ConceptSuggestionItem[]) {
    await onApplySuggestions?.(accepted);
    // The applied concepts change the details of the nodes they are attached to, the annotations
    // and the counts in the file tree
    clearNodeDetailsCache();
    clearGraphCache();
    focus.invalidate();
  }

  // The dependence graph of a statement, shown next to the code
  const graph = new GraphPanel();
  const graphWidthKey = 'codyze-agent-graph-width';
  let graphWidth = $state(loadGraphWidth());

  function loadGraphWidth(): number {
    try {
      const stored = Number(localStorage.getItem(graphWidthKey));
      if (stored >= 360) return stored;
    } catch {
      // Storage is not available, e.g. during SSR or in a private window
    }
    return 540;
  }

  function startGraphResize(event: PointerEvent) {
    event.preventDefault();
    const startX = event.clientX;
    const startWidth = graphWidth;
    const move = (e: PointerEvent) => {
      const max = Math.max(400, window.innerWidth * 0.6);
      graphWidth = Math.round(Math.min(max, Math.max(360, startWidth + startX - e.clientX)));
    };
    const stop = () => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', stop);
      document.body.style.removeProperty('cursor');
      try {
        localStorage.setItem(graphWidthKey, String(graphWidth));
      } catch {
        // Not remembering the width is fine
      }
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', stop);
    document.body.style.cursor = 'col-resize';
  }

  // How many statements are in the slices of the inspected node, to preview them in the commands
  let sliceCounts = $state.raw<SliceCounts | null>(null);
  $effect(() => {
    const id = focus.details?.node.id;
    void focus.revision;
    const hops = graph.hops;
    sliceCounts = null;
    if (!id) return;
    getSliceCounts(id, hops)
      .then((counts) => {
        if (id === focus.details?.node.id) sliceCounts = counts;
      })
      .catch(() => {});
  });

  function slicePreview(count: number | undefined): string {
    return count === undefined
      ? ''
      : count > 0
        ? ` · ${count} ${count === 1 ? 'node' : 'nodes'}`
        : '';
  }

  function graphDisabledReason(count: number | undefined): string | undefined {
    return count === 0 ? 'None in this function' : undefined;
  }

  // The statement of the slice a node belongs to: the statement itself, or the innermost one that
  // contains the start of the node
  function sliceNodeFor(node: NodeRefJSON): GraphNode | null {
    const nodes = graph.slice?.nodes ?? [];
    const exact = nodes.find((n) => n.id === node.id);
    if (exact || node.startLine < 1) return exact ?? null;
    return (
      nodes
        .filter(
          (n) =>
            n.kind !== 'STUB' &&
            n.kind !== 'OVERLAY' &&
            n.node.translationUnitId === node.translationUnitId &&
            n.startLine <= node.startLine &&
            node.startLine <= n.endLine
        )
        .sort((a, b) => a.endLine - a.startLine - (b.endLine - b.startLine))[0] ?? null
    );
  }

  // The graph needs the width: while it is open, the navigation and the sidebar are collapsed. When
  // it closes, what it collapsed is opened again, unless it was toggled in between
  let collapsedForGraph = { navigation: false, sidebar: false };
  $effect(() => {
    const open = graph.open;
    untrack(() => {
      if (open) {
        collapsedForGraph = { navigation: !navigation.collapsed, sidebar: sidebarOpen };
        navigation.collapsed = true;
        sidebarOpen = false;
      } else {
        if (collapsedForGraph.navigation && navigation.collapsed) navigation.collapsed = false;
        if (collapsedForGraph.sidebar && !sidebarOpen) sidebarOpen = true;
        collapsedForGraph = { navigation: false, sidebar: false };
      }
    });
  });

  // The graph follows what is inspected: inside the slice it selects the statement, outside of it
  // the slice is re-rooted at the node, unless the graph is pinned
  $effect(() => {
    const details = focus.details;
    untrack(() => {
      if (!graph.open || !details) return;
      const hit = sliceNodeFor(details.node);
      if (hit) {
        if (graph.selectedId !== hit.id) graph.select(hit.id);
        return;
      }
      // Functions and nodes outside of functions have no slice of their own
      const fn = details.enclosingFunction;
      // Paths stay as they are, like a pinned slice
      if (graph.pinned || graph.paths || !fn || fn.id === details.node.id) return;
      graph.show(details.node.id, graph.direction, graph.hops);
    });
  });

  // Clicking a statement in the graph inspects it, which shows it in the code
  function selectGraphNode(node: GraphNode) {
    if (node.kind === 'STUB') return;
    if (contextTab !== 'inspector' || contextCollapsed) markInspectorUnseen();
    focus.inspect(() => getNodeDetails(node.id), true);
  }

  // The statements of the slice in the open file, shown as tinted lines in the code
  const graphSlice = $derived.by(() => {
    const slice = graph.open ? graph.slice : null;
    const unit = openedUnit;
    if (!slice || !unit) return undefined;
    return {
      ranges: slice.nodes
        .filter(
          (n) => n.kind !== 'STUB' && n.node.translationUnitId === unit.id && n.startLine >= 1
        )
        .map((n) => ({
          id: n.id,
          first: n.startLine,
          last: n.endLine,
          label: `Dependence graph: ${n.code}`
        })),
      selectedId: graph.selectedId,
      hoveredId: graph.hoveredId,
      onHover: (id: string | null) => (graph.hoveredId = id)
    };
  });

  // The menu of a right click in the code, with the size of the slices for its entries
  let codeMenu = $state.raw<{
    details: NodeDetailsJSON;
    x: number;
    y: number;
    counts: SliceCounts | null;
  } | null>(null);

  function openCodeMenu(target: { details: NodeDetailsJSON; x: number; y: number }) {
    codeMenu = { ...target, counts: null };
    getSliceCounts(target.details.node.id, graph.hops)
      .then((counts) => {
        if (codeMenu?.details === target.details) codeMenu = { ...codeMenu, counts };
      })
      .catch(() => {});
  }

  // Shows a list of the inspector for a node, e.g. its callers
  function showSectionOf(details: NodeDetailsJSON, section: InspectorSection) {
    showInspector();
    focus.inspect(() => Promise.resolve(details), true, section);
  }

  const codeMenuItems = $derived.by((): ContextMenuItem[] => {
    if (!codeMenu) return [];
    const { details, counts } = codeMenu;
    const size = (count: number | undefined) =>
      count === undefined
        ? undefined
        : count > 0
          ? `${count} ${count === 1 ? 'node' : 'nodes'}`
          : undefined;
    const none = (count: number | undefined) => (count === 0 ? 'none' : undefined);
    const hasCallees = details.callTargets.length > 0 || details.callees.length > 0;
    return [
      {
        label: 'PDG: What affects this?',
        hint: size(counts?.backward),
        disabledReason: none(counts?.backward),
        run: () => graph.show(details.node.id, 'BACKWARD', undefined, undefined, 'PDG')
      },
      {
        label: 'PDG: What does this affect?',
        hint: size(counts?.forward),
        disabledReason: none(counts?.forward),
        run: () => graph.show(details.node.id, 'FORWARD', undefined, undefined, 'PDG')
      },
      {
        label: 'Show callers',
        separator: true,
        hint: details.callers.length > 0 ? `${details.callers.length}` : undefined,
        disabledReason: details.callers.length > 0 ? undefined : 'none',
        run: () => showSectionOf(details, 'callers')
      },
      {
        label: 'Show callees',
        hint: hasCallees ? `${details.callTargets.length + details.callees.length}` : undefined,
        disabledReason: hasCallees ? undefined : 'none',
        run: () =>
          showSectionOf(details, details.callTargets.length > 0 ? 'callTargets' : 'callees')
      },
      {
        label: 'Ask agent about this…',
        separator: true,
        run: () => {
          showAgent();
          askAboutNode(details, nodeQuestions[0].question);
        }
      }
    ];
  });

  // The findings of the requirements: the file of each, and showing it in the code
  function findingFile(finding: FindingsJSON): string {
    const unit = finding.translationUnit ? findUnitById(finding.translationUnit) : null;
    const component = unit ? findComponentForTu(unit.id) : null;
    return unit && component
      ? relativePath(component, unit)
      : (finding.path.split('/').pop() ?? finding.path);
  }

  function openFinding(finding: FindingsJSON) {
    const unit = finding.translationUnit ? findUnitById(finding.translationUnit) : null;
    const component = unit ? findComponentForTu(unit.id) : null;
    if (!unit || !component) return;
    if (unit.id !== selectedTranslationUnit?.id) handleFileSelect(unit);
    showInspector();
    // The node at the place of the finding, which shows it in the code
    focus.inspect(
      () =>
        getNodeDetailsAt(component.name, unit.id, finding.startLine, finding.startColumn, {
          line: finding.endLine,
          column: finding.endColumn
        }),
      true
    );
  }

  function askAboutRequirement(requirement: RequirementJSON, finding?: FindingsJSON) {
    showAgent();
    onMessageChange(
      finding
        ? `The requirement "${requirement.name}" (${requirement.id}: ${requirement.description}) is violated at ${findingFile(finding)}:${finding.startLine}. Why? Follow the evidence in the code and explain what would have to change for the requirement to hold.`
        : `Why is the requirement "${requirement.name}" (${requirement.id}: ${requirement.description}) not fulfilled? Find the places in the code that violate it and explain why.`
    );
  }

  // The places in the open file that violate a requirement, for the findings layer
  const openFindings = $derived(
    (analysisResult?.findings ?? [])
      .filter((f) => f.translationUnit === openedUnit?.id && f.kind.toLowerCase() === 'fail')
      .map((f) => ({
        startLine: f.startLine,
        endLine: f.endLine,
        label: `Violates ${f.rule ?? 'a requirement'}`
      }))
  );

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

  // Selects and reveals a node the agent refers to (a citation, a node a tool returned, a
  // suggestion, ...), opening its file if needed. The agent tab stays open, so the answer stays in
  // view; the inspector tab is only marked. The location is resolved by the backend, since tool
  // results only name the file
  async function revealFromAgent(nodeId: string | null | undefined) {
    if (!nodeId) return;
    const details = await getNodeDetails(nodeId).catch(() => null);
    if (!details) return;
    const unitId = details.node.translationUnitId;
    const unit = unitId ? findUnitById(unitId) : null;
    if (unit && unit.id !== selectedTranslationUnit?.id) handleFileSelect(unit);
    if (contextTab !== 'inspector' || contextCollapsed) markInspectorUnseen();
    // The user did not select the node, so it is not sent as the context of the next question
    dismissedContextId = details.node.id;
    focus.inspect(() => Promise.resolve(details), true);
  }

  // The nodes cited in the answers of the agent, resolved to their locations to label and open them
  let citedRefs = $state.raw<Map<string, NodeRefJSON>>(new Map());
  const citedKey = $derived(
    [
      ...new Set([
        ...messages.flatMap((m) =>
          m.role === 'assistant' && m.contentType !== 'tool-result'
            ? extractCitations(m.content)
            : []
        ),
        ...extractCitations(streamingContent)
      ])
    ].join(',')
  );

  $effect(() => {
    const key = citedKey;
    if (!key) return;
    resolveNodes(key.split(',')).then((refs) => {
      if (key === citedKey) citedRefs = refs;
    });
  });

  // Inspects a node cited by the agent and shows it in the code
  function openCitation(nodeId: string) {
    citeHover = null;
    followCited(nodeId);
  }

  // The lines of the citation that was followed last, which light up once in the code
  let citedFlash = $state.raw<{
    first: number;
    last: number;
    key: number;
    unitId: string;
  } | null>(null);
  let flashTimer: ReturnType<typeof setTimeout> | undefined;

  // Shows a node the answer cites in the code and lights up its lines, so that the eye finds it
  // after the jump. The light is gone after a moment, so that it does not light up again when the
  // file is opened later
  async function followCited(nodeId: string) {
    await revealFromAgent(nodeId);
    const ref = citedRefs.get(nodeId) ?? evidenceRefs.get(nodeId);
    if (!ref?.translationUnitId || ref.startLine < 1) return;
    citedFlash = {
      ...citedRange(ref),
      key: (citedFlash?.key ?? 0) + 1,
      unitId: ref.translationUnitId
    };
    clearTimeout(flashTimer);
    flashTimer = setTimeout(() => (citedFlash = null), 2000);
  }

  // The citation under the mouse in an answer: its node is shown in a preview next to it, its
  // lines are tinted in the code and its statement stands out in the graph, without changing what
  // is selected. A citation is the only way the answer marks the code
  let citeHover = $state.raw<{ ref: NodeRefJSON; chip: DOMRect } | null>(null);
  let citeGraphHover = false;

  function hoverCitation(nodeId: string | null, chip: HTMLElement | null) {
    const ref = nodeId ? (citedRefs.get(nodeId) ?? evidenceRefs.get(nodeId)) : undefined;
    citeHover = ref && chip ? { ref, chip: chip.getBoundingClientRect() } : null;
    const statement = nodeId ? graph.evidence?.statementOf[nodeId] : undefined;
    if (statement && graph.slice?.nodes.some((n) => n.id === statement)) {
      graph.hoveredId = statement;
      citeGraphHover = true;
    } else if (citeGraphHover) {
      graph.hoveredId = null;
      citeGraphHover = false;
    }
  }

  // The lines a citation refers to: a function only by its head, anything else by at most a few
  // lines, so that a citation never tints a whole body
  function citedRange(ref: NodeRefJSON): { first: number; last: number } {
    const last = ref.type === 'Function' ? ref.startLine : Math.max(ref.endLine, ref.startLine);
    return { first: ref.startLine, last: Math.min(last, ref.startLine + 2) };
  }

  // The lines of the hovered citation in the open file
  const citedLines = $derived(
    citeHover && openedUnit && citeHover.ref.translationUnitId === openedUnit.id
      ? citedRange(citeHover.ref)
      : null
  );

  // The code of the files cited in answers, for the previews of their citations
  // eslint-disable-next-line svelte/prefer-svelte-reactivity
  const previewCode = new Map<string, Promise<string[] | null>>();

  function codeOf(ref: NodeRefJSON): Promise<string[] | null> {
    const unitId = ref.translationUnitId;
    const componentName = ref.componentName;
    if (!unitId || !componentName) return Promise.resolve(null);
    let code = previewCode.get(unitId);
    if (!code) {
      code = fetch(`/api/component/${componentName}/translation-unit/${unitId}`)
        .then((r) => (r.ok ? r.json() : null))
        .then((unit: TranslationUnitJSON | null) => unit?.code?.split('\n') ?? null)
        .catch(() => null);
      previewCode.set(unitId, code);
    }
    return code;
  }

  // The lines of the preview: the cited lines with one line around them, at most five
  let previewLines = $state.raw<{ number: number; text: string; cited: boolean }[]>([]);
  $effect(() => {
    const ref = citeHover?.ref;
    previewLines = [];
    if (!ref || ref.startLine < 1) return;
    codeOf(ref).then((lines) => {
      if (!lines || citeHover?.ref !== ref) return;
      const { last } = citedRange(ref);
      const first = Math.max(1, ref.startLine - 1);
      const end = Math.min(lines.length, last + 1, first + 4);
      previewLines = lines.slice(first - 1, end).map((text, i) => ({
        number: first + i,
        text,
        cited: first + i >= ref.startLine && first + i <= last
      }));
    });
  });

  // The conversation as a timeline: one block per question, with the tool calls as numbered steps
  // (the evidence trail) and the answer last
  interface TimelineEntry {
    message: ChatMessage;
    /** The index of the message in [messages] */
    index: number;
    /** The number of the step, for tool calls */
    step?: number;
  }

  const timeline = $derived.by(() => {
    const blocks: { question: ChatMessage | null; entries: TimelineEntry[] }[] = [];
    messages.forEach((message, index) => {
      if (message.role === 'user') {
        blocks.push({ question: message, entries: [] });
        return;
      }
      if (blocks.length === 0) blocks.push({ question: null, entries: [] });
      const block = blocks[blocks.length - 1];
      const step =
        message.contentType === 'tool-result'
          ? block.entries.filter((e) => e.step).length + 1
          : undefined;
      block.entries.push({ message, index, step });
    });
    return blocks;
  });

  // The evidence of each block: the node IDs returned by each step, and the IDs that are only
  // cited in the answer
  const blockEvidence = $derived(
    timeline.map((block) => {
      const steps = block.entries.flatMap((e) =>
        e.step ? [{ step: e.step, ids: extractNodeIds(e.message.toolResult?.content) }] : []
      );
      const returned = new Set(steps.flatMap((s) => s.ids));
      const cited = block.entries.flatMap((e) =>
        e.message.contentType === 'text' ? extractCitations(e.message.content) : []
      );
      return { steps, claimed: [...new Set(cited)].filter((id) => !returned.has(id)) };
    })
  );

  // The nodes of the evidence, resolved to their locations. IDs that are no nodes are missing
  let evidenceRefs = $state.raw<Map<string, NodeRefJSON>>(new Map());
  const evidenceKey = $derived(
    [...new Set(blockEvidence.flatMap((e) => [...e.steps.flatMap((s) => s.ids), ...e.claimed]))]
      .sort()
      .join(',')
  );

  $effect(() => {
    const key = evidenceKey;
    if (!key) return;
    resolveNodes(key.split(',')).then((refs) => {
      if (key === evidenceKey) evidenceRefs = refs;
    });
  });

  // The block whose evidence is shown in the code: the one the user chose, or else the latest one
  // with evidence
  let chosenBlock = $state<number | null>(null);
  const activeBlock = $derived(
    chosenBlock !== null && chosenBlock < timeline.length
      ? chosenBlock
      : blockEvidence.findLastIndex((e) => e.steps.some((s) => s.ids.length) || e.claimed.length)
  );

  function stepNodes(ids: string[]): NodeRefJSON[] {
    return ids.flatMap((id) => evidenceRefs.get(id) ?? []);
  }

  // The places the answer of the active block cites, in the order it cites them, to follow the
  // answer through the code one place after the other
  const threadSteps = $derived.by(() => {
    const block = timeline[activeBlock];
    if (!block || !layers.visible.agent) return [];
    const ids = new Set(
      block.entries.flatMap((e) =>
        e.message.contentType === 'text' ? extractCitations(e.message.content) : []
      )
    );
    return [...ids].flatMap((id) => {
      const ref = citedRefs.get(id) ?? evidenceRefs.get(id);
      if (!ref || ref.startLine < 1) return [];
      const code = ref.code || ref.name;
      return [
        {
          nodes: [ref],
          label: code.length > 32 ? `${code.slice(0, 31)}…` : code,
          title: `${code} · ${ref.fileName ?? ''}:${ref.startLine}`
        }
      ];
    });
  });

  // Shows paths a tool returned: a single path as the path in the code, several ones as a graph next
  // to the code
  async function showToolPaths(toolPaths: NodePathsJSON) {
    if (toolPaths.paths.length === 1) {
      const ids = pathNodeIds(toolPaths)[0];
      const refs = await resolveNodes(ids);
      const path = ids.flatMap((id) => refs.get(id) ?? []);
      if (path.length < 2) return;
      focus.setPath(path);
      revealFromAgent(path[0].id);
      return;
    }
    const pathsGraph = await getPathsGraph(toolPaths).catch(() => null);
    if (!pathsGraph || pathsGraph.nodes.length === 0) return;
    graph.showPaths(
      { description: toolPaths.description, kind: toolPaths.kind, count: toolPaths.paths.length },
      pathsGraph
    );
    // The pathsGraph is shown next to the code, at the end of the paths
    const end = pathsGraph.nodes.find((n) => n.id === pathsGraph.root);
    if (end) revealFromAgent(end.node.id);
  }

  // Shows a slice a tool computed in the graph panel, like a slice asked for in the code
  function showToolSlice(toolSlice: NodeSliceJSON) {
    graph.show(
      toolSlice.start.nodeId,
      toolSlice.direction,
      toolSlice.hops,
      undefined,
      toolSlice.graph,
      toolSlice.interprocedural ? 'interprocedural' : 'intraprocedural'
    );
    revealFromAgent(toolSlice.start.nodeId);
  }

  // Shows what a tool call found as a path or a graph, if it found paths or a slice. Returns
  // whether it did
  function showToolGraph(content: unknown): boolean {
    const toolSlice = extractToolSlice(content);
    if (toolSlice) {
      showToolSlice(toolSlice);
      return true;
    }
    const toolPaths = extractToolPaths(content);
    if (toolPaths) showToolPaths(toolPaths);
    return !!toolPaths;
  }

  // The steps of a block that found paths or a slice, which make up its evidence graph
  function graphSteps(block: (typeof timeline)[number]): EvidenceStep[] {
    return block.entries.flatMap((e): EvidenceStep[] => {
      const content = e.message.toolResult?.content;
      if (!e.step) return [];
      const slice = extractToolSlice(content);
      if (slice) return [{ step: e.step, slice }];
      const paths = extractToolPaths(content);
      return paths ? [{ step: e.step, paths }] : [];
    });
  }

  // Shows the evidence of a question as one graph next to the code: the paths and slices of all of
  // its steps, with the statements its answer cites marked
  async function showEvidence(b: number, reveal = true) {
    const block = timeline[b];
    if (!block) return;
    const steps = graphSteps(block);
    if (steps.length === 0) return;
    const citations = [
      ...new Set(
        block.entries.flatMap((e) =>
          e.message.contentType === 'text' ? extractCitations(e.message.content) : []
        )
      )
    ];
    const evidence = await getEvidenceGraph(steps, citations).catch(() => null);
    if (!evidence || evidence.graph.nodes.length === 0) return;
    graph.showPaths(
      { description: 'the evidence of the answer', kind: 'evidence', count: steps.length },
      evidence.graph,
      {
        all: evidence.graph,
        path: evidencePath(evidence.graph, new Set(evidence.cited)),
        statementOf: evidence.statementOf
      }
    );
    const first = evidence.graph.nodes.find((n) => evidence.cited.includes(n.id));
    if (reveal && first) revealFromAgent(first.node.id);
  }

  // Once the agent has answered, the page stays as it is: the answer offers to step through its
  // places and to show its graph. Only a graph that shows the evidence of an earlier answer follows
  // the new one, or closes if the new one has none, so that it does not show something else than
  // the answer. Without an open file, the first place the answer cites is opened
  let wasLoading = false;
  $effect(() => {
    const loading = isLoading;
    if (wasLoading && !loading) untrack(afterAnswer);
    wasLoading = loading;
  });

  function afterAnswer() {
    const last = timeline.length - 1;
    const block = timeline[last];
    if (!block) return;
    if (graph.open && graph.evidence) {
      if (graphSteps(block).length > 0) showEvidence(last, false);
      else graph.close();
    }
    if (!openedUnit) {
      const first = block.entries
        .flatMap((e) =>
          e.message.contentType === 'text' ? extractCitations(e.message.content) : []
        )
        .at(0);
      if (first) followCited(first);
    }
  }

  // The steps shown in the code and in the bar above it: the path or the agent's thread
  const sequence = $derived(focus.sequence(layers.visible.agent));

  // A new question with evidence, or choosing another one, turns to the agent's thread
  $effect(() => {
    void activeBlock;
    untrack(() => (focus.showing = 'thread'));
  });

  // The places where the analysis is uncertain that the evidence of each block relies on. The block
  // the agent is still working on is checked once it is done
  let blockTrust = $state.raw<TrustIssueJSON[][]>([]);
  $effect(() => {
    const evidence = blockEvidence;
    const pending = isLoading ? evidence.length - 1 : -1;
    Promise.all(
      evidence.map((e, b) =>
        b === pending ? [] : checkTrust([...e.steps.flatMap((s) => s.ids), ...e.claimed])
      )
    ).then((trust) => {
      if (evidence === blockEvidence) blockTrust = trust;
    });
  });

  // Whether the limits of the evidence of a block are listed below its answer
  let limitsOpen = $state<Record<number, boolean>>({});

  // Whether the steps of a block are listed, by block. Without a choice of the user, only the line
  // above them is shown: what the agent does right now, or how many steps it took
  let stepsOpen = $state<Record<number, boolean>>({});

  function stepsShown(b: number): boolean {
    return stepsOpen[b] ?? false;
  }

  // A new chat starts without the choices made for the blocks of the last one
  $effect(() => {
    if (messages.length > 0) return;
    stepsOpen = {};
    limitsOpen = {};
  });

  // Transitions answer what the user did; they are left out if the user asked for less motion
  const reducedMotion =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const motion = (ms: number) => (reducedMotion ? 0 : ms);

  // The current time while the agent works, to show for how long
  let now = $state(Date.now());
  $effect(() => {
    if (!isLoading) return;
    now = Date.now();
    const timer = setInterval(() => (now = Date.now()), 1000);
    return () => clearInterval(timer);
  });

  // How long the agent worked on a block, e.g. "52 s" or "2 min 5 s"
  function duration(b: number): string {
    const block = timeline[b];
    const start = block?.question?.timestamp;
    if (!start) return '';
    const running = b === timeline.length - 1 && isLoading;
    const end = running ? now : (block.entries.at(-1)?.message.timestamp.getTime() ?? now);
    const seconds = Math.max(0, Math.round((end - new Date(start).getTime()) / 1000));
    return seconds < 60 ? `${seconds} s` : `${Math.floor(seconds / 60)} min ${seconds % 60} s`;
  }

  // The places in the code the answer of a block cites, in the order it cites them
  function citedPlaces(b: number): NodeRefJSON[] {
    const ids = (timeline[b]?.entries ?? []).flatMap((e) =>
      e.message.contentType === 'text' ? extractCitations(e.message.content) : []
    );
    return [...new Set(ids)].flatMap((id) => {
      const ref = citedRefs.get(id) ?? evidenceRefs.get(id);
      return ref && ref.startLine >= 1 ? [ref] : [];
    });
  }

  // The nodes among the arguments of the tool calls, to name them in the labels of the calls
  let argRefs = $state.raw<Map<string, NodeRefJSON>>(new Map());
  const argKey = $derived(
    [
      ...new Set([
        ...toolNodeIds(activity?.arguments),
        ...messages.flatMap((m) => toolNodeIds(m.toolResult?.arguments))
      ])
    ]
      .sort()
      .join(',')
  );

  $effect(() => {
    const key = argKey;
    if (!key) return;
    resolveNodes(key.split(',')).then((refs) => {
      if (key === argKey) argRefs = refs;
    });
  });

  // A tool call with its main argument, e.g. `cpg_list_calls_to net_send`
  function labelOf(toolName: string, args: Record<string, unknown> | undefined): ToolCallLabel {
    return toolCallLabel(toolName, args, (id) => {
      const ref = argRefs.get(id);
      return ref ? citationName(ref) : undefined;
    });
  }

  // Whether two labels show the same, so that the row only fades when the call changes
  function labelKey(label: ToolCallLabel | string): string {
    return typeof label === 'string'
      ? label
      : `${label.tool} ${label.argument ?? ''} ${label.detail ?? ''}`;
  }

  // The calls of the running question, in their order
  const runningCalls = $derived.by((): ToolCallLabel[] => {
    if (!isLoading) return [];
    return (timeline.at(-1)?.entries ?? []).flatMap((e) => {
      const call = e.message.toolResult;
      return call ? [labelOf(call.toolName ?? '', call.arguments)] : [];
    });
  });

  // The call the row shows. The tools take milliseconds and the model often calls several at once,
  // so the row shows each call for a moment, skipping ahead if it falls too far behind
  let shownCall = $state(0);
  const callDwell = 450;
  const maxBehind = 6;
  $effect(() => {
    const count = runningCalls.length;
    if (count === 0) {
      shownCall = 0;
      return;
    }
    if (shownCall >= count - 1) return;
    if (reducedMotion || count - 1 - shownCall > maxBehind) {
      shownCall = reducedMotion ? count - 1 : count - 1 - maxBehind;
      return;
    }
    const timer = setTimeout(() => (shownCall += 1), callDwell);
    return () => clearTimeout(timer);
  });

  // What the agent does right now, in the row above its answer: the calls one after the other, a
  // call that takes longer while it runs, writing, or else thinking about the last call
  const liveLabel = $derived.by((): ToolCallLabel | string => {
    const caughtUp = shownCall >= runningCalls.length - 1;
    if (!caughtUp) return runningCalls[shownCall];
    if (activity) return labelOf(activity.toolName, activity.arguments);
    if (displayContent) return 'Writing';
    return runningCalls.at(-1) ?? 'Thinking';
  });

  // Steps through the places the answer of a block cites, in the code and the bar above it
  function stepThrough(b: number) {
    chosenBlock = b;
    focus.showing = 'thread';
    if (!layers.visible.agent) layers.toggle('agent');
    goToThreadStep(0);
  }

  // Who relies on an issue, e.g. "Step 1", "Steps 1, 3" or "The answer"
  function trustSubject(issue: TrustIssueJSON, b: number): { text: string; plural: boolean } {
    const evidence = blockEvidence[b];
    const steps = (evidence?.steps ?? [])
      .filter((s) => s.ids.some((id) => issue.evidence.includes(id)))
      .map((s) => s.step);
    if (steps.length === 0) return { text: 'The answer', plural: false };
    return {
      text: `${steps.length === 1 ? 'Step' : 'Steps'} ${steps.join(', ')}`,
      plural: steps.length > 1
    };
  }

  function trustLocation(ref: NodeRefJSON): string {
    return ref.startLine >= 1 ? `${ref.fileName ?? ''}:${ref.startLine}` : 'no source';
  }

  // The files with evidence of the active thread, marked in the file tree
  const threadUnits = $derived(
    new Set(
      layers.visible.agent
        ? focus.thread.flatMap((n) => (n.ref.translationUnitId ? [n.ref.translationUnitId] : []))
        : []
    )
  );

  // The step of the inspected node, if it belongs to the active thread
  const threadStepIndex = $derived(
    threadSteps.findIndex((s) => s.nodes.some((n) => n.id === focus.details?.node.id))
  );

  function goToThreadStep(index: number) {
    const step = threadSteps[index];
    if (step) followCited(step.nodes[0].id);
  }

  // The evidence of the active block, shown as numbered markers in the code
  $effect(() => {
    const evidence = blockEvidence[activeBlock];
    const nodes: ThreadNode[] = [];
    if (evidence) {
      for (const { step, ids } of evidence.steps) {
        for (const ref of stepNodes(ids)) nodes.push({ ref, step, claimed: false });
      }
      const answerStep = evidence.steps.length + 1;
      for (const ref of stepNodes(evidence.claimed)) {
        nodes.push({ ref, step: answerStep, claimed: true });
      }
    }
    focus.thread = nodes;
  });

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

  // Whether the questions to the agent below the inspector are collapsed, remembered per browser
  const askCollapsedKey = 'codyze-inspector-ask-collapsed';
  let askCollapsed = $state(readAskCollapsed());

  function readAskCollapsed(): boolean {
    try {
      return localStorage.getItem(askCollapsedKey) === 'true';
    } catch {
      return false;
    }
  }

  function setAskCollapsed(collapsed: boolean) {
    askCollapsed = collapsed;
    try {
      localStorage.setItem(askCollapsedKey, String(collapsed));
    } catch {
      // Storage is not available, e.g. in a private window
    }
  }
  // Whether the inspector shows a node that has not been seen because the agent tab stayed open
  let inspectorUnseen = $state(false);
  // The same while the column is collapsed. This is only shown on the collapsed strip and does not
  // mark the inspector tab after the column was opened again
  let collapsedUnseen = $state(false);
  let sidebarOpen = $state(true);

  // The views of the sidebar, switched in the activity bar
  type SidebarView = 'files' | 'outline' | 'findings';
  let sidebarView = $state<SidebarView>('files');
  const sidebarViews: { id: SidebarView; label: string; icon: string }[] = [
    {
      id: 'files',
      label: 'Files',
      icon: 'M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'
    },
    {
      id: 'outline',
      label: 'Outline',
      icon: 'M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01'
    },
    {
      id: 'findings',
      label: 'Findings',
      icon: 'M12 3l9 16H3zM12 10v4M12 17h.01'
    }
  ];

  // Clicking the active view hides the sidebar
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
    openedUnit ? relativePath(selectedComponent, openedUnit).split('/') : []
  );

  // The first visible line of the open file
  let topLine = $state(1);

  // The function containing the inspected node in the open file, or the node if it is a function.
  // Without a selection in the file, it is the function at the top of what is visible
  const breadcrumbFunction = $derived.by((): NodeRefJSON | null => {
    const details = focus.details;
    if (details && details.node.translationUnitId === openedUnit?.id) {
      return (
        details.enclosingFunction ?? (details.node.type.includes('Function') ? details.node : null)
      );
    }
    const containing = (fileAnnotations?.functions ?? [])
      .map((f) => f.function)
      .filter((f) => f.startLine <= topLine && topLine <= f.endLine);
    return (
      containing.sort((a, b) => a.endLine - a.startLine - (b.endLine - b.startLine))[0] ?? null
    );
  });

  // The annotations of the open file, loaded by the code viewer and shown in the outline
  let fileAnnotations = $state.raw<FileAnnotationsJSON | null>(null);

  // The status of the calls of the open file by their node ID
  const callStatus = $derived(
    fileAnnotations ? new Map(fileAnnotations.calls.map((c) => [c.id, c.status])) : undefined
  );

  // Switches to the inspector after the user inspected a node, unless the agent is working: then
  // its tab stays open and the inspector tab is only marked
  function showInspector() {
    // A node the user inspects is the context of the next question again, also if the agent
    // revealed it before
    dismissedContextId = null;
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

  // Why the agent cannot be asked right now
  const askDisabledReason = $derived(
    isLoading ? 'The agent is still working' : !selectedModel ? 'No LLM provider configured' : null
  );
  // Incremented to open the input for the selected code, e.g. from the command palette
  let askRequest = $state(0);

  // The selected node is attached to questions as context, unless the user removed it
  let dismissedContextId = $state<string | null>(null);
  const contextNode = $derived(
    focus.details && focus.details.node.id !== dismissedContextId ? focus.details.node : null
  );

  function send() {
    showAgent();
    // The new question becomes the active thread
    chosenBlock = null;
    onSendMessage(
      contextNode ? describeNode(contextNode) : undefined,
      contextNode ? nodeLabel(contextNode) : undefined
    );
  }

  // The file tree shows the first component from the start, but no file is opened for the user
  $effect(() => {
    if (selectedComponentName || !analysisResult) return;
    selectedComponentName = analysisResult.components[0]?.name ?? null;
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
      class="rounded border border-blue-200 bg-white px-2 py-0.5 text-[11px] text-blue-700 hover:bg-blue-50 disabled:border-gray-200 disabled:text-gray-400"
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

<!-- What the agent does: a tool call with its main argument, or else e.g. "Writing". Its parts only
set their font, so that the light running over the row covers them -->
{#snippet labelText(label: ToolCallLabel | string)}
  {#if typeof label === 'string'}{label}{:else}<span class="font-mono">{label.tool}</span
    >{#if label.argument}&nbsp;<span class="font-mono">{label.argument}</span
      >{/if}{#if label.detail}&nbsp;· {label.detail}{/if}{/if}
{/snippet}

<!-- What the model wrote before a tool call, with code between backticks in a mono font -->
{#snippet narrationText(text: string)}
  {#each text.split('`') as part, i (i)}{#if i % 2 === 1}<span class="font-mono">{part}</span
      >{:else}{part}{/if}{/each}
{/snippet}

<!-- The end of an answer: how many places it cites, to step through them or see them in a graph, and
where its evidence reaches the limits of the analysis -->
{#snippet evidenceRow(b: number)}
  {@const places = citedPlaces(b)}
  {@const limits = (blockTrust[b]?.length ?? 0) + (blockEvidence[b]?.claimed.length ?? 0)}
  {@const hasGraph = graphSteps(timeline[b]).length > 0}
  <div class="mt-1 border-t border-gray-100 pt-1.5 pb-0.5">
    <div class="flex flex-wrap items-center gap-x-1 gap-y-1 text-xs">
      {#if places.length > 0}
        <span class="mr-1 text-gray-600"
          ><span class="text-teal-600" aria-hidden="true">✦</span>
          {places.length}
          {places.length === 1 ? 'place' : 'places'} cited</span
        >
        <button
          type="button"
          class="rounded px-1.5 py-0.5 text-blue-700 hover:bg-blue-50"
          title="Go through the places the answer cites in the code, one after the other"
          onclick={() => stepThrough(b)}
        >
          Step through
        </button>
        {#if hasGraph}
          <button
            type="button"
            class="rounded px-1.5 py-0.5 text-blue-700 hover:bg-blue-50"
            title="Show the paths and slices of all steps as one graph, with the dependences the answer relies on"
            onclick={() => {
              chosenBlock = b;
              focus.showing = 'thread';
              showEvidence(b);
            }}
          >
            Graph
          </button>
        {/if}
      {:else}
        <span
          class="text-gray-500"
          title="The answer cites no places in the code, so it is not backed by evidence from the analysis"
          >No places cited</span
        >
      {/if}
      {#if limits > 0}
        <button
          type="button"
          class="ml-auto flex items-center gap-1 rounded px-1.5 py-0.5 text-amber-700 hover:bg-amber-50"
          aria-expanded={!!limitsOpen[b]}
          onclick={() => (limitsOpen = { ...limitsOpen, [b]: !limitsOpen[b] })}
        >
          <span aria-hidden="true">⚠</span>
          {limits}
          {limits === 1 ? 'limit' : 'limits'}
          <svg
            class="h-3 w-3 transition-transform {limitsOpen[b] ? 'rotate-180' : ''}"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      {/if}
    </div>
    {#if limitsOpen[b]}
      <div transition:slide={{ duration: motion(150) }}>{@render trustNotes(b)}</div>
    {/if}
  </div>
{/snippet}

<!-- Where the analysis is uncertain for the evidence of a block, in the color of uncertainty -->
{#snippet trustNotes(b: number)}
  {@const issues = blockTrust[b] ?? []}
  {@const claimed = stepNodes(blockEvidence[b]?.claimed ?? [])}
  {#if issues.length > 0 || claimed.length > 0}
    <!-- Where the evidence reaches the limits of the analysis: code outside of it is a note, what the
    analysis could not resolve or the model claims without a tool is a warning -->
    <ul class="space-y-0.5 pt-1 text-[11px] leading-snug">
      {#each issues as issue (issue.kind + issue.location.id)}
        {@const subject = trustSubject(issue, b)}
        {@const info = issue.kind === 'EXTERNAL_CODE'}
        <li>
          <button
            type="button"
            class="flex w-full min-w-0 items-start gap-1 rounded px-1 text-left disabled:hover:bg-transparent {info
              ? 'text-gray-600 hover:bg-gray-50'
              : 'text-amber-700 hover:bg-amber-50'}"
            disabled={issue.location.startLine < 1}
            title={issue.location.code || issue.location.name}
            onclick={() => {
              chosenBlock = b;
              revealFromAgent(issue.location.id);
            }}
          >
            <span class={info ? 'text-orange-600' : ''} aria-hidden="true">{info ? '↗' : '⚠'}</span>
            <span class="min-w-0 flex-1"
              >{subject.text}
              {subject.plural ? 'rely' : 'relies'} on {issue.reason}
              <span class="font-mono whitespace-nowrap opacity-80"
                >· {trustLocation(issue.location)}</span
              ></span
            >
          </button>
        </li>
      {/each}
      {#if claimed.length > 0}
        <!-- Cited nodes no tool returned are claims of the model, not results of the analysis -->
        <li class="flex flex-wrap items-center gap-1 px-1 text-amber-700">
          <span aria-hidden="true">⚠</span>
          <span
            >The answer cites {claimed.length === 1 ? 'a node' : `${claimed.length} nodes`} no tool returned:</span
          >
          {#each claimed as ref (ref.id)}
            <button
              type="button"
              class="rounded border border-dashed border-gray-300 px-1 font-mono text-[10px] text-gray-700 hover:border-gray-500 hover:bg-gray-50"
              onclick={() => {
                chosenBlock = b;
                revealFromAgent(ref.id);
              }}
            >
              {citationLabel(ref)}
            </button>
          {/each}
        </li>
      {/if}
    </ul>
  {/if}
{/snippet}

<!-- The reasoning of the model before a message, collapsed -->
{#snippet reasoning(message: ChatMessage)}
  {#if message.reasoning}
    <div class="my-1 inline-block">
      <button
        class="flex items-center gap-1.5 text-xs text-gray-400 transition-colors hover:text-gray-600"
        onclick={() => toggleReasoning(message.id)}
      >
        <svg
          class="h-3 w-3 transition-transform duration-200 {expandedReasoning.has(message.id)
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
{/snippet}

{#snippet workingDot()}
  <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-teal-500" title="The agent is working"
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

<CommandPalette
  bind:query={paletteQuery}
  files={paletteFiles}
  recentIds={recentUnitIds}
  commands={paletteCommands}
  onOpenFile={(unit) => handleFileSelect(unit)}
/>

<div class="flex h-full min-h-0 bg-white">
  <!-- Activity bar: switches the view of the sidebar -->
  <div class="flex w-11 shrink-0 flex-col items-center border-r border-gray-200 bg-gray-100">
    {#each sidebarViews as view (view.id)}
      {@const active = sidebarOpen && sidebarView === view.id}
      <button
        type="button"
        class="flex h-11 w-11 items-center justify-center {active
          ? 'text-gray-900 shadow-[inset_2px_0_0_#2563eb]'
          : 'text-gray-500 hover:text-gray-900'}"
        onclick={() => toggleSidebar(view.id)}
        aria-label={active ? `Hide ${view.label}` : `Show ${view.label}`}
        aria-pressed={active}
        title={view.label}
      >
        <svg
          class="h-5 w-5"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          stroke-width="1.6"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d={view.icon} />
        </svg>
      </button>
    {/each}
  </div>

  <!-- Sidebar -->
  {#if sidebarOpen && selectedComponent}
    <div class="flex min-h-0 w-56 shrink-0 flex-col border-r border-gray-200">
      {#if sidebarView === 'files'}
        <FileTree
          component={selectedComponent}
          allComponents={analysisResult?.components}
          currentUnitId={selectedTranslationUnit?.id}
          onFileSelect={handleFileSelect}
          onComponentSelect={handleComponentSelect}
          conceptSuggestions={tusWithSuggestions}
          {conceptCounts}
          agentUnits={threadUnits}
          revealUnit={treeReveal}
          embedded
        />
      {:else if sidebarView === 'findings'}
        <FindingsView
          categories={analysisResult?.requirementCategories ?? []}
          findings={analysisResult?.findings ?? []}
          fileOf={findingFile}
          onOpen={openFinding}
          onAsk={askAboutRequirement}
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
  <div class="flex min-h-0 min-w-[20rem] flex-1 flex-col">
    {#if tabs.tabs.length > 0}
      <!-- Editor tabs -->
      <div
        class="flex h-9 shrink-0 items-stretch overflow-x-auto border-b border-gray-200 bg-gray-50"
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
              class="flex min-w-0 items-center gap-1.5 py-1 pr-1 pl-3 text-[12.5px]"
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
    <!-- One bar for the steps shown in the code: the path or the agent's thread -->
    {#if sequence === 'thread' && threadSteps.length > 0}
      <StepBar
        agent
        title="Answer"
        steps={threadSteps.map((s) => ({ label: s.label, title: s.title }))}
        index={threadStepIndex}
        onSelect={goToThreadStep}
        onClose={() => layers.toggle('agent')}
        closeTitle="Hide the places the answer cites"
        other={focus.path.length >= 2
          ? { label: 'Path', onSwitch: () => (focus.showing = 'path') }
          : undefined}
      />
    {:else if sequence === 'path'}
      <StepBar
        title="Path"
        steps={pathSteps}
        index={focus.pathIndex}
        onSelect={goToStep}
        onClose={() => focus.clearPath()}
        closeTitle="Close the dataflow path"
        other={threadSteps.length > 0
          ? { label: 'Answer', onSwitch: () => (focus.showing = 'thread') }
          : undefined}
      />
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
          componentName={selectedComponentName ?? undefined}
          selectedNodeId={selectedNode?.id}
          onNavigateToNode={handleNavigateToNode}
          {focus}
          bind:annotations={fileAnnotations}
          bind:topLine
          {scrollPositions}
          externalInspector
          lanes
          onInspect={showInspector}
          onAsk={askAboutSelection}
          {askDisabledReason}
          {askRequest}
          slice={graphSlice}
          {citedLines}
          citedFlash={citedFlash?.unitId === openedUnit.id ? citedFlash : null}
          findings={openFindings}
          onCodeContextMenu={openCodeMenu}
        >
          {#snippet layerActions()}
            {@const details = focus.details}
            <button
              type="button"
              class="h-6 rounded-md border px-2.5 text-[11.5px] disabled:text-gray-300 {graph.open
                ? 'border-blue-200 bg-blue-50 text-blue-700'
                : 'border-gray-200 text-gray-500 hover:text-gray-800'}"
              aria-pressed={graph.open}
              disabled={!graph.open && !details}
              title={graph.open
                ? 'Close the dependence graph (Esc)'
                : details
                  ? `Show what affects the selected node in the dependence graph (${graph.graph})`
                  : 'Select a node first'}
              onclick={() =>
                graph.open ? graph.close() : details && graph.show(details.node.id, 'BACKWARD')}
            >
              Graph
            </button>
          {/snippet}
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
          No file is open. Open one in the file tree or with {modifierLabel()}P.
        </div>
      {/if}
    </div>
  </div>

  <!-- The dependence graph, which narrows the code -->
  {#if graph.open && openedUnit}
    <!-- svelte-ignore a11y_no_static_element_interactions -->
    <div
      class="w-1 shrink-0 cursor-col-resize border-l border-gray-200 hover:bg-blue-200"
      onpointerdown={startGraphResize}
      title="Drag to resize"
    ></div>
    <div class="min-h-0 min-w-[18.75rem]" style:flex="0 1 {graphWidth}px">
      <GraphPanelView
        panel={graph}
        inspectedId={focus.details?.node.id ?? null}
        onInspect={selectRef}
        onSelectNode={selectGraphNode}
      />
    </div>
  {/if}

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
            callFilter={focus.callFilter}
            {callStatus}
            onClearCallFilter={() => (focus.callFilter = null)}
            onFollow={(chain, direction) => focus.follow(chain, direction)}
            onSelect={selectRef}
          />
        </div>
        {#if focus.details}
          <!-- The questions to the agent about the inspected node, always at hand below its details.
          They collapse to their title when the details need the room -->
          <div class="shrink-0 border-t border-gray-200 bg-white px-3 py-2">
            <button
              type="button"
              class="flex w-full items-center gap-1.5 text-[10px] font-semibold tracking-wide text-gray-500 uppercase hover:text-gray-800"
              aria-expanded={!askCollapsed}
              onclick={() => setAskCollapsed(!askCollapsed)}
            >
              <span class="text-[11px] text-teal-600" aria-hidden="true">✦</span>
              Ask the agent
              <span class="ml-auto text-gray-400" aria-hidden="true"
                >{askCollapsed ? '▸' : '▾'}</span
              >
            </button>
            {#if !askCollapsed}
              <div class="mt-2 flex flex-wrap gap-1.5">{@render nodeActions(focus.details)}</div>
            {/if}
          </div>
        {/if}
      {:else}
        <!-- Messages -->
        <div
          class="min-h-0 flex-1 overflow-y-auto bg-gray-50"
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
                    class="w-full rounded-lg border border-gray-200 px-3 py-2 text-left text-xs text-gray-700 hover:border-teal-300 hover:bg-teal-50 disabled:opacity-50"
                    disabled={!selectedModel}
                    onclick={() => ask(question)}
                  >
                    {question}
                  </button>
                {/each}
              </div>
            </div>
          {/if}

          {#each timeline as block, b (b)}
            {@const isLast = b === timeline.length - 1}
            {@const running = isLast && isLoading}
            {@const isActive = b === activeBlock}
            {@const evidence = blockEvidence[b]}
            {@const hasEvidence =
              evidence.steps.some((s) => s.ids.length) || evidence.claimed.length > 0}
            {@const stepEntries = block.entries.filter(
              (e) =>
                e.message.contentType === 'tool-result' || e.message.contentType === 'narration'
            )}
            {@const answers = block.entries.filter(
              (e) =>
                e.message.contentType !== 'tool-result' &&
                e.message.contentType !== 'narration' &&
                e.message.content
            )}
            {@const stepCount = stepEntries.filter((e) => e.step).length}
            {@const showSteps = stepsShown(b)}
            <section
              class="mx-2 my-2 rounded-md border bg-white {isActive && hasEvidence
                ? 'border-slate-400'
                : 'border-gray-200'}"
            >
              {#if block.question}
                <!-- The question, with the code it refers to -->
                <header class="border-b border-gray-100 px-3 py-2">
                  <p class="text-sm leading-snug font-medium whitespace-pre-wrap text-gray-900">
                    {block.question.content}
                  </p>
                  {#if block.question.context}
                    <span
                      class="mt-1.5 inline-flex h-5 max-w-full min-w-0 items-center gap-1 rounded-full bg-slate-100 px-2 text-[11px] text-slate-600"
                      title="Sent as context: {block.question.context}"
                    >
                      <svg
                        class="h-3 w-3 shrink-0 text-slate-400"
                        viewBox="0 0 16 16"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.75"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        aria-hidden="true"
                        ><path d="M5.5 4.5 2 8l3.5 3.5M10.5 4.5 14 8l-3.5 3.5" /></svg
                      >
                      <span class="min-w-0 truncate font-mono"
                        >{block.question.contextLabel ?? block.question.context}</span
                      >
                    </span>
                  {/if}
                </header>
              {/if}
              <div class="px-3 py-1.5">
                {#if stepEntries.length > 0 || running}
                  <!-- The steps of the agent: while it works, one line with what it does right now,
                  which changes with each tool call. Once it has answered, the line sums up the
                  steps, which it unfolds -->
                  <button
                    type="button"
                    class="-mx-1 flex w-[calc(100%+0.5rem)] min-w-0 items-center gap-1.5 rounded px-1 py-1 text-left text-xs text-gray-500 hover:bg-gray-50 hover:text-gray-800"
                    aria-expanded={showSteps}
                    onclick={() => (stepsOpen = { ...stepsOpen, [b]: !showSteps })}
                  >
                    <svg
                      class="h-3 w-3 shrink-0 transition-transform {showSteps ? 'rotate-90' : ''}"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="2"
                      aria-hidden="true"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                    </svg>
                    {#if running}
                      {@const label = liveLabel}
                      <span class="relative min-w-0 flex-1 truncate">
                        {#key labelKey(label)}
                          <span class="activity block truncate" in:fade={{ duration: motion(150) }}
                            >{@render labelText(label)}</span
                          >
                        {/key}
                      </span>
                      <span class="shrink-0 text-gray-400 tabular-nums"
                        >{stepCount} {stepCount === 1 ? 'step' : 'steps'} · {duration(b)}</span
                      >
                    {:else}
                      <span class="tabular-nums"
                        >{stepCount}
                        {stepCount === 1 ? 'step' : 'steps'} · {duration(b)}</span
                      >
                    {/if}
                  </button>
                  {#if showSteps}
                    <div class="pb-1" transition:slide={{ duration: motion(200) }}>
                      {#each stepEntries as entry (entry.index)}
                        {@const message = entry.message}
                        {@render reasoning(message)}
                        {#if message.contentType === 'narration'}
                          <!-- What the model wrote before its next tool call -->
                          <p class="py-0.5 pl-5.5 text-xs leading-snug text-gray-500">
                            {@render narrationText(message.content)}
                          </p>
                        {:else if message.toolResult}
                          <!-- A tool call: a numbered step of the evidence trail -->
                          {@const nodes = stepNodes(
                            evidence.steps.find((s) => s.step === entry.step)?.ids ?? []
                          )}
                          {@const toolPaths = extractToolPaths(message.toolResult.content)}
                          {@const toolSlice = extractToolSlice(message.toolResult.content)}
                          <ToolStep
                            toolResult={message.toolResult}
                            label={message.toolResult.arguments
                              ? labelOf(
                                  message.toolResult.toolName ?? '',
                                  message.toolResult.arguments
                                )
                              : undefined}
                            paths={toolPaths}
                            slice={toolSlice}
                            step={entry.step ?? 0}
                            {nodes}
                            selectedId={focus.details?.node.id}
                            onSelect={(ref) => revealFromAgent(ref.id)}
                            onActivate={() => {
                              chosenBlock = b;
                              focus.showing = 'thread';
                              if (!showToolGraph(message.toolResult?.content) && nodes[0])
                                revealFromAgent(nodes[0].id);
                            }}
                            onHover={(hovered) => {
                              // Only the steps of the thread in the code can stand out there
                              focus.highlightedStep =
                                hovered && isActive ? (entry.step ?? null) : null;
                            }}
                          />
                        {/if}
                      {/each}
                    </div>
                  {/if}
                {/if}

                {#if block.entries.some((e) => e.index === suggestionAnchorIndex)}
                  <!-- The pending concept suggestions of the agent, to accept or reject. They stay in
                  view when the steps are folded -->
                  <div class="my-1 overflow-hidden rounded border border-gray-200">
                    <ConceptChecklist
                      bind:items={suggestions}
                      onApplySuggestions={handleApplyAndReload}
                      onHighlightNode={revealFromAgent}
                    />
                  </div>
                {/if}

                {#each answers as entry (entry.index)}
                  <!-- The answer (or an error) -->
                  {@render reasoning(entry.message)}
                  <div class="max-w-none min-w-0 py-1.5 text-gray-800">
                    <MarkdownRenderer
                      content={entry.message.content}
                      citations={citedRefs}
                      onCite={openCitation}
                      onCiteHover={hoverCitation}
                      compact
                    />
                  </div>
                {/each}

                {#if running}
                  <!-- Below the line of what the agent does: what it writes right now, the start of
                  its answer or what it is about to do, or else what it last said it is about to do.
                  The latter joins its steps -->
                  {@const said = stepEntries.findLast((e) => e.message.contentType === 'narration')}
                  {#if displayContent}
                    <div class="max-w-none min-w-0 py-1.5 text-gray-800">
                      <MarkdownRenderer
                        content={displayContent}
                        citations={citedRefs}
                        onCite={openCitation}
                        onCiteHover={hoverCitation}
                        compact
                      />
                    </div>
                  {:else if said}
                    <p
                      class="py-1 text-xs leading-snug text-gray-500"
                      in:fade={{ duration: motion(150) }}
                    >
                      {@render narrationText(said.message.content)}
                    </p>
                  {/if}
                {/if}

                {#if !running && answers.some((e) => e.message.contentType === 'text')}
                  {@render evidenceRow(b)}
                {:else if !running && answers.length === 0}
                  <p class="pb-0.5 text-xs text-gray-500">
                    No answer: the agent was stopped or ended without one.
                  </p>
                {/if}
              </div>
            </section>
          {/each}
        </div>

        <!-- Input -->
        <div class="shrink-0 border-t border-gray-200 px-3 pt-2 pb-2">
          {#if contextNode}
            <!-- The selected node, which is sent along with the question -->
            <div class="mb-1.5 flex">
              <span
                class="flex h-6 max-w-full min-w-0 items-center gap-1.5 rounded-full bg-slate-100 pr-0.5 pl-2 text-[11px] text-slate-700"
                title="Sent as context: {describeNode(contextNode)}"
              >
                <svg
                  class="h-3 w-3 shrink-0 text-slate-500"
                  viewBox="0 0 16 16"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.75"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"><path d="M5.5 4.5 2 8l3.5 3.5M10.5 4.5 14 8l-3.5 3.5" /></svg
                >
                <span class="min-w-0 truncate font-mono font-medium"
                  >{contextNode.name || contextNode.code}</span
                >
                {#if contextNode.fileName}
                  <span class="shrink-0 text-slate-400"
                    >{fileName(contextNode.fileName)}:{contextNode.startLine}</span
                  >
                {/if}
                <button
                  type="button"
                  class="flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-slate-400 hover:bg-slate-200 hover:text-slate-700"
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
            disabled={!selectedModel}
            running={isLoading}
            {onStop}
            prompts={agentSession.mcpCapabilities?.prompts}
            onPromptSelect={(name, args) => {
              if (isLoading) return;
              showAgent();
              onPromptSelect?.(name, args);
            }}
            onNewChat={onReset}
          >
            {#snippet toolbar()}
              <SessionBar {models} {selectedModel} {onModelSelect} />
            {/snippet}
          </MessageInput>
        </div>
      {/if}
    </div>
  {/if}
</div>

{#if codeMenu}
  <CodeContextMenu
    x={codeMenu.x}
    y={codeMenu.y}
    items={codeMenuItems}
    title="{codeMenu.details.node.type} {codeMenu.details.node.code}"
    onClose={() => (codeMenu = null)}
  />
{/if}

{#if citeHover}
  <!-- The preview of the hovered citation: where it is and its code, the cited lines emphasized -->
  {@const ref = citeHover.ref}
  <div
    class="pointer-events-none fixed z-[60] w-[22rem] overflow-hidden rounded-md border border-gray-200 bg-white shadow-lg"
    style:left="{Math.max(8, Math.min(citeHover.chip.left, window.innerWidth - 360))}px"
    style:top="{citeHover.chip.bottom + 6}px"
  >
    <div class="flex items-center gap-2 border-b border-gray-100 px-2.5 py-1.5 text-[11px]">
      <span class="font-mono text-gray-700"
        >{ref.fileName ?? ref.name}{ref.startLine >= 1 ? `:${ref.startLine}` : ''}</span
      >
      <span class="text-gray-400">{ref.type}</span>
      {#if ref.isInferred || !ref.translationUnitId}
        <span class="ml-auto text-orange-600" title="Not part of the analysed code">↗ external</span
        >
      {/if}
    </div>
    {#if previewLines.length > 0}
      <pre
        class="m-0 overflow-hidden bg-gray-50 py-1 font-mono text-[11px] leading-[1.15rem]">{#each previewLines as line (line.number)}<div
            class="flex px-2 {line.cited ? 'bg-teal-50 text-gray-900' : 'text-gray-500'}"><span
              class="mr-2 w-6 shrink-0 text-right text-gray-400 select-none">{line.number}</span
            ><span class="overflow-hidden text-ellipsis whitespace-pre">{line.text}</span
            ></div>{/each}</pre>
    {:else}
      <div class="truncate px-2.5 py-1.5 font-mono text-[11px] text-gray-700">
        {ref.code || ref.name}
      </div>
    {/if}
    <div class="px-2.5 py-1 text-[10.5px] text-gray-400">Click to show it in the code</div>
  </div>
{/if}

<style>
  /* The tool call the agent is running: a light that runs over the text, as long as it runs */
  .activity {
    color: rgb(55, 65, 81);
  }

  @media (prefers-reduced-motion: no-preference) {
    .activity {
      background: linear-gradient(
          90deg,
          rgb(75, 85, 99) 0%,
          rgb(75, 85, 99) 40%,
          rgb(209, 213, 219) 50%,
          rgb(75, 85, 99) 60%,
          rgb(75, 85, 99) 100%
        )
        0 0 / 250% 100%;
      background-clip: text;
      -webkit-background-clip: text;
      color: transparent;
      animation: activity-shimmer 2.2s linear infinite;
    }
  }

  @keyframes activity-shimmer {
    from {
      background-position: 100% 0;
    }
    to {
      background-position: -150% 0;
    }
  }
</style>
