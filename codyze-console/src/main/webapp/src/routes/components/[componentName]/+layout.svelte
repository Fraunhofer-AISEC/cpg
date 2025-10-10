<script lang="ts">
  import type { LayoutProps } from './$types';
  import type { TranslationUnitJSON } from '$lib/types';
  import { PageHeader } from '$lib/components/navigation';
  import { page } from '$app/stores';

  let { data, children }: LayoutProps = $props();

  // Clean breadcrumb navigation
  const referrerUrl = $derived(() => {
    const urlParams = new URLSearchParams($page.url.search);
    return urlParams.get('referrer');
  });

  const breadcrumbText = $derived(() => {
    return referrerUrl() ? 'Back to Query Explorer' : 'Back to Components';
  });

  const breadcrumbHref = $derived(() => {
    const referrer = referrerUrl();
    if (referrer) {
      // If we have a queryTreeNodeId, add it as targetNodeId to the referrer URL
      const urlParams = new URLSearchParams($page.url.search);
      const queryTreeNodeId = urlParams.get('queryTreeNodeId');

      if (queryTreeNodeId) {
        const url = new URL(referrer, window?.location?.origin || 'http://localhost:3000');
        url.searchParams.set('targetNodeId', queryTreeNodeId);
        return url.pathname + url.search;
      }

      return referrer;
    }
    return '/components';
  });

  // Build folder tree structure from translation units
  interface TreeNode {
    name: string;
    type: 'folder' | 'file' | 'component';
    path: string;
    unit?: TranslationUnitJSON;
    componentName?: string;
    children: TreeNode[];
    expanded?: boolean;
    isCurrent?: boolean;
  }

  function buildFileTree(units: TranslationUnitJSON[]): TreeNode[] {
    const filesRoot: TreeNode[] = [];
    const topLevel = data.component.topLevel;

    units.forEach((unit) => {
      // Handle file URI scheme and make path relative to component's top-level directory
      let relativePath = unit.path;

      // Remove "file:" scheme if present
      if (relativePath.startsWith('file:')) {
        relativePath = relativePath.slice(5);
      }

      // Make path relative to component's top-level directory
      if (topLevel && relativePath.startsWith(topLevel)) {
        relativePath = relativePath.slice(topLevel.length);
        // Remove leading slash if present
        if (relativePath.startsWith('/')) {
          relativePath = relativePath.slice(1);
        }
      }

      // Skip empty paths (files at root level)
      if (!relativePath) {
        relativePath = unit.name || 'root';
      }

      const pathParts = relativePath.split('/').filter((part) => part.length > 0);
      let currentLevel = filesRoot;
      let currentPath = '';

      pathParts.forEach((part, index) => {
        currentPath += (currentPath ? '/' : '') + part;
        const isFile = index === pathParts.length - 1;

        let existingNode = currentLevel.find((node) => node.name === part);

        if (!existingNode) {
          existingNode = {
            name: part,
            type: isFile ? 'file' : 'folder',
            path: currentPath,
            unit: isFile ? unit : undefined,
            children: [],
            expanded: true // Start with folders expanded
          };
          currentLevel.push(existingNode);
        }

        if (!isFile) {
          currentLevel = existingNode.children;
        }
      });
    });

    // Build full tree including all components
    const allComponentNodes: TreeNode[] = data.allComponents.map((comp) => ({
      name: comp.name,
      type: 'component' as const,
      path: '',
      componentName: comp.name,
      children: comp.name === data.component.name ? filesRoot : [],
      expanded: comp.name === data.component.name,
      isCurrent: comp.name === data.component.name
    }));

    return allComponentNodes;
  }

  const fileTree = $derived(buildFileTree(data.component.translationUnits));
  let expandedFolders = $state(new Set<string>());

  function toggleFolder(path: string) {
    if (expandedFolders.has(path)) {
      expandedFolders.delete(path);
    } else {
      expandedFolders.add(path);
    }
    expandedFolders = new Set(expandedFolders); // Trigger reactivity
  }

  // Get current unit ID from the URL using page store
  const currentUnitId = $derived(() => {
    const match = $page.url.pathname.match(/\/translation-unit\/([^\/]+)/);
    return match ? match[1] : null;
  });
</script>

<div class="h-full">
  <PageHeader
    title={data.component.name}
    subtitle={data.component.topLevel}
    breadcrumbText={breadcrumbText()}
    breadcrumbHref={breadcrumbHref()}
  />

  <div
    class="flex h-[calc(100vh-180px)] overflow-hidden rounded-lg border border-gray-200 bg-white"
  >
    <!-- Translation units sidebar -->
    <div class="w-72 overflow-auto border-r border-gray-200">
      <nav class="p-4">
        <h2 class="mb-3 text-xs font-semibold text-gray-500 uppercase">Files</h2>

        <!-- Recursive tree component -->
        {#snippet TreeNode(nodes: TreeNode[], depth = 0)}
          <ul class="space-y-1">
            {#each nodes as node}
              <li>
                {#if node.type === 'component'}
                  <!-- Component -->
                  <a
                    href={`/components/${node.componentName}`}
                    class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm font-semibold {node.isCurrent
                      ? 'border border-indigo-200 bg-indigo-100 text-indigo-800'
                      : 'text-indigo-700 hover:bg-indigo-50'}"
                    style="padding-left: {depth * 12 + 8}px"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      class="mr-2 h-4 w-4 {node.isCurrent ? 'text-indigo-600' : 'text-indigo-500'}"
                      fill="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"
                      />
                    </svg>
                    <span class="text-sm">{node.name}</span>
                    {#if node.isCurrent}
                      <span
                        class="ml-auto rounded-full bg-indigo-200 px-2 py-0.5 text-xs text-indigo-600"
                        >Current</span
                      >
                    {/if}
                  </a>

                  {#if node.isCurrent && node.children.length > 0}
                    <div class="mt-1 ml-4 border-l border-indigo-200 pl-2">
                      {@render TreeNode(node.children, depth + 1)}
                    </div>
                  {/if}
                {:else if node.type === 'folder'}
                  <!-- Folder -->
                  <button
                    class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm text-gray-600 hover:bg-gray-50"
                    style="padding-left: {depth * 12 + 8}px"
                    onclick={() => toggleFolder(node.path)}
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      class="mr-1 h-3 w-3 transition-transform {expandedFolders.has(node.path) ||
                      node.expanded
                        ? 'rotate-90'
                        : ''}"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M9 5l7 7-7 7"
                      />
                    </svg>
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      class="mr-1 h-3 w-3 text-blue-500"
                      fill="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        d="M10 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2h-8l-2-2z"
                      />
                    </svg>
                    <span class="text-xs font-medium">{node.name}</span>
                  </button>

                  {#if expandedFolders.has(node.path) || node.expanded}
                    {@render TreeNode(node.children, depth + 1)}
                  {/if}
                {:else}
                  <!-- File -->
                  <a
                    href={`/components/${data.component.name}/translation-unit/${node.unit?.id}`}
                    class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm {currentUnitId() ===
                    node.unit?.id
                      ? 'border border-blue-200 bg-blue-50 text-blue-700'
                      : 'text-gray-700 hover:bg-gray-50'}"
                    style="padding-left: {depth * 12 + 20}px"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      class="mr-1 h-3 w-3 {currentUnitId() === node.unit?.id
                        ? 'text-blue-600'
                        : 'text-gray-400'}"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                      />
                    </svg>
                    <span class="text-xs">{node.name}</span>
                  </a>
                {/if}
              </li>
            {/each}
          </ul>
        {/snippet}

        {@render TreeNode(fileTree)}
      </nav>
    </div>

    <!-- Main content area -->
    <div class="flex flex-1 overflow-hidden">
      {@render children()}
    </div>
  </div>
</div>
