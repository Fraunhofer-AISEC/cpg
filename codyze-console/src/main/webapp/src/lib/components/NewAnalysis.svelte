<script lang="ts">
  interface Props {
    submit: (
      sourceDir: string,
      includeDir?: string,
      topLevel?: string,
      conceptSummaries?: string
    ) => void;
    loading: boolean;
    error?: string;
  }

  let { submit, loading, error }: Props = $props();

  let sourceDir = $state('');
  let includeDir = $state<string | undefined>(undefined);
  let topLevel = $state<string | undefined>(undefined);
  let conceptsFile = $state<string | undefined>(undefined);
</script>

<div class="mb-6 rounded bg-white p-6 shadow-md">
  <h2 class="mb-4 text-xl font-semibold">Generate CPG</h2>
  <form
    onsubmit={(e) => {
      e.preventDefault();
      submit(sourceDir, includeDir, topLevel, conceptsFile);
    }}
  >
    <div class="mb-4">
      <label for="sourceDir" class="mb-1 block text-sm font-medium text-gray-700">
        Source Directory
      </label>
      <input
        type="text"
        id="sourceDir"
        bind:value={sourceDir}
        class="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 focus:outline-none"
        placeholder="/path/to/source/code"
        required
      />
    </div>
    <div class="mb-4">
      <label for="includeDir" class="mb-1 block text-sm font-medium text-gray-700">
        Include Directory (optional)
      </label>
      <input
        type="text"
        id="includeDir"
        bind:value={includeDir}
        class="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 focus:outline-none"
        placeholder="/path/to/source/code"
      />
    </div>
    <div class="mb-4">
      <label for="topLevel" class="mb-1 block text-sm font-medium text-gray-700">
        Top Level Directory (optional)
      </label>
      <input
        type="text"
        id="topLevel"
        bind:value={topLevel}
        class="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 focus:outline-none"
        placeholder="/path/to/source/code"
      />
    </div>
    <div class="mb-4">
      <label for="conceptsFile" class="mb-1 block text-sm font-medium text-gray-700">
        Concepts File (.yaml) (optional)
      </label>
      <input
        type="text"
        id="conceptsFile"
        bind:value={conceptsFile}
        class="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 focus:outline-none"
        placeholder="/path/to/concept-summaries.yaml"
      />
    </div>
    <button
      type="submit"
      disabled={loading}
      class="rounded bg-blue-600 px-4 py-2 font-medium text-white shadow-sm hover:bg-blue-700 disabled:opacity-70"
    >
      {loading ? 'Generating...' : 'Generate CPG'}
    </button>
  </form>

  {#if error}
    <div class="mt-4 rounded bg-red-100 p-3 text-red-700">{error}</div>
  {/if}
</div>
