import {
  getGraphSlice,
  mergeSlice,
  type DependenceGraph,
  type GraphDirection,
  type GraphNode,
  type GraphSlice
} from '$lib/graph';
import type { NodePathsJSON } from '$lib/types';

/** A view of the panel that can be returned to, e.g. before the slice was re-rooted. */
interface GraphView {
  nodeId: string;
  graph: DependenceGraph;
  direction: GraphDirection;
  hops: number;
  selectedId: string | null;
}

/**
 * Paths a tool found that the panel shows instead of a slice: what they follow and show, as the
 * tool returned it, and how many there are.
 */
export interface GraphPaths extends Pick<NodePathsJSON, 'kind' | 'description'> {
  count: number;
}

/**
 * The state of the graph panel: which slice is shown, what is selected in it and where to go back
 * to. The slice is loaded from the backend whenever its root, graph, direction or hops change. Instead of
 * a slice, the panel can show the statements of paths (see [showPaths]), which do not change with
 * the hops or the inspected node.
 */
export class GraphPanel {
  open = $state(false);
  /** The paths shown instead of a slice, or null while a slice is shown */
  paths = $state.raw<GraphPaths | null>(null);
  /** Which dependences the slice follows */
  graph = $state<DependenceGraph>('PDG');
  direction = $state<GraphDirection>('BACKWARD');
  hops = $state(2);
  slice = $state.raw<GraphSlice | null>(null);
  selectedId = $state<string | null>(null);
  /** The statement under the mouse, in the graph or in the code */
  hoveredId = $state<string | null>(null);
  loading = $state(false);
  error = $state<string | null>(null);
  /** Whether the slice stays when another node is inspected, instead of following it */
  pinned = $state(false);
  history = $state.raw<GraphView[]>([]);

  // The node the slice was asked for, and a counter to discard answers that arrived too late
  private nodeId: string | null = null;
  private request = 0;

  get root(): GraphNode | null {
    return this.slice?.nodes.find((n) => n.id === this.slice?.root) ?? null;
  }

  get selected(): GraphNode | null {
    return this.slice?.nodes.find((n) => n.id === this.selectedId) ?? null;
  }

  /** The number of the statements outside of the slice, to offer showing more of them. */
  get more(): number {
    return this.slice?.nodes.reduce((sum, n) => sum + n.more, 0) ?? 0;
  }

  /**
   * Shows the slice around a node, in [graph] or else the graph shown before. A slice shown before
   * can be returned to with [back].
   */
  async show(
    nodeId: string,
    direction: GraphDirection,
    hops = this.hops,
    selectedId?: string,
    graph = this.graph
  ) {
    if (this.open && this.slice && this.nodeId !== nodeId) this.remember();
    await this.load(nodeId, graph, direction, hops, selectedId ?? null);
  }

  private remember() {
    if (!this.nodeId || this.paths) return;
    const view = {
      nodeId: this.nodeId,
      graph: this.graph,
      direction: this.direction,
      hops: this.hops,
      selectedId: this.selectedId
    };
    this.history = [...this.history.slice(-19), view];
  }

  /** Shows the statements of paths, given as a graph in the format of a slice. */
  showPaths(paths: GraphPaths, graph: GraphSlice) {
    if (this.open && this.slice && !this.paths) this.remember();
    this.request++;
    this.open = true;
    this.loading = false;
    this.error = null;
    this.paths = paths;
    this.slice = graph;
    this.selectedId = graph.root || null;
  }

  private async load(
    nodeId: string,
    graph: DependenceGraph,
    direction: GraphDirection,
    hops: number,
    selectedId: string | null
  ) {
    const request = ++this.request;
    this.open = true;
    this.paths = null;
    this.loading = true;
    this.error = null;
    try {
      const slice = await getGraphSlice(nodeId, graph, direction, hops);
      if (request !== this.request) return;
      if (!slice) {
        this.error = 'The node is not part of the analysis any more';
        return;
      }
      this.nodeId = nodeId;
      this.graph = graph;
      this.direction = direction;
      this.hops = hops;
      this.slice = slice;
      this.selectedId =
        selectedId && slice.nodes.some((n) => n.id === selectedId) ? selectedId : slice.root;
    } catch (e) {
      if (request === this.request) this.error = e instanceof Error ? e.message : String(e);
    } finally {
      if (request === this.request) this.loading = false;
    }
  }

  /** Changes how far the slice reaches, keeping what is selected. */
  setHops(hops: number) {
    if (!this.nodeId || this.paths || hops === this.hops) return;
    this.load(this.nodeId, this.graph, this.direction, hops, this.selectedId);
  }

  /** Changes which dependences the slice follows, keeping what is selected. */
  setGraph(graph: DependenceGraph) {
    if (!this.nodeId || this.paths || graph === this.graph) return;
    this.load(this.nodeId, graph, this.direction, this.hops, this.selectedId);
  }

  /** Shows the slice around a statement of the slice in the given direction. */
  reroot(nodeId: string, direction: GraphDirection) {
    this.show(nodeId, direction, this.hops, nodeId);
  }

  select(id: string | null) {
    this.selectedId = id;
  }

  /** Adds the statements around one at the border of the slice. */
  async expand(id: string) {
    const slice = this.slice;
    if (!slice) return;
    const extra = await getGraphSlice(id, this.graph, slice.direction, 1).catch(() => null);
    // The slice may have changed meanwhile
    if (extra && this.slice === slice) this.slice = mergeSlice(slice, extra, id);
  }

  back() {
    const view = this.history.at(-1);
    if (!view) return;
    this.history = this.history.slice(0, -1);
    this.load(view.nodeId, view.graph, view.direction, view.hops, view.selectedId);
  }

  close() {
    this.request++;
    this.open = false;
    this.loading = false;
    this.hoveredId = null;
    this.history = [];
    this.paths = null;
  }
}
