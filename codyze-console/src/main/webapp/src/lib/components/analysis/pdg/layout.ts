import type { ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk-api';
import type { PdgEdge, PdgSlice } from '$lib/pdg';

/** The size of the card of a statement, which the layout reserves space for. */
export const cardWidth = 168;
export const cardHeight = 56;

export interface Point {
  x: number;
  y: number;
}

export interface EdgeLayout {
  /** The route of the edge, from the start to the end */
  points: Point[];
  /** The center of the label, if the edge has one */
  label: Point | null;
}

export interface SliceLayout {
  positions: Map<string, Point>;
  edges: Map<string, EdgeLayout>;
}

/** Identifies an edge of a slice. */
export function edgeKey(e: PdgEdge): string {
  return `${e.from}>${e.to}:${e.kind}:${e.label ?? ''}`;
}

/** The size of the label of an edge, which the layout keeps clear. */
export function labelSize(label: string): { width: number; height: number } {
  return { width: label.length * 6.6 + 10, height: 16 };
}

/**
 * Lays out the statements of a slice in layers, so that what is depended on is above what depends
 * on it: the causes of a backward slice end up above its root. Edges are routed orthogonally
 * around the cards, with their labels on them. ELK is only loaded when the first slice is shown.
 */
export async function layoutSlice(slice: PdgSlice): Promise<SliceLayout> {
  const { default: ELK } = await import('elkjs/lib/elk.bundled.js');
  const elk = new ELK();
  const keyed = slice.edges.map((e, i) => ({ edge: e, id: `e${i}` }));
  const graph: ElkNode = await elk.layout({
    id: 'root',
    layoutOptions: {
      'elk.algorithm': 'layered',
      'elk.direction': 'DOWN',
      'elk.edgeRouting': 'ORTHOGONAL',
      'elk.spacing.nodeNode': '36',
      'elk.layered.spacing.nodeNodeBetweenLayers': '64',
      'elk.layered.spacing.edgeNodeBetweenLayers': '20',
      'elk.spacing.edgeEdge': '14',
      'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
      'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
      'elk.edgeLabels.inline': 'true',
      'elk.padding': '[top=24,left=24,bottom=24,right=24]'
    },
    children: slice.nodes.map((n) => ({ id: n.id, width: cardWidth, height: cardHeight })),
    edges: keyed.map(({ edge, id }) => ({
      id,
      sources: [edge.from],
      targets: [edge.to],
      labels: edge.label ? [{ text: edge.label, ...labelSize(edge.label) }] : []
    }))
  });

  const positions = new Map<string, Point>();
  for (const child of graph.children ?? []) positions.set(child.id, { x: child.x!, y: child.y! });

  const edges = new Map<string, EdgeLayout>();
  for (const elkEdge of (graph.edges ?? []) as ElkExtendedEdge[]) {
    const original = keyed.find((k) => k.id === elkEdge.id)?.edge;
    const section = elkEdge.sections?.[0];
    if (!original || !section) continue;
    const label = elkEdge.labels?.[0];
    edges.set(edgeKey(original), {
      points: [section.startPoint, ...(section.bendPoints ?? []), section.endPoint],
      label: label ? { x: label.x! + label.width! / 2, y: label.y! + label.height! / 2 } : null
    });
  }
  return { positions, edges };
}

/** An SVG path through the points with rounded corners. */
export function roundedPath(points: Point[], radius = 8): string {
  if (points.length === 0) return '';
  let path = `M ${points[0].x} ${points[0].y}`;
  for (let i = 1; i < points.length - 1; i++) {
    const [a, b, c] = [points[i - 1], points[i], points[i + 1]];
    const before = Math.hypot(b.x - a.x, b.y - a.y);
    const after = Math.hypot(c.x - b.x, c.y - b.y);
    const r = Math.min(radius, before / 2, after / 2);
    if (r <= 0) {
      path += ` L ${b.x} ${b.y}`;
      continue;
    }
    const start = { x: b.x + ((a.x - b.x) / before) * r, y: b.y + ((a.y - b.y) / before) * r };
    const end = { x: b.x + ((c.x - b.x) / after) * r, y: b.y + ((c.y - b.y) / after) * r };
    path += ` L ${start.x} ${start.y} Q ${b.x} ${b.y} ${end.x} ${end.y}`;
  }
  const last = points[points.length - 1];
  return `${path} L ${last.x} ${last.y}`;
}

/** The color of an edge: the selected statement's edges are blue, data is darker than control. */
export function edgeColor(kind: 'DATA' | 'CONTROL', active: boolean, marked: boolean): string {
  if (active) return '#3b82f6';
  if (marked) return '#0f172a';
  return kind === 'DATA' ? '#475569' : '#94a3b8';
}
