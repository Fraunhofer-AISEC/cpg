<script lang="ts">
  import NewAnalysis from '$lib/components/NewAnalysis.svelte';
  import { goto, invalidateAll } from '$app/navigation';

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

      await response.json();

      // Invalidate all data and redirect to dashboard
      await invalidateAll();
      await goto('/dashboard');

    } catch (error) {
      console.error('Error during analysis:', error);
    } finally {
      loading = false;
    }
  }
</script>

<div class="mx-auto max-w-4xl">
  <header class="mb-8">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Start New Analysis</h1>
        <p class="mt-2 text-lg text-gray-600">Configure and run a new code analysis project</p>
      </div>
      <a 
        href="/dashboard" 
        class="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
      >
        ← Back to Dashboard
      </a>
    </div>
  </header>

  <div class="rounded-lg border border-gray-200 bg-white p-6">
    <NewAnalysis submit={handleSubmit} {loading} />
  </div>
</div>
