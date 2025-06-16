<script lang="ts">
  interface Props {
    label: string;
    id: string;
    type?: 'text' | 'email' | 'password' | 'number';
    placeholder?: string;
    required?: boolean;
    helpText?: string;
    value: string | undefined;
  }

  let { label, id, type = 'text', placeholder, required = false, helpText, value = $bindable() }: Props = $props();
</script>

<div class="mb-4">
  <label for={id} class="mb-1 block text-sm font-medium text-gray-700">
    {label}
    {#if required}
      <span class="text-red-500">*</span>
    {/if}
  </label>
  <input
    {type}
    {id}
    bind:value
    class="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 focus:outline-none"
    {placeholder}
    {required}
  />
  {#if helpText}
    <p class="mt-1 text-sm text-gray-500">{helpText}</p>
  {/if}
</div>
