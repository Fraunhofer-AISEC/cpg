<script lang="ts">
  import type { PageProps } from './$types';
  import AnalysisResult from '$lib/components/AnalysisResult.svelte';
  import NewAnalysis from '$lib/components/NewAnalysis.svelte';

  let { data }: PageProps = $props();
  let regenerateEnabled = $state(false);
  let loading = $state(false);

  function handleSubmit(sourceDir: string, includeDir?: string, topLevel?: string) {
    loading = true;
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

  <AnalysisResult result={data.result} />
</div>
