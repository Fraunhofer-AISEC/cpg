<script lang="ts">
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import type { AnalysisProjectJSON } from '$lib/types';

  interface Props {
    project?: AnalysisProjectJSON | null;
  }

  let { project = null }: Props = $props();

  // Feature flags from backend
  let mcpEnabled = $state(true); // Default to true, will be updated on mount

  let collapsed = $state(false);

  onMount(async () => {
    try {
      const response = await fetch('/api/features');
      if (response.ok) {
        const features = await response.json();
        mcpEnabled = features.mcpEnabled ?? true;
      }
    } catch (e) {
      console.error('Failed to fetch feature flags:', e);
    }
  });

  // Function to get the last segment of a path
  const getLastPathSegment = (path: string): string => {
    if (!path) return '';
    const segments = path.split(/[/\\]/);
    return segments[segments.length - 1] || segments[segments.length - 2] || path;
  };

  // Navigation items with optional disabled state
  const navItems = $derived([
    { name: 'Dashboard', path: '/dashboard', icon: 'home', disabled: false },
    { name: 'Requirements', path: '/requirements', icon: 'clipboard-check', disabled: false },
    { name: 'Components', path: '/components', icon: 'code', disabled: false },
    { name: 'Agent', path: '/chat', icon: 'robot', disabled: !mcpEnabled }
  ]);

  let currentPath = $derived($page.url.pathname);
</script>

<aside
  class="flex h-full flex-col border-r border-gray-200 bg-white transition-all duration-200 {collapsed
    ? 'w-12'
    : 'w-64'}"
>
  <div class="flex h-16 items-center justify-between border-b border-gray-200 {collapsed ? 'px-2' : 'px-4'}">
    {#if !collapsed}
      <div class="text-xl font-semibold text-blue-600">Codyze</div>
    {/if}
    <button
      type="button"
      onclick={() => (collapsed = !collapsed)}
      class="flex h-8 w-8 items-center justify-center rounded-md text-gray-500 hover:bg-gray-100 hover:text-gray-700"
      title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
      aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        class="h-5 w-5 transition-transform duration-200 {collapsed ? 'rotate-180' : ''}"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
    </button>
  </div>

  {#if project && !collapsed}
    <div class="border-b border-gray-200 p-4">
      <h2 class="text-sm font-medium text-gray-500">PROJECT</h2>
      <div class="mt-1">
        <div class="text-lg font-semibold text-gray-900">
          {project.name}
        </div>
        <div class="mt-1 text-sm text-gray-500" title={project.sourceDir}>
          {getLastPathSegment(project.sourceDir)}
        </div>
      </div>
    </div>
  {/if}

  <nav class="flex-grow {collapsed ? 'p-2' : 'p-4'}">
    <ul class="space-y-2">
      {#each navItems as item}
        <li>
          {#if item.disabled}
            <span
              class="flex cursor-not-allowed items-center rounded-md py-2 text-sm font-medium text-gray-400 {collapsed
                ? 'justify-center px-1'
                : 'px-3'}"
              title={collapsed ? item.name : "The module 'cpg-mcp' is not enabled."}
            >
              {#if item.icon === 'robot'}
                <svg class="{collapsed ? 'h-6 w-6' : 'mr-3 h-5 w-5'}" fill="currentColor" viewBox="0 -960 960 960">
                  <path d="M160-360q-50 0-85-35t-35-85q0-50 35-85t85-35v-80q0-33 23.5-56.5T240-760h120q0-50 35-85t85-35q50 0 85 35t35 85h120q33 0 56.5 23.5T800-680v80q50 0 85 35t35 85q0 50-35 85t-85 35v160q0 33-23.5 56.5T720-120H240q-33 0-56.5-23.5T160-200v-160Zm200-80q25 0 42.5-17.5T420-500q0-25-17.5-42.5T360-560q-25 0-42.5 17.5T300-500q0 25 17.5 42.5T360-440Zm240 0q25 0 42.5-17.5T660-500q0-25-17.5-42.5T600-560q-25 0-42.5 17.5T540-500q0 25 17.5 42.5T600-440ZM320-280h320v-80H320v80Zm-80 80h480v-480H240v480Zm240-240Z" />
                </svg>
              {/if}
              {#if !collapsed}{item.name}{/if}
            </span>
          {:else}
            <a
              href={item.path}
              title={collapsed ? item.name : undefined}
              class="flex items-center rounded-md py-2 text-sm font-medium transition-all {collapsed
                ? 'justify-center px-1'
                : 'px-3'} {currentPath.startsWith(item.path)
                ? item.icon === 'robot'
                  ? 'bg-gradient-to-r from-blue-50 to-purple-50'
                  : 'bg-blue-100 text-blue-600'
                : 'text-gray-700 hover:text-gray-900'}"
            >
              {#if item.icon === 'robot'}
                <svg class="drop-shadow-sm {collapsed ? 'h-6 w-6' : 'mr-3 h-5 w-5'}" style="color: #975ff1" fill="currentColor" viewBox="0 -960 960 960">
                  <path d="M160-360q-50 0-85-35t-35-85q0-50 35-85t85-35v-80q0-33 23.5-56.5T240-760h120q0-50 35-85t85-35q50 0 85 35t35 85h120q33 0 56.5 23.5T800-680v80q50 0 85 35t35 85q0 50-35 85t-85 35v160q0 33-23.5 56.5T720-120H240q-33 0-56.5-23.5T160-200v-160Zm200-80q25 0 42.5-17.5T420-500q0-25-17.5-42.5T360-560q-25 0-42.5 17.5T300-500q0 25 17.5 42.5T360-440Zm240 0q25 0 42.5-17.5T660-500q0-25-17.5-42.5T600-560q-25 0-42.5 17.5T540-500q0 25 17.5 42.5T600-440ZM320-280h320v-80H320v80Zm-80 80h480v-480H240v480Zm240-240Z" />
                </svg>
              {:else}
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class={collapsed ? 'h-6 w-6' : 'mr-3 h-5 w-5'}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  {#if item.icon === 'home'}
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                    />
                  {:else if item.icon === 'clipboard-check'}
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                    />
                  {:else if item.icon === 'code'}
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"
                    />
                  {/if}
                </svg>
              {/if}
              {#if !collapsed}
                <span class={item.icon === 'robot' ? 'bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent' : ''}>{item.name}</span>
              {/if}
            </a>
          {/if}
        </li>
      {/each}
    </ul>
  </nav>

  <div class="border-t border-gray-200 {collapsed ? 'p-2' : 'p-4'}">
    <a
      href="/new-analysis"
      title={collapsed ? 'New Project' : undefined}
      class="flex w-full items-center justify-center rounded-md bg-blue-600 text-sm font-medium text-white hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:outline-none {collapsed
        ? 'p-2'
        : 'px-4 py-2'}"
    >
      <svg class={collapsed ? 'h-5 w-5' : 'mr-2 -ml-1 h-4 w-4'} fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
      {#if !collapsed}New Project{/if}
    </a>
  </div>
</aside>
