<script lang="ts">
  import type { ComponentJSON } from '$lib/types';

  interface Props {
    component: ComponentJSON;
  }

  let { component }: Props = $props();
</script>

<a
  href={`/components/${component.name}`}
  class="group block rounded-lg border border-gray-200 bg-white p-6 transition-all hover:border-blue-300 hover:shadow-md"
>
  <div class="flex items-start">
    <div class="flex-shrink-0">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        class="h-8 w-8 text-blue-600"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
        />
      </svg>
    </div>
    <div class="ml-4 min-w-0 flex-1">
      <h3 class="text-lg font-medium text-gray-900 transition-colors group-hover:text-blue-600">
        {component.name}
      </h3>
      <p class="mt-1 truncate text-sm text-gray-500" title={component.topLevel}>
        {component.topLevel}
      </p>
      <div class="mt-2 flex items-center text-sm text-gray-400">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="mr-1 h-4 w-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
        {component.translationUnits?.length || 0} files
      </div>
    </div>
  </div>
</a>
