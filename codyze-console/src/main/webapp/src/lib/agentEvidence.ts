import type { NodeJSON, NodeRefJSON, TrustIssueJSON } from '$lib/types';

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
 * Paths through the graph in the result of a tool call (the format `NodePaths` of the tools that
 * find paths): what they follow, what they show, and the node IDs of each path in the direction of
 * the flow.
 */
export interface ToolPaths {
  kind: string;
  description: string;
  paths: string[][];
  truncated: boolean;
}

function asToolPaths(value: unknown): ToolPaths | null {
  if (!value || typeof value !== 'object') return null;
  const v = value as Record<string, unknown>;
  if (typeof v.kind !== 'string' || !Array.isArray(v.paths)) return null;
  const paths = v.paths
    .filter(Array.isArray)
    .map((path) =>
      (path as unknown[]).flatMap((n) =>
        n && typeof n === 'object' && typeof (n as { nodeId?: unknown }).nodeId === 'string'
          ? [(n as { nodeId: string }).nodeId]
          : []
      )
    )
    .filter((path) => path.length > 0);
  if (paths.length === 0) return null;
  return {
    kind: v.kind,
    description: typeof v.description === 'string' ? v.description : v.kind,
    paths,
    truncated: v.truncated === true
  };
}

/**
 * The paths in the result of a tool call, if it returned paths. The result may be the JSON as text,
 * parsed, or the text contents of the tool.
 */
export function extractToolPaths(content: unknown): ToolPaths | null {
  if (typeof content === 'string') {
    try {
      return asToolPaths(JSON.parse(content));
    } catch {
      return null;
    }
  }
  if (Array.isArray(content)) {
    for (const part of content) {
      const text = (part as { text?: unknown })?.text;
      const paths = typeof text === 'string' ? extractToolPaths(text) : null;
      if (paths) return paths;
    }
    return null;
  }
  return asToolPaths(content);
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
const resolved = new Map<string, NodeRefJSON | null>();

function toRef(node: NodeJSON): NodeRefJSON {
  return {
    id: node.id,
    type: node.type,
    name: node.name,
    code: (node.code ?? '').split('\n')[0].trim(),
    fileName: node.fileName?.split('/').pop(),
    startLine: node.startLine,
    startColumn: node.startColumn,
    endLine: node.endLine,
    endColumn: node.endColumn,
    translationUnitId: node.translationUnitId,
    componentName: node.componentName,
    isInferred: false
  };
}

/**
 * Resolves node IDs to references with their location, asking the backend only for IDs that were
 * not resolved before. IDs that are no nodes are missing from the result.
 */
export async function resolveNodes(ids: string[]): Promise<Map<string, NodeRefJSON>> {
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
  const result = new Map<string, NodeRefJSON>();
  for (const id of ids) {
    const ref = resolved.get(id);
    if (ref) result.set(id, ref);
  }
  return result;
}

/** A short label for a cited node, e.g. `encrypt · main.c:13`. */
export function citationLabel(ref: NodeRefJSON): string {
  const name = ref.name || (ref.code.length <= 24 ? ref.code : `${ref.code.slice(0, 23)}…`);
  return ref.startLine >= 1 ? `${name} · ${ref.fileName ?? ''}:${ref.startLine}` : name;
}

// The trust issues of each set of evidence, by its sorted IDs
const trustCache = new Map<string, Promise<TrustIssueJSON[]>>();

/**
 * The places where the analysis is uncertain that the given evidence relies on, e.g. unresolved
 * calls in the functions of the nodes. Cached by the set of IDs.
 */
export function checkTrust(ids: string[]): Promise<TrustIssueJSON[]> {
  const key = [...new Set(ids)].sort().join(',');
  if (!key) return Promise.resolve([]);
  let issues = trustCache.get(key);
  if (!issues) {
    issues = fetch('/api/trust', {
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
        trustCache.delete(key);
        return [];
      });
    trustCache.set(key, issues);
  }
  return issues;
}
