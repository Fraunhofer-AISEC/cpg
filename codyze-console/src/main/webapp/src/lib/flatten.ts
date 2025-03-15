import type { NodeJSON } from '$lib/types';

/**
 * A node in a tree of nodes, enriched with its depth in the tree.
 *
 * @property depth The depth of the node in the tree.
 */
export interface FlattenedNode extends NodeJSON {
  depth: number;
}

/**
 * Flattens a tree of nodes into a list of nodes with their depth in the tree.
 * @param nodes The root nodes of the tree.
 * @returns The flattened list of nodes.
 */
export const flattenNodes = (nodes: NodeJSON[]): FlattenedNode[] => {
  const result: FlattenedNode[] = [];
  const stack: { node: NodeJSON; depth: number }[] = nodes.map((node) => ({ node, depth: 0 }));

  while (stack.length) {
    const { node, depth } = stack.pop()!;
    result.push({ ...node, depth });
    if (node.astChildren) {
      stack.push(...node.astChildren.map((child) => ({ node: child, depth: depth + 1 })));
    }
  }

  return result;
};
