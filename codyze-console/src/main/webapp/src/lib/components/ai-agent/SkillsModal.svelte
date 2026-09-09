<script lang="ts">
  import MarkdownRenderer from './MarkdownRenderer.svelte';
  import type { SkillInfo } from '$lib/types';

  interface Props {
    skills: SkillInfo[];
    onClose: () => void;
  }

  let { skills, onClose }: Props = $props();

  function handleBackdropClick(e: MouseEvent) {
    if (e.target === e.currentTarget) onClose();
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') onClose();
  }
</script>

<svelte:window onkeydown={handleKeydown} />

<div
  class="fixed inset-0 z-50 flex items-center justify-center bg-black/20 p-4 backdrop-blur-[2px]"
  role="presentation"
  onclick={handleBackdropClick}
  onkeydown={handleKeydown}
>
  <div class="flex w-full max-w-2xl flex-col rounded-2xl bg-white shadow-2xl max-h-[80vh]">
    <div class="flex shrink-0 items-center justify-between border-b border-gray-200 px-5 py-4">
      <div class="flex items-center gap-3">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-50 text-amber-600">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <div>
          <h2 class="text-sm font-semibold text-gray-900">Skills</h2>
          <p class="text-xs text-gray-400">{skills.length} loaded</p>
        </div>
      </div>
      <button
        class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
        onclick={onClose}
        aria-label="Close"
      >
        <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <div class="flex-1 overflow-y-auto px-5 py-3">
      {#if skills.length === 0}
        <p class="py-4 text-center text-sm text-gray-400">No skills discovered.</p>
      {:else}
        <ul class="space-y-2">
          {#each skills as skill}
            <li class="rounded-xl border border-gray-200 bg-gray-50">
              <details class="group">
                <summary class="flex cursor-pointer list-none items-start gap-3 px-4 py-3">
                  <svg
                    class="mt-0.5 h-3.5 w-3.5 shrink-0 text-gray-400 transition-transform duration-150 group-open:rotate-90"
                    fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                  <div class="min-w-0 flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-mono text-xs font-semibold text-gray-900">{skill.name}</span>
                    </div>
                    {#if skill.description}
                      <p class="mt-1 line-clamp-2 text-xs leading-snug text-gray-500 group-open:line-clamp-none">
                        {skill.description}
                      </p>
                    {/if}
                  </div>
                </summary>

                {#if skill.body}
                  <div class="border-t border-gray-200 px-4 pb-3 pt-2.5">
                    <p class="mb-2 text-xs font-medium uppercase tracking-wide text-gray-400">Instructions</p>
                    <div class="prose prose-sm max-w-none text-xs text-gray-700">
                      <MarkdownRenderer content={skill.body} />
                    </div>
                  </div>
                {/if}
              </details>
            </li>
          {/each}
        </ul>
      {/if}
    </div>
  </div>
</div>