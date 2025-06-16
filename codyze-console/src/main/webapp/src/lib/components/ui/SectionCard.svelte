<script lang="ts">
  interface Props {
    title: string;
    subtitle?: string;
    children?: any;
  }

  let { title, subtitle, children }: Props = $props();
</script>

<div class="bg-white border rounded-lg p-6">
  <div class="mb-4">
    <h3 class="text-lg font-semibold text-gray-900">
      {title}
    </h3>
    {#if subtitle}
      <p class="text-sm text-gray-600 mt-1">
        {subtitle}
      </p>
    {/if}
  </div>
  
  <div>
    {@render children?.()}
  </div>
</div>
