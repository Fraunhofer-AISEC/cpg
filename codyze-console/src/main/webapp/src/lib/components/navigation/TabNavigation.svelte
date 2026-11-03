<script lang="ts">
  interface Tab {
    id: string;
    label: string;
    count?: number;
  }

  interface Props {
    tabs: Tab[];
    activeTab: string;
    onTabChange: (tabId: string) => void;
  }

  let { tabs, activeTab, onTabChange }: Props = $props();
</script>

<div class="mx-2 my-1.5 flex gap-1 rounded-lg bg-gray-100 p-1.5">
  {#each tabs as tab}
    <button
      class="flex flex-1 items-center justify-center gap-1.5 rounded-md px-2 py-1 text-xs font-medium whitespace-nowrap transition-colors
        {activeTab === tab.id
        ? 'bg-white text-blue-600 shadow-sm'
        : 'text-gray-500 hover:text-gray-700'}"
      onclick={() => onTabChange(tab.id)}
      aria-current={activeTab === tab.id ? 'page' : undefined}
    >
      {tab.label}
      {#if tab.count !== undefined}
        <span
          class="rounded-full px-1.5 py-0.5 text-[10px] font-semibold
          {activeTab === tab.id ? 'bg-blue-100 text-blue-600' : 'bg-gray-200 text-gray-500'}"
        >
          {tab.count}
        </span>
      {/if}
    </button>
  {/each}
</div>
