<script lang="ts">
  import StatCard from './StatCard.svelte';

  interface Props {
    stats: Array<{
      title: string;
      value: string | number;
      subtitle?: string;
      variant?: 'default' | 'success' | 'danger' | 'warning';
    }>;
    columns?: 2 | 3 | 4;
  }

  let { stats, columns = 4 }: Props = $props();

  const gridClasses = {
    2: 'sm:grid-cols-2',
    3: 'grid-cols-2 lg:grid-cols-3',
    4: 'sm:grid-cols-2 lg:grid-cols-4'
  };
</script>

<div class="grid gap-4 {gridClasses[columns]}">
  {#each stats as stat}
    <StatCard {...stat} />
  {/each}
</div>
