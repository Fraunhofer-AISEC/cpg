<script lang="ts">
  import type { RequirementsCategoryJSON } from '$lib/types';

  interface Props {
    categories: RequirementsCategoryJSON[];
    maxRows?: number;
  }

  let { categories, maxRows = 5 }: Props = $props();

  // Get all violated requirements, limited by maxRows
  const violatedRequirements = $derived(
    categories
      .flatMap((cat) =>
        cat.requirements
          .filter((r) => r.status === 'VIOLATED')
          .map((req) => ({ ...req, categoryName: cat.name }))
      )
      .slice(0, maxRows)
  );
</script>

<div class="overflow-hidden rounded-md border border-gray-200">
  <table class="min-w-full divide-y divide-gray-200">
    <thead class="bg-gray-50">
      <tr>
        <th
          scope="col"
          class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
        >
          Requirement
        </th>
        <th
          scope="col"
          class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
        >
          Category
        </th>
        <th
          scope="col"
          class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase"
        >
          Status
        </th>
      </tr>
    </thead>
    <tbody class="divide-y divide-gray-200 bg-white">
      {#each violatedRequirements as req}
        <tr>
          <td class="px-6 py-4 text-sm whitespace-nowrap text-gray-900">{req.name}</td>
          <td class="px-6 py-4 text-sm whitespace-nowrap text-gray-500">{req.categoryName}</td>
          <td class="px-6 py-4 whitespace-nowrap">
            <span
              class="inline-flex rounded-full bg-red-100 px-2 py-1 text-xs leading-5 font-semibold text-red-800"
            >
              {req.status}
            </span>
          </td>
        </tr>
      {/each}
      {#if violatedRequirements.length === 0}
        <tr>
          <td colspan="3" class="px-6 py-4 text-center text-sm text-gray-500">
            No violations found
          </td>
        </tr>
      {/if}
    </tbody>
  </table>
</div>
