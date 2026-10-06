import type { NodeDetailsJSON } from '$lib/types';

/** A section of the inspector that can be revealed, e.g. from the code lens of a function. */
export type InspectorSection = 'callers' | 'callees' | 'callTargets';

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

  /** The visited locations, oldest first, for going back and forward like in an editor */
  history = $state.raw<CodeLocation[]>([]);
  /** The index of the current location in [history] */
  historyIndex = $state(-1);

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
    record = true
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
        const node = details.node;
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
