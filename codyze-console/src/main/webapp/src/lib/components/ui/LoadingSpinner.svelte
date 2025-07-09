<script lang="ts">
  interface Props {
    message?: string;
    size?: 'sm' | 'md' | 'lg';
  }

  let { message = 'Loading...', size = 'md' }: Props = $props();

  const sizeClasses = {
    sm: 'h-6 w-6',
    md: 'h-8 w-8',
    lg: 'h-12 w-12'
  };
</script>

<div class="flex items-center justify-center py-12">
  <div
    class="{sizeClasses[size]} animate-spin rounded-full border-2 border-gray-300 border-t-blue-600"
  ></div>
  <p class="ml-2 text-gray-700">{message}</p>
</div>
