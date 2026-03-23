<script lang="ts">
  import type { LayoutProps } from './$types';
  import { PageHeader } from '$lib/components/navigation';
  import { FileTree } from '$lib/components/analysis';
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
    <FileTree
      component={data.component}
      allComponents={data.allComponents}
      currentUnitId={currentUnitId()}
      fileHref={(unit) => `/components/${data.component.name}/translation-unit/${unit.id}`}
      componentHref={(name) => `/components/${name}`}
    />

    <!-- Main content area -->
    <div class="flex flex-1 overflow-hidden">
      {@render children()}
    </div>
  </div>
</div>
