import type { FileAnnotationsJSON, FunctionAnnotationJSON } from '$lib/types';

/** The key figure by which functions are colored in the code viewer. */
export type Lens = 'callers' | 'external' | 'uncertain' | 'concepts' | 'none';

export const lenses: { id: Lens; label: string; color: string; describe: (n: number) => string }[] =
  [
    { id: 'callers', label: 'Callers', color: '37, 99, 235', describe: (n) => `${n} callers` },
    {
      id: 'external',
      label: 'External calls',
      color: '234, 88, 12',
      describe: (n) => `${n} calls to external code`
    },
    {
      id: 'uncertain',
      label: 'Uncertainty',
      color: '220, 38, 38',
      describe: (n) => `${n} unresolved calls`
    },
    {
      id: 'concepts',
      label: 'Concepts',
      color: '147, 51, 234',
      describe: (n) => `${n} concepts / operations`
    },
    { id: 'none', label: 'No coloring', color: '0, 0, 0', describe: () => '' }
  ];

/** The value of a function for a lens. */
export function lensValue(
  lens: Lens,
  fn: FunctionAnnotationJSON,
  annotations: FileAnnotationsJSON
): number {
  switch (lens) {
    case 'callers':
      return fn.callers;
    case 'external':
      return fn.externalCalls;
    case 'uncertain':
      return fn.unresolvedCalls;
    case 'concepts':
      return annotations.concepts.filter(
        (c) => c.line >= fn.function.startLine && c.line <= fn.function.endLine
      ).length;
    case 'none':
      return 0;
  }
}

const categoryIcons: Record<string, string> = {
  crypto: '🔒',
  file: '📄',
  http: '🌐',
  network: '🌐',
  auth: '🔑',
  logging: '📝',
  memory: '🧠',
  config: '⚙️',
  flows: '🚪',
  diskEncryption: '💽',
  policy: '📋'
};

/** An icon for the category (package) of a concept. */
export function conceptIcon(category: string): string {
  return categoryIcons[category] ?? '◆';
}

/** Fetches the number of concepts and operations per translation unit (by ID) of a component. */
export async function getConceptCounts(componentName: string): Promise<Map<string, number>> {
  const res = await fetch(
    `/api/component/${encodeURIComponent(componentName)}/concept-counts`
  ).catch(() => null);
  const counts: Record<string, number> = res?.ok ? await res.json().catch(() => ({})) : {};
  return new Map(Object.entries(counts));
}

/** Fetches the annotations of a translation unit, or null if they are not available. */
export async function getAnnotations(
  componentName: string,
  unitId: string
): Promise<FileAnnotationsJSON | null> {
  const res = await fetch(
    `/api/component/${encodeURIComponent(componentName)}/translation-unit/${unitId}/annotations`
  ).catch(() => null);
  return res?.ok ? res.json() : null;
}
