<script lang="ts">
  import RequirementsPieChart from './RequirementsPieChart.svelte';
  import RequirementsBarChart from './RequirementsBarChart.svelte';

  interface Props {
    fulfilled: number;
    notFulfilled: number;
    rejected: number;
    undecided: number;
    notYetEvaluated: number;
  }

  let { fulfilled, notFulfilled, rejected, undecided, notYetEvaluated }: Props = $props();

  let chartType = $state<'pie' | 'bar'>('pie');

  const total = $derived(fulfilled + notFulfilled + rejected + undecided + notYetEvaluated);
</script>

<div class="space-y-4">
  <!-- Chart and stats container -->
  {#if total > 0}
    <div class="grid gap-6 lg:grid-cols-3">
      <!-- Chart container -->
      <div class="space-y-4 lg:col-span-2">
        <div class="rounded-lg border border-gray-200 bg-white p-6">
          {#if chartType === 'pie'}
            <RequirementsPieChart {fulfilled} {notFulfilled} {rejected} {undecided} {notYetEvaluated} />
          {:else}
            <RequirementsBarChart {fulfilled} {notFulfilled} {rejected} {undecided} {notYetEvaluated} />
          {/if}
        </div>

        <!-- Chart type selector - moved below chart -->
        <div class="flex justify-center">
          <div class="inline-flex rounded-lg border border-gray-200 bg-gray-50 p-1">
            <button
              type="button"
              class="rounded-md px-3 py-1.5 text-sm font-medium transition-all duration-200 {chartType ===
              'pie'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'}"
              onclick={() => (chartType = 'pie')}
            >
              Pie Chart
            </button>
            <button
              type="button"
              class="rounded-md px-3 py-1.5 text-sm font-medium transition-all duration-200 {chartType ===
              'bar'
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'}"
              onclick={() => (chartType = 'bar')}
            >
              Bar Chart
            </button>
          </div>
        </div>
      </div>

      <!-- Stats summary -->
      <div class="lg:col-span-1">
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-2 lg:gap-4">
          <div class="rounded-lg border border-green-200 bg-green-50 p-3 text-center lg:p-4">
            <div class="text-2xl font-bold text-green-700 lg:text-3xl">{fulfilled}</div>
            <div class="text-sm font-medium text-green-600">Fulfilled</div>
            <div class="text-xs text-green-500">{Math.round((fulfilled / total) * 100) || 0}%</div>
          </div>
          <div class="rounded-lg border border-red-200 bg-red-50 p-3 text-center lg:p-4">
            <div class="text-2xl font-bold text-red-700 lg:text-3xl">{notFulfilled}</div>
            <div class="text-sm font-medium text-red-600">Not Fulfilled</div>
            <div class="text-xs text-red-500">{Math.round((notFulfilled / total) * 100) || 0}%</div>
          </div>
          <div class="rounded-lg border border-orange-200 bg-orange-50 p-3 text-center lg:p-4">
            <div class="text-2xl font-bold text-orange-700 lg:text-3xl">{rejected}</div>
            <div class="text-sm font-medium text-orange-600">Rejected</div>
            <div class="text-xs text-orange-500">{Math.round((rejected / total) * 100) || 0}%</div>
          </div>
          <div class="rounded-lg border border-yellow-200 bg-yellow-50 p-3 text-center lg:p-4">
            <div class="text-2xl font-bold text-yellow-700 lg:text-3xl">{undecided}</div>
            <div class="text-sm font-medium text-yellow-600">Undecided</div>
            <div class="text-xs text-yellow-500">{Math.round((undecided / total) * 100) || 0}%</div>
          </div>
          <div
            class="col-span-2 rounded-lg border border-gray-200 bg-gray-50 p-3 text-center lg:col-span-2 lg:p-4"
          >
            <div class="text-2xl font-bold text-gray-700 lg:text-3xl">{notYetEvaluated}</div>
            <div class="text-sm font-medium text-gray-600">Not Yet Evaluated</div>
            <div class="text-xs text-gray-500">
              {Math.round((notYetEvaluated / total) * 100) || 0}%
            </div>
          </div>
        </div>
      </div>
    </div>
  {:else}
    <div class="rounded-lg border border-gray-200 bg-gray-50 p-8 text-center">
      <div class="text-gray-500">
        <svg
          class="mx-auto h-12 w-12 text-gray-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
          />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">No requirements data</h3>
        <p class="mt-1 text-sm text-gray-500">Run an analysis to see requirements distribution.</p>
      </div>
    </div>
  {/if}
</div>
