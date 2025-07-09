<script lang="ts">
  import { getFindingStyle } from '$lib/colors';

  interface Props {
    finding: string;
    kind: string | null;
    line: number;
    lineHeight: number;
    offsetTop: number;
  }

  let { finding, kind, line, lineHeight, offsetTop }: Props = $props();

  let { backgroundColor, borderColor, color } = getFindingStyle(kind);
</script>

<div
  class="absolute right-0 left-0 border-l-4 px-4 py-2"
  style:top={`${line * lineHeight + offsetTop}rem`}
  style:z-index="30"
  style:max-width="100%"
  style:overflow-wrap="break-word"
  style:background-color={backgroundColor}
  style:border-color={borderColor}
  style:color
>
  {finding}
</div>
