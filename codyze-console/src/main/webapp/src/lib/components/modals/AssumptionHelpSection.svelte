<script lang="ts">
  interface Props {
    hasAssumptions: boolean;
  }

  let { hasAssumptions }: Props = $props();
</script>

{#if hasAssumptions}
  <div class="mt-6 border-t pt-4">
    <h3 class="text-sm font-semibold text-gray-900 mb-2">💡 Managing Assumptions</h3>
    <div class="text-xs text-gray-600 space-y-2">
      <p>
        You can accept or reject assumptions using the Codyze DSL in your project configuration.
        Use the <strong>Copy ID</strong> button above to get the assumption ID.
      </p>
      <div class="bg-gray-100 rounded p-3 font-mono text-xs">
        <div class="text-gray-500 mb-1">// Example Codyze DSL configuration:</div>
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
        Replace <code class="bg-gray-200 px-1 rounded">assumption-id-here</code>
        with the actual assumption ID. Accepting assumptions increases
        confidence in the result, while rejecting them rejects also the
        evaluation results that depend on it.
      </p>
    </div>
  </div>
{/if}
