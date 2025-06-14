<script lang="ts">
  import { page } from "$app/stores";
  import type { AnalysisProjectJSON } from "$lib/types";

  interface Props {
    project?: AnalysisProjectJSON | null;
  }

  let { project = null }: Props = $props();

  // Function to get the last segment of a path
  const getLastPathSegment = (path: string): string => {
    if (!path) return "";
    const segments = path.split(/[/\\]/);
    return segments[segments.length - 1] || segments[segments.length - 2] || path;
  };

  // Navigation items
  const navItems = [
    { name: "Dashboard", path: "/dashboard", icon: "home" },
    { name: "Requirements", path: "/requirements", icon: "clipboard-check" },
    { name: "Source Code", path: "/source", icon: "code" },
  ];

  let currentPath = $derived($page.url.pathname);
</script>

<aside class="flex h-full w-64 flex-col border-r border-gray-200 bg-white">
  <div class="flex h-16 items-center justify-between border-b border-gray-200 px-4">
    <div class="text-xl font-semibold text-blue-600">Codyze</div>
  </div>

  {#if project}
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

  <nav class="flex-grow p-4">
    <ul class="space-y-2">
      {#each navItems as item}
        <li>
          <a
            href={item.path}
            class="flex items-center rounded-md px-3 py-2 text-sm font-medium {currentPath.startsWith(item.path) ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'}"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="mr-3 h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              {#if item.icon === 'home'}
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              {:else if item.icon === 'clipboard-check'}
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
              {:else if item.icon === 'code'}
                <path stroke-linecap="round" stroke-linejoin="round" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
              {/if}
            </svg>
            {item.name}
          </a>
        </li>
      {/each}
    </ul>
  </nav>

  <div class="border-t border-gray-200 p-4">
    <a 
      href="/new-analysis" 
      class="flex w-full items-center justify-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
    >
      <svg class="-ml-1 mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
      New Project
    </a>
  </div>
</aside>
