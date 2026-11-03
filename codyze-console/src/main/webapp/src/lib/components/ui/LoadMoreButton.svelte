<script lang="ts">
  interface Props {
    onclick: () => void;
    loading: boolean;
    loadedCount: number;
    totalCount: number;
    itemName: string;
  }

  let { onclick, loading, loadedCount, totalCount, itemName }: Props = $props();
</script>

<button
  {onclick}
  disabled={loading}
  class="inline-flex items-center rounded-md border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:outline-none disabled:cursor-not-allowed disabled:opacity-50"
>
  {#if loading}
    <div class="mr-2 h-4 w-4 animate-spin rounded-full border-b-2 border-gray-600"></div>
    Loading...
  {:else}
    Load more {itemName} ({loadedCount} of {totalCount} loaded)
  {/if}
</button>