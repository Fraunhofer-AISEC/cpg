<script lang="ts">
  import type { AssumptionJSON } from '$lib/types';
  import QueryTreeNodeValue from '../analysis/QueryTreeNodeValue.svelte';

  interface Props {
    assumption: AssumptionJSON;
    onCopyId: (id: string) => void;
  }

  let { assumption, onCopyId }: Props = $props();

  let showCopyDropdown = $state(false);

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

  // Copy functions
  async function copyToClipboard(text: string) {
    try {
      await navigator.clipboard.writeText(text);
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
      // Fallback for older browsers
      const textArea = document.createElement('textarea');
      textArea.value = text;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
  }

  async function copyId() {
    await copyToClipboard(assumption.id);
    showCopyDropdown = false;
  }

  async function copyAcceptDSL() {
    const dsl = `project {
    assumptions {
        decisions {
            accept("${assumption.id}")
        }
    }
}`;
    await copyToClipboard(dsl);
    showCopyDropdown = false;
  }

  async function copyRejectDSL() {
    const dsl = `project {
    assumptions {
        decisions {
            reject("${assumption.id}")
        }
    }
}`;
    await copyToClipboard(dsl);
    showCopyDropdown = false;
  }

  // Close dropdown when clicking outside
  function handleClickOutside(event: MouseEvent) {
    if (!event.target || !(event.target as Element).closest('.copy-dropdown-container')) {
      showCopyDropdown = false;
    }
  }
</script>

<svelte:window onclick={handleClickOutside} />

<div class="rounded-lg border p-4 {getAssumptionStatusColor(assumption.status)}">
  <!-- Assumption Header -->
  <div class="mb-2 flex items-start justify-between">
    <div class="flex items-center space-x-2">
      <span
        class="rounded border px-2 py-1 text-xs font-medium {getAssumptionStatusColor(
          assumption.status
        )}"
      >
        {assumption.status}
      </span>
      <span class="text-xs text-gray-600">
        {getAssumptionTypeDisplay(assumption.assumptionType)}
      </span>
    </div>

    <!-- Copy Dropdown -->
    <div class="copy-dropdown-container relative">
      <button
        onclick={() => (showCopyDropdown = !showCopyDropdown)}
        class="flex items-center space-x-1 rounded px-2 py-1 text-xs text-gray-600 transition-colors hover:bg-gray-100 hover:text-gray-800"
        title="Copy options"
      >
        <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
          />
        </svg>
        <span>Copy</span>
        <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M19 9l-7 7-7-7"
          />
        </svg>
      </button>

      {#if showCopyDropdown}
        <div
          class="absolute top-full right-0 z-10 mt-1 min-w-48 rounded-md border border-gray-200 bg-white shadow-lg"
        >
          <div class="py-1">
            <button
              onclick={copyId}
              class="flex w-full items-center px-3 py-2 text-xs text-gray-700 transition-colors hover:bg-gray-100"
            >
              <svg
                class="mr-2 h-3 w-3 text-gray-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M7 7h.01M7 3h5c1.1 0 2 .9 2 2v1M7 7V3a1 1 0 011-1h5m-5 5a1 1 0 00-1 1v5a1 1 0 001 1h5a1 1 0 001-1V7a1 1 0 00-1-1H7z"
                />
              </svg>
              Copy ID only
            </button>
            <button
              onclick={copyAcceptDSL}
              class="flex w-full items-center px-3 py-2 text-xs text-gray-700 transition-colors hover:bg-gray-100"
            >
              <svg
                class="mr-2 h-3 w-3 text-green-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M5 13l4 4L19 7"
                />
              </svg>
              Copy accept DSL
            </button>
            <button
              onclick={copyRejectDSL}
              class="flex w-full items-center px-3 py-2 text-xs text-gray-700 transition-colors hover:bg-gray-100"
            >
              <svg
                class="mr-2 h-3 w-3 text-red-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
              Copy reject DSL
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>

  <!-- Assumption Message -->
  <div class="mb-3">
    <p class="text-sm leading-relaxed">
      {assumption.message}
    </p>
  </div>

  <!-- Assumption Details -->
  {#if assumption.node || assumption.nodeId || assumption.edgeLabel || assumption.assumptionScopeId}
    <div class="space-y-2 border-t pt-3 text-xs text-gray-600">
      {#if assumption.node}
        <div>
          <div class="mb-1 font-medium text-purple-700">🎯 Related Node:</div>
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
