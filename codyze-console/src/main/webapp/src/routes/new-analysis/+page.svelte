<script lang="ts">
  import { NewAnalysis } from '$lib/components/forms';
  import { PageHeader } from '$lib/components/navigation';
  import { goto, invalidateAll } from '$app/navigation';
  import { clearQueryTreeCache } from '$lib/stores/queryTreeStore';

  let loading = $state(false);

  async function handleSubmit(
    sourceDir: string,
    includeDir?: string,
    topLevel?: string,
    conceptSummaries?: string
  ) {
    loading = true;
    try {
      // Clear QueryTree cache before starting new analysis
      clearQueryTreeCache();

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

<PageHeader
  title="Start New Analysis"
  subtitle="Configure and run a new code analysis project"
  breadcrumbText="Back to Dashboard"
  breadcrumbHref="/dashboard"
/>

<div class="space-y-6">
  <div class="mx-auto max-w-4xl rounded-lg border border-gray-200 bg-white p-6">
    <NewAnalysis submit={handleSubmit} {loading} />
  </div>
</div>
