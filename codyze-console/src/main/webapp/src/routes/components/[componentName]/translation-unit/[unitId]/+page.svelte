<script lang="ts">
  import { CodeViewer } from '$lib/components/analysis';
  import { Button } from '$lib/components/ui';
  import { page } from '$app/stores';
  import type { PageProps } from './$types';

  let { data }: PageProps = $props();

  const line = $derived($page.url.searchParams.get('line'));
  const finding = $derived($page.url.searchParams.get('finding'));
  const kind = $derived($page.url.searchParams.get('kind'));

  async function exportConcepts() {
    const res = await fetch('/api/export-concepts');
    if (!res.ok) {
      console.error('Error exporting concepts');
      return;
    }
    const content = await res.text();
    const blob = new Blob([content], { type: 'text/yaml' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'concepts.yaml';
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }
</script>

<CodeViewer
  translationUnit={data.translationUnit}
  astNodes={data.astNodes}
  overlayNodes={data.overlayNodes}
  conceptGroups={data.conceptGroups}
  highlightLine={line ? parseInt(line) : undefined}
  finding={finding ?? undefined}
  findingKind={kind ?? undefined}
>
  {#snippet headerActions()}
    <Button variant="primary" size="sm" onclick={exportConcepts}>Export Concepts</Button>
  {/snippet}
</CodeViewer>