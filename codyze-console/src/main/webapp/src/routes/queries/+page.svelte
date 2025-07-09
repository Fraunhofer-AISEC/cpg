<script lang="ts">
  import { onMount } from 'svelte';
  import { PageHeader } from '$lib/components/navigation';

  let scriptCode = $state(`// Kotlin scripting examples - full CPG API access:

// Basic node counting:
result.nodes.size

// Function analysis:
result.allChildren<FunctionDeclaration>().size

// Call expression analysis:
result.allChildren<CallExpression>().size

// Filter for specific calls (e.g., malloc):
result.allChildren<CallExpression>().filter { it.name.localName == "malloc" }.size

// Variable declarations:
result.allChildren<VariableDeclaration>().size

// More complex analysis - function names:
result.allChildren<FunctionDeclaration>().map { it.name.localName }.take(5)

// Find functions with specific parameters:
result.allChildren<FunctionDeclaration>().filter { it.parameters.size > 2 }.size`);
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
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ scriptCode: scriptCode.trim() }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Failed to execute query');
      }

      const data = await response.json();
      queryResult = data.result || 'Query executed successfully (no result)';
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unexpected error occurred';
      console.error('Error executing query:', e);
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
        Write Kotlin scripts to analyze the current translation result. The result is available as the 'result' variable.
        Full Kotlin scripting is now supported with access to the complete CPG API.
      </p>
    </div>
    
    <div class="px-6 pb-4">
      <textarea
        bind:value={scriptCode}
        class="block w-full rounded-md border border-gray-300 px-3 py-2 font-mono text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        rows="12"
        placeholder="Enter your Kotlin query script..."
      ></textarea>
      
      <div class="mt-4 flex justify-between">
        <button
          onclick={executeQuery}
          disabled={isExecuting}
          class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:bg-blue-300"
        >
          {#if isExecuting}
            <svg class="mr-2 h-4 w-4 animate-spin" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Executing...
          {:else}
            Execute Query
          {/if}
        </button>
        
        <button
          onclick={() => (scriptCode = '')}
          class="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          Clear
        </button>
      </div>
    </div>
  </div>

  {#if error}
    <div class="rounded-lg bg-red-50 p-4">
      <div class="flex">
        <div class="flex-shrink-0">
          <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
        </div>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-red-800">Error</h3>
          <div class="mt-2 text-sm text-red-700">
            {error}
          </div>
        </div>
      </div>
    </div>
  {/if}

  {#if queryResult}
    <div class="rounded-lg bg-white shadow">
      <div class="px-6 py-4">
        <h2 class="text-lg font-medium text-gray-900">Query Result</h2>
      </div>
      
      <div class="px-6 pb-4">
        <pre class="rounded-md bg-gray-50 p-4 text-sm overflow-x-auto">{queryResult}</pre>
      </div>
    </div>
  {/if}
</div>