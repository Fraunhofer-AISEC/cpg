import type { NodeDetailsJSON } from '$lib/types';

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

  private request = 0;

  /**
   * Inspects the node returned by [load]. Only the result of the latest call is kept, so that slow
   * responses do not replace newer ones.
   */
  async inspect(load: () => Promise<NodeDetailsJSON | null>, reveal = false) {
    const request = ++this.request;
    this.loading = true;
    this.error = null;
    try {
      const details = await load();
      if (request !== this.request) return;
      if (details) {
        this.details = details;
        if (reveal) this.revealCount++;
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
