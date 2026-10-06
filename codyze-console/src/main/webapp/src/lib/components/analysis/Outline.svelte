<script lang="ts">
  import type { FileAnnotationsJSON, FunctionAnnotationJSON, NodeRefJSON } from '$lib/types';
  import { conceptsInFunction } from '$lib/annotations';

  /**
   * The functions of a file with their key figures as small inline bars, sortable per column, to
   * compare the functions of a file and find its hotspots.
   */
  interface Props {
    /** The annotations of the open file, or null while they are loading */
    annotations: FileAnnotationsJSON | null;
    fileName?: string;
    /** The 1-based line of the selection in the open file, to mark the function containing it */
    selectionLine?: number;
    onSelect: (fn: NodeRefJSON) => void;
  }

  let { annotations, fileName, selectionLine, onSelect }: Props = $props();

  type Column = 'callers' | 'external' | 'unresolved' | 'concepts';
  type SortKey = 'position' | 'name' | Column;

  const columns: { id: Column; label: string; title: string; bar: string }[] = [
    { id: 'callers', label: '←', title: 'Callers', bar: 'bg-gray-400' },
    { id: 'external', label: '↗', title: 'Calls to external code', bar: 'bg-orange-500' },
    { id: 'unresolved', label: '⚠', title: 'Unresolved calls', bar: 'bg-red-500' },
    { id: 'concepts', label: '◆', title: 'Concepts and operations', bar: 'bg-purple-500' }
  ];

  interface Row {
    fn: FunctionAnnotationJSON;
    values: Record<Column, number>;
  }

  const rows = $derived.by((): Row[] => {
    if (!annotations) return [];
    return annotations.functions.map((fn) => ({
      fn,
      values: {
        callers: fn.callers,
        external: fn.externalCalls,
        unresolved: fn.unresolvedCalls,
        concepts: conceptsInFunction(fn, annotations)
      }
    }));
  });

  // The largest value per column, which is a full bar
  const maxima = $derived.by(() => {
    const max: Record<Column, number> = { callers: 0, external: 0, unresolved: 0, concepts: 0 };
    for (const row of rows) {
      for (const column of columns)
        max[column.id] = Math.max(max[column.id], row.values[column.id]);
    }
    return max;
  });

  let sortKey = $state<SortKey>('position');
  // Numbers are sorted descending by default, names ascending
  let descending = $state(true);

  function sortBy(key: SortKey) {
    if (sortKey === key) {
      descending = !descending;
    } else {
      sortKey = key;
      descending = key !== 'name' && key !== 'position';
    }
  }

  const sortedRows = $derived.by(() => {
    const byPosition = (a: Row, b: Row) => a.fn.function.startLine - b.fn.function.startLine;
    const compare = (a: Row, b: Row): number => {
      if (sortKey === 'position') return byPosition(a, b);
      if (sortKey === 'name') return a.fn.function.name.localeCompare(b.fn.function.name);
      return a.values[sortKey] - b.values[sortKey] || -byPosition(a, b);
    };
    const sorted = [...rows].sort(compare);
    return descending ? sorted.reverse() : sorted;
  });

  // The innermost function containing the selection
  const selectedFunctionId = $derived.by(() => {
    if (!selectionLine) return null;
    let best: FunctionAnnotationJSON | null = null;
    for (const { fn } of rows) {
      const { startLine, endLine } = fn.function;
      if (selectionLine < startLine || selectionLine > endLine) continue;
      if (!best || endLine - startLine < best.function.endLine - best.function.startLine) best = fn;
    }
    return best?.function.id ?? null;
  });

  function sortIndicator(key: SortKey): string {
    if (sortKey !== key) return '';
    return descending ? '↓' : '↑';
  }
</script>

<div class="flex h-full min-h-0 flex-col bg-white">
  <div class="shrink-0 border-b border-gray-200 px-2 py-2">
    <p class="text-[11px] font-semibold tracking-wider text-gray-500 uppercase">Outline</p>
    {#if fileName}
      <p class="truncate font-mono text-[11px] text-gray-400" title={fileName}>{fileName}</p>
    {/if}
  </div>

  {#if !annotations}
    <p class="px-3 py-2 text-xs text-gray-400">Loading…</p>
  {:else if rows.length === 0}
    <p class="px-3 py-2 text-xs text-gray-400">No functions in this file</p>
  {:else}
    <!-- Column headers, which sort the list -->
    <div
      class="flex shrink-0 items-center gap-1 border-b border-gray-100 py-1 pr-2 pl-2 text-[10px] text-gray-500"
    >
      <button
        type="button"
        class="min-w-0 flex-1 truncate text-left hover:text-gray-900"
        onclick={() => sortBy(sortKey === 'position' ? 'name' : 'position')}
        title={sortKey === 'position'
          ? 'Sorted by position in the file; click to sort by name'
          : 'Click to sort by position in the file'}
      >
        Function {sortKey === 'name' ? `(name ${sortIndicator('name')})` : ''}
      </button>
      {#each columns as column (column.id)}
        <button
          type="button"
          class="w-8 shrink-0 text-right hover:text-gray-900 {sortKey === column.id
            ? 'font-semibold text-gray-900'
            : ''}"
          onclick={() => sortBy(column.id)}
          title="Sort by: {column.title}"
        >
          {sortIndicator(column.id)}{column.label}
        </button>
      {/each}
    </div>

    <ul class="min-h-0 flex-1 overflow-y-auto py-0.5">
      {#each sortedRows as row (row.fn.function.id)}
        {@const selected = row.fn.function.id === selectedFunctionId}
        <li>
          <button
            type="button"
            class="relative flex w-full items-center gap-1 py-1 pr-2 pl-2 text-left text-xs {selected
              ? 'bg-gray-200/70 text-gray-900'
              : 'text-gray-700 hover:bg-gray-100'}"
            onclick={() => onSelect(row.fn.function)}
            title="{row.fn.function.name} (line {row.fn.function.startLine})"
          >
            {#if selected}
              <span class="absolute top-0 bottom-0 left-0 w-0.5 bg-blue-500"></span>
            {/if}
            <span class="min-w-0 flex-1 truncate font-mono text-[11px]">{row.fn.function.name}</span
            >
            {#each columns as column (column.id)}
              {@const value = row.values[column.id]}
              <span
                class="flex w-8 shrink-0 flex-col items-end gap-px"
                title="{column.title}: {value}"
              >
                <span
                  class="text-[10px] tabular-nums {value > 0 ? 'text-gray-600' : 'text-gray-300'}"
                  >{value}</span
                >
                <span class="h-1 w-full rounded-sm bg-gray-100">
                  {#if value > 0}
                    <span
                      class="block h-full rounded-sm {column.bar}"
                      style:width="{Math.max((value / maxima[column.id]) * 100, 8)}%"
                    ></span>
                  {/if}
                </span>
              </span>
            {/each}
          </button>
        </li>
      {/each}
    </ul>
  {/if}
</div>
