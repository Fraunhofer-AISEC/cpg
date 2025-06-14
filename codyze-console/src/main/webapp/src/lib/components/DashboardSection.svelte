<script lang="ts">
  interface Props {
    title: string;
    actionText?: string;
    actionHref?: string;
  }

  let { title, actionText, actionHref }: Props = $props();
</script>

<div class="mb-8 rounded-lg border border-gray-200 bg-white p-6">
  <div class="flex items-center justify-between">
    <h2 class="text-lg font-medium text-gray-900">{title}</h2>
    {#if actionText && actionHref}
      <a href={actionHref} class="text-sm font-medium text-blue-600 hover:text-blue-800">
        {actionText}
      </a>
    {/if}
  </div>
  <div class="mt-4">
    <slot />
  </div>
</div>
