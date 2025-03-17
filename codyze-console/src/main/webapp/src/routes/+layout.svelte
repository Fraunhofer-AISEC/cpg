<script lang="ts">
  import Navbar from '$lib/components/Navbar.svelte';

  let { children } = $props();
</script>

<div class="min-h-screen bg-gray-100">
  <Navbar />
  <main class="py-6">
    {@render children()}
  </main>
</div>
