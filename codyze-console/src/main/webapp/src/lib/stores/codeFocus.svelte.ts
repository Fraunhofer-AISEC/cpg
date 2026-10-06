import type { NodeDetailsJSON, NodeRefJSON } from '$lib/types';

/** A section of the inspector that can be revealed, e.g. from the code lens of a function. */
export type InspectorSection = 'callers' | 'callees' | 'callTargets';

/** Which of the calls in a function to list: only those to external code or unresolved ones. */
export type CallFilter = 'EXTERNAL' | 'UNRESOLVED';

/** A place in the code that was visited: a file, or a node in it. */
export interface CodeLocation {
  unitId: string;
  /** The inspected node, if any */
  nodeId?: string;
  /** A short description for tooltips, e.g. `buf · main.c:14` */
  label: string;
}

// The number of locations kept in the history
const maxHistory = 100;
// The number of steps kept in a path
const maxPathSteps = 50;

/** A node of the evidence trail of the agent, see [CodeFocus.thread]. */
export interface ThreadNode {
  ref: NodeRefJSON;
  /** The number of the tool call of the agent that returned the node (one after the last for claimed nodes) */
  step: number;
  /** Whether the node is only cited in the answer, but was not returned by a tool */
  claimed: boolean;
}

/** The direction in which a dataflow is followed: backwards to its origins, or forwards. */
export type FlowDirection = 'from' | 'to';

/**
 * The node that is inspected in a code view. It is shared between the code viewer (which draws the
 * selection and scrolls to it) and wherever the inspector is shown, e.g. a column next to the code.
 */
export class CodeFocus {
  /** The details of the inspected node */
  details = $state.raw<NodeDetailsJSON | null>(null);
  loading = $state(false);
  error = $state<string | null>(null);
  /**
   * Incremented whenever the inspected node should be scrolled into view, e.g. after selecting it
   * in the inspector (but not after clicking on it in the code)
   */
  revealCount = $state(0);
  /**
   * Incremented whenever the analysis result changed, e.g. after concepts were added, so that views
   * reload what they derived from it (annotations, counts)
   */
  revision = $state(0);
  /**
   * The section of the inspector to expand and scroll into view, set when a node is inspected with
   * a section. The request number changes with every such inspection, so the same section can be
   * revealed again
   */
  revealSection = $state<{ section: InspectorSection; request: number } | null>(null);
  /** The calls of the inspected function that are listed, or null for all of them */
  callFilter = $state<CallFilter | null>(null);

  /** The visited locations, oldest first, for going back and forward like in an editor */
  history = $state.raw<CodeLocation[]>([]);
  /** The index of the current location in [history] */
  historyIndex = $state(-1);

  /**
   * The dataflow path the user followed, from the source to the sink, like the code-flow steps of
   * CodeQL. It is built by following dataflows hop by hop (in the inspector or along the arcs in
   * the code) and stays until it is cleared, so the user can step through it
   */
  path = $state.raw<NodeRefJSON[]>([]);
  /** The index of the inspected node in [path], or -1 if it is not on the path */
  pathIndex = $state(-1);

  /**
   * The evidence of the agent's active thread (one question and its steps): the nodes returned by
   * its tool calls and the nodes only cited in its answer, shown as numbered markers in the code
   */
  thread = $state.raw<ThreadNode[]>([]);

  private request = 0;

  get canGoBack(): boolean {
    return this.historyIndex > 0;
  }

  get canGoForward(): boolean {
    return this.historyIndex < this.history.length - 1;
  }

  /**
   * Adds a visited location after the current one, dropping the locations that could have been
   * reached by going forward. Visiting the current location again does not add an entry.
   */
  record(location: CodeLocation) {
    const current = this.history[this.historyIndex];
    if (current && current.unitId === location.unitId && current.nodeId === location.nodeId) return;
    const history = [...this.history.slice(0, this.historyIndex + 1), location].slice(-maxHistory);
    this.history = history;
    this.historyIndex = history.length - 1;
  }

  /** Moves back in the history and returns the location to show, if there is one. */
  back(): CodeLocation | null {
    if (!this.canGoBack) return null;
    this.historyIndex--;
    return this.history[this.historyIndex];
  }

  /** Moves forward in the history and returns the location to show, if there is one. */
  forward(): CodeLocation | null {
    if (!this.canGoForward) return null;
    this.historyIndex++;
    return this.history[this.historyIndex];
  }

  /**
   * Inspects the node returned by [load]. Only the result of the latest call is kept, so that slow
   * responses do not replace newer ones. The node is added to the history unless [record] is false,
   * e.g. when going back to it.
   */
  async inspect(
    load: () => Promise<NodeDetailsJSON | null>,
    reveal = false,
    section?: InspectorSection,
    record = true,
    filter?: CallFilter
  ) {
    const request = ++this.request;
    this.loading = true;
    this.error = null;
    try {
      const details = await load();
      if (request !== this.request) return;
      if (details) {
        this.details = details;
        if (reveal) this.revealCount++;
        this.revealSection = section ? { section, request } : null;
        this.callFilter = filter ?? null;
        const node = details.node;
        this.pathIndex = this.path.findIndex((step) => step.id === node.id);
        if (record && node.translationUnitId) {
          const file = node.fileName?.split('/').pop() ?? '';
          this.record({
            unitId: node.translationUnitId,
            nodeId: node.id,
            label: `${node.name || node.code} · ${file}:${node.startLine}`
          });
        }
      }
    } catch (e) {
      if (request === this.request) this.error = e instanceof Error ? e.message : String(e);
    } finally {
      if (request === this.request) this.loading = false;
    }
  }

  /**
   * Extends the path by following a dataflow [chain], which starts at the inspected node and goes
   * one or more hops in [direction]. If the inspected node is on the path, the chain replaces
   * what comes after it (forwards) or before it (backwards), like taking another branch; otherwise
   * the chain starts a new path. Call this before inspecting the end of the chain.
   */
  follow(chain: NodeRefJSON[], direction: FlowDirection) {
    if (chain.length < 2) return;
    const start = this.path.findIndex((step) => step.id === chain[0].id);
    let path: NodeRefJSON[];
    if (start < 0) {
      path = direction === 'to' ? chain : [...chain].reverse();
    } else if (direction === 'to') {
      path = [...this.path.slice(0, start), ...chain];
    } else {
      path = [...[...chain].reverse(), ...this.path.slice(start + 1)];
    }
    // Following a cycle would visit a node twice, so the path ends at the first visit
    const end = path.findIndex((step, i) => path.findIndex((s) => s.id === step.id) < i);
    if (end >= 0) path = path.slice(0, end);
    this.path = path.slice(0, maxPathSteps);
  }

  /**
   * Returns the step of the path [delta] steps away from the current one, e.g. 1 for the next
   * step. From outside of the path, the first step comes next and the last one before.
   */
  pathStep(delta: number): NodeRefJSON | null {
    if (this.path.length === 0) return null;
    const index =
      this.pathIndex < 0 ? (delta > 0 ? 0 : this.path.length - 1) : this.pathIndex + delta;
    return this.path[index] ?? null;
  }

  clearPath() {
    this.path = [];
    this.pathIndex = -1;
  }

  /** Marks the analysis result as changed, see [revision]. */
  invalidate() {
    this.revision++;
  }

  clear() {
    this.request++;
    this.details = null;
    this.loading = false;
    this.error = null;
  }
}
