import type { NodeRefJSON } from '$lib/types';

/**
 * Slices of the program dependence graph (PDG) of a function: the statements that affect a
 * statement, or that it affects, with their data and control dependences.
 */

export type PdgDirection = 'BACKWARD' | 'FORWARD';
export type PdgNodeKind = 'STATEMENT' | 'BRANCH' | 'STUB';

export interface PdgNode {
  /** The ID of the statement, which is also its node ID */
  id: string;
  kind: PdgNodeKind;
  node: NodeRefJSON;
  startLine: number;
  endLine: number;
  code: string;
  /** The hops from the root of the slice */
  depth: number;
  /** The statements further away that are not part of the slice, to offer expanding it */
  more: number;
  concept: boolean;
  unresolved: boolean;
  /** A call to code that is not analysed; for a stub: the code is not part of the analysis */
  external: boolean;
  warning: boolean;
}

export interface PdgEdge {
  from: string;
  to: string;
  kind: 'DATA' | 'CONTROL';
  /** The variable of a data dependence, or `true`/`false` for a control dependence */
  label: string | null;
}

export interface PdgSlice {
  root: string;
  direction: PdgDirection;
  hops: number;
  function: NodeRefJSON | null;
  nodes: PdgNode[];
  edges: PdgEdge[];
  truncated: boolean;
}

export interface PdgCounts {
  backward: number;
  forward: number;
}

// Slices do not change during a session, except for added concepts, so requests are shared
const sliceCache = new Map<string, Promise<PdgSlice | null>>();
const countsCache = new Map<string, Promise<PdgCounts | null>>();

async function fetchJson<T>(url: string): Promise<T | null> {
  const res = await fetch(url);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Failed to load the dependence graph: ${res.statusText}`);
  return res.json();
}

/** The slice around the statement of a node, limited to its function. */
export function getPdgSlice(
  nodeId: string,
  direction: PdgDirection,
  hops: number
): Promise<PdgSlice | null> {
  const key = `${nodeId}/${direction}/${hops}`;
  let request = sliceCache.get(key);
  if (!request) {
    request = fetchJson<PdgSlice>(
      `/api/node/${nodeId}/pdg?direction=${direction.toLowerCase()}&hops=${hops}`
    );
    request.catch(() => sliceCache.delete(key));
    sliceCache.set(key, request);
  }
  return request;
}

/** How many statements are in the slice of a node in each direction. */
export function getPdgCounts(nodeId: string, hops: number): Promise<PdgCounts | null> {
  const key = `${nodeId}/${hops}`;
  let request = countsCache.get(key);
  if (!request) {
    request = fetchJson<PdgCounts>(`/api/node/${nodeId}/pdg-counts?hops=${hops}`);
    request.catch(() => countsCache.delete(key));
    countsCache.set(key, request);
  }
  return request;
}

export function clearPdgCache() {
  sliceCache.clear();
  countsCache.clear();
}

/**
 * Adds the slice of one hop around a statement to the slice, e.g. to expand it at the border. The
 * statement has no further statements then, the new ones carry their own.
 */
export function mergeSlice(slice: PdgSlice, extra: PdgSlice, at: string): PdgSlice {
  const known = new Set(slice.nodes.map((n) => n.id));
  const depth = slice.nodes.find((n) => n.id === at)?.depth ?? 0;
  const key = (e: PdgEdge) => `${e.from}>${e.to}:${e.kind}:${e.label}`;
  const edges = new Set(slice.edges.map(key));
  return {
    ...slice,
    nodes: [
      ...slice.nodes.map((n) => (n.id === at ? { ...n, more: 0 } : n)),
      ...extra.nodes.filter((n) => !known.has(n.id)).map((n) => ({ ...n, depth: depth + 1 }))
    ],
    edges: [...slice.edges, ...extra.edges.filter((e) => !edges.has(key(e)))]
  };
}

const branchNames: Record<string, string> = {
  IfElse: 'if',
  While: 'while',
  DoWhile: 'do while',
  For: 'for',
  ForEach: 'for each',
  Switch: 'switch',
  CatchClause: 'catch'
};

/** The keyword of a branching statement, e.g. `if`. */
export function branchName(node: PdgNode): string {
  return branchNames[node.node.type] ?? 'branch';
}

/** The place of a statement, e.g. `main.c:16`. */
export function locationOf(node: PdgNode): string {
  if (node.node.startLine < 1) return 'outside the analysed code';
  return `${node.node.fileName ?? ''}:${node.startLine}`;
}

/**
 * The dependences of a statement in words, e.g. `Data ← lines 10, 12 (buf) · line 14 (len)`.
 * Lines of the function are given by their number, others by their file.
 */
export function describeDependences(slice: PdgSlice, id: string): string {
  const byId = new Map(slice.nodes.map((n) => [n.id, n]));
  const where = (n: PdgNode) =>
    n.kind === 'STUB' ? locationOf(n) : n.startLine > 0 ? `${n.startLine}` : '?';

  // Groups the statements by what the dependence is on, e.g. the variable
  const group = (edges: PdgEdge[], end: 'from' | 'to') => {
    const groups = new Map<string, string[]>();
    for (const edge of edges) {
      const node = byId.get(edge[end]);
      if (!node) continue;
      const label = edge.label ?? '';
      const lines = groups.get(label) ?? [];
      if (!lines.includes(where(node))) lines.push(where(node));
      groups.set(label, lines);
    }
    return [...groups].map(([label, lines]) => {
      const numbers = lines.every((l) => /^\d+$/.test(l));
      const text = `${numbers ? (lines.length === 1 ? 'line' : 'lines') : ''} ${lines.join(', ')}`;
      return `${text.trim()}${label ? ` (${label})` : ''}`;
    });
  };

  const parts: string[] = [];
  const kinds: [PdgEdge['kind'], string][] = [
    ['DATA', 'Data'],
    ['CONTROL', 'Control']
  ];
  for (const [kind, name] of kinds) {
    const incoming = group(
      slice.edges.filter((e) => e.kind === kind && e.to === id),
      'from'
    );
    const outgoing = group(
      slice.edges.filter((e) => e.kind === kind && e.from === id),
      'to'
    );
    if (incoming.length) parts.push(`${name} ← ${incoming.join(' · ')}`);
    if (outgoing.length) parts.push(`${name} → ${outgoing.join(' · ')}`);
  }
  return parts.join(' · ') || 'No dependences in this slice';
}
