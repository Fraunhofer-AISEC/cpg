import type { NodeDetailsJSON, NodeRefJSON } from '$lib/types';

// Node details do not change during a session (except for added concepts), so requests are shared
const cache = new Map<string, Promise<NodeDetailsJSON | null>>();

async function fetchDetails(url: string): Promise<NodeDetailsJSON | null> {
  const res = await fetch(url);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Failed to load node details: ${res.statusText}`);
  return res.json();
}

/** Returns the details of the node with the given ID, or null if it does not exist. */
export function getNodeDetails(nodeId: string): Promise<NodeDetailsJSON | null> {
  let request = cache.get(nodeId);
  if (!request) {
    request = fetchDetails(`/api/node/${nodeId}`);
    request.catch(() => cache.delete(nodeId));
    cache.set(nodeId, request);
  }
  return request;
}

/** Returns the details of the innermost node at a (1-based) line and column of a translation unit. */
export async function getNodeDetailsAt(
  componentName: string,
  unitId: string,
  line: number,
  column: number
): Promise<NodeDetailsJSON | null> {
  const details = await fetchDetails(
    `/api/component/${encodeURIComponent(componentName)}/translation-unit/${unitId}/node-at?line=${line}&column=${column}`
  );
  if (details) cache.set(details.node.id, Promise.resolve(details));
  return details;
}

/** Drops all cached details, e.g. after concepts have been added. */
export function clearNodeDetailsCache() {
  cache.clear();
}

/** The URL of the code viewer showing (and selecting) the referenced node. */
export function nodeHref(ref: NodeRefJSON, fallbackComponent: string): string | undefined {
  if (!ref.translationUnitId) return undefined;
  const component = ref.componentName ?? fallbackComponent;
  return `/components/${encodeURIComponent(component)}/translation-unit/${ref.translationUnitId}?line=${ref.startLine}&node=${ref.id}`;
}