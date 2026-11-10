<script lang="ts" module>
  /** A mark in the ruler, e.g. an external call or a concept. */
  export interface RulerMark {
    /** 1-based line */
    line: number;
    /** The lane of the mark, from left to right; marks in the same lane are merged */
    lane: number;
    /** The CSS color of the mark */
    color: string;
    /** What the mark stands for, shown in the tooltip */
    label: string;
    /** Drawn faintly, e.g. for other layers while a path is shown */
    faded?: boolean;
  }
</script>

<script lang="ts">
  /**
   * A thin strip next to the code that shows the marks of a whole file at their relative position,
   * and the visible part of the file. Clicking a mark
   * (or anywhere in the strip) scrolls the code there.
   */
  interface Props {
    marks: RulerMark[];
    totalLines: number;
    /** The number of lanes */
    lanes: number;
    /** The visible lines, 1-based and inclusive */
    viewport: { first: number; last: number };
    /** The 1-based line of the selection, marked across all lanes */
    selectionLine?: number;
    onScrollTo: (line: number) => void;
  }

  let { marks, totalLines, lanes, viewport, selectionLine, onScrollTo }: Props = $props();

  let height = $state(0);
  // Marks closer than this (in px) are merged, so they stay visible and clickable
  const bucketSize = 3;

  const top = (line: number) => ((line - 1) / Math.max(totalLines, 1)) * height;

  // The marks merged per lane and bucket
  const groups = $derived.by(() => {
    if (height === 0) return [];
    const byKey: Record<string, { lane: number; top: number; marks: RulerMark[] }> = {};
    for (const mark of marks) {
      const bucket = Math.floor(top(mark.line) / bucketSize);
      const key = `${mark.lane}:${bucket}`;
      (byKey[key] ??= { lane: mark.lane, top: bucket * bucketSize, marks: [] }).marks.push(mark);
    }
    return Object.values(byKey);
  });

  let hovered = $state<(typeof groups)[number] | null>(null);

  function tooltip(group: (typeof groups)[number]): string[] {
    const sorted = [...group.marks].sort((a, b) => a.line - b.line);
    const lines = sorted.slice(0, 6).map((m) => `${m.line}: ${m.label}`);
    if (sorted.length > 6) lines.push(`… ${sorted.length - 6} more`);
    return lines;
  }

  function handleClick(event: MouseEvent) {
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const fraction = (event.clientY - rect.top) / rect.height;
    onScrollTo(Math.max(1, Math.round(fraction * totalLines) + 1));
  }
</script>

<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
<div
  class="relative w-3.5 shrink-0 cursor-pointer border-l border-gray-200 bg-gray-50"
  bind:clientHeight={height}
  onclick={handleClick}
>
  <!-- The visible part of the file -->
  <div
    class="pointer-events-none absolute right-0 left-0 bg-gray-300/40"
    style:top="{top(viewport.first)}px"
    style:height="{Math.max(top(viewport.last + 1) - top(viewport.first), 4)}px"
  ></div>

  {#each groups as group (group.lane + ':' + group.top)}
    <button
      type="button"
      class="absolute h-[3px] rounded-[1px]"
      style:top="{group.top}px"
      style:left="{1 + (group.lane * 12) / lanes}px"
      style:width="{Math.max(12 / lanes - 1, 2)}px"
      style:background-color={group.marks[0].color}
      style:opacity={group.marks[0].faded ? 0.35 : undefined}
      aria-label={tooltip(group).join(', ')}
      onmouseenter={() => (hovered = group)}
      onmouseleave={() => (hovered = null)}
      onclick={(e) => {
        e.stopPropagation();
        onScrollTo(Math.min(...group.marks.map((m) => m.line)));
      }}
    ></button>
  {/each}

  {#if selectionLine}
    <div
      class="pointer-events-none absolute right-0 left-0 h-0.5 bg-blue-600"
      style:top="{top(selectionLine)}px"
    ></div>
  {/if}

  {#if hovered}
    <div
      class="pointer-events-none absolute right-full z-40 mr-1 rounded border border-gray-200 bg-white px-2 py-1 font-mono text-[11px] whitespace-nowrap text-gray-700 shadow"
      style:top="{Math.max(0, Math.min(hovered.top - 8, height - 24))}px"
    >
      {#each tooltip(hovered) as line, i (i)}
        <div>{line}</div>
      {/each}
    </div>
  {/if}
</div>
