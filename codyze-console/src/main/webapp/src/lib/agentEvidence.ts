import type { NodeJSON, EvidenceDiagnosticJSON } from '$lib/types';

/**
 * The evidence of the agent's answers: the nodes returned by its tool calls and the nodes it cites
 * in its answers as `[[node:<id>]]`.
 */

const uuid = '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}';

/** Matches a citation of a node in an answer of the agent, `[[node:<id>]]`. */
export const citationPattern = new RegExp(`\\[\\[node:(${uuid})\\]\\]`, 'gi');

/** The IDs of the nodes cited in [text], in the order of their first citation. */
export function extractCitations(text: string): string[] {
  return [...new Set([...text.matchAll(citationPattern)].map((m) => m[1].toLowerCase()))];
}

/**
 * The IDs of the nodes in the result of a tool call, in the order of their first occurrence. Tool
 * results have no common format, so every UUID in them is taken; IDs that are no nodes are dropped
 * when they are resolved.
 */
export function extractNodeIds(content: unknown, max = 50): string[] {
  const text = typeof content === 'string' ? content : JSON.stringify(content ?? '');
  const ids = new Set<string>();
  for (const match of text.matchAll(new RegExp(uuid, 'gi'))) {
    ids.add(match[0].toLowerCase());
    if (ids.size >= max) break;
  }
  return [...ids];
}

// The resolved nodes, shared by all callers. Null for IDs that are no nodes
const resolved = new Map<string, NodeJSON | null>();

/** The node as a reference, with only the first line of its code. */
function toRef(node: NodeJSON): NodeJSON {
  return { ...node, code: (node.code ?? '').split('\n')[0].trim() };
}

/**
 * Resolves node IDs to references with their location, asking the backend only for IDs that were
 * not resolved before. IDs that are no nodes are missing from the result.
 */
export async function resolveNodes(ids: string[]): Promise<Map<string, NodeJSON>> {
  const missing = ids.filter((id) => !resolved.has(id));
  if (missing.length > 0) {
    const nodes: NodeJSON[] = await fetch('/api/nodes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(missing)
    })
      .then((r) => (r.ok ? r.json() : []))
      .catch(() => []);
    for (const node of nodes) resolved.set(node.id.toLowerCase(), toRef(node));
    for (const id of missing) if (!resolved.has(id)) resolved.set(id, null);
  }
  const result = new Map<string, NodeJSON>();
  for (const id of ids) {
    const ref = resolved.get(id);
    if (ref) result.set(id, ref);
  }
  return result;
}

/** A short label for a cited node, e.g. `encrypt · main.c:13`. */
export function citationLabel(ref: NodeJSON): string {
  const name = ref.name || (ref.code.length <= 24 ? ref.code : `${ref.code.slice(0, 23)}…`);
  return ref.startLine >= 1 ? `${name} · ${ref.fileName ?? ''}:${ref.startLine}` : name;
}

// The diagnostics of each set of evidence, by its sorted IDs
const diagnosticsCache = new Map<string, Promise<EvidenceDiagnosticJSON[]>>();

/**
 * The places where the analysis is incomplete, uncertain or ends that the given evidence relies on,
 * e.g. unresolved calls in the functions of the nodes. Cached by the set of IDs.
 */
export function checkDiagnostics(ids: string[]): Promise<EvidenceDiagnosticJSON[]> {
  const key = [...new Set(ids)].sort().join(',');
  if (!key) return Promise.resolve([]);
  let diagnostics = diagnosticsCache.get(key);
  if (!diagnostics) {
    diagnostics = fetch('/api/diagnostics', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(key.split(','))
    })
      .then((r) => {
        if (!r.ok) throw new Error(r.statusText);
        return r.json();
      })
      .catch(() => {
        // Ask again next time instead of keeping the failure
        diagnosticsCache.delete(key);
        return [];
      });
    diagnosticsCache.set(key, diagnostics);
  }
  return diagnostics;
}
