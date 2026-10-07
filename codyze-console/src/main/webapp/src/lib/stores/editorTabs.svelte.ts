import type { TranslationUnitJSON } from '$lib/types';

/**
 * The files opened in tabs. When more than [max] files are open, the least recently used tab is
 * closed.
 */
export class EditorTabs {
  /** The open files, in the order of their tabs */
  tabs = $state.raw<TranslationUnitJSON[]>([]);

  // Only used to pick the tab to close, so it does not need to be reactive
  // eslint-disable-next-line svelte/prefer-svelte-reactivity
  private lastUsed = new Map<string, number>();
  private clock = 0;

  constructor(private readonly max = 8) {}

  /**
   * Opens a tab for [unit], or marks its tab as used if it is open already. Returns the files whose
   * tabs were closed to make room.
   */
  open(unit: TranslationUnitJSON): TranslationUnitJSON[] {
    this.lastUsed.set(unit.id, ++this.clock);
    if (this.tabs.some((t) => t.id === unit.id)) return [];

    let tabs = [...this.tabs, unit];
    const closed: TranslationUnitJSON[] = [];
    while (tabs.length > this.max) {
      const oldest = tabs.reduce((a, b) =>
        (this.lastUsed.get(a.id) ?? 0) <= (this.lastUsed.get(b.id) ?? 0) ? a : b
      );
      tabs = tabs.filter((t) => t !== oldest);
      this.lastUsed.delete(oldest.id);
      closed.push(oldest);
    }
    this.tabs = tabs;
    return closed;
  }

  /**
   * Closes the tab of the file with the given ID and returns the file to show instead if it was the
   * active one: the most recently used of the remaining tabs, or null if none is left.
   */
  close(unitId: string): TranslationUnitJSON | null {
    this.tabs = this.tabs.filter((t) => t.id !== unitId);
    this.lastUsed.delete(unitId);
    return this.tabs.reduce<TranslationUnitJSON | null>(
      (best, t) =>
        !best || (this.lastUsed.get(t.id) ?? 0) > (this.lastUsed.get(best.id) ?? 0) ? t : best,
      null
    );
  }
}
