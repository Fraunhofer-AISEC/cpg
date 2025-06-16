<script lang="ts">
  import type { ComponentJSON } from '$lib/types';

  interface Props {
    component: ComponentJSON;
  }

  let { component }: Props = $props();
</script>

<table class="min-w-full divide-y divide-gray-200">
  <thead class="bg-gray-50">
    <tr>
      <th class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
        Name
      </th>
      <th class="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
        Actions
      </th>
    </tr>
  </thead>
  <tbody class="divide-y divide-gray-200 bg-white">
    {#each component.translationUnits as unit (unit.path)}
      <tr>
        <td class="px-6 py-4 text-sm font-medium whitespace-nowrap text-gray-900">
          {unit.name}
        </td>
        <td class="px-6 py-4 text-sm whitespace-nowrap text-gray-500">
          <a
            href={`/components/${component.name}/translation-unit/${unit.id}`}
            class="text-blue-600 hover:underline"
          >
            View Details
          </a>
        </td>
      </tr>
    {/each}
  </tbody>
</table>
