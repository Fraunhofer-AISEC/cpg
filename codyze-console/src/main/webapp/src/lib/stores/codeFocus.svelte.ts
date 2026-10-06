import type { NodeDetailsJSON } from '$lib/types';

/** A section of the inspector that can be revealed, e.g. from the code lens of a function. */
export type InspectorSection = 'callers' | 'callees' | 'callTargets';

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

  private request = 0;

  /**
   * Inspects the node returned by [load]. Only the result of the latest call is kept, so that slow
   * responses do not replace newer ones.
   */
  async inspect(
    load: () => Promise<NodeDetailsJSON | null>,
    reveal = false,
    section?: InspectorSection
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
