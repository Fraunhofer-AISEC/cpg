/**
 * Reactive scroll position and height of a scroll container, used to render only the entries of a
 * long list that are currently visible.
 */
export class ScrollViewport {
  scrollTop = $state(0);
  height = $state(0);

  /**
   * Starts tracking [element] and returns a function that stops it, so it can be returned from an
   * `$effect`. Scroll bursts are coalesced into at most one update per frame.
   */
  track(element: HTMLElement): () => void {
    const sync = () => {
      this.scrollTop = element.scrollTop;
      this.height = element.clientHeight;
    };

    let frame: number | undefined;
    const onScroll = () => {
      if (frame !== undefined) return;
      frame = requestAnimationFrame(() => {
        frame = undefined;
        sync();
      });
    };

    sync();
    element.addEventListener('scroll', onScroll, { passive: true });
    const resizeObserver = new ResizeObserver(sync);
    resizeObserver.observe(element);

    return () => {
      element.removeEventListener('scroll', onScroll);
      resizeObserver.disconnect();
      if (frame !== undefined) cancelAnimationFrame(frame);
    };
  }

  /**
   * The index range `[start, end)` of the entries that are visible, plus [overscan] entries above
   * and below.
   *
   * @param entrySize the height of one entry in px
   * @param count the number of entries
   * @param overscan the number of extra entries on each side
   * @param offset the space above the first entry in px
   */
  range(entrySize: number, count: number, overscan: number, offset = 0) {
    const first = Math.floor((this.scrollTop - offset) / entrySize);
    const last = Math.ceil((this.scrollTop + this.height - offset) / entrySize);
    return {
      start: Math.max(0, first - overscan),
      end: Math.min(count, last + overscan)
    };
  }
}

/** The size of 1rem in px. */
export function remInPx(): number {
  if (typeof document === 'undefined') return 16;
  return parseFloat(getComputedStyle(document.documentElement).fontSize) || 16;
}
