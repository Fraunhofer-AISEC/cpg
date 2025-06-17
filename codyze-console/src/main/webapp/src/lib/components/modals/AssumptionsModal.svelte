<script lang="ts">
  import type { AssumptionJSON } from '$lib/types';
  import QueryTreeNodeValue from '../analysis/QueryTreeNodeValue.svelte';

  interface Props {
    assumptions: AssumptionJSON[];
    isOpen: boolean;
    onClose: () => void;
  }

  let { assumptions, isOpen, onClose }: Props = $props();

  // Get assumption status color
  function getAssumptionStatusColor(status: string): string {
    switch (status) {
      case 'Accepted':
        return 'text-green-700 bg-green-50 border-green-200';
      case 'Rejected':
        return 'text-red-700 bg-red-50 border-red-200';
      case 'Ignored':
        return 'text-gray-700 bg-gray-50 border-gray-200';
      case 'Undecided':
      default:
        return 'text-yellow-700 bg-yellow-50 border-yellow-200';
    }
  }

  // Get assumption type display name
  function getAssumptionTypeDisplay(type: string): string {
    return type.replace(/([A-Z])/g, ' $1').trim();
  }

  // Close modal when clicking outside
  function handleBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      onClose();
    }
  }

  // Handle escape key
  function handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      onClose();
    }
  }
</script>

<svelte:window onkeydown={handleKeyDown} />

{#if isOpen}
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
    onclick={handleBackdropClick}
    onkeydown={handleKeyDown}
    role="dialog"
    aria-modal="true"
    aria-labelledby="assumptions-modal-title"
    tabindex="-1"
  >
    <div class="max-h-[80vh] w-full max-w-2xl overflow-hidden rounded-lg bg-white shadow-xl">
      <!-- Modal Header -->
      <div class="flex items-center justify-between border-b bg-gray-50 p-4">
        <h2 id="assumptions-modal-title" class="text-lg font-semibold text-gray-900">
          Query Tree Assumptions
        </h2>
        <button
          onclick={onClose}
          class="rounded-full p-1 text-gray-400 transition-colors hover:bg-gray-200 hover:text-gray-600"
          aria-label="Close modal"
        >
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Modal Content -->
      <div class="max-h-[60vh] overflow-y-auto p-4">
        {#if assumptions.length === 0}
          <div class="py-8 text-center text-gray-500">
            <div class="text-4xl mb-2">✅</div>
            <p class="text-lg font-medium">No Assumptions</p>
            <p class="mt-1 text-sm">
              This query tree evaluation was made without any assumptions.
            </p>
          </div>
        {:else}
          <div class="space-y-4">
            <div class="text-sm text-gray-600 mb-4">
              <p>
                This query tree has <strong>{assumptions.length}</strong> assumption{assumptions.length !== 1 ? 's' : ''} 
                that were made during evaluation. These assumptions affect the confidence of the results.
              </p>
            </div>

            {#each assumptions as assumption (assumption.id)}
              <div class="border rounded-lg p-4 {getAssumptionStatusColor(assumption.status)}">
                <!-- Assumption Header -->
                <div class="flex items-start justify-between mb-2">
                  <div class="flex items-center space-x-2">
                    <span class="text-xs font-medium px-2 py-1 rounded border {getAssumptionStatusColor(assumption.status)}">
                      {assumption.status}
                    </span>
                    <span class="text-xs text-gray-600">
                      {getAssumptionTypeDisplay(assumption.assumptionType)}
                    </span>
                  </div>
                  <span class="text-xs font-mono text-gray-500">
                    {assumption.id.substring(0, 8)}...
                  </span>
                </div>

                <!-- Assumption Message -->
                <div class="mb-3">
                  <p class="text-sm leading-relaxed">
                    {assumption.message}
                  </p>
                </div>

                <!-- Assumption Details -->
                {#if assumption.node || assumption.nodeId || assumption.edgeLabel || assumption.assumptionScopeId}
                  <div class="text-xs text-gray-600 space-y-2 border-t pt-3">
                    {#if assumption.node}
                      <div>
                        <div class="font-medium mb-1 text-purple-700">🎯 Related Node:</div>
                        <QueryTreeNodeValue node={assumption.node} />
                      </div>
                    {:else if assumption.nodeId}
                      <div>
                        <span class="font-medium">Related Node ID:</span>
                        <span class="font-mono">{assumption.nodeId.substring(0, 8)}...</span>
                      </div>
                    {/if}
                    {#if assumption.edgeLabel}
                      <div>
                        <span class="font-medium">Related Edge:</span>
                        <span class="font-mono">{assumption.edgeLabel}</span>
                      </div>
                    {/if}
                    {#if assumption.assumptionScopeId}
                      <div>
                        <span class="font-medium">Scope:</span>
                        <span class="font-mono">{assumption.assumptionScopeId.substring(0, 8)}...</span>
                      </div>
                    {/if}
                  </div>
                {/if}
              </div>
            {/each}
          </div>
        {/if}
      </div>

      <!-- Modal Footer -->
      <div class="border-t bg-gray-50 p-4">
        <div class="flex justify-end space-x-2">
          <button
            onclick={onClose}
            class="rounded-md bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-200"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}
