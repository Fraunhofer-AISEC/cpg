<script lang="ts">
  import type { FindingsJSON } from '$lib/types';
  import Finding from './Finding.svelte';

  interface Props {
    findings: FindingsJSON[];
  }

  let { findings }: Props = $props();
</script>

<h3 class="mb-2 text-lg font-medium">Findings</h3>
<ul class="divide-y divide-gray-200">
  {#each findings as finding, index (index)}
    <Finding {finding} />
  {/each}
</ul>
