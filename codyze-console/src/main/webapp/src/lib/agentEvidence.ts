import type {
  NodeInfoJSON,
  NodeJSON,
  NodePathsJSON,
  NodeRefJSON,
  TrustIssueJSON
} from '$lib/types';

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

function isNodeInfo(value: unknown): value is NodeInfoJSON {
  return !!value && typeof (value as NodeInfoJSON).nodeId === 'string';
}

/** Whether a value is paths in the format of the tools that find paths. */
function isNodePaths(value: unknown): value is NodePathsJSON {
  if (!value || typeof value !== 'object') return false;
  const v = value as NodePathsJSON;
  return (
    typeof v.kind === 'string' &&
    Array.isArray(v.paths) &&
    v.paths.length > 0 &&
    v.paths.every((path) => Array.isArray(path) && path.every(isNodeInfo))
  );
}

/** The node IDs of each of the paths. */
export function pathNodeIds(paths: NodePathsJSON): string[][] {
  return paths.paths.map((path) => path.map((node) => node.nodeId));
}

/**
 * The paths in the result of a tool call, if it returned paths. The result may be the JSON as text,
 * parsed, or the text contents of the tool.
 */
export function extractToolPaths(content: unknown): NodePathsJSON | null {
  if (typeof content === 'string') {
    try {
      return extractToolPaths(JSON.parse(content));
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
  return isNodePaths(content) ? content : null;
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
