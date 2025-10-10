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

<div class="border-b border-gray-200">
  <nav class="-mb-px flex space-x-8" aria-label="Tabs">
    {#each tabs as tab}
      <button
        class="border-b-2 px-1 py-2 text-sm font-medium whitespace-nowrap {activeTab === tab.id
          ? 'border-blue-500 text-blue-600'
          : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'}"
        onclick={() => onTabChange(tab.id)}
        aria-current={activeTab === tab.id ? 'page' : undefined}
      >
        {tab.label}
        {#if tab.count !== undefined}
          <span
            class="ml-2 rounded-full px-2.5 py-0.5 text-xs {activeTab === tab.id
              ? 'bg-blue-100 text-blue-600'
              : 'bg-gray-100 text-gray-900'}"
          >
            {tab.count}
          </span>
        {/if}
      </button>
    {/each}
  </nav>
</div>
