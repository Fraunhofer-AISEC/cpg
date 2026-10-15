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
    offsetLeft
  }: Props = $props();

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
  class="hljs relative text-base"
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

  {#each visibleLines as line, i (start + i)}
    <div
      class="absolute right-0 left-0 flex whitespace-pre"
      style:top="{offsetTop + (start + i) * lineHeight}rem"
      style:height="{lineHeight}rem"
      style:line-height="{lineHeight}rem"
    >
      <span
        class="sticky left-0 z-[25] shrink-0 bg-white pr-3 text-right text-gray-400 select-none"
        style:width="{codeStart}rem"
      >
        {start + i + 1}
      </span>
      <span>{@html line}</span>
    </div>
  {/each}
</div>
