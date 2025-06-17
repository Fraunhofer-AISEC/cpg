<script lang="ts">
  interface BreadcrumbItem {
    label: string;
    href: string;
  }

  interface Props {
    items: BreadcrumbItem[];
  }

  let { items }: Props = $props();
</script>

{#if items.length > 0}
  <nav class="py-1" aria-label="Breadcrumb">
    <ol class="flex items-center space-x-2 text-sm">
      {#each items as item, index}
        {#if index > 0}
          <li class="text-gray-400">
            <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
            </svg>
          </li>
        {/if}
        <li>
          {#if index < items.length - 1}
            <a href={item.href} class="text-blue-600 hover:text-blue-800 hover:underline font-medium">
              {item.label}
            </a>
          {:else}
            <span class="text-gray-900 font-semibold">{item.label}</span>
          {/if}
        </li>
      {/each}
    </ol>
  </nav>
{/if}
