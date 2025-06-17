<script lang="ts">
  import { Button } from '$lib/components/ui';
  import { onMount } from 'svelte';

  interface Props {
    fallbackHref?: string;
    fallbackText?: string;
  }

  let { fallbackHref, fallbackText = "Back" }: Props = $props();

  let hasReferrer = $state(false);

  onMount(() => {
    // Check if there's a referrer and it's from the same origin
    hasReferrer = !!(document.referrer && 
                     new URL(document.referrer).origin === window.location.origin);
  });

  function goBack() {
    if (hasReferrer) {
      history.back();
    } else if (fallbackHref) {
      window.location.href = fallbackHref;
    }
  }
</script>

{#if hasReferrer || fallbackHref}
  <Button variant="secondary" onclick={goBack}>
    ← {hasReferrer ? "Back" : fallbackText}
  </Button>
{/if}
