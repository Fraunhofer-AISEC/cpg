<script lang="ts">
  import { PageHeader } from '$lib/components/navigation';
  import MonacoEditor from '$lib/components/MonacoEditor.svelte';

  let scriptCode = $state(`// CPG Query Script — the TranslationResult is available as \`result\`
// All CPG packages are imported automatically.

// Count all functions (Shortcut API):
result.functions.size

// Get function names:
// result.functions.map { it.name.localName }

// Count all call expressions:
// result.calls.size

// Find calls to a specific function:
// result.calls.filter { it.name.localName == "malloc" }.size

// Count all variables:
// result.variables.size

// Count all nodes:
// result.nodes.size
`);
  let queryResult = $state('');
  let isExecuting = $state(false);
  let error = $state<string | null>(null);

  async function executeQuery() {
    if (!scriptCode.trim()) {
      error = 'Please enter a query script';
      return;
    }

    try {
      isExecuting = true;
      error = null;
      queryResult = '';

      const response = await fetch('/api/execute-query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ scriptCode: scriptCode.trim() })
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Failed to execute query');
      }

      const data = await response.json();
      queryResult = data.result ?? 'Query executed successfully (no return value)';
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unexpected error occurred';
    } finally {
      isExecuting = false;
    }
  }
</script>

<PageHeader title="Query Interface" />

<div class="space-y-6">
  <div class="rounded-lg bg-white shadow">
    <div class="px-6 py-4">
      <h2 class="text-lg font-medium text-gray-900">Kotlin Query Script</h2>
      <p class="mt-1 text-sm text-gray-500">
        Write Kotlin scripts to query the current analysis result. The
        <a
          href="https://fraunhofer-aisec.github.io/cpg/GettingStarted/shortcuts/"
          class="text-blue-600 hover:underline"
          target="_blank"
          rel="noopener noreferrer">Shortcut API</a
        >
        (e.g. <code class="rounded bg-gray-100 px-1 font-mono text-xs">result.functions</code>,
        <code class="rounded bg-gray-100 px-1 font-mono text-xs">result.calls</code>) and the
        <a
          href="https://fraunhofer-aisec.github.io/cpg/GettingStarted/query/"
          class="text-blue-600 hover:underline"
          target="_blank"
          rel="noopener noreferrer">Query API</a
        >
        are available. All standard CPG packages are imported automatically.
      </p>
    </div>

    <div class="px-6 pb-4">
      <MonacoEditor bind:value={scriptCode} height="320px" />

      <div class="mt-4 flex items-center gap-3">
        <button
          onclick={executeQuery}
          disabled={isExecuting}
          class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:outline-none disabled:opacity-50"
        >
          {#if isExecuting}
            <svg class="mr-2 h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle
                class="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                stroke-width="4"
              ></circle>
              <path
                class="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              ></path>
            </svg>
            Executing…
          {:else}
            Execute
          {/if}
        </button>

        <button
          onclick={() => {
            scriptCode = '';
            queryResult = '';
            error = null;
          }}
          class="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:outline-none"
        >
          Clear
        </button>
      </div>
    </div>
  </div>

  {#if error}
    <div class="rounded-lg bg-red-50 p-4">
      <div class="flex">
        <svg
          class="mr-3 h-5 w-5 flex-shrink-0 text-red-400"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path
            fill-rule="evenodd"
            d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
            clip-rule="evenodd"
          />
        </svg>
        <div>
          <h3 class="text-sm font-medium text-red-800">Error</h3>
          <p class="mt-1 text-sm text-red-700">{error}</p>
        </div>
      </div>
    </div>
  {/if}

  {#if queryResult}
    <div class="rounded-lg bg-white shadow">
      <div class="px-6 py-4">
        <h2 class="text-lg font-medium text-gray-900">Result</h2>
      </div>
      <div class="px-6 pb-4">
        <pre
          class="overflow-x-auto rounded-md bg-gray-50 p-4 text-sm text-gray-800">{queryResult}</pre>
      </div>
    </div>
  {/if}
</div>
