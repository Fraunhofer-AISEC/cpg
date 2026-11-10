import type { CallSiteJSON, FunctionCallStatsJSON, NodeJSON, OverlayJSON } from '$lib/types';

/** What the code viewer shows about a file without any interaction. */
export interface FileOverview {
  functions: FunctionCallStatsJSON[];
  calls: CallSiteJSON[];
  overlays: OverlayJSON[];
}

/**
 * Describes a concept (a thing, e.g. a secret) or an operation (what the code does with it, e.g.
 * getting the secret) and the node it is attached to, e.g. `Operation GetSecret of Secret · key`.
 */
export function describeOverlay(overlay: OverlayJSON): string {
  const node = overlay.underlyingNode;
  const target = node ? ` · ${node.name || node.code.split('\n')[0]}` : '';
  return overlay.concept
    ? `Operation ${overlay.overlay.type} of ${overlay.concept.type}${target}`
    : `Concept ${overlay.overlay.type}${target}`;
}

/** A short label for the kind of the overlay, e.g. `Concept` or `Operation of File`. */
export function overlayLabel(overlay: OverlayJSON): string {
  return overlay.concept ? `Operation of ${overlay.concept.type}` : 'Concept';
}

/** The line of the code the overlay is attached to. */
export function overlayLine(overlay: OverlayJSON): number {
  return (overlay.underlyingNode ?? overlay.overlay).startLine;
}

/** The part of the code that stands for a call: the name of its target, or else the whole call. */
export function callRegion(call: CallSiteJSON): NodeJSON {
  return call.callee ?? call.call;
}

/** The number of concepts and operations in a function. */
export function overlaysInFunction(fn: FunctionCallStatsJSON, overview: FileOverview): number {
  return overview.overlays.filter((o) => {
    const line = overlayLine(o);
    return line >= fn.function.startLine && line <= fn.function.endLine;
  }).length;
}

/** Fetches the concepts and operations of all translation units of a component. */
export async function getComponentOverlays(componentName: string): Promise<OverlayJSON[]> {
  const res = await fetch(`/api/component/${encodeURIComponent(componentName)}/overlays`).catch(
    () => null
  );
  return res?.ok ? res.json().catch(() => []) : [];
}

/** The number of concepts and operations per translation unit (by ID). */
export function countByTranslationUnit(overlays: OverlayJSON[]): Map<string, number> {
  const counts = new Map<string, number>();
  for (const { overlay } of overlays) {
    if (overlay.translationUnitId) {
      counts.set(overlay.translationUnitId, (counts.get(overlay.translationUnitId) ?? 0) + 1);
    }
  }
  return counts;
}

/**
 * Fetches the functions, calls and overlays of a translation unit, or null if they are not
 * available.
 */
export async function getFileOverview(
  componentName: string,
  unitId: string
): Promise<FileOverview | null> {
  const base = `/api/component/${encodeURIComponent(componentName)}/translation-unit/${unitId}`;
  const load = async <T>(path: string): Promise<T | null> => {
    const res = await fetch(`${base}/${path}`).catch(() => null);
    return res?.ok ? res.json().catch(() => null) : null;
  };
  const [functions, calls, overlays] = await Promise.all([
    load<FunctionCallStatsJSON[]>('functions'),
    load<CallSiteJSON[]>('calls'),
    load<OverlayJSON[]>('overlays')
  ]);
  return functions && calls && overlays ? { functions, calls, overlays } : null;
}
