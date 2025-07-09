<script lang="ts">
  import type { RequirementsCategoryJSON } from '$lib/types';

  interface Props {
    requirementCategories: RequirementsCategoryJSON[];
  }

  let { requirementCategories }: Props = $props();

  // Keep track of expanded categories
  let expandedCategories = $state(new Set<string>());

  function toggleCategory(categoryId: string) {
    // Create a new Set to ensure reactivity
    const newSet = new Set(expandedCategories);

    if (newSet.has(categoryId)) {
      newSet.delete(categoryId);
    } else {
      newSet.add(categoryId);
    }

    expandedCategories = newSet;
  }

  // Status color mapping
  function getStatusColor(status: string): string {
    switch (status.toUpperCase()) {
      case 'FULFILLED':
        return 'bg-green-100 text-green-800';
      case 'NOT_FULFILLED':
        return 'bg-red-100 text-red-800';
      case 'REJECTED':
        return 'bg-orange-100 text-orange-800';
      case 'UNDECIDED':
      default:
        return 'bg-yellow-100 text-yellow-800';
    }
  }
</script>

<div class="mb-6">
  <h3 class="mb-2 text-lg font-semibold">Requirements</h3>

  {#if requirementCategories && requirementCategories.length > 0}
    <div class="space-y-4">
      {#each requirementCategories as category (category.id)}
        <div class="rounded-lg border border-gray-200 bg-white shadow-sm">
          <!-- Category header -->
          <button
            type="button"
            class="flex w-full cursor-pointer items-center justify-between rounded-t-lg bg-gray-50 px-4 py-3 hover:bg-gray-100"
            onclick={() => toggleCategory(category.id)}
          >
            <div>
              <h4 class="font-medium">{category.name}</h4>
              <p class="text-sm text-gray-600">{category.description}</p>
            </div>
            <div class="ml-3 text-gray-500">
              <span
                class="inline-flex items-center rounded bg-blue-100 px-2 py-1 text-xs font-medium text-blue-800"
              >
                {category.requirements.length} requirements
              </span>
              <span class="ml-2">
                {expandedCategories.has(category.id) ? '▼' : '►'}
              </span>
            </div>
          </button>

          <!-- Requirements list (expandable) -->
          {#if expandedCategories.has(category.id)}
            <div class="divide-y divide-gray-200 px-4 py-3">
              {#each category.requirements as requirement (requirement.id)}
                <div class="py-3">
                  <div class="flex items-center justify-between">
                    <h5 class="font-medium">{requirement.name}</h5>
                    <span
                      class={`rounded px-2 py-1 text-xs font-medium ${getStatusColor(requirement.status)}`}
                    >
                      {requirement.status}
                    </span>
                  </div>
                  <p class="mt-1 text-sm text-gray-600">{requirement.description}</p>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      {/each}
    </div>
  {:else}
    <p class="text-gray-500">No requirements defined for this project.</p>
  {/if}
</div>
