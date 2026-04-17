<script lang="ts">
  import type { ConceptSuggestionItem, OperationSuggestionItem, SuggestionItemStatus } from '$lib/types';

  interface Props {
    items: ConceptSuggestionItem[];
    onApplySuggestions?: (accepted: ConceptSuggestionItem[]) => void;
    onHighlightNode?: (nodeId: string | null) => void;
  }

  let { items = $bindable(), onApplySuggestions, onHighlightNode }: Props = $props();

  let expandedConcepts = $state<Set<number>>(new Set());
  let activeNodeId = $state<string | null>(null);

  function toggleExpand(index: number) {
    const next = new Set(expandedConcepts);
    if (next.has(index)) next.delete(index);
    else next.add(index);
    expandedConcepts = next;
  }

  function focusNode(nodeId: string) {
    if (activeNodeId === nodeId) {
      activeNodeId = null;
      onHighlightNode?.(null);
    } else {
      activeNodeId = nodeId;
      onHighlightNode?.(nodeId);
    }
  }

  function setConceptStatus(idx: number, status: SuggestionItemStatus) {
    const concept = items[idx];
    const propagatedOps = concept.operations.map((op) =>
      op.status === 'pending' || status !== 'accepted'
        ? { ...op, status }
        : op
    );
    items[idx] = { ...concept, status, operations: propagatedOps };
  }

  function setOperationStatus(conceptIdx: number, opIdx: number, status: SuggestionItemStatus) {
    const concept = items[conceptIdx];
    const newOps = concept.operations.map((op, i) => (i === opIdx ? { ...op, status } : op));
    items[conceptIdx] = { ...concept, operations: newOps };
  }

  function acceptAll() {
    items = items.map((item) => ({
      ...item,
      status: 'accepted' as SuggestionItemStatus,
      operations: item.operations.map((op) => ({ ...op, status: 'accepted' as SuggestionItemStatus })),
    }));
  }

  function rejectAll() {
    items = items.map((item) => ({
      ...item,
      status: 'rejected' as SuggestionItemStatus,
      operations: item.operations.map((op) => ({ ...op, status: 'rejected' as SuggestionItemStatus })),
    }));
  }

  function apply() {
    const accepted = items
      .filter((item) => item.status === 'accepted')
      .map((item) => ({
        ...item,
        operations: item.operations.filter((op) => op.status === 'accepted'),
      }));
    onApplySuggestions?.(accepted);
  }

  function statusBadge(status: SuggestionItemStatus): string {
    if (status === 'accepted') return 'bg-green-100 text-green-700';
    if (status === 'rejected') return 'bg-red-100 text-red-700';
    return 'bg-yellow-100 text-yellow-700';
  }

  const pendingCount = $derived(items.filter((i) => i.status === 'pending').length);
  const acceptedCount = $derived(items.filter((i) => i.status === 'accepted').length);

  function isActive(nodeId: string) {
    return activeNodeId === nodeId;
  }
</script>

<div class="flex h-full flex-col text-xs">
  <!-- Header -->
  <div class="sticky top-0 z-10 flex items-center justify-between border-b border-gray-200 bg-gray-50 px-3 py-2">
    <span class="text-[11px] font-semibold uppercase tracking-wide text-gray-500">Suggestions</span>
    <span
      class="rounded-full px-2 py-0.5 text-[11px] font-semibold {pendingCount > 0
        ? 'bg-yellow-100 text-yellow-700'
        : 'bg-green-100 text-green-700'}"
    >
      {acceptedCount}/{items.length}
    </span>
  </div>

  <!-- Bulk actions -->
  {#if pendingCount > 0}
    <div class="flex items-center gap-1.5 border-b border-gray-100 px-3 py-2">
      <button
        onclick={acceptAll}
        class="flex-1 rounded bg-green-50 px-2 py-1 text-[11px] font-medium text-green-700 transition-colors hover:bg-green-100"
        type="button">Accept all</button
      >
      <button
        onclick={rejectAll}
        class="flex-1 rounded bg-red-50 px-2 py-1 text-[11px] font-medium text-red-700 transition-colors hover:bg-red-100"
        type="button">Reject all</button
      >
    </div>
  {/if}

  {#if items.length === 0}
    <p class="p-4 text-center italic text-gray-400">No suggestions</p>
  {:else}
    <ul class="flex-1 overflow-y-auto py-1">
      {#each items as item, idx (idx)}
        {@const expanded = expandedConcepts.has(idx)}
        {@const conceptActive = isActive(item.suggestion.nodeId)}
        <li class="mx-1 mb-1 overflow-hidden rounded border {conceptActive ? 'border-blue-400 bg-blue-50/30' : 'border-gray-200 bg-white'}">
          <!-- Concept header -->
          <div class="flex items-center gap-1.5 px-2 py-1.5">
            <button
              onclick={() => toggleExpand(idx)}
              class="shrink-0 p-0.5"
              type="button"
              aria-label="Toggle details for {item.suggestion.name}"
            >
              <svg
                class="h-3 w-3 text-gray-400 transition-transform {expanded ? 'rotate-90' : ''}"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
                stroke-width="2"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>

            <span class="shrink-0 rounded px-1 py-0.5 font-mono text-[10px] font-semibold {statusBadge(item.status)}">
              Concept
            </span>

            <button
              onclick={() => focusNode(item.suggestion.nodeId)}
              class="flex-1 truncate text-left font-medium text-gray-900 hover:text-blue-700"
              type="button"
              title="Focus on code"
            >
              {item.suggestion.name}
            </button>

            {#if item.status === 'pending'}
              <button
                onclick={() => setConceptStatus(idx, 'accepted')}
                class="shrink-0 rounded p-1 text-green-600 transition-colors hover:bg-green-100"
                type="button"
                aria-label="Accept"
              >
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              </button>
              <button
                onclick={() => setConceptStatus(idx, 'rejected')}
                class="shrink-0 rounded p-1 text-red-600 transition-colors hover:bg-red-100"
                type="button"
                aria-label="Reject"
              >
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            {:else}
              <button
                onclick={() => setConceptStatus(idx, 'pending')}
                class="shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold {statusBadge(item.status)} transition-opacity hover:opacity-70"
                type="button"
                aria-label="Reset"
              >
                {item.status}
              </button>
            {/if}
          </div>

          {#if expanded}
            <div class="border-t border-gray-100 bg-gray-50/60 px-3 py-2">
              {#if item.suggestion.description}
                <p class="mb-2 text-[11px] leading-snug text-gray-700">{item.suggestion.description}</p>
              {/if}

              {#if item.suggestion.properties.length > 0}
                <div class="mb-2">
                  <div class="mb-0.5 text-[10px] font-semibold uppercase tracking-wide text-gray-500">Properties</div>
                  <ul class="space-y-0.5">
                    {#each item.suggestion.properties as prop}
                      <li class="flex items-start gap-1.5 font-mono text-[11px]">
                        <span class="text-gray-600">{prop.name}</span>
                        <span class="text-gray-400">:</span>
                        <span class="text-gray-500">{prop.type}</span>
                        <span class="text-gray-400">=</span>
                        <span class="text-gray-900">{prop.value}</span>
                      </li>
                    {/each}
                  </ul>
                </div>
              {/if}

              {#if item.operations.length > 0}
                <div class="mb-0.5 text-[10px] font-semibold uppercase tracking-wide text-gray-500">
                  Operations ({item.operations.length})
                </div>
                <ul class="space-y-1">
                  {#each item.operations as opItem, opIdx}
                    {@const opActive = isActive(opItem.operation.nodeId)}
                    <li class="rounded border {opActive ? 'border-blue-400 bg-blue-50/40' : 'border-gray-200 bg-white'} p-1.5">
                      <div class="flex items-center gap-1.5">
                        <span class="shrink-0 rounded px-1 py-0.5 font-mono text-[10px] font-semibold {statusBadge(opItem.status)}">
                          Op
                        </span>
                        <button
                          onclick={() => focusNode(opItem.operation.nodeId)}
                          class="flex-1 truncate text-left text-[11px] font-medium text-gray-800 hover:text-blue-700"
                          type="button"
                          title="Focus on code"
                        >
                          {opItem.operation.name}
                        </button>
                        {#if opItem.status === 'pending'}
                          <button
                            onclick={() => setOperationStatus(idx, opIdx, 'accepted')}
                            class="shrink-0 rounded p-0.5 text-green-600 hover:bg-green-100"
                            type="button"
                            aria-label="Accept operation"
                          >
                            <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                            </svg>
                          </button>
                          <button
                            onclick={() => setOperationStatus(idx, opIdx, 'rejected')}
                            class="shrink-0 rounded p-0.5 text-red-600 hover:bg-red-100"
                            type="button"
                            aria-label="Reject operation"
                          >
                            <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          </button>
                        {:else}
                          <button
                            onclick={() => setOperationStatus(idx, opIdx, 'pending')}
                            class="shrink-0 rounded px-1 py-0.5 text-[10px] font-semibold {statusBadge(opItem.status)} hover:opacity-70"
                            type="button"
                          >
                            {opItem.status}
                          </button>
                        {/if}
                      </div>
                      {#if opItem.operation.description}
                        <p class="mt-1 pl-1 text-[11px] leading-snug text-gray-600">{opItem.operation.description}</p>
                      {/if}
                      {#if opItem.operation.properties.length > 0}
                        <ul class="mt-1 space-y-0.5 pl-1">
                          {#each opItem.operation.properties as prop}
                            <li class="flex items-start gap-1.5 font-mono text-[10px]">
                              <span class="text-gray-600">{prop.name}</span>
                              <span class="text-gray-400">:</span>
                              <span class="text-gray-500">{prop.type}</span>
                              <span class="text-gray-400">=</span>
                              <span class="text-gray-900">{prop.value}</span>
                            </li>
                          {/each}
                        </ul>
                      {/if}
                    </li>
                  {/each}
                </ul>
              {/if}
            </div>
          {/if}
        </li>
      {/each}
    </ul>
  {/if}

  {#if acceptedCount > 0 && pendingCount === 0}
    <div class="sticky bottom-0 border-t border-gray-200 bg-white px-3 py-2">
      <button
        onclick={apply}
        class="w-full rounded-md bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-blue-700"
        type="button"
      >
        Apply {acceptedCount} suggestion{acceptedCount !== 1 ? 's' : ''}
      </button>
    </div>
  {/if}
</div>