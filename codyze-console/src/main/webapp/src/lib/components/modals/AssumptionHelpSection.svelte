<script lang="ts">
  interface Props {
    hasAssumptions: boolean;
  }

  let { hasAssumptions }: Props = $props();
</script>

{#if hasAssumptions}
  <div class="mt-6 border-t pt-4">
    <h3 class="mb-2 text-sm font-semibold text-gray-900">💡 Managing Assumptions</h3>
    <div class="space-y-2 text-xs text-gray-600">
      <p>
        You can accept or reject assumptions using the Codyze DSL in your project configuration. Use
        the <strong>Copy ID</strong> button above to get the assumption ID.
      </p>
      <div class="rounded bg-gray-100 p-3 font-mono text-xs">
        <div class="mb-1 text-gray-500">// Example Codyze DSL configuration:</div>
        <div class="text-gray-800">
          <div>project {`{`}</div>
          <div class="ml-4">assumptions {`{`}</div>
          <div class="ml-8">decisions {`{`}</div>
          <div class="ml-12 text-green-600">accept("assumption-id-here")</div>
          <div class="ml-12 text-red-600">// or reject("assumption-id-here")</div>
          <div class="ml-8">{`}`}</div>
          <div class="ml-4">{`}`}</div>
          <div>{`}`}</div>
        </div>
      </div>
      <p class="text-xs">
        Replace <code class="rounded bg-gray-200 px-1">assumption-id-here</code>
        with the actual assumption ID. Accepting assumptions increases confidence in the result, while
        rejecting them rejects also the evaluation results that depend on it.
      </p>
    </div>
  </div>
{/if}
