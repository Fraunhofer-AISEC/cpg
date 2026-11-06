import { untrack } from 'svelte';
import { innerWidth } from 'svelte/reactivity/window';

/**
 * Calls [collapse] when the window is narrower than [maxWidth] px, at first and whenever it becomes
 * narrower than that. What the user opens or closes in between is left alone, so it only
 * acts at the moment the window crosses the limit. Call it while a component is initialized.
 */
export function collapseWhenNarrow(maxWidth: number, collapse: () => void) {
  const narrow = $derived((innerWidth.current ?? Infinity) < maxWidth);
  $effect(() => {
    if (narrow) untrack(collapse);
  });
}
