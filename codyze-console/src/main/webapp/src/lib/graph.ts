import type { NodePathsJSON, NodeRefJSON, NodeSliceJSON } from '$lib/types';

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

/** Whether a slice follows dependences into other functions (interprocedural) or not. */
export type SliceScope = 'interprocedural' | 'intraprocedural';
/** An overlay is a concept or operation the dependences pass through, see [GraphNode.node]. */
export type GraphNodeKind = 'STATEMENT' | 'BRANCH' | 'STUB' | 'OVERLAY';

export interface GraphNode {
  /** The ID of the statement, which is also its node ID */
  id: string;
  kind: GraphNodeKind;
  /** For an overlay: the concept or operation, labelled `Concept` or `Operation` */
  node: NodeRefJSON;
  startLine: number;
  endLine: number;
  code: string;
  /** The hops from the root of the slice */
  depth: number;
  /** The statements further away that are not part of the slice, to offer expanding it */
  more: number;
  /** The concepts and operations attached to the nodes of the statement */
  overlays: NodeRefJSON[];
  unresolved: boolean;
  /** A call to code that is not analysed; for a stub: the code is not part of the analysis */
  external: boolean;
  warning: boolean;
}

export interface GraphEdge {
  from: string;
  to: string;
  /** An overlay edge links an overlay to the statement of the node it is attached to */
  kind: 'DATA' | 'CONTROL' | 'OVERLAY';
  /** The variable of a data dependence, or `true`/`false` for a control dependence */
  label: string | null;
  /** The node of the `from` statement the dependence starts at, if it is not the statement */
  fromPart?: NodeRefJSON | null;
  /** The node of the `to` statement the dependence ends at, if it is not the statement */
  toPart?: NodeRefJSON | null;
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

/** The slice around the statement of a node in the given graph. */
export function getGraphSlice(
  nodeId: string,
  graph: DependenceGraph,
  direction: GraphDirection,
  hops: number,
  scope: SliceScope = 'interprocedural'
): Promise<GraphSlice | null> {
  const key = `${nodeId}/${graph}/${direction}/${hops}/${scope}`;
  let request = sliceCache.get(key);
  if (!request) {
    request = fetchJson<GraphSlice>(
      `/api/node/${nodeId}/${graph.toLowerCase()}?direction=${direction.toLowerCase()}&hops=${hops}&scope=${scope}`
    );
    request.catch(() => sliceCache.delete(key));
    sliceCache.set(key, request);
  }
  return request;
}

/** How many statements are in the slice of a node in each direction. */
export function getSliceCounts(
  nodeId: string,
  hops: number,
  scope: SliceScope = 'interprocedural'
): Promise<SliceCounts | null> {
  const key = `${nodeId}/${hops}/${scope}`;
  let request = countsCache.get(key);
  if (!request) {
    request = fetchJson<SliceCounts>(`/api/node/${nodeId}/pdg-counts?hops=${hops}&scope=${scope}`);
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

/** A dependence of a statement, grouped by what it is on, e.g. the variable `buf`. */
export interface DependenceGroup {
  kind: 'DATA' | 'CONTROL';
  /** The variable of data dependences, or `true`/`false` of control dependences */
  label: string | null;
  /** The statements at the other end, with where they are, e.g. `L12` or `crypto.c:3` */
  statements: { node: GraphNode; place: string }[];
}

/**
 * The dependences of a statement in the slice: where it comes from (`from`) and what depends on it
 * (`to`), each grouped by what it is on. Lines of the file of the root are given by their number,
 * others with their file.
 */
export function dependencesOf(
  slice: GraphSlice,
  id: string
): { from: DependenceGroup[]; to: DependenceGroup[] } {
  const byId = new Map(slice.nodes.map((n) => [n.id, n]));
  const rootFile = byId.get(slice.root)?.node.fileName;
  const place = (n: GraphNode) =>
    n.kind === 'STUB' || n.node.fileName !== rootFile
      ? locationOf(n)
      : n.startLine > 0
        ? `L${n.startLine}`
        : '?';

  const group = (edges: GraphEdge[], end: 'from' | 'to') => {
    const groups = new Map<string, DependenceGroup>();
    for (const edge of edges) {
      if (edge.kind === 'OVERLAY') continue;
      const node = byId.get(edge[end]);
      if (!node) continue;
      const key = `${edge.kind}:${edge.label ?? ''}`;
      const entry = groups.get(key) ?? { kind: edge.kind, label: edge.label, statements: [] };
      if (!entry.statements.some((s) => s.node.id === node.id)) {
        entry.statements.push({ node, place: place(node) });
      }
      groups.set(key, entry);
    }
    // Data before control
    return [...groups.values()].sort((a, b) =>
      a.kind === b.kind ? 0 : a.kind === 'DATA' ? -1 : 1
    );
  };

  return {
    from: group(
      slice.edges.filter((e) => e.to === id),
      'from'
    ),
    to: group(
      slice.edges.filter((e) => e.from === id),
      'to'
    )
  };
}

/**
 * The evidence of a question to the agent as one graph: the paths and slices of its steps, which
 * steps found each statement, and the statements of the nodes its answer cites.
 */
export interface EvidenceGraph {
  graph: GraphSlice;
  /** The steps that found each statement, by the ID of the statement */
  steps: Record<string, number[]>;
  /** The statements of the nodes the answer cites */
  cited: string[];
  /** The statement of each node the answer cites, by the ID of the node */
  statementOf: Record<string, string>;
}

/**
 * The statements of a graph the agent's answer leads along: the cited statements and those between
 * them, i.e. that a cited statement leads to and that lead to a cited statement. `null` if fewer
 * than two cited statements are part of the graph.
 */
export function evidencePath(graph: GraphSlice, cited: Set<string>): Set<string> | null {
  const start = [...cited].filter((id) => graph.nodes.some((n) => n.id === id));
  if (start.length < 2) return null;
  const dependences = graph.edges.filter((e) => e.kind !== 'OVERLAY');
  const reach = (forward: boolean) => {
    const reached = new Set(start);
    const queue = [...start];
    while (queue.length > 0) {
      const id = queue.pop()!;
      for (const e of dependences) {
        const [from, to] = forward ? [e.from, e.to] : [e.to, e.from];
        if (from === id && !reached.has(to)) {
          reached.add(to);
          queue.push(to);
        }
      }
    }
    return reached;
  };
  const after = reach(true);
  const before = reach(false);
  return new Set([...after].filter((id) => before.has(id)));
}

/**
 * The part of a graph on the path of the agent's answer (see [evidencePath]), with the overlays of
 * its statements.
 */
export function onPath(graph: GraphSlice, path: Set<string>): GraphSlice {
  const keep = new Set(path);
  for (const e of graph.edges) {
    if (e.kind === 'OVERLAY' && keep.has(e.from)) keep.add(e.to);
  }
  return {
    ...graph,
    root: keep.has(graph.root) ? graph.root : [...path][0],
    nodes: graph.nodes.filter((n) => keep.has(n.id)),
    edges: graph.edges.filter((e) => keep.has(e.from) && keep.has(e.to))
  };
}

/** A step of the agent that found paths or a slice, with the result as the tool returned it. */
export interface EvidenceStep {
  step: number;
  paths?: NodePathsJSON;
  slice?: NodeSliceJSON;
}

/** The evidence graph of the steps of a question and the nodes cited in its answer. */
export async function getEvidenceGraph(
  steps: EvidenceStep[],
  cited: string[]
): Promise<EvidenceGraph> {
  const res = await fetch('/api/graph/evidence', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ steps, cited })
  });
  if (!res.ok) throw new Error(`Failed to load the evidence: ${res.statusText}`);
  return res.json();
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
