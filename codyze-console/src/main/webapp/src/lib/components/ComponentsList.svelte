<script lang="ts">
  import type { ComponentJSON } from '$lib/types';

  interface Props {
    components: ComponentJSON[];
  }

  let { components }: Props = $props();
</script>

<h3 class="mb-2 text-lg font-medium">Components</h3>
<ul class="divide-y divide-gray-200">
  {#each components as component (component.name)}
    <li class="py-3">
      <a
        href={`/component/${component.name}`}
        class="text-lg font-medium text-blue-600 hover:underline"
      >
        {component.name}
      </a>
      <p class="mt-1 text-sm text-gray-600">
        {component.translationUnits.length} translation units
      </p>
    </li>
  {/each}
</ul>
