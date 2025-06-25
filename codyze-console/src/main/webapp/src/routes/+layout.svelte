<script lang="ts">
  import '../app.css';
  import { Navbar, Sidebar } from '$lib/components/navigation';
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import type { AnalysisProjectJSON } from '$lib/types';

  let project = $state<AnalysisProjectJSON | null>(null);
  let loading = $state(true);
  let error = $state<string | null>(null);

  // Fetch project data
  async function fetchProject() {
    try {
      loading = true;
      const response = await fetch('/api/project');
      if (response.ok) {
        project = await response.json();
      }
    } catch (e) {
      console.error('Error fetching project:', e);
      error = 'Failed to load project information';
    } finally {
      loading = false;
    }
  }

  onMount(async () => {
    await fetchProject();

    // Redirect to dashboard if on root path
    if ($page.url.pathname === '/') {
      goto('/dashboard');
    }
  });
</script>

<div class="flex h-screen bg-gray-50">
  <Sidebar {project} />

  <main class="flex-1 overflow-auto">
    <div class="p-6">
      {#if error}
        <div class="mb-4 rounded-md bg-red-50 p-4 text-sm text-red-800">
          {error}
        </div>
      {/if}

      <slot />
    </div>
  </main>
</div>
