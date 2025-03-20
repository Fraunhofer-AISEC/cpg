<script lang="ts">
  import TranslationUnitTable from '$lib/components/TranslationUnitTable.svelte';
  import type { PageProps } from './$types';

  let { data }: PageProps = $props();
</script>

<div class="container mx-auto p-4">
  <div class="mb-4">
    <a href="/" class="text-blue-600 hover:underline">Back to Dashboard </a>
  </div>

  <h1 class="mb-6 text-2xl font-bold">{data.component.name}</h1>
  <p class="mb-4 text-gray-500">{data.component.topLevel}</p>

  <div class="rounded bg-white p-6 shadow-md">
    <h2 class="mb-4 text-xl font-semibold">Translation Units</h2>
    <div class="overflow-x-auto">
      <TranslationUnitTable component={data.component} />
    </div>
  </div>
</div>
