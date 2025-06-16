<script lang="ts">
  import type { RequirementsCategoryJSON } from '$lib/types';
  import RequirementCard from '../requirements/RequirementCard.svelte';

  interface Props {
    category: RequirementsCategoryJSON;
  }

  let { category }: Props = $props();

  let isExpanded = $state(false);

  // Calculate category stats
  const stats = $derived({
    total: category.requirements.length,
    fulfilled: category.requirements.filter(r => r.status === 'FULFILLED').length,
    violated: category.requirements.filter(r => r.status === 'VIOLATED').length,
    notEvaluated: category.requirements.filter(r => r.status === 'NOT_EVALUATED').length
  });

  const fulfillmentRate = $derived(
    stats.total > 0 ? Math.round((stats.fulfilled / stats.total) * 100) : 0
  );
</script>

<div class="rounded-lg border border-gray-200 bg-white overflow-hidden">
  <!-- Category Header -->
  <button
    type="button"
    class="flex w-full items-center justify-between p-6 text-left hover:bg-gray-50 transition-colors"
    onclick={() => isExpanded = !isExpanded}
  >
    <div class="flex-1">
      <div class="flex items-center gap-3">
        <h3 class="text-lg font-semibold text-gray-900">{category.name}</h3>
        <span class="inline-flex items-center rounded-full bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800">
          {category.requirements.length} requirements
        </span>
      </div>
      <p class="mt-1 text-sm text-gray-600">{category.description}</p>
      
      <!-- Quick stats -->
      <div class="mt-3 flex gap-4 text-xs">
        <span class="text-green-600">✓ {stats.fulfilled} fulfilled</span>
        <span class="text-red-600">✕ {stats.violated} violated</span>
        <span class="text-gray-500">? {stats.notEvaluated} not evaluated</span>
        <span class="font-medium text-gray-700">({fulfillmentRate}% complete)</span>
      </div>
    </div>
    
    <div class="ml-4 flex items-center">
      <svg 
        class="h-5 w-5 text-gray-400 transition-transform duration-200 {isExpanded ? 'rotate-180' : ''}"
        fill="none" 
        viewBox="0 0 24 24" 
        stroke="currentColor"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </div>
  </button>

  <!-- Requirements List -->
  {#if isExpanded}
    <div class="border-t border-gray-200 p-6 bg-gray-50">
      <div class="space-y-3">
        {#each category.requirements as requirement (requirement.id)}
          <RequirementCard {requirement} />
        {/each}
      </div>
    </div>
  {/if}
</div>
