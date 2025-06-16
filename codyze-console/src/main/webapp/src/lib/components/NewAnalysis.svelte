<script lang="ts">
  import FormField from './FormField.svelte';
  import Button from './Button.svelte';

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
    <FormField
      label="Source Directory"
      id="sourceDir"
      placeholder="/path/to/source/code"
      required={true}
      bind:value={sourceDir}
    />
    
    <FormField
      label="Include Directory"
      id="includeDir"
      placeholder="/path/to/include/files"
      helpText="Optional: Path to additional include files"
      bind:value={includeDir}
    />
    
    <FormField
      label="Top Level Directory"
      id="topLevel"
      placeholder="/path/to/top/level"
      helpText="Optional: Top-level directory for the project"
      bind:value={topLevel}
    />
    
    <FormField
      label="Concepts File (.yaml)"
      id="conceptsFile"
      placeholder="/path/to/concept-summaries.yaml"
      helpText="Optional: YAML file containing concept definitions"
      bind:value={conceptsFile}
    />

    <Button
      type="submit"
      disabled={loading}
      {loading}
    >
      Generate CPG
    </Button>
  </form>

  {#if error}
    <div class="mt-4 rounded bg-red-100 p-3 text-red-700">{error}</div>
  {/if}
</div>
