<script lang="ts">
  import type { PageProps } from './$types';
  import NewAnalysis from '$lib/components/NewAnalysis.svelte';
  import { invalidate } from '$app/navigation';

  // Correctly access data with $props()
  let { data }: PageProps = $props();

  let loading = $state(false);

  // Calculate requirement stats with the $derived rune
  const fulfillmentStats = $derived(data.result?.requirementCategories ? {
    total: data.result.requirementCategories.reduce((acc, cat) => acc + cat.requirements.length, 0),
    fulfilled: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'FULFILLED').length, 0),
    violated: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'VIOLATED').length, 0),
    notEvaluated: data.result.requirementCategories.reduce((acc, cat) =>
      acc + cat.requirements.filter(r => r.status === 'NOT_EVALUATED').length, 0)
  } : null);

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

      // Invalidate data to trigger reload of both project and result data
      await invalidate('/api/project');
      await invalidate('/api/result');

    } catch (error) {
      console.error('Error during analysis:', error);
    } finally {
      loading = false;
    }
  }
</script>

<div>
  <header class="mb-6">
    <h1 class="text-2xl font-bold text-gray-900">Dashboard</h1>
    <p class="mt-1 text-sm text-gray-600">Overview of your analysis project</p>
  </header>

  {#if !data.project && !data.result}
    <div class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"></div>
      <p class="ml-2 text-gray-700">Loading dashboard data...</p>
    </div>
  {:else}
    <!-- Project Overview -->
    {#if data.project}
      <div class="mb-8 rounded-lg border border-gray-200 bg-white p-6">
        <h2 class="text-lg font-medium text-gray-900">Project Overview</h2>
        <div class="mt-4 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Project Name</h3>
            <p class="mt-1 text-base font-semibold text-gray-900">{data.project.name}</p>
          </div>
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Source Directory</h3>
            <p class="mt-1 overflow-hidden text-ellipsis text-sm text-gray-700">{data.project.sourceDir}</p>
          </div>
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Created</h3>
            <p class="mt-1 text-sm text-gray-700">
              {new Date(data.project.projectCreatedAt).toLocaleString()}
            </p>
          </div>
          {#if data.project.lastAnalyzedAt}
            <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
              <h3 class="text-xs font-medium uppercase text-gray-500">Last Analyzed</h3>
              <p class="mt-1 text-sm text-gray-700">
                {new Date(data.project.lastAnalyzedAt).toLocaleString()}
              </p>
            </div>
          {/if}
        </div>
      </div>
    {/if}

    <!-- Requirements Summary -->
    {#if data.result?.requirementCategories && data.result.requirementCategories.length > 0 && fulfillmentStats}
      <div class="mb-8 rounded-lg border border-gray-200 bg-white p-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-medium text-gray-900">Requirements Summary</h2>
          <a href="/requirements" class="text-sm font-medium text-blue-600 hover:text-blue-800">View all</a>
        </div>
        <div class="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Total Requirements</h3>
            <p class="mt-1 text-lg font-semibold text-gray-900">{fulfillmentStats.total}</p>
          </div>
          <div class="rounded-md border border-gray-200 bg-green-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Fulfilled</h3>
            <p class="mt-1 text-lg font-semibold text-green-600">
              {fulfillmentStats.fulfilled}
              <span class="ml-1 text-xs font-normal text-gray-500">
                ({Math.round(fulfillmentStats.fulfilled / fulfillmentStats.total * 100) || 0}%)
              </span>
            </p>
          </div>
          <div class="rounded-md border border-gray-200 bg-red-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Violated</h3>
            <p class="mt-1 text-lg font-semibold text-red-600">
              {fulfillmentStats.violated}
              <span class="ml-1 text-xs font-normal text-gray-500">
                ({Math.round(fulfillmentStats.violated / fulfillmentStats.total * 100) || 0}%)
              </span>
            </p>
          </div>
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Not Evaluated</h3>
            <p class="mt-1 text-lg font-semibold text-gray-600">
              {fulfillmentStats.notEvaluated}
              <span class="ml-1 text-xs font-normal text-gray-500">
                ({Math.round(fulfillmentStats.notEvaluated / fulfillmentStats.total * 100) || 0}%)
              </span>
            </p>
          </div>
        </div>
      </div>

      <!-- Recent Requirements -->
      <div class="mb-8 rounded-lg border border-gray-200 bg-white p-6">
        <h2 class="mb-4 text-lg font-medium text-gray-900">Recent Violations</h2>
        <div class="overflow-hidden rounded-md border border-gray-200">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Requirement</th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Category</th>
                <th scope="col" class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 bg-white">
              {#each data.result.requirementCategories as category}
                {#each category.requirements.filter(r => r.status === 'VIOLATED').slice(0, 5) as req}
                  <tr>
                    <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-900">{req.name}</td>
                    <td class="whitespace-nowrap px-6 py-4 text-sm text-gray-500">{category.name}</td>
                    <td class="whitespace-nowrap px-6 py-4">
                      <span class="inline-flex rounded-full bg-red-100 px-2 py-1 text-xs font-semibold leading-5 text-red-800">
                        {req.status}
                      </span>
                    </td>
                  </tr>
                {/each}
              {/each}
            </tbody>
          </table>
        </div>
      </div>
    {/if}

    <!-- Components Summary -->
    {#if data.result?.components && data.result.components.length > 0}
      <div class="mb-8 rounded-lg border border-gray-200 bg-white p-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-medium text-gray-900">Source Code Summary</h2>
          <a href="/source" class="text-sm font-medium text-blue-600 hover:text-blue-800">View source</a>
        </div>
        <div class="mt-4 grid gap-4 grid-cols-2 lg:grid-cols-3">
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Components</h3>
            <p class="mt-1 text-lg font-semibold text-gray-900">{data.result.components.length}</p>
          </div>
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Translation Units</h3>
            <p class="mt-1 text-lg font-semibold text-gray-900">
              {data.result.components.reduce((acc, comp) => acc + comp.translationUnits.length, 0)}
            </p>
          </div>
          <div class="rounded-md border border-gray-200 bg-gray-50 p-4">
            <h3 class="text-xs font-medium uppercase text-gray-500">Total Nodes</h3>
            <p class="mt-1 text-lg font-semibold text-gray-900">{data.result.totalNodes}</p>
          </div>
        </div>
      </div>
    {/if}

    <!-- Start New Analysis -->
    <div class="rounded-lg border border-gray-200 bg-white p-6">
      <h2 class="mb-4 text-lg font-medium text-gray-900">Start New Analysis</h2>
      <NewAnalysis submit={handleSubmit} {loading} />
    </div>
  {/if}
</div>
