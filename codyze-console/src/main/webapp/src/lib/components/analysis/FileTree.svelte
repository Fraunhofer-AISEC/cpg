<script lang="ts">
  import type { ComponentJSON, TranslationUnitJSON } from '$lib/types';

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

  interface Props {
    component: ComponentJSON;
    currentUnitId?: string;
    onFileSelect?: (unit: TranslationUnitJSON) => void;
  }

  let { component, currentUnitId, onFileSelect }: Props = $props();

  function buildFileTree(units: TranslationUnitJSON[]): TreeNode[] {
    const filesRoot: TreeNode[] = [];
    const topLevel = component.topLevel;

    units.forEach((unit) => {
      let relativePath = unit.path;

      if (relativePath.startsWith('file:')) {
        relativePath = relativePath.slice(5);
      }

      if (topLevel && relativePath.startsWith(topLevel)) {
        relativePath = relativePath.slice(topLevel.length);
        if (relativePath.startsWith('/')) {
          relativePath = relativePath.slice(1);
        }
      }

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
            expanded: true
          };
          currentLevel.push(existingNode);
        }

        if (!isFile) {
          currentLevel = existingNode.children;
        }
      });
    });

    return filesRoot;
  }

  const fileTree = $derived(buildFileTree(component.translationUnits));
  let expandedFolders = $state(new Set<string>());

  function toggleFolder(path: string) {
    if (expandedFolders.has(path)) {
      expandedFolders.delete(path);
    } else {
      expandedFolders.add(path);
    }
    expandedFolders = new Set(expandedFolders);
  }

  function handleFileClick(unit: TranslationUnitJSON) {
    onFileSelect?.(unit);
  }
</script>

<div class="p-4">
  <h3 class="text-sm font-semibold text-gray-900 mb-3">Files</h3>

  {#snippet TreeNode(nodes: TreeNode[], depth = 0)}
    <ul class="space-y-1">
      {#each nodes as node}
        <li>
          {#if node.type === 'folder'}
            <!-- Folder -->
            <button
              class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm text-gray-600 hover:bg-gray-50"
              style="padding-left: {depth * 12 + 8}px"
              onclick={() => toggleFolder(node.path)}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="mr-1 h-3 w-3 transition-transform {expandedFolders.has(node.path) || node.expanded ? 'rotate-90' : ''}"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
              </svg>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="mr-1 h-3 w-3 text-blue-500"
                fill="currentColor"
                viewBox="0 0 24 24"
              >
                <path d="M10 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2h-8l-2-2z"/>
              </svg>
              <span class="text-xs font-medium">{node.name}</span>
            </button>

            {#if expandedFolders.has(node.path) || node.expanded}
              {@render TreeNode(node.children, depth + 1)}
            {/if}
          {:else}
            <!-- File -->
            <button
              onclick={() => node.unit && handleFileClick(node.unit)}
              class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm {currentUnitId === node.unit?.id
                ? 'border border-blue-200 bg-blue-50 text-blue-700'
                : 'text-gray-700 hover:bg-gray-50'}"
              style="padding-left: {depth * 12 + 20}px"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="mr-1 h-3 w-3 {currentUnitId === node.unit?.id ? 'text-blue-600' : 'text-gray-400'}"
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
            </button>
          {/if}
        </li>
      {/each}
    </ul>
  {/snippet}

  {@render TreeNode(fileTree)}
</div>