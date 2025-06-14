<script lang="ts">
  import type { AnalysisResultJSON } from '$lib/types';
  import ComponentsList from '$lib/components/ComponentsList.svelte';
  import FindingsList from '$lib/components/FindingsList.svelte';
  import RequirementsList from '$lib/components/RequirementsList.svelte';

  interface Props {
    result: AnalysisResultJSON;
  }

  let { result }: Props = $props();
</script>

<div>
  <div class="rounded bg-white p-6 shadow-md">
    <h2 class="mb-4 text-xl font-semibold">Analysis Results</h2>
    <div class="mb-4">
      <p class="text-gray-700">
        <span class="font-medium">Total Components:</span>
        {result.components.length}
      </p>
      <p class="text-gray-700">
        <span class="font-medium">Total Nodes:</span>
        {result.totalNodes}
      </p>
    </div>

    {#if result.requirementCategories && result.requirementCategories.length > 0}
      <div class="mb-6">
        <RequirementsList requirementCategories={result.requirementCategories} />
      </div>
    {/if}

    <ComponentsList components={result.components} />
    <FindingsList findings={result.findings} />
  </div>
</div>
