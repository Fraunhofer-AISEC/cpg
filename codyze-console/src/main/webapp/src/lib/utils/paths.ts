import type { TranslationUnitJSON } from '$lib/types';

/** The path of a translation unit relative to the top level directory of its component. */
export function relativePath(unit: TranslationUnitJSON, topLevel?: string): string {
  let path = unit.path;
  if (path.startsWith('file:')) path = path.slice(5);
  if (topLevel && path.startsWith(topLevel)) {
    path = path.slice(topLevel.length);
    if (path.startsWith('/')) path = path.slice(1);
  }
  return path || unit.name || 'root';
}
