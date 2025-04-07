<script lang="ts">
  import type { PageProps } from './$types';
  import AnalysisResult from '$lib/components/AnalysisResult.svelte';
  import NewAnalysis from '$lib/components/NewAnalysis.svelte';
  import { invalidate } from '$app/navigation';

  let { data }: PageProps = $props();
  let regenerateEnabled = $state(false);
  let loading = $state(false);

  async function handleSubmit(
    sourceDir: string,
    includeDir?: string,
    topLevel?: string,
    conceptSummaries?: string
  ) {
    loading = true;
    try {
      const response = await fetch('/api/analyze', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ sourceDir, includeDir, topLevel, conceptSummaries })
      });

      if (!response.ok) {
        throw new Error('Network response was not ok');
      }

      const data = await response.json();
      await invalidate('/api/result');
      console.log('Generation successful:', data);
    } catch (error) {
      console.error('Error during generation:', error);
    } finally {
      loading = false;
    }
  }
</script>

<div class="container mx-auto p-4">
  <NewAnalysis submit={handleSubmit} {loading} />

  {#if regenerateEnabled}
    <div class="mb-6">
      <button
        class="mt-4 rounded bg-green-600 px-4 py-2 font-bold text-white hover:bg-green-700"
        disabled={loading}
      >
        {loading ? 'Working...' : 'Re-Generate CPG'}
      </button>
      <p class="mt-1 text-sm text-gray-600">Re-run analysis with the current configuration</p>
    </div>
  {/if}

  {#if data.result && data.result.components}
    <AnalysisResult result={data.result} />
  {/if}
</div>
