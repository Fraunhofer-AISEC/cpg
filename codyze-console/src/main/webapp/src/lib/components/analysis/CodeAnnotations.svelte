<script lang="ts">
  import type { FileAnnotationsJSON } from '$lib/types';
  import { describeConcept } from '$lib/annotations';
  import type { Layer } from '$lib/stores/layers.svelte';
  import type { CallFilter, InspectorSection } from '$lib/stores/codeFocus.svelte';

  /**
   * Draws the annotations of a file on top of the code: the key figures of each function at the end
   * of its first line, and, depending on the visible layers, markers under external and unresolved
   * calls and purple marks in the gutter: ◆ for the concepts of a line, ◇ for its operations.
   */
  interface Props {
    annotations: FileAnnotationsJSON;
    /** Which layers are shown */
    layers: Record<Layer, boolean>;
    codeLines: string[];
    /** 0-based index of the first visible line */
    startLine: number;
    /** 0-based index after the last visible line */
    endLine: number;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    /** The left edge and the width of the lane of the concept icons in rem */
    glyphLeft: number;
    glyphWidth: number;
    /** Inspects a node, revealing a section of the inspector if given */
    onInspect: (nodeId: string, section?: InspectorSection, filter?: CallFilter) => void;
    /** Draws the annotations faintly, e.g. while a path is shown */
    dimmed?: boolean;
  }

  let {
    annotations,
    layers,
    codeLines,
    startLine,
    endLine,
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft,
    glyphLeft,
    glyphWidth,
    onInspect,
    dimmed = false
  }: Props = $props();

  // 1-based lines; visible if they overlap the rendered lines
  const isVisible = (first: number, last: number) => first - 1 < endLine && last - 1 >= startLine;
  const top = (line: number) => (line - 1) * lineHeight + offsetTop;

  const functions = $derived(annotations.functions);

  // Rendered width of a line in characters, with tabs being 8 characters wide
  function lineColumns(line: number): number {
    const text = codeLines[line - 1] ?? '';
    return text.length + (text.split('\t').length - 1) * 7;
  }

  // The parts of the code lens of a function, each revealing its section of the inspector
  function lensParts(fn: (typeof functions)[number]) {
    const parts: {
      label: string;
      count: number;
      section: InspectorSection;
      filter?: CallFilter;
      title: string;
    }[] = [
      {
        label: `${fn.callers} callers`,
        count: fn.callers,
        section: 'callers',
        title: `Show the callers of ${fn.function.name}`
      },
      {
        label: `calls ${fn.callees}`,
        count: fn.callees,
        section: 'callees',
        title: `Show the calls in ${fn.function.name}`
      }
    ];
    if (fn.externalCalls > 0) {
      parts.push({
        label: `${fn.externalCalls} external`,
        count: fn.externalCalls,
        section: 'callees',
        filter: 'EXTERNAL',
        title: `Show the calls in ${fn.function.name}, ${fn.externalCalls} of them to external code`
      });
    }
    if (fn.unresolvedCalls > 0) {
      parts.push({
        label: `⚠ ${fn.unresolvedCalls} unresolved`,
        count: fn.unresolvedCalls,
        section: 'callees',
        filter: 'UNRESOLVED',
        title: `Show the calls in ${fn.function.name}, ${fn.unresolvedCalls} of them unresolved`
      });
    }
    return parts;
  }

  const conceptsByLine = $derived.by(() => {
    const byLine = new Map<number, typeof annotations.concepts>();
    for (const concept of annotations.concepts) {
      byLine.set(concept.line, [...(byLine.get(concept.line) ?? []), concept]);
    }
    return [...byLine.entries()].map(([line, concepts]) => ({ line, concepts }));
  });

  const markedCalls = $derived(
    annotations.calls.filter(
      (c) =>
        c.startLine === c.endLine &&
        ((c.status === 'EXTERNAL' && layers.external) ||
          (c.status === 'UNRESOLVED' && layers.uncertain))
    )
  );
</script>

<div
  class="pointer-events-none absolute top-0 left-0 h-full w-full transition-opacity"
  class:opacity-35={dimmed}
>
  <!-- Key figures at the end of the first line of each function, each part clickable -->
  {#each functions as fn (fn.function.id)}
    {#if isVisible(fn.function.startLine, fn.function.startLine)}
      <span
        class="absolute z-[22] font-sans text-[11px] whitespace-nowrap text-gray-400 italic"
        style:top="{top(fn.function.startLine)}rem"
        style:line-height="{lineHeight}rem"
        style:left="{offsetLeft + charWidth * (lineColumns(fn.function.startLine) + 4)}rem"
      >
        {#each lensParts(fn) as part, i (part.label)}
          {#if i > 0}<span class="px-0.5">·</span>{/if}
          {#if part.count > 0}
            <button
              type="button"
              class="pointer-events-auto cursor-pointer italic hover:text-blue-600 hover:underline"
              title={part.title}
              onclick={(e) => {
                e.stopPropagation();
                onInspect(fn.function.id, part.section, part.filter);
              }}
            >
              {part.label}
            </button>
          {:else}
            <span>{part.label}</span>
          {/if}
        {/each}
      </span>
    {/if}
  {/each}

  <!-- Markers under calls to external code (dashed) and unresolved calls (dotted) -->
  {#each markedCalls as call (call.id)}
    {#if isVisible(call.startLine, call.startLine)}
      <div
        class="absolute z-[21] border-b-2 {call.status === 'EXTERNAL'
          ? 'border-dashed border-orange-500'
          : 'border-dotted border-red-500'}"
        style:top="{top(call.startLine)}rem"
        style:height="{lineHeight - 0.1}rem"
        style:left="{call.startColumn * charWidth + offsetLeft}rem"
        style:width="{Math.max(call.endColumn - call.startColumn, 1) * charWidth}rem"
      ></div>
    {/if}
  {/each}

  <!-- Concepts (◆, the things) and operations (◇, what the code does with them) in the gutter, the
  tooltip lists them -->
  {#each layers.concepts ? conceptsByLine : [] as entry (entry.line)}
    {#if isVisible(entry.line, entry.line)}
      <button
        type="button"
        class="pointer-events-auto absolute z-[26] flex cursor-pointer items-center justify-center font-sans text-[11px] tracking-[-0.15em] text-purple-600 hover:text-purple-800"
        style:top="{top(entry.line)}rem"
        style:height="{lineHeight}rem"
        style:left="{glyphLeft}rem"
        style:width="{glyphWidth}rem"
        title={entry.concepts.map(describeConcept).join('\n')}
        aria-label={entry.concepts.map(describeConcept).join(', ')}
        onclick={(e) => {
          e.stopPropagation();
          onInspect(entry.concepts[0].id);
        }}
      >
        {#if entry.concepts.some((c) => !c.isOperation)}◆{/if}{#if entry.concepts.some((c) => c.isOperation)}◇{/if}
      </button>
    {/if}
  {/each}
</div>
