<script lang="ts">
  import { untrack, type Snippet } from 'svelte';
  import type { TranslationUnitJSON, NodeJSON } from '$lib/types';
  import { TabNavigation } from '$lib/components/navigation';
  import { CollapsiblePanel } from '$lib/components/ui';
  import { NodeTable, NodeOverlays, FindingOverlay } from '$lib/components/analysis';
  import NodeInspector from './inspector/NodeInspector.svelte';
  import CodeAnnotations from './CodeAnnotations.svelte';
  import OverviewRuler, { type RulerMark } from './OverviewRuler.svelte';
  import HoverCard from './HoverCard.svelte';
  import DataflowArcs from './DataflowArcs.svelte';
  import StepMarkers, { type StepMarker } from './StepMarkers.svelte';
  import { getAnnotations } from '$lib/annotations';
  import { layers, layerInfos, type Layer } from '$lib/stores/layers.svelte';
  import type { FileAnnotationsJSON } from '$lib/types';
  import AddConceptDialog from '../forms/AddConceptDialog.svelte';
  import type { NodeDetailsJSON, NodeRefJSON } from '$lib/types';
  import type { FlattenedNode } from '$lib/flatten';
  import { getNodeDetails, getNodeDetailsAt, clearNodeDetailsCache } from '$lib/nodeDetails';
  import {
    CodeFocus,
    type FlowDirection,
    type InspectorSection
  } from '$lib/stores/codeFocus.svelte';
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
    '.rb': ruby
  };

  function getLanguage(fileName: string) {
    const ext = fileName.substring(fileName.lastIndexOf('.'));
    return languageMap[ext] || plaintext;
  }

  interface Props {
    translationUnit: TranslationUnitJSON;
    /** The nodes listed in the panel next to the code; not needed with an external inspector */
    astNodes?: NodeJSON[];
    overlayNodes?: NodeJSON[];
    conceptGroups?: any[];
    highlightLine?: number;
    finding?: string;
    findingKind?: string;
    headerActions?: Snippet;
    /** Replaces the file name at the start of the header, e.g. with a breadcrumb */
    headerStart?: Snippet;
    nodePanelCollapsed?: boolean;
    onClose?: () => void;
    /** The component of the unit; enables selecting nodes by clicking into the code */
    componentName?: string;
    /** The node to inspect initially, e.g. from a deep link */
    selectedNodeId?: string;
    /** Called when a node in another file is selected in the inspector */
    onNavigateToNode?: (ref: NodeRefJSON) => void;
    /** Additional actions for the inspected node, e.g. asking the agent about it */
    nodeActions?: Snippet<[NodeDetailsJSON]>;
    /**
     * The inspected node. Pass it to share the inspected node with the parent, e.g. to show the
     * inspector outside of the viewer
     */
    focus?: CodeFocus;
    /**
     * Whether the inspector is shown by the parent (using [focus]). The viewer then has no panel
     * at all and the code takes the full space
     */
    externalInspector?: boolean;
    /** Called when the user inspects a node in the viewer, e.g. by clicking into the code */
    onInspect?: () => void;
    /**
     * The scroll positions of files by their ID, to restore them when the viewer shows a file again
     * (e.g. when switching between tabs). The viewer keeps them up to date
     */
    scrollPositions?: Map<string, number>;
    /** The annotations of the file, loaded by the viewer; bind it to use them outside */
    annotations?: FileAnnotationsJSON | null;
    /**
     * Adds lanes to the gutter: one for the numbered steps of the path (see [CodeFocus.path]) and
     * one between the line numbers and the code for the dataflow arcs of the inspected node
     */
    lanes?: boolean;
  }

  let {
    translationUnit,
    astNodes = [],
    overlayNodes = [],
    conceptGroups,
    highlightLine,
    finding,
    findingKind,
    headerActions,
    headerStart,
    nodePanelCollapsed = $bindable(false),
    onClose,
    componentName,
    selectedNodeId,
    onNavigateToNode,
    nodeActions,
    focus: sharedFocus,
    externalInspector = false,
    onInspect,
    scrollPositions,
    annotations = $bindable(null),
    lanes = false
  }: Props = $props();

  // The focus and the placement of the inspector never change for a viewer
  // svelte-ignore state_referenced_locally
  const focus = sharedFocus ?? new CodeFocus();
  // svelte-ignore state_referenced_locally
  const inspectorInPanel = !!componentName && !externalInspector;
  // svelte-ignore state_referenced_locally
  const hasPanel = !externalInspector;

  let activeTab = $state(inspectorInPanel ? 'inspector' : 'astNodes');
  let nodes = $derived(
    flattenNodes(activeTab === 'overlayNodes' ? overlayNodes : astNodes, '', translationUnit.id)
  );
  let highlightedNode = $state<NodeJSON | null>(null);
  let codeContainerElement = $state<HTMLDivElement>();

  const tabs = $derived([
    ...(inspectorInPanel ? [{ id: 'inspector', label: 'Inspector' }] : []),
    { id: 'astNodes', label: 'AST Nodes', count: astNodes?.length || 0 },
    { id: 'overlayNodes', label: 'Overlay Nodes', count: overlayNodes?.length || 0 }
  ]);

  // The node shown in the inspector
  const inspected = $derived(focus.details);

  function inspect(
    load: () => Promise<NodeDetailsJSON | null>,
    reveal = false,
    section?: InspectorSection
  ) {
    if (inspectorInPanel) {
      activeTab = 'inspector';
      nodePanelCollapsed = false;
    }
    onInspect?.();
    focus.inspect(load, reveal, section);
  }

  // When another file is shown in the same viewer (e.g. by switching tabs), go back to where it was
  // scrolled to before. This runs before revealing a node, which takes precedence
  $effect(() => {
    const unitId = translationUnit.id;
    if (!scrollPositions || !codeContainerElement) return;
    codeContainerElement.scrollTop = untrack(() => scrollPositions.get(unitId) ?? 0);
  });

  function rememberScrollPosition() {
    hideHover();
    if (codeContainerElement)
      scrollPositions?.set(translationUnit.id, codeContainerElement.scrollTop);
  }

  // Scroll to the inspected node when it is to be revealed, e.g. after selecting it in the inspector.
  // If the node is in another file, it is revealed once that file is shown
  let revealed = 0;
  $effect(() => {
    const count = focus.revealCount;
    const unitId = translationUnit.id;
    if (count === revealed) return;
    const node = untrack(() => focus.details?.node);
    if (node && node.translationUnitId === unitId) {
      revealed = count;
      scrollToLine(node.startLine);
    }
  });

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
    if (componentName && id) inspect(() => getNodeDetails(id), true);
  });

  // The position in the code under the mouse: a 1-based line and a CPG column (1 is the first
  // character), or null outside of the lines
  function positionAt(event: MouseEvent): { line: number; column: number } | null {
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const rem = remInPx();
    const line = Math.floor(((event.clientY - rect.top) / rem - offsetTop) / lineHeight) + 1;
    const column = Math.floor(((event.clientX - rect.left) / rem - offsetLeft) / charWidth);
    if (line < 1 || line > totalLines || column < 0) return null;
    return { line, column };
  }

  function handleCodeClick(event: MouseEvent) {
    hideHover();
    if (!componentName) return;
    // Do not interfere with selecting text
    if (window.getSelection()?.toString()) return;
    const position = positionAt(event);
    if (!position) return;
    const component = componentName;
    inspect(() => getNodeDetailsAt(component, translationUnit.id, position.line, position.column));
  }

  // The hover card for the node under the mouse, shown after resting on it for a moment. The
  // position is relative to the code area
  let hover = $state.raw<{
    details: NodeDetailsJSON;
    x: number;
    /** The distance of the card from the top, or from the bottom near the end of the code area */
    y: number;
    above: boolean;
  } | null>(null);
  let hoverTimer: ReturnType<typeof setTimeout> | undefined;
  // Incremented to discard pending lookups, e.g. when the mouse moved on
  let hoverRequest = 0;
  let codeAreaElement = $state<HTMLDivElement>();
  const hoverDelay = 400;

  function hideHover() {
    clearTimeout(hoverTimer);
    hoverRequest++;
    hover = null;
  }

  function isInside(node: NodeRefJSON, line: number, column: number): boolean {
    if (line < node.startLine || line > node.endLine) return false;
    if (line === node.startLine && column < node.startColumn) return false;
    return !(line === node.endLine && column >= node.endColumn);
  }

  function handleCodeMouseMove(event: MouseEvent) {
    if (!componentName || event.buttons !== 0) return hideHover();
    const position = positionAt(event);
    // Only over the code itself, not over whitespace or after the end of a line
    const char = position ? codeLines[position.line - 1]?.[position.column - 1] : undefined;
    if (!position || !char || /\s/.test(char)) return hideHover();
    // A shown card stays while the mouse is on its node
    if (hover && isInside(hover.details.node, position.line, position.column)) return;

    hideHover();
    const request = hoverRequest;
    const component = componentName;
    const unitId = translationUnit.id;
    const area = codeAreaElement?.getBoundingClientRect();
    const x = area ? event.clientX - area.left : 0;
    const y = area ? event.clientY - area.top : 0;
    hoverTimer = setTimeout(async () => {
      const details = await getNodeDetailsAt(
        component,
        unitId,
        position.line,
        position.column
      ).catch(() => null);
      if (request !== hoverRequest || !details) return;
      // Keep the card inside the code area (it is 18rem wide)
      const maxX = (area?.width ?? 0) - 18 * remInPx() - 8;
      // Show the card above the mouse if there is not enough room below it
      const height = area?.height ?? 0;
      const above = y + 180 > height && y > 180;
      hover = {
        details,
        x: Math.max(4, Math.min(x + 12, maxX)),
        y: above ? height - y + 8 : y + 18,
        above
      };
    }, hoverDelay);
  }

  // Annotations of the file (function key figures, call status, concepts), loaded per unit
  async function loadAnnotations(component: string, unitId: string) {
    const result = await getAnnotations(component, unitId);
    if (unitId === translationUnit.id) annotations = result;
  }

  // Reloaded when the analysis result changed, e.g. after concepts were added
  $effect(() => {
    void focus.revision;
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

  // The number of elements of each layer in this file, and a tooltip for its toggle
  function layerSummary(layer: Layer): { count: number; title: string } {
    const info = layerInfos.find((l) => l.id === layer)!;
    if (!fileSummary) return { count: 0, title: info.description };
    switch (layer) {
      case 'concepts':
        return { count: fileSummary.concepts, title: info.description };
      case 'external':
        return {
          count: fileSummary.external,
          title:
            info.description +
            (fileSummary.topExternal.length ? ` (${fileSummary.topExternal.join(', ')})` : '')
        };
      case 'uncertain':
        return { count: fileSummary.unresolved, title: info.description };
      case 'dataflow': {
        const count = inspected ? inspected.dataflowFrom.length + inspected.dataflowTo.length : 0;
        return {
          count,
          title: inspected ? info.description : `${info.description} (nothing is selected)`
        };
      }
      case 'agent':
        return {
          count: new Set(agentMarkers.map((m) => m.nodeId)).size,
          title: focus.thread.length
            ? info.description
            : `${info.description} (the agent has not returned any nodes yet)`
        };
    }
  }

  // The dataflow and agent layers need the lanes in the gutter
  const shownLayers = $derived(
    layerInfos.filter((l) => lanes || (l.id !== 'dataflow' && l.id !== 'agent'))
  );

  // The marks of the visible layers in the overview ruler, one lane per layer. The steps of the
  // path and the dataflows of the inspected node share the last lane, with the steps on top
  const rulerMarks = $derived.by((): RulerMark[] => {
    if (!annotations) return [];
    const color = (layer: Layer) => layerInfos.find((l) => l.id === layer)!.color;
    const marks: RulerMark[] = [];
    if (lanes) {
      for (const step of agentMarkers) {
        marks.push({
          line: step.line,
          lane: 4,
          color: color('agent'),
          label: `${step.index + 1}. ${step.title}`
        });
      }
      for (const step of pathMarkers) {
        marks.push({ line: step.line, lane: 3, color: 'rgb(15, 23, 42)', label: step.title });
      }
      if (layers.visible.dataflow && inspected?.node.translationUnitId === translationUnit.id) {
        const dataflows = [
          ...inspected.dataflowFrom.map((ref) => ({ ref, label: 'comes from' })),
          ...inspected.dataflowTo.map((ref) => ({ ref, label: 'goes to' }))
        ];
        for (const { ref, label } of dataflows) {
          if (ref.translationUnitId !== translationUnit.id || ref.startLine < 1) continue;
          marks.push({
            line: ref.startLine,
            lane: 3,
            color: color('dataflow'),
            label: `${label} ${ref.code || ref.name}`
          });
        }
      }
    }
    // While a path or the agent's thread is shown, the other layers step back
    const faded = pathActive || threadActive;
    if (layers.visible.concepts) {
      for (const c of annotations.concepts) {
        const kind = c.isOperation ? 'Operation' : 'Concept';
        marks.push({
          line: c.line,
          lane: 0,
          color: color('concepts'),
          label: `${kind} ${c.type}`,
          faded
        });
      }
    }
    for (const call of annotations.calls) {
      if (call.status === 'EXTERNAL' && layers.visible.external) {
        marks.push({
          line: call.startLine,
          lane: 1,
          color: color('external'),
          label: `external call ${call.name}()`,
          faded
        });
      } else if (call.status === 'UNRESOLVED' && layers.visible.uncertain) {
        marks.push({
          line: call.startLine,
          lane: 2,
          color: color('uncertain'),
          label: `unresolved call ${call.name}()`,
          faded
        });
      }
    }
    return marks;
  });

  // The lines of the inspected node, if it is in this file
  const selectedLines = $derived.by(() => {
    const node = inspected?.node;
    if (!node || node.translationUnitId !== translationUnit.id || node.startLine < 1) return null;
    return { first: node.startLine, last: Math.max(node.endLine, node.startLine) };
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
      singleLine,
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
      focus.invalidate();
    }
    conceptDialogWasOpen = showConceptDialog;
  });

  function scrollToLine(line: number, behavior: ScrollBehavior = 'smooth') {
    if (!codeContainerElement || typeof window === 'undefined') return;
    const top = (offsetTop + (line - 3) * lineHeight) * remInPx();
    codeContainerElement.scrollTo({ top: Math.max(0, top), behavior });
  }

  // The highlighted line, 0-based
  const allHighlightLines = $derived(highlightLine ? [highlightLine - 1] : []);

  const lineHeight = 1.5;
  const charWidth = 0.60015625;
  const offsetTop = 1;
  const baseOffsetLeft = 2.4;

  const code = $derived(translationUnit.code ?? '');
  const codeLines = $derived(code.split('\n'));
  const totalLines = $derived(codeLines.length);
  const lineNumberWidth = $derived(Math.ceil(Math.log10(totalLines + 1)));
  // The lanes of the gutter: the markers of the agent's steps and of the path before the line
  // numbers, the arcs after them
  // svelte-ignore state_referenced_locally
  const agentLaneWidth = lanes ? 1.2 : 0;
  // svelte-ignore state_referenced_locally
  const markerLaneWidth = lanes ? 1.2 : 0;
  // svelte-ignore state_referenced_locally
  const arcLaneWidth = lanes ? 2.4 : 0;
  const offsetLeft = $derived(
    baseOffsetLeft + agentLaneWidth + markerLaneWidth + lineNumberWidth * charWidth + arcLaneWidth
  );
  // The arc lane ends shortly before the first character of a line (see CodeLines)
  const arcLaneRight = $derived(offsetLeft + charWidth - 0.35);
  const arcLaneLeft = $derived(arcLaneRight - arcLaneWidth + 0.2);
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
  // The lines that are actually visible (1-based, inclusive), for the overview ruler
  const viewportLines = $derived.by(() => {
    const rem = remInPx();
    const first = (codeViewport.scrollTop / rem - offsetTop) / lineHeight;
    const last = ((codeViewport.scrollTop + codeViewport.height) / rem - offsetTop) / lineHeight;
    return {
      first: Math.max(1, Math.floor(first) + 1),
      last: Math.min(totalLines, Math.ceil(last))
    };
  });

  const visibleLines = $derived.by(() => {
    const remPx = remInPx();
    return codeViewport.range(lineHeight * remPx, totalLines, 40, offsetTop * remPx);
  });

  // The visible part of the code in rem, e.g. to clip the arcs at its edges
  const viewTop = $derived(codeViewport.scrollTop / remInPx());
  const viewBottom = $derived((codeViewport.scrollTop + codeViewport.height) / remInPx());

  // Rendered width of a line in characters, with tabs being 8 characters wide
  function lineColumns(line: number): number {
    const text = codeLines[line - 1] ?? '';
    return text.length + (text.split('\t').length - 1) * 7;
  }

  // Where the free space at the end of a line starts, after the code lens of a function if there
  // is one (its width is estimated, since it is set in a proportional font)
  function endOfLine(line: number): number {
    const end = offsetLeft + charWidth * (lineColumns(line) + 3);
    const fn = annotations?.functions.find((f) => f.function.startLine === line);
    if (!fn) return end;
    const lens = `${fn.callers} callers · calls ${fn.callees} · ${fn.externalCalls} external · ${fn.unresolvedCalls} unresolved`;
    return end + charWidth + lens.length * 0.42;
  }

  // Follows a dataflow of the inspected node to [ref], extending the path, and inspects it
  function followDataflow(ref: NodeRefJSON, direction: FlowDirection) {
    if (inspected) focus.follow([inspected.node, ref], direction);
    selectRef(ref);
  }

  // The steps of the path in this file, and whether a path is shown at all
  const pathActive = $derived(lanes && focus.path.length >= 2);
  const pathMarkers = $derived.by((): StepMarker[] => {
    if (!pathActive) return [];
    return focus.path.flatMap((ref, index) =>
      ref.translationUnitId === translationUnit.id && ref.startLine >= 1
        ? [
            {
              index,
              line: ref.startLine,
              title: `${ref.code || ref.name} · ${ref.fileName}:${ref.startLine}`,
              current: index === focus.pathIndex
            }
          ]
        : []
    );
  });

  // The evidence of the agent's active thread in this file, and whether it is shown at all
  const threadActive = $derived(lanes && layers.visible.agent && focus.thread.length > 0);
  const agentMarkers = $derived.by((): StepMarker[] => {
    if (!threadActive) return [];
    return focus.thread.flatMap(({ ref, step, claimed }) =>
      ref.translationUnitId === translationUnit.id && ref.startLine >= 1
        ? [
            {
              index: step - 1,
              line: ref.startLine,
              title: `${claimed ? 'only cited in the answer, no tool returned it: ' : ''}${ref.code || ref.name} · ${ref.fileName}:${ref.startLine}`,
              current: ref.id === inspected?.node.id,
              dashed: claimed,
              // Nodes only cited in the answer are no step of the agent
              label: claimed ? '?' : undefined,
              nodeId: ref.id
            }
          ]
        : []
    );
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
  <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
    <!-- Header: where the file is, and below it the layer toggles -->
    <div class="shrink-0 border-b border-gray-200 bg-white px-4 py-1.5">
      <div class="flex min-h-6 items-center justify-between gap-3">
        <div class="flex min-w-0 items-center gap-3">
          {#if headerStart}
            {@render headerStart()}
          {:else}
            <div class="shrink-0 font-mono text-xs text-gray-500">{translationUnit.name}</div>
          {/if}
          {#if fileSummary}
            <span class="shrink-0 text-[11px] text-gray-400">{fileSummary.functions} functions</span
            >
          {/if}
        </div>
        {#if headerActions || onClose}
          <div class="flex shrink-0 items-center gap-2">
            {#if headerActions}
              {@render headerActions()}
            {/if}
            {#if onClose}
              <button
                onclick={onClose}
                class="flex h-8 w-8 items-center justify-center rounded-md text-gray-500 transition-colors hover:bg-gray-200 hover:text-gray-700"
                type="button"
                aria-label="Close panel"
              >
                <svg
                  class="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  stroke-width="2"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            {/if}
          </div>
        {/if}
      </div>
      {#if annotations}
        <!-- Layer toggles: each one shows or hides one kind of marks in the code -->
        <div class="mt-1 flex flex-wrap items-center gap-1" role="group" aria-label="Layers">
          {#each shownLayers as layer (layer.id)}
            {@const summary = layerSummary(layer.id)}
            <button
              type="button"
              class="flex items-center gap-1 rounded-full border px-2 py-px text-[11px] {layers
                .visible[layer.id]
                ? layer.activeClass
                : 'border-gray-200 text-gray-400 hover:text-gray-600'}"
              aria-pressed={layers.visible[layer.id]}
              title="{summary.title}. Click to {layers.visible[layer.id] ? 'hide' : 'show'}."
              onclick={() => layers.toggle(layer.id)}
            >
              <span>{layer.icon}</span>
              {layer.label}
              <span class="tabular-nums opacity-70">{summary.count}</span>
            </button>
          {/each}
        </div>
      {/if}
    </div>

    <div class="relative flex min-h-0 flex-1" bind:this={codeAreaElement}>
      <div
        class="relative min-w-0 flex-1 overflow-auto"
        style="transform: translateZ(0);"
        bind:this={codeContainerElement}
        onscroll={rememberScrollPosition}
      >
        <!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
        <div
          class="relative inline-block w-max min-w-full align-top"
          class:cursor-pointer={!!componentName}
          onclick={handleCodeClick}
          onmousemove={handleCodeMouseMove}
          onmouseleave={hideHover}
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
                gutterPadding={0.75 + arcLaneWidth}
                selection={selectedLines}
              />
            </Highlight>
          </div>

          {#if finding && highlightLine}
            <FindingOverlay
              {finding}
              kind={findingKind}
              line={highlightLine}
              {lineHeight}
              {offsetTop}
            />
          {/if}

          {#if annotations}
            <CodeAnnotations
              dimmed={pathActive || threadActive}
              {annotations}
              layers={layers.visible}
              {codeLines}
              startLine={visibleLines.start}
              endLine={visibleLines.end}
              {lineHeight}
              {charWidth}
              {offsetTop}
              {offsetLeft}
              onInspect={(id, section) => inspect(() => getNodeDetails(id), true, section)}
            />
          {/if}

          {#if lanes && layers.visible.dataflow && inspected}
            <DataflowArcs
              details={inspected}
              unitId={translationUnit.id}
              {codeLines}
              startLine={visibleLines.start}
              endLine={visibleLines.end}
              {viewTop}
              {viewBottom}
              {lineHeight}
              {charWidth}
              {offsetTop}
              {offsetLeft}
              laneLeft={arcLaneLeft}
              laneRight={arcLaneRight}
              {endOfLine}
              onFollow={followDataflow}
              onScrollTo={(line) => scrollToLine(line)}
            />
          {/if}

          {#if pathMarkers.length > 0}
            <StepMarkers
              markers={pathMarkers}
              startLine={visibleLines.start}
              endLine={visibleLines.end}
              {lineHeight}
              {offsetTop}
              left={baseOffsetLeft - 0.3 + agentLaneWidth}
              onSelect={(marker) => selectRef(focus.path[marker.index])}
            />
          {/if}

          {#if agentMarkers.length > 0}
            <StepMarkers
              markers={agentMarkers}
              startLine={visibleLines.start}
              endLine={visibleLines.end}
              {lineHeight}
              {offsetTop}
              left={baseOffsetLeft - 0.3}
              shape="square"
              onSelect={(marker) => {
                const node = focus.thread.find((n) => n.ref.id === marker.nodeId);
                if (node) selectRef(node.ref);
              }}
            />
          {/if}

          <!-- The selected node: a tint without a frame behind it, if it fits on one line. Larger nodes
          are only shown by their tinted lines (see CodeLines), so they do not cover the code -->
          {#if selectionBox?.singleLine}
            <div
              class="pointer-events-none absolute z-20 rounded bg-blue-600/[0.13]"
              style:top="{selectionBox.top}rem"
              style:left="{selectionBox.left}rem"
              style:width="{selectionBox.width}rem"
              style:height="{selectionBox.height}rem"
            ></div>
          {/if}

          <!-- The node boxes belong to the node lists in the panel -->
          {#if hasPanel && (activeTab === 'astNodes' || activeTab === 'overlayNodes')}
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

      {#if hover}
        <div
          class="pointer-events-none absolute z-40"
          style:left="{hover.x}px"
          style:top={hover.above ? undefined : `${hover.y}px`}
          style:bottom={hover.above ? `${hover.y}px` : undefined}
        >
          <HoverCard details={hover.details} />
        </div>
      {/if}

      {#if annotations}
        <OverviewRuler
          marks={rulerMarks}
          {totalLines}
          lanes={lanes ? 5 : 3}
          viewport={viewportLines}
          selectionLine={inspected?.node.translationUnitId === translationUnit.id
            ? inspected.node.startLine
            : undefined}
          onScrollTo={(line) => scrollToLine(line)}
        />
      {/if}
    </div>
  </div>

  <!-- Node information panel -->
  {#snippet panelContent()}
    {#if activeTab === 'inspector'}
      <div class="min-h-0 flex-1 overflow-y-auto">
        <NodeInspector
          details={inspected}
          loading={focus.loading}
          error={focus.error}
          revealSection={focus.revealSection}
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
    {:else}
      <!-- NodeTable is virtualized and needs to be its own scroll container -->
      <div class="min-h-0 flex-1 px-2">
        <NodeTable
          {nodes}
          bind:highlightedNode
          nodeClick={(node) =>
            componentName
              ? inspect(() => getNodeDetails(node.id), true)
              : scrollToLine(node.startLine)}
        />
      </div>
    {/if}
  {/snippet}

  {#if hasPanel}
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
  <AddConceptDialog
    bind:showDialog={showConceptDialog}
    node={conceptTarget}
    conceptGroups={conceptGroups || []}
  />
{/if}
