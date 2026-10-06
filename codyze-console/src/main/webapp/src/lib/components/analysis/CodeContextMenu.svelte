<script lang="ts" module>
  /** An entry of a context menu. */
  export interface ContextMenuItem {
    label: string;
    /** Shown at the end of the entry, e.g. how many nodes it leads to */
    hint?: string;
    /** Why the entry cannot be used right now; the entry is disabled then */
    disabledReason?: string;
    /** A separator is drawn above the entry */
    separator?: boolean;
    run: () => void;
  }
</script>

<script lang="ts">
  import { tick } from 'svelte';

  /**
   * A small menu at a position on the screen, e.g. for a right click in the code. It closes when
   * something else is clicked, with Escape, when the page is scrolled or resized, and after an
   * entry was used. The entries can be reached with the arrow keys.
   */
  interface Props {
    x: number;
    y: number;
    items: ContextMenuItem[];
    /** A short description of what the entries refer to, e.g. the node */
    title?: string;
    onClose: () => void;
  }

  let { x, y, items, title, onClose }: Props = $props();

  let menu = $state<HTMLDivElement>();
  let left = $state(0);
  let top = $state(0);

  // Keep the menu on the screen: it opens towards the left or the top near the edges
  $effect(() => {
    left = x;
    top = y;
    tick().then(() => {
      if (!menu) return;
      const rect = menu.getBoundingClientRect();
      left = Math.max(4, Math.min(x, window.innerWidth - rect.width - 4));
      top = Math.max(4, Math.min(y, window.innerHeight - rect.height - 4));
      menu.querySelector<HTMLButtonElement>('button:not([disabled])')?.focus();
    });
  });

  function handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      event.preventDefault();
      onClose();
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const buttons = [
        ...(menu?.querySelectorAll<HTMLButtonElement>('button:not([disabled])') ?? [])
      ];
      const current = buttons.indexOf(document.activeElement as HTMLButtonElement);
      const next = event.key === 'ArrowDown' ? current + 1 : current - 1;
      buttons[(next + buttons.length) % buttons.length]?.focus();
    }
  }

  function use(item: ContextMenuItem) {
    if (item.disabledReason) return;
    onClose();
    item.run();
  }
</script>

<svelte:window
  onkeydown={handleKeydown}
  onmousedown={(e) => !menu?.contains(e.target as Node) && onClose()}
  onresize={onClose}
  onblur={onClose}
/>

<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<div
  bind:this={menu}
  role="menu"
  tabindex="-1"
  class="fixed z-50 w-72 rounded-md border border-gray-300 bg-white p-1 text-xs shadow-lg"
  style:left="{left}px"
  style:top="{top}px"
  oncontextmenu={(e) => e.preventDefault()}
>
  {#if title}
    <div class="truncate px-2.5 py-1 font-mono text-[10.5px] text-gray-400" {title}>{title}</div>
  {/if}
  {#each items as item (item.label)}
    {#if item.separator}
      <div class="mx-1.5 my-1 h-px bg-gray-100"></div>
    {/if}
    <button
      type="button"
      role="menuitem"
      class="flex h-8 w-full items-center justify-between gap-2 rounded px-2.5 text-left {item.disabledReason
        ? 'cursor-default text-gray-400'
        : 'text-gray-800 hover:bg-blue-50 focus:bg-blue-50 focus:outline-none'}"
      disabled={!!item.disabledReason}
      title={item.disabledReason}
      onclick={() => use(item)}
    >
      <span class="truncate">{item.label}</span>
      {#if item.disabledReason || item.hint}
        <span class="shrink-0 text-[11px] text-gray-400">{item.disabledReason ?? item.hint}</span>
      {/if}
    </button>
  {/each}
</div>
