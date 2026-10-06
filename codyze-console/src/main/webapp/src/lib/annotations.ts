import type { FileAnnotationsJSON, FunctionAnnotationJSON } from '$lib/types';

/** The number of concepts and operations in a function. */
export function conceptsInFunction(
  fn: FunctionAnnotationJSON,
  annotations: FileAnnotationsJSON
): number {
  return annotations.concepts.filter(
    (c) => c.line >= fn.function.startLine && c.line <= fn.function.endLine
  ).length;
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
