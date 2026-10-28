import type {
  ConceptAnnotationJSON,
  FileAnnotationsJSON,
  FunctionAnnotationJSON
} from '$lib/types';

/** The number of concepts and operations in a function. */
export function conceptsInFunction(
  fn: FunctionAnnotationJSON,
  annotations: FileAnnotationsJSON
): number {
  return annotations.concepts.filter(
    (c) => c.line >= fn.function.startLine && c.line <= fn.function.endLine
  ).length;
}

/**
 * Describes a concept (a thing, e.g. a secret) or an operation (what the code does with it, e.g.
 * getting the secret) and the node it is attached to.
 */
export function describeConcept(c: ConceptAnnotationJSON): string {
  return c.isOperation
    ? `Operation ${c.type}${c.concept ? ` (of ${c.concept})` : ''} · ${c.target}`
    : `Concept ${c.type} · ${c.target}`;
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
