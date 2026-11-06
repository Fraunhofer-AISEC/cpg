<script lang="ts">
  import { tick } from 'svelte';
  import { useSvelteFlow } from '@xyflow/svelte';

  /** Fits the graph into the view whenever [trigger] changes, e.g. after a new layout. */
  let { trigger }: { trigger: unknown } = $props();

  const { fitView } = useSvelteFlow();

  $effect(() => {
    void trigger;
    tick().then(() => fitView({ padding: 0.12, maxZoom: 1, duration: 200 }));
  });
</script>
