import { getPdgSlice, mergeSlice, type PdgDirection, type PdgNode, type PdgSlice } from '$lib/pdg';

/** A view of the panel that can be returned to, e.g. before the slice was re-rooted. */
interface PdgView {
  nodeId: string;
  direction: PdgDirection;
  hops: number;
  selectedId: string | null;
}

/**
 * The state of the PDG panel: which slice is shown, what is selected in it and where to go back
 * to. The slice is loaded from the backend whenever its root, direction or hops change.
 */
export class PdgPanel {
  open = $state(false);
  direction = $state<PdgDirection>('BACKWARD');
  hops = $state(2);
  slice = $state.raw<PdgSlice | null>(null);
  selectedId = $state<string | null>(null);
  /** The statement under the mouse, in the graph or in the code */
  hoveredId = $state<string | null>(null);
  loading = $state(false);
  error = $state<string | null>(null);
  /** Whether the slice stays when another node is inspected, instead of following it */
  pinned = $state(false);
  history = $state.raw<PdgView[]>([]);

  // The node the slice was asked for, and a counter to discard answers that arrived too late
  private nodeId: string | null = null;
  private request = 0;

  get root(): PdgNode | null {
    return this.slice?.nodes.find((n) => n.id === this.slice?.root) ?? null;
  }

  get selected(): PdgNode | null {
    return this.slice?.nodes.find((n) => n.id === this.selectedId) ?? null;
  }

  /** The number of the statements outside of the slice, to offer showing more of them. */
  get more(): number {
    return this.slice?.nodes.reduce((sum, n) => sum + n.more, 0) ?? 0;
  }

  /** Shows the slice around a node. A slice shown before can be returned to with [back]. */
  async show(nodeId: string, direction: PdgDirection, hops = this.hops, selectedId?: string) {
    if (this.open && this.slice && this.nodeId !== nodeId) this.remember();
    await this.load(nodeId, direction, hops, selectedId ?? null);
  }

  private remember() {
    if (!this.nodeId) return;
    const view = {
      nodeId: this.nodeId,
      direction: this.direction,
      hops: this.hops,
      selectedId: this.selectedId
    };
    this.history = [...this.history.slice(-19), view];
  }

  private async load(
    nodeId: string,
    direction: PdgDirection,
    hops: number,
    selectedId: string | null
  ) {
    const request = ++this.request;
    this.open = true;
    this.loading = true;
    this.error = null;
    try {
      const slice = await getPdgSlice(nodeId, direction, hops);
      if (request !== this.request) return;
      if (!slice) {
        this.error = 'The node is not part of the analysis any more';
        return;
      }
      this.nodeId = nodeId;
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
    if (!this.nodeId || hops === this.hops) return;
    this.load(this.nodeId, this.direction, hops, this.selectedId);
  }

  /** Shows the slice around a statement of the slice in the given direction. */
  reroot(nodeId: string, direction: PdgDirection) {
    this.show(nodeId, direction, this.hops, nodeId);
  }

  select(id: string | null) {
    this.selectedId = id;
  }

  /** Adds the statements around one at the border of the slice. */
  async expand(id: string) {
    const slice = this.slice;
    if (!slice) return;
    const extra = await getPdgSlice(id, slice.direction, 1).catch(() => null);
    // The slice may have changed meanwhile
    if (extra && this.slice === slice) this.slice = mergeSlice(slice, extra, id);
  }

  back() {
    const view = this.history.at(-1);
    if (!view) return;
    this.history = this.history.slice(0, -1);
    this.load(view.nodeId, view.direction, view.hops, view.selectedId);
  }

  close() {
    this.request++;
    this.open = false;
    this.loading = false;
    this.hoveredId = null;
    this.history = [];
  }
}
