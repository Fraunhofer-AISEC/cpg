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
    allComponents?: ComponentJSON[];
    onFileSelect?: (unit: TranslationUnitJSON) => void;
    fileHref?: (unit: TranslationUnitJSON) => string;
    componentHref?: (componentName: string) => string;
    collapsed?: boolean;
    hideHeader?: boolean;
  }

  let {
    component,
    currentUnitId,
    allComponents,
    onFileSelect,
    fileHref,
    componentHref,
    collapsed = $bindable(false),
    hideHeader = false
  }: Props = $props();

  function buildFileTree(units: TranslationUnitJSON[]): TreeNode[] {
    const filesRoot: TreeNode[] = [];
    const topLevel = component.topLevel;

    units.forEach((unit) => {
      let relativePath = unit.path;
      if (relativePath.startsWith('file:')) relativePath = relativePath.slice(5);
      if (topLevel && relativePath.startsWith(topLevel)) {
        relativePath = relativePath.slice(topLevel.length);
        if (relativePath.startsWith('/')) relativePath = relativePath.slice(1);
      }
      if (!relativePath) relativePath = unit.name || 'root';

      const pathParts = relativePath.split('/').filter((p) => p.length > 0);
      let currentLevel = filesRoot;
      let currentPath = '';

      pathParts.forEach((part, index) => {
        currentPath += (currentPath ? '/' : '') + part;
        const isFile = index === pathParts.length - 1;
        let existingNode = currentLevel.find((n) => n.name === part);
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
        if (!isFile) currentLevel = existingNode.children;
      });
    });

    if (allComponents) {
      return allComponents.map((comp) => ({
        name: comp.name,
        type: 'component' as const,
        path: '',
        componentName: comp.name,
        children: comp.name === component.name ? filesRoot : [],
        expanded: comp.name === component.name,
        isCurrent: comp.name === component.name
      }));
    }

    return filesRoot;
  }

  const fileTree = $derived(buildFileTree(component.translationUnits));
  let expandedFolders = $state(new Set<string>());

  function toggleFolder(path: string) {
    expandedFolders = new Set(
      expandedFolders.has(path)
        ? [...expandedFolders].filter((p) => p !== path)
        : [...expandedFolders, path]
    );
  }

  function handleFileClick(unit: TranslationUnitJSON) {
    onFileSelect?.(unit);
  }
</script>

{#snippet TreeNodeSnippet(nodes: TreeNode[], depth = 0)}
  <ul class="space-y-1">
    {#each nodes as node}
      <li>
        {#if node.type === 'component'}
          <a
            href={componentHref ? componentHref(node.componentName!) : '#'}
            class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm font-semibold {node.isCurrent ? 'border border-indigo-200 bg-indigo-100 text-indigo-800' : 'text-indigo-700 hover:bg-indigo-50'}"
            style="padding-left: {depth * 12 + 8}px"
          >
            <svg class="mr-2 h-4 w-4 {node.isCurrent ? 'text-indigo-600' : 'text-indigo-500'}" fill="currentColor" viewBox="0 0 24 24">
              <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/>
            </svg>
            <span class="text-sm">{node.name}</span>
            {#if node.isCurrent}
              <span class="ml-auto rounded-full bg-indigo-200 px-2 py-0.5 text-xs text-indigo-600">Current</span>
            {/if}
          </a>
          {#if node.isCurrent && node.children.length > 0}
            <div class="mt-1 ml-4 border-l border-indigo-200 pl-2">
              {@render TreeNodeSnippet(node.children, depth + 1)}
            </div>
          {/if}

        {:else if node.type === 'folder'}
          <button
            class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm text-gray-600 hover:bg-gray-50"
            style="padding-left: {depth * 12 + 8}px"
            onclick={() => toggleFolder(node.path)}
          >
            <svg class="mr-1 h-3 w-3 transition-transform {expandedFolders.has(node.path) || node.expanded ? 'rotate-90' : ''}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7"/>
            </svg>
            <svg class="mr-1 h-3 w-3 text-blue-500" fill="currentColor" viewBox="0 0 24 24">
              <path d="M10 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2h-8l-2-2z"/>
            </svg>
            <span class="text-xs font-medium">{node.name}</span>
          </button>
          {#if expandedFolders.has(node.path) || node.expanded}
            {@render TreeNodeSnippet(node.children, depth + 1)}
          {/if}

        {:else}
          {#if fileHref}
            <a
              href={fileHref(node.unit!)}
              class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm
                {currentUnitId === node.unit?.id ? 'border border-blue-200 bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'}"
              style="padding-left: {depth * 12 + 20}px"
            >
              <svg class="mr-1 h-3 w-3 {currentUnitId === node.unit?.id ? 'text-blue-600' : 'text-gray-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <span class="text-xs">{node.name}</span>
            </a>
          {:else}
            <button
              onclick={() => node.unit && handleFileClick(node.unit)}
              class="flex w-full items-center rounded-md px-2 py-1 text-left text-sm
                {currentUnitId === node.unit?.id ? 'border border-blue-200 bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-50'}"
              style="padding-left: {depth * 12 + 20}px"
            >
              <svg class="mr-1 h-3 w-3 {currentUnitId === node.unit?.id ? 'text-blue-600' : 'text-gray-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <span class="text-xs">{node.name}</span>
            </button>
          {/if}
        {/if}
      </li>
    {/each}
  </ul>
{/snippet}

{#if hideHeader}
  {#if collapsed}
    <!-- Collapsed: narrow strip with vertical label -->
    <button
      onclick={() => (collapsed = false)}
      class="group flex h-full w-8 shrink-0 flex-col items-center justify-start gap-2 border-r border-gray-200 bg-white pt-4 text-gray-400 transition-all rounded-l-xl hover:bg-blue-50 hover:text-blue-600"
      aria-label="Show file tree"
    >
      <svg class="w-3 h-3 transition-transform group-hover:translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
      </svg>
      <span class="text-[10px] font-semibold tracking-widest uppercase group-hover:text-blue-500" style="writing-mode: vertical-rl;">Files</span>
    </button>
  {:else}
    <!-- Expanded: left panel with clickable header + blue left accent bar -->
    <div class="flex h-full w-56 shrink-0 flex-col border-r border-gray-200 bg-white rounded-l-xl overflow-hidden">
      <button
        onclick={() => (collapsed = true)}
        class="group flex w-full shrink-0 items-center justify-between border-b border-gray-200 px-3 py-2 text-left transition-colors hover:bg-gray-50"
        aria-label="Collapse file tree"
      >
        <div class="flex items-center gap-2">
          <div class="h-3.5 w-0.5 rounded-full bg-blue-500"></div>
          <span class="text-[11px] font-semibold uppercase tracking-widest text-gray-500 group-hover:text-gray-700">Files</span>
        </div>
        <svg class="h-3.5 w-3.5 text-gray-300 transition-transform group-hover:text-gray-500 group-hover:-translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <nav class="flex-1 overflow-y-auto p-3">
        {@render TreeNodeSnippet(fileTree)}
      </nav>
    </div>
  {/if}
{:else}
  <div class="flex h-full flex-col border-r border-gray-200 bg-white transition-all duration-200 {collapsed ? 'w-8' : 'w-64'}">
    <div class="flex shrink-0 items-center border-b border-gray-200 px-2 py-2 {collapsed ? 'justify-center' : 'justify-between'}">
      {#if !collapsed}
        <span class="text-[11px] font-semibold uppercase tracking-wide text-gray-500">Files</span>
      {/if}
      <button
        onclick={() => (collapsed = !collapsed)}
        class="flex items-center justify-center w-8 h-8 rounded-md text-gray-500 transition-colors hover:bg-gray-200 hover:text-gray-700"
        aria-label={collapsed ? 'Expand file tree' : 'Collapse file tree'}
      >
        <svg class="w-5 h-5 transition-transform duration-300 {collapsed ? 'rotate-180' : ''}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
        </svg>
      </button>
    </div>
    {#if !collapsed}
      <nav class="flex-1 overflow-y-auto p-4">
        {@render TreeNodeSnippet(fileTree)}
      </nav>
    {/if}
  </div>
{/if}