<script lang="ts">
  import { splitHighlightedLines } from '$lib/highlight';

  /**
   * Renders highlighted code with line numbers, but only the lines in `[start, end)`. The element
   * itself has the full height of the file, so the scrollbar and absolutely positioned overlays
   * (which use the same rem-based geometry) behave as if every line was rendered.
   */
  interface Props {
    highlighted: string;
    /** 0-based index of the first line to render */
    start: number;
    /** 0-based index after the last line to render */
    end: number;
    /** 0-based line indices to highlight */
    highlightedLines: number[];
    /** The widest line in characters, used for the horizontal extent */
    maxColumns: number;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    /** The space between the line numbers and the code in rem, e.g. for a lane of the gutter */
    gutterPadding?: number;
    /** The lines of the selection (1-based, inclusive): tinted, with emphasized line numbers */
    selection?: { first: number; last: number } | null;
    /** The lines of a slice of the dependence graph (1-based): tinted, with a bar at the gutter */
    sliceLines?: number[];
    /** The lines of the statement hovered in the dependence graph, tinted more strongly */
    sliceHover?: { first: number; last: number } | null;
  }

  let {
    highlighted,
    start,
    end,
    highlightedLines,
    maxColumns,
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft,
    gutterPadding = 0.75,
    selection = null,
    sliceLines = [],
    sliceHover = null
  }: Props = $props();

  const isSelected = (line: number) =>
    !!selection && line >= selection.first && line <= selection.last;

  const inSlice = $derived(new Set(sliceLines));

  // Contiguous runs of the lines of the slice
  const sliceRanges = $derived.by(() => {
    const ranges: { first: number; last: number }[] = [];
    for (const line of [...inSlice].sort((a, b) => a - b)) {
      const previous = ranges.at(-1);
      if (previous && previous.last === line - 1) previous.last = line;
      else ranges.push({ first: line, last: line });
    }
    return ranges;
  });

  const lines = $derived(splitHighlightedLines(highlighted));
  const visibleLines = $derived(lines.slice(start, end));

  // Contiguous runs of highlighted lines, so that a highlighted node spanning many lines is a
  // single element
  const highlightedRanges = $derived.by(() => {
    const sorted = [...new Set(highlightedLines)].sort((a, b) => a - b);
    const ranges: { first: number; last: number }[] = [];
    for (const line of sorted) {
      const previous = ranges.at(-1);
      if (previous && previous.last === line - 1) {
        previous.last = line;
      } else {
        ranges.push({ first: line, last: line });
      }
    }
    return ranges;
  });

  // CPG columns are 1-based and overlays are placed at `column * charWidth + offsetLeft`, so the
  // first character of a line has to start one character after offsetLeft
  const codeStart = $derived(offsetLeft + charWidth);
</script>

<div
  class="hljs relative min-w-full text-base"
  style:height="{2 * offsetTop + lines.length * lineHeight}rem"
  style:width="{codeStart + maxColumns * charWidth + 2}rem"
>
  {#each highlightedRanges as range (range.first)}
    <div
      class="pointer-events-none absolute right-0 left-0"
      style:top="{offsetTop + range.first * lineHeight}rem"
      style:height="{(range.last - range.first + 1) * lineHeight}rem"
      style:background-color="rgba(254, 241, 96, 0.35)"
    ></div>
  {/each}

  {#each sliceRanges as range (range.first)}
    <div
      class="pointer-events-none absolute right-0 left-0"
      style:top="{offsetTop + (range.first - 1) * lineHeight}rem"
      style:height="{(range.last - range.first + 1) * lineHeight}rem"
      style:background-color="rgba(99, 133, 203, 0.08)"
    ></div>
  {/each}

  {#if sliceHover}
    <div
      class="pointer-events-none absolute right-0 left-0"
      style:top="{offsetTop + (sliceHover.first - 1) * lineHeight}rem"
      style:height="{(sliceHover.last - sliceHover.first + 1) * lineHeight}rem"
      style:background-color="rgba(99, 133, 203, 0.18)"
    ></div>
  {/if}

  {#if selection}
    <div
      class="pointer-events-none absolute right-0 left-0 bg-blue-600/5"
      style:top="{offsetTop + (selection.first - 1) * lineHeight}rem"
      style:height="{(selection.last - selection.first + 1) * lineHeight}rem"
    ></div>
  {/if}

  {#each visibleLines as line, i (start + i)}
    {@const selected = isSelected(start + i + 1)}
    <div
      class="absolute right-0 left-0 flex whitespace-pre"
      data-line={start + i + 1}
      style:top="{offsetTop + (start + i) * lineHeight}rem"
      style:height="{lineHeight}rem"
      style:line-height="{lineHeight}rem"
    >
      <!-- The line numbers of the selection are tinted like their lines (blue-600 at 5% on white) -->
      <span
        class="sticky left-0 z-[25] shrink-0 text-right select-none {selected
          ? 'bg-[#f4f7fe] font-semibold text-blue-600'
          : inSlice.has(start + i + 1)
            ? 'bg-[#f1f5fb] text-gray-500'
            : 'bg-white text-gray-400'} {inSlice.has(start + i + 1)
          ? 'shadow-[inset_3px_0_0_#a9c1ec]'
          : ''}"
        style:width="{codeStart}rem"
        style:padding-right="{gutterPadding}rem"
      >
        {start + i + 1}
      </span>
      <span data-code>{@html line}</span>
    </div>
  {/each}
</div>
