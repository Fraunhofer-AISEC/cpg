<script lang="ts">
  interface Props {
    icon?: 'document' | 'component' | 'chart' | 'folder' | 'custom';
    title: string;
    description: string;
    actionText?: string;
    actionHref?: string;
    customIcon?: string; // For custom SVG content
  }

  let { icon = 'document', title, description, actionText, actionHref, customIcon }: Props = $props();

  const icons = {
    document: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />`,
    component: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />`,
    chart: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />`,
    folder: `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />`,
    custom: customIcon || ''
  };
</script>

<div class="text-center py-12">
  <div class="mx-auto mb-4 h-16 w-16 rounded-full bg-gray-100 p-3 text-gray-500">
    <svg xmlns="http://www.w3.org/2000/svg" class="h-full w-full" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      {#if icon === 'custom' && customIcon}
        {@html customIcon}
      {:else}
        {@html icons[icon]}
      {/if}
    </svg>
  </div>
  <h3 class="text-lg font-medium text-gray-900">{title}</h3>
  <p class="mt-2 text-sm text-gray-600">{description}</p>
  
  {#if actionText && actionHref}
    <div class="mt-6">
      <a 
        href={actionHref}
        class="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
      >
        <svg class="-ml-1 mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        {actionText}
      </a>
    </div>
  {/if}
</div>
