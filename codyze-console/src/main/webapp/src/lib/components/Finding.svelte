<script lang="ts">
  import type { FindingsJSON } from '$lib/types';

  interface Props {
    finding: FindingsJSON;
  }

  let { finding }: Props = $props();

  function getText(finding: FindingsJSON): string {
    return `${finding.kind}: ${finding.rule}`;
  }
</script>

<li class="py-3">
  <p class="text-gray-700">
    <span class="font-medium">Kind:</span>
    {finding.kind}
  </p>
  <p class="text-gray-700">
    <span class="font-medium">Path: </span>
    {#if finding.component && finding.translationUnit}
      <a
        href={`/component/${finding.component}/translation-unit/${finding.translationUnit}?line=${finding.startLine}&findingText=${getText(finding)}&kind=${finding.kind.toLowerCase()}`}
        class="text-blue-600 hover:underline"
      >
        {finding.path}
      </a>
    {:else}
      {finding.path}
    {/if}
  </p>
  <p class="text-gray-700">
    <span class="font-medium">Rule:</span>
    {finding.rule}
  </p>
  <p class="text-gray-700">
    <span class="font-medium">Location:</span>
    {finding.startLine}
    :{finding.startColumn} - {finding.endLine}:{finding.endColumn}
  </p>
</li>
