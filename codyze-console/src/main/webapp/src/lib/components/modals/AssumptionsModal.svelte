<script lang="ts">
  import type { AssumptionJSON, QueryTreeJSON } from '$lib/types';
  import AssumptionCard from './AssumptionCard.svelte';
  import AssumptionHelpSection from './AssumptionHelpSection.svelte';
  import ChildrenWithAssumptions from './ChildrenWithAssumptions.svelte';
  import ConfidencePill, { type ConfidenceType } from '$lib/components/ui/ConfidencePill.svelte';

  interface Props {
    assumptions: AssumptionJSON[];
    queryTree?: QueryTreeJSON;
    isOpen: boolean;
    onClose: () => void;
    baseUrl?: string;
    requirementId?: string;
  }

  let { assumptions, queryTree, isOpen, onClose, baseUrl, requirementId }: Props = $props();

  // Check if result is undecided or rejected
  const isUndecidedOrRejected = $derived(() => {
    if (!queryTree) return false;
    return queryTree.confidence === 'RejectedResult' || queryTree.confidence === 'UndecidedResult';
  });

  // Check if we should show children with assumptions
  const shouldShowChildrenWithAssumptions = $derived(() => {
    return assumptions.length === 0 && 
           isUndecidedOrRejected() && 
           queryTree?.childrenWithAssumptionIds && 
           queryTree.childrenWithAssumptionIds.length > 0;
  });

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

  // Copy assumption ID to clipboard
  async function copyAssumptionId(id: string) {
    try {
      await navigator.clipboard.writeText(id);
      // You could add a toast notification here if you have one
    } catch (err) {
      console.error('Failed to copy assumption ID:', err);
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = id;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
  }
</script>

<svelte:window onkeydown={handleKeyDown} />

{#if isOpen}
  <div
    class="fixed inset-0 z-50 flex items-center justify-center backdrop-blur backdrop-brightness-75"
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
        {#if assumptions.length === 0 && !shouldShowChildrenWithAssumptions()}
          <div class="py-8 text-center text-gray-500">
            <div class="text-4xl mb-2">✅</div>
            <p class="text-lg font-medium">No Assumptions</p>
            <p class="mt-1 text-sm">
              This query tree evaluation was made without any assumptions.
            </p>
          </div>
        {:else if shouldShowChildrenWithAssumptions()}
          <div class="py-2 text-center">
            <p class="text-xs text-gray-600 mb-4">
              This evaluation has no direct assumptions, but the overall confidence is 
              <ConfidencePill confidence={queryTree?.confidence || 'UndecidedResult'} size="sm" />
              due to assumptions made in child evaluations.
            </p>
          </div>
          <ChildrenWithAssumptions 
            childrenIds={queryTree?.childrenWithAssumptionIds || []}
            {baseUrl}
            {requirementId}
          />
        {:else}
          <div class="space-y-4">
            <div class="text-sm text-gray-600 mb-4">
              <p>
                This query tree has <strong>{assumptions.length}</strong> assumption{assumptions.length !== 1 ? 's' : ''} 
                that were made during evaluation. These assumptions affect the confidence of the results.
              </p>
            </div>

            {#each assumptions as assumption (assumption.id)}
              <AssumptionCard {assumption} onCopyId={copyAssumptionId} />
            {/each}
          </div>
        {/if}

        <AssumptionHelpSection hasAssumptions={assumptions.length > 0} />
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
