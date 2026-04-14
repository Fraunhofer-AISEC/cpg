<script lang="ts">
  import type { ConceptSuggestionItem, SuggestionItemStatus } from '$lib/types';

  interface Props {
    items: ConceptSuggestionItem[];
    onApplySuggestions?: (accepted: ConceptSuggestionItem[]) => void;
    onHighlightNode?: (nodeId: string | null) => void;
  }

  let {
    items = $bindable(),
    onApplySuggestions,
    onHighlightNode,
  }: Props = $props();

  function setStatus(index: number, status: SuggestionItemStatus) {
    items[index] = { ...items[index], status };
  }

  function acceptAll() {
    items = items.map(item => item.status === 'pending' ? { ...item, status: 'accepted' as SuggestionItemStatus } : item);
  }

  function rejectAll() {
    items = items.map(item => item.status === 'pending' ? { ...item, status: 'rejected' as SuggestionItemStatus } : item);
  }

  function apply() {
    const accepted = items.filter(item => item.status === 'accepted');
    onApplySuggestions?.(accepted);
  }

  let expandedConcepts = $state<Set<number>>(new Set());

  function toggleExpand(index: number) {
    const next = new Set(expandedConcepts);
    if (next.has(index)) next.delete(index);
    else next.add(index);
    expandedConcepts = next;
  }

  function statusBadge(status: SuggestionItemStatus): string {
    if (status === 'accepted') return 'bg-green-100 text-green-700';
    if (status === 'rejected') return 'bg-red-100 text-red-700';
    return 'bg-yellow-100 text-yellow-700';
  }

  const pendingCount = $derived(items.filter(i => i.status === 'pending').length);
  const acceptedCount = $derived(items.filter(i => i.status === 'accepted').length);
</script>

<div class="flex h-full flex-col text-xs">
  <!-- Header -->
  <div class="sticky top-0 z-10 flex items-center justify-between px-3 py-2 bg-gray-50 border-b border-gray-200">
    <span class="text-[11px] font-semibold uppercase tracking-wide text-gray-500">Suggestions</span>
    <span class="rounded-full px-2 py-0.5 text-[11px] font-semibold {pendingCount > 0 ? 'bg-yellow-100 text-yellow-700' : 'bg-green-100 text-green-700'}">
      {acceptedCount}/{items.length}
    </span>
  </div>

  <!-- Bulk actions -->
  {#if pendingCount > 0}
    <div class="flex items-center gap-1.5 px-3 py-2 border-b border-gray-100">
      <button
        onclick={acceptAll}
        class="flex-1 rounded px-2 py-1 text-[11px] font-medium text-green-700 bg-green-50 hover:bg-green-100 transition-colors"
        type="button"
      >Accept all</button>
      <button
        onclick={rejectAll}
        class="flex-1 rounded px-2 py-1 text-[11px] font-medium text-red-700 bg-red-50 hover:bg-red-100 transition-colors"
        type="button"
      >Reject all</button>
    </div>
  {/if}

  {#if items.length === 0}
    <p class="p-4 text-center text-gray-400 italic">No suggestions</p>
  {:else}
    <ul class="flex-1 overflow-y-auto py-1">
      {#each items as item, idx (idx)}
        <li class="mx-1 mb-1">
          <!-- Concept row -->
          <div
            class="flex items-center gap-1.5 rounded px-2 py-1.5 transition-colors hover:bg-gray-50"
            role="group"
            aria-label="Concept: {item.suggestion.name}"
            onmouseenter={() => onHighlightNode?.(item.suggestion.nodeId)}
            onmouseleave={() => onHighlightNode?.(null)}
          >
            <!-- Expand toggle -->
            <button
              onclick={() => toggleExpand(idx)}
              class="shrink-0 p-0.5"
              type="button"
              aria-label="Toggle operations for {item.suggestion.name}"
            >
              <svg
                class="h-3 w-3 text-gray-400 transition-transform {expandedConcepts.has(idx) ? 'rotate-90' : ''}"
                fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>

            <!-- Badge -->
            <span class="shrink-0 rounded px-1 py-0.5 font-mono text-[10px] font-semibold {statusBadge(item.status)}">
              Concept
            </span>

            <!-- Name -->
            <span class="flex-1 truncate font-medium text-gray-900">{item.suggestion.name}</span>

            <!-- Accept / Reject -->
            {#if item.status === 'pending'}
              <button
                onclick={() => setStatus(idx, 'accepted')}
                class="shrink-0 rounded p-1 text-green-600 hover:bg-green-100 transition-colors"
                type="button"
                aria-label="Accept"
              >
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              </button>
              <button
                onclick={() => setStatus(idx, 'rejected')}
                class="shrink-0 rounded p-1 text-red-600 hover:bg-red-100 transition-colors"
                type="button"
                aria-label="Reject"
              >
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            {:else}
              <!-- Click to reset to pending -->
              <button
                onclick={() => setStatus(idx, 'pending')}
                class="shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold {statusBadge(item.status)} hover:opacity-70 transition-opacity"
                type="button"
                aria-label="Reset"
              >
                {item.status}
              </button>
            {/if}
          </div>

          <!-- Operations + Properties (expanded) -->
          {#if expandedConcepts.has(idx)}
            <ul class="ml-5 mt-0.5 border-l border-gray-200 pl-2">
              {#each item.operations as opItem}
                <li
                  class="flex items-center gap-1.5 rounded px-2 py-1 transition-colors hover:bg-gray-50"
                  role="listitem"
                  onmouseenter={() => onHighlightNode?.(opItem.operation.nodeId)}
                  onmouseleave={() => onHighlightNode?.(null)}
                >
                  <span class="shrink-0 rounded px-1 py-0.5 font-mono text-[10px] font-semibold bg-amber-100 text-amber-700">
                    Op
                  </span>
                  <span class="flex-1 truncate text-gray-800">{opItem.operation.name}</span>
                  <span class="shrink-0 font-mono text-[10px] text-gray-400">{opItem.operation.nodeId.slice(0, 8)}</span>
                </li>
              {/each}

              {#if item.suggestion.properties.length > 0}
                <li class="px-2 py-1.5">
                  <span class="text-[10px] font-semibold uppercase tracking-wide text-gray-500">Properties</span>
                  {#each item.suggestion.properties as prop}
                    <div class="flex gap-1 mt-0.5">
                      <span class="font-mono text-gray-600">{prop.name}:</span>
                      <span class="text-gray-800">{prop.value}</span>
                    </div>
                  {/each}
                </li>
              {/if}
            </ul>
          {/if}
        </li>
      {/each}
    </ul>
  {/if}

  <!-- Apply button -->
  {#if acceptedCount > 0 && pendingCount === 0}
    <div class="sticky bottom-0 px-3 py-2 border-t border-gray-200 bg-white">
      <button
        onclick={apply}
        class="w-full rounded-md px-3 py-1.5 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 transition-colors"
        type="button"
      >
        Apply {acceptedCount} suggestion{acceptedCount !== 1 ? 's' : ''}
      </button>
    </div>
  {/if}
</div>