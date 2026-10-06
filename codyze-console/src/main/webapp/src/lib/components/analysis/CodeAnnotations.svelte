<script lang="ts">
  import type { FileAnnotationsJSON } from '$lib/types';
  import { type Lens, lenses, lensValue, conceptIcon } from '$lib/annotations';

  /**
   * Draws the annotations of a file on top of the code: a colored bar next to each function (by the
   * selected lens), the key figures of each function at the end of its first line, markers under
   * external and unresolved calls, and icons for concepts in the gutter.
   */
  interface Props {
    annotations: FileAnnotationsJSON;
    lens: Lens;
    codeLines: string[];
    /** 0-based index of the first visible line */
    startLine: number;
    /** 0-based index after the last visible line */
    endLine: number;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    onInspect: (nodeId: string) => void;
  }

  let { annotations, lens, codeLines, startLine, endLine, lineHeight, charWidth, offsetTop, offsetLeft, onInspect }: Props =
    $props();

  const lensInfo = $derived(lenses.find((l) => l.id === lens)!);

  // 1-based lines; visible if they overlap the rendered lines
  const isVisible = (first: number, last: number) => first - 1 < endLine && last - 1 >= startLine;
  const top = (line: number) => (line - 1) * lineHeight + offsetTop;

  const functions = $derived.by(() => {
    const values = annotations.functions.map((fn) => lensValue(lens, fn, annotations));
    const max = Math.max(1, ...values);
    return annotations.functions.map((fn, i) => ({ ...fn, value: values[i], intensity: values[i] / max }));
  });

  // Rendered width of a line in characters, with tabs being 8 characters wide
  function lineColumns(line: number): number {
    const text = codeLines[line - 1] ?? '';
    return text.length + (text.split('\t').length - 1) * 7;
  }

  function summary(fn: (typeof functions)[number]): string {
    const parts = [`${fn.callers} callers`, `calls ${fn.callees}`];
    if (fn.externalCalls > 0) parts.push(`${fn.externalCalls} external`);
    if (fn.unresolvedCalls > 0) parts.push(`⚠ ${fn.unresolvedCalls} unresolved`);
    return parts.join(' · ');
  }

  const conceptsByLine = $derived.by(() => {
    const byLine = new Map<number, typeof annotations.concepts>();
    for (const concept of annotations.concepts) {
      byLine.set(concept.line, [...(byLine.get(concept.line) ?? []), concept]);
    }
    return [...byLine.entries()].map(([line, concepts]) => ({
      line,
      concepts,
      icons: [...new Set(concepts.map((c) => conceptIcon(c.category)))].slice(0, 2).join('')
    }));
  });

  const markedCalls = $derived(
    annotations.calls.filter((c) => c.status !== 'RESOLVED' && c.startLine === c.endLine)
  );
</script>

<div class="pointer-events-none absolute top-0 left-0 h-full w-full">
  <!-- Bars next to the functions, by the selected lens -->
  {#if lens !== 'none'}
    {#each functions as fn (fn.function.id)}
      {#if fn.value > 0 && isVisible(fn.function.startLine, fn.function.endLine)}
        <button
          type="button"
          class="pointer-events-auto absolute left-0 z-[26] w-1 cursor-pointer rounded-r"
          style:top="{top(fn.function.startLine)}rem"
          style:height="{(fn.function.endLine - fn.function.startLine + 1) * lineHeight}rem"
          style:background-color="rgba({lensInfo.color}, {0.15 + 0.85 * fn.intensity})"
          title="{fn.function.name}: {lensInfo.describe(fn.value)}"
          aria-label="Inspect {fn.function.name}"
          onclick={(e) => {
            e.stopPropagation();
            onInspect(fn.function.id);
          }}
        ></button>
      {/if}
    {/each}
  {/if}

  <!-- Key figures at the end of the first line of each function -->
  {#each functions as fn (fn.function.id)}
    {#if isVisible(fn.function.startLine, fn.function.startLine)}
      <button
        type="button"
        class="pointer-events-auto absolute z-[22] cursor-pointer font-sans text-[11px] whitespace-nowrap text-gray-400 italic hover:text-blue-600 hover:underline"
        style:top="{top(fn.function.startLine)}rem"
        style:line-height="{lineHeight}rem"
        style:left="{offsetLeft + charWidth * (lineColumns(fn.function.startLine) + 4)}rem"
        onclick={(e) => {
          e.stopPropagation();
          onInspect(fn.function.id);
        }}
        title="Inspect {fn.function.name}"
      >
        {summary(fn)}
      </button>
    {/if}
  {/each}

  <!-- Markers under calls to external code (dashed) and unresolved calls (dotted) -->
  {#each markedCalls as call (call.id)}
    {#if isVisible(call.startLine, call.startLine)}
      <div
        class="absolute z-[21] border-b-2 {call.status === 'EXTERNAL' ? 'border-dashed border-orange-500' : 'border-dotted border-red-500'}"
        style:top="{top(call.startLine)}rem"
        style:height="{lineHeight - 0.1}rem"
        style:left="{call.startColumn * charWidth + offsetLeft}rem"
        style:width="{Math.max(call.endColumn - call.startColumn, 1) * charWidth}rem"
      ></div>
    {/if}
  {/each}

  <!-- Concept icons in the gutter -->
  {#each conceptsByLine as entry (entry.line)}
    {#if isVisible(entry.line, entry.line)}
      <button
        type="button"
        class="pointer-events-auto absolute left-1.5 z-[26] cursor-pointer text-[11px]"
        style:top="{top(entry.line)}rem"
        style:line-height="{lineHeight}rem"
        title={entry.concepts.map((c) => `${c.isOperation ? 'Operation' : 'Concept'}: ${c.type}`).join('\n')}
        onclick={(e) => {
          e.stopPropagation();
          onInspect(entry.concepts[0].id);
        }}
      >
        {entry.icons}
      </button>
    {/if}
  {/each}
</div>