<script lang="ts" module>
  /** A numbered step at a line of the code, e.g. of a dataflow path. */
  export interface StepMarker {
    /** The index of the step, shown as its number plus one */
    index: number;
    /** 1-based line */
    line: number;
    title: string;
    /** Whether this is the current step */
    current?: boolean;
    /** Drawn dashed, e.g. for steps that are only claimed and not backed by evidence */
    dashed?: boolean;
    /** The node of the step, if the markers are not identified by their index alone */
    nodeId?: string;
    /** Shown instead of the number, e.g. for a marker that is no step */
    label?: string;
    /** Drawn faintly, e.g. while another step is pointed at */
    faded?: boolean;
  }
</script>

<script lang="ts">
  /**
   * Numbered badges for the steps of a path in a lane of the gutter. Several steps in the same line
   * share one badge, which shows the current step, else one that is not faded, else the first. Clicking a badge goes to its step.
   */
  interface Props {
    markers: StepMarker[];
    /** 0-based index of the first rendered line */
    startLine: number;
    /** 0-based index after the last rendered line */
    endLine: number;
    lineHeight: number;
    offsetTop: number;
    /** The left edge of the lane in rem */
    left: number;
    /** Round badges (e.g. for a dataflow path) or square ones (e.g. for the agent's steps) */
    shape?: 'round' | 'square';
    onSelect: (marker: StepMarker) => void;
  }

  let {
    markers,
    startLine,
    endLine,
    lineHeight,
    offsetTop,
    left,
    shape = 'round',
    onSelect
  }: Props = $props();

  const byLine = $derived.by(() => {
    const lines: Record<number, StepMarker[]> = {};
    for (const marker of markers) {
      if (marker.line - 1 < startLine || marker.line - 1 >= endLine) continue;
      (lines[marker.line] ??= []).push(marker);
    }
    return Object.entries(lines).map(([line, steps]) => ({
      line: Number(line),
      steps,
      shown: steps.find((s) => s.current) ?? steps.find((s) => !s.faded) ?? steps[0]
    }));
  });
</script>

<div class="pointer-events-none absolute top-0 left-0 h-full w-full">
  {#each byLine as entry (entry.line)}
    {@const marker = entry.shown}
    <button
      type="button"
      class="pointer-events-auto absolute z-[27] flex h-4 min-w-4 -translate-y-1/2 items-center justify-center {shape ===
      'round'
        ? 'rounded-full'
        : 'rounded-sm'} px-0.5 font-sans text-[10px] leading-none font-semibold tabular-nums {marker.dashed
        ? 'border border-dashed border-slate-500 bg-white text-slate-700'
        : 'bg-slate-700 text-white'} {marker.current ? 'ring-2 ring-blue-500' : ''} {marker.faded
        ? 'opacity-30'
        : ''}"
      style:top="{offsetTop + (entry.line - 0.5) * lineHeight}rem"
      style:left="{left}rem"
      title={entry.steps.map((s) => `${s.label ?? s.index + 1}. ${s.title}`).join('\n')}
      onclick={(e) => {
        e.stopPropagation();
        onSelect(marker);
      }}
    >
      {marker.label ?? marker.index + 1}{#if entry.steps.length > 1}<sup class="ml-px">+</sup>{/if}
    </button>
  {/each}
</div>
