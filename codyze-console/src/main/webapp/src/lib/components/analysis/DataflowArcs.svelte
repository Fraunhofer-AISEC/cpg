<script lang="ts">
  import type { NodeDetailsJSON, NodeRefJSON } from '$lib/types';
  import type { FlowDirection } from '$lib/stores/codeFocus.svelte';

  /**
   * Draws the direct dataflows of the inspected node as arcs in a lane next to the line numbers,
   * nested with shorter arcs inside and longer ones outside. An arc starts
   * with a dot at the line the value comes from and ends with an arrow at the line it goes to.
   * Small dots mark the endpoint tokens in the code. Arcs to lines outside of the viewport are
   * clipped at its edge with a hint, and dataflows into other files become chips at the end of the
   * line of the inspected node.
   *
   * Everything uses the rem-based geometry of the code viewer.
   */
  interface Props {
    details: NodeDetailsJSON;
    unitId: string;
    codeLines: string[];
    /** 0-based index of the first rendered line */
    startLine: number;
    /** 0-based index after the last rendered line */
    endLine: number;
    /** The visible part of the code, as the distance of its top and bottom from the top in rem */
    viewTop: number;
    viewBottom: number;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    /** The horizontal extent of the arc lane in rem */
    laneLeft: number;
    laneRight: number;
    /** Where the free space at the end of a (1-based) line starts, in rem */
    endOfLine: (line: number) => number;
    /** Follows the dataflow to [ref], i.e. inspects it */
    onFollow: (ref: NodeRefJSON, direction: FlowDirection) => void;
    onScrollTo: (line: number) => void;
  }

  let {
    details,
    unitId,
    codeLines,
    startLine,
    endLine,
    viewTop,
    viewBottom,
    lineHeight,
    charWidth,
    offsetTop,
    offsetLeft,
    laneLeft,
    laneRight,
    endOfLine,
    onFollow,
    onScrollTo
  }: Props = $props();

  // The distance between the nesting levels of the arcs and the number of levels that fit into the
  // lane; further arcs share the outermost level
  const levelStep = 0.2;
  const innerOffset = 0.4;
  const maxLevels = $derived(
    Math.max(1, Math.floor((laneRight - laneLeft - innerOffset) / levelStep) + 1)
  );
  // The number of chips for other files at the end of the line, the rest is summarized
  const maxChips = 3;

  interface Arc {
    key: string;
    direction: FlowDirection;
    /** The line of the other endpoint, i.e. not the inspected node */
    line: number;
    refs: NodeRefJSON[];
    level: number;
  }

  const node = $derived(details.node);
  const nodeLine = $derived(
    node.translationUnitId === unitId && node.startLine >= 1 ? node.startLine : null
  );

  const neighbours = $derived([
    ...details.dataflowFrom.map((ref) => ({ ref, direction: 'from' as const })),
    ...details.dataflowTo.map((ref) => ({ ref, direction: 'to' as const }))
  ]);

  const isLocal = (ref: NodeRefJSON) => ref.translationUnitId === unitId && ref.startLine >= 1;

  // The arcs, one per direction and line of the other endpoint, nested by their length
  const arcs = $derived.by((): Arc[] => {
    const line = nodeLine;
    if (line == null) return [];
    const byKey: Record<string, Arc> = {};
    for (const { ref, direction } of neighbours) {
      if (!isLocal(ref) || ref.id === node.id || ref.startLine === line) continue;
      const key = `${direction}:${ref.startLine}`;
      (byKey[key] ??= { key, direction, line: ref.startLine, refs: [], level: 0 }).refs.push(ref);
    }
    return Object.values(byKey)
      .sort((a, b) => Math.abs(a.line - line) - Math.abs(b.line - line))
      .map((arc, i) => ({ ...arc, level: Math.min(i, maxLevels - 1) }));
  });

  // Endpoints in the line of the inspected node itself, which only get their token dots
  const sameLine = $derived(
    neighbours.filter(({ ref }) => isLocal(ref) && ref.id !== node.id && ref.startLine === nodeLine)
  );

  // The endpoint tokens in this file, with the key of their arc (empty in the line of the node)
  const ends = $derived([
    ...arcs.flatMap((arc) =>
      arc.refs.map((ref) => ({ ref, direction: arc.direction, key: arc.key }))
    ),
    ...sameLine.map((end) => ({ ...end, key: '' }))
  ]);

  // Dataflows into other files or into code without a location, e.g. external functions
  const remote = $derived(
    nodeLine == null ? [] : neighbours.filter(({ ref }) => !isLocal(ref) && ref.id !== node.id)
  );

  const isRendered = (line: number) => line - 1 >= startLine && line - 1 < endLine;
  const lineTop = (line: number) => offsetTop + (line - 1) * lineHeight;
  const lineMiddle = (line: number) => lineTop(line) + lineHeight / 2;
  const isVisible = (line: number) => lineMiddle(line) >= viewTop && lineMiddle(line) <= viewBottom;
  const tokenStart = (ref: NodeRefJSON) => offsetLeft + ref.startColumn * charWidth;
  const tokenWidth = (ref: NodeRefJSON) =>
    ref.startLine === ref.endLine
      ? Math.max(ref.endColumn - ref.startColumn, 1) * charWidth
      : Math.max((codeLines[ref.startLine - 1]?.length ?? 0) + 1 - ref.startColumn, 1) * charWidth;

  // The SVG covers the lane in the visible part of the code, so arcs are clipped at its edges
  const height = $derived(Math.max(viewBottom - viewTop, 0));
  const y = (line: number) => lineMiddle(line) - viewTop;
  const clampY = (value: number) => Math.min(Math.max(value, 0), height);

  // The geometry of an arc in the coordinates of the SVG: the path, and where its ends are drawn
  function geometry(arc: Arc) {
    const from = arc.direction === 'from' ? arc.line : nodeLine!;
    const to = arc.direction === 'from' ? nodeLine! : arc.line;
    const startVisible = isVisible(from);
    const endVisible = isVisible(to);
    const ya = y(from);
    const yb = y(to);
    // Both ends are beyond the same edge, so nothing of the arc is visible
    if (!startVisible && !endVisible && ya < 0 === yb < 0) return null;

    const x = laneRight - innerOffset - arc.level * levelStep;
    const sign = yb > ya ? 1 : -1;
    const r = Math.min(0.3, Math.abs(yb - ya) / 2);
    const stubEnd = laneRight - 0.12;
    let d = startVisible
      ? `M ${stubEnd} ${ya} H ${x + r} Q ${x} ${ya} ${x} ${ya + sign * r}`
      : `M ${x} ${clampY(ya)}`;
    d += ` V ${endVisible ? yb - sign * r : clampY(yb)}`;
    if (endVisible) d += ` Q ${x} ${yb} ${x + r} ${yb} H ${stubEnd - 0.2}`;
    return {
      d,
      dot: startVisible ? { x: stubEnd, y: ya } : null,
      arrow: endVisible
        ? `${stubEnd - 0.3},${yb - 0.17} ${stubEnd},${yb} ${stubEnd - 0.3},${yb + 0.17}`
        : null
    };
  }

  // Hints at the edges of the viewport for arc endpoints beyond them, merged per edge
  const hints = $derived.by(() => {
    const above: number[] = [];
    const below: number[] = [];
    for (const arc of arcs) {
      if (!geometry(arc)) continue;
      for (const line of [arc.line, nodeLine!]) {
        if (isVisible(line)) continue;
        (y(line) < 0 ? above : below).push(line);
      }
    }
    const firstVisible = Math.floor((viewTop - offsetTop) / lineHeight) + 1;
    const lastVisible = Math.ceil((viewBottom - offsetTop) / lineHeight);
    const hint = (lines: number[], edge: 'top' | 'bottom') => {
      const distinct = [...new Set(lines)].sort((a, b) => (edge === 'top' ? b - a : a - b));
      if (distinct.length === 0) return null;
      const nearest = distinct[0];
      const distance = edge === 'top' ? firstVisible - nearest : nearest - lastVisible;
      return {
        edge,
        line: nearest,
        label: `${edge === 'top' ? '↑' : '↓'} ${Math.max(distance, 1)} lines`,
        more: distinct.length - 1,
        title: `Dataflow ${edge === 'top' ? 'above' : 'below'}: line${distinct.length > 1 ? 's' : ''} ${distinct.join(', ')}. Click to scroll there.`
      };
    };
    return [hint(above, 'top'), hint(below, 'bottom')].filter((h) => h != null);
  });

  // The arc under the mouse, whose endpoints are highlighted
  let hovered = $state<string | null>(null);
  const hoveredArc = $derived(arcs.find((arc) => arc.key === hovered) ?? null);

  // The lines of the endpoints of the hovered arc, with a connector from the lane to their tokens
  const connectors = $derived.by(() => {
    if (!hoveredArc) return [];
    const endpoints = [...hoveredArc.refs, node];
    return endpoints
      .filter((ref) => isRendered(ref.startLine))
      .map((ref) => ({ ref, top: lineTop(ref.startLine), x: tokenStart(ref) }));
  });

  function describe(ref: NodeRefJSON, direction: FlowDirection): string {
    const what = ref.code || ref.name;
    const where = ref.startLine >= 1 ? `${ref.fileName ?? ''}:${ref.startLine}` : 'no location';
    const label = ref.label ? ` (${ref.label})` : '';
    return `${direction === 'from' ? 'Comes from' : 'Goes to'} ${what} · ${where}${label}`;
  }

  function chipLabel(ref: NodeRefJSON, direction: FlowDirection): string {
    const arrow = direction === 'from' ? '←' : '→';
    const name = ref.name || ref.code;
    if (ref.startLine < 1 || !ref.translationUnitId) return `${arrow} external ${name}`;
    return `${arrow} ${ref.fileName}:${ref.startLine} ${name}`;
  }

  const isExternal = (ref: NodeRefJSON) => ref.isInferred || !ref.translationUnitId;

  const strokeOf = (direction: FlowDirection, key: string) =>
    key === hovered
      ? 'rgb(15, 23, 42)'
      : direction === 'from'
        ? 'rgb(71, 85, 105)'
        : 'rgb(148, 163, 184)';
</script>

<div class="pointer-events-none absolute top-0 left-0 h-full w-full">
  {#if arcs.length > 0 && height > 0}
    <svg
      class="absolute z-[27] overflow-visible"
      style:top="{viewTop}rem"
      style:left="0"
      style:width="{laneRight}rem"
      style:height="{height}rem"
      viewBox="0 0 {laneRight} {height}"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      {#each arcs as arc (arc.key)}
        {@const g = geometry(arc)}
        {#if g}
          {@const stroke = strokeOf(arc.direction, arc.key)}
          <path
            d={g.d}
            fill="none"
            {stroke}
            stroke-width={arc.key === hovered ? 2 : 1.25}
            vector-effect="non-scaling-stroke"
          />
          {#if g.dot}
            <circle cx={g.dot.x} cy={g.dot.y} r="0.11" fill={stroke} />
          {/if}
          {#if g.arrow}
            <polygon points={g.arrow} fill={stroke} />
          {/if}
          <!-- A wider, invisible stroke to make the arc easier to hover and click -->
          <!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
          <path
            d={g.d}
            fill="none"
            stroke="transparent"
            stroke-width="8"
            vector-effect="non-scaling-stroke"
            class="cursor-pointer"
            style:pointer-events="stroke"
            onmouseenter={() => (hovered = arc.key)}
            onmouseleave={() => (hovered = null)}
            onclick={(e) => {
              e.stopPropagation();
              hovered = null;
              onFollow(arc.refs[0], arc.direction);
            }}
          >
            <title>{arc.refs.map((ref) => describe(ref, arc.direction)).join('\n')}</title>
          </path>
        {/if}
      {/each}
    </svg>
  {/if}

  <!-- Highlights of the endpoints of the hovered arc, connected to the lane -->
  {#each connectors as c (c.ref.id)}
    <div
      class="absolute z-[19] border-b border-dashed border-slate-400"
      style:top="{c.top}rem"
      style:height="{lineHeight - 0.15}rem"
      style:left="{laneRight}rem"
      style:width="{Math.max(c.x - laneRight, 0)}rem"
    ></div>
    <div
      class="absolute z-[19] rounded-sm bg-slate-400/20 ring-1 ring-slate-400"
      style:top="{c.top}rem"
      style:height="{lineHeight}rem"
      style:left="{c.x}rem"
      style:width="{tokenWidth(c.ref)}rem"
    ></div>
  {/each}

  <!-- Dots under the endpoint tokens, each one inspects its node -->
  {#each ends as end (end.direction + end.ref.id)}
    {#if isRendered(end.ref.startLine)}
      <button
        type="button"
        class="pointer-events-auto absolute z-[23] flex h-2 w-2 -translate-x-1/2 cursor-pointer items-center justify-center"
        style:top="{lineTop(end.ref.startLine) + lineHeight - 0.4}rem"
        style:left="{tokenStart(end.ref) + tokenWidth(end.ref) / 2}rem"
        title="{describe(end.ref, end.direction)}. Click to inspect it."
        aria-label={describe(end.ref, end.direction)}
        onmouseenter={() => end.key && (hovered = end.key)}
        onmouseleave={() => (hovered = null)}
        onclick={(e) => {
          e.stopPropagation();
          hovered = null;
          onFollow(end.ref, end.direction);
        }}
      >
        <span
          class="h-[5px] w-[5px] rounded-full {end.direction === 'from'
            ? 'bg-slate-600'
            : 'bg-slate-400'} {end.key && end.key === hovered ? 'scale-150' : ''}"
        ></span>
      </button>
    {/if}
  {/each}

  <!-- Hints for endpoints beyond the edges of the viewport -->
  {#each hints as hint (hint.edge)}
    <button
      type="button"
      class="pointer-events-auto absolute z-[28] rounded border border-slate-300 bg-white/95 px-1 font-sans text-[10px] leading-4 whitespace-nowrap text-slate-600 shadow-sm hover:bg-slate-100 hover:text-slate-900"
      style:left="{laneLeft}rem"
      style:top={hint.edge === 'top' ? `${viewTop + 0.15}rem` : `${viewBottom - 1.2}rem`}
      title={hint.title}
      onclick={(e) => {
        e.stopPropagation();
        onScrollTo(hint.line);
      }}
    >
      {hint.label}{#if hint.more > 0}<span class="text-slate-400"> +{hint.more}</span>{/if}
    </button>
  {/each}

  <!-- Dataflows into other files, as chips at the end of the line of the inspected node -->
  {#if nodeLine != null && remote.length > 0 && isRendered(nodeLine)}
    <div
      class="absolute z-[22] flex items-center gap-1 font-sans text-[11px] whitespace-nowrap"
      style:top="{lineTop(nodeLine)}rem"
      style:height="{lineHeight}rem"
      style:left="{endOfLine(nodeLine)}rem"
    >
      {#each remote.slice(0, maxChips) as { ref, direction } (direction + ref.id)}
        <button
          type="button"
          class="pointer-events-auto rounded-full border px-1.5 leading-4 {isExternal(ref)
            ? 'border-orange-300 bg-orange-50 text-orange-700 hover:bg-orange-100'
            : 'border-gray-300 bg-white text-gray-600 hover:bg-gray-100 hover:text-gray-900'}"
          title="{describe(ref, direction)}. Click to {isExternal(ref) ? 'inspect it' : 'open it'}."
          onclick={(e) => {
            e.stopPropagation();
            onFollow(ref, direction);
          }}
        >
          <span class="font-mono">{chipLabel(ref, direction)}</span>
        </button>
      {/each}
      {#if remote.length > maxChips}
        <span
          class="pointer-events-auto text-gray-400"
          title={remote
            .slice(maxChips)
            .map(({ ref, direction }) => describe(ref, direction))
            .join('\n')}
        >
          +{remote.length - maxChips} more
        </span>
      {/if}
    </div>
  {/if}
</div>
