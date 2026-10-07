import type { NodePathsJSON, NodeRefJSON } from '$lib/types';

/**
 * Slices of the program dependence graph (PDG) of a function: the statements that affect a
 * statement, or that it affects, with their data and control dependences.
 */

export type GraphDirection = 'BACKWARD' | 'FORWARD';

/**
 * Which dependences a slice follows: both kinds (the program dependence graph), only the data
 * dependences (the dataflow) or only the control dependences.
 */
export type DependenceGraph = 'PDG' | 'DFG' | 'CDG';
export type GraphNodeKind = 'STATEMENT' | 'BRANCH' | 'STUB';

export interface GraphNode {
  /** The ID of the statement, which is also its node ID */
  id: string;
  kind: GraphNodeKind;
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

export interface GraphEdge {
  from: string;
  to: string;
  kind: 'DATA' | 'CONTROL';
  /** The variable of a data dependence, or `true`/`false` for a control dependence */
  label: string | null;
}

export interface GraphSlice {
  root: string;
  direction: GraphDirection;
  hops: number;
  function: NodeRefJSON | null;
  nodes: GraphNode[];
  edges: GraphEdge[];
  truncated: boolean;
}

export interface SliceCounts {
  backward: number;
  forward: number;
}

// Slices do not change during a session, except for added concepts, so requests are shared
const sliceCache = new Map<string, Promise<GraphSlice | null>>();
const countsCache = new Map<string, Promise<SliceCounts | null>>();

async function fetchJson<T>(url: string): Promise<T | null> {
  const res = await fetch(url);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Failed to load the dependence graph: ${res.statusText}`);
  return res.json();
}

/** The slice around the statement of a node in the given graph, limited to its function. */
export function getGraphSlice(
  nodeId: string,
  graph: DependenceGraph,
  direction: GraphDirection,
  hops: number
): Promise<GraphSlice | null> {
  const key = `${nodeId}/${graph}/${direction}/${hops}`;
  let request = sliceCache.get(key);
  if (!request) {
    request = fetchJson<GraphSlice>(
      `/api/node/${nodeId}/${graph.toLowerCase()}?direction=${direction.toLowerCase()}&hops=${hops}`
    );
    request.catch(() => sliceCache.delete(key));
    sliceCache.set(key, request);
  }
  return request;
}

/** How many statements are in the slice of a node in each direction. */
export function getSliceCounts(nodeId: string, hops: number): Promise<SliceCounts | null> {
  const key = `${nodeId}/${hops}`;
  let request = countsCache.get(key);
  if (!request) {
    request = fetchJson<SliceCounts>(`/api/node/${nodeId}/pdg-counts?hops=${hops}`);
    request.catch(() => countsCache.delete(key));
    countsCache.set(key, request);
  }
  return request;
}

export function clearGraphCache() {
  sliceCache.clear();
  countsCache.clear();
}

/**
 * Adds the slice of one hop around a statement to the slice, e.g. to expand it at the border. The
 * statement has no further statements then, the new ones carry their own.
 */
export function mergeSlice(slice: GraphSlice, extra: GraphSlice, at: string): GraphSlice {
  const known = new Set(slice.nodes.map((n) => n.id));
  const depth = slice.nodes.find((n) => n.id === at)?.depth ?? 0;
  const key = (e: GraphEdge) => `${e.from}>${e.to}:${e.kind}:${e.label}`;
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
export function branchName(node: GraphNode): string {
  return branchNames[node.node.type] ?? 'branch';
}

/** The place of a statement, e.g. `main.c:16`. */
export function locationOf(node: GraphNode): string {
  if (node.node.startLine < 1) return 'outside the analysed code';
  return `${node.node.fileName ?? ''}:${node.startLine}`;
}

/**
 * The dependences of a statement in words, e.g. `Data ← lines 10, 12 (buf) · line 14 (len)`.
 * Lines of the function are given by their number, others by their file.
 */
export function describeDependences(slice: GraphSlice, id: string): string {
  const byId = new Map(slice.nodes.map((n) => [n.id, n]));
  const where = (n: GraphNode) =>
    n.kind === 'STUB' ? locationOf(n) : n.startLine > 0 ? `${n.startLine}` : '?';

  // Groups the statements by what the dependence is on, e.g. the variable
  const group = (edges: GraphEdge[], end: 'from' | 'to') => {
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
  const kinds: [GraphEdge['kind'], string][] = [
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

/**
 * The statements of the paths a tool found and the steps between them, in the format of a slice.
 * [paths] is the result of the tool as it returned it.
 */
export async function getPathsGraph(paths: NodePathsJSON): Promise<GraphSlice> {
  const res = await fetch('/api/graph/from-paths', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(paths)
  });
  if (!res.ok) throw new Error(`Failed to load the paths: ${res.statusText}`);
  return res.json();
}
