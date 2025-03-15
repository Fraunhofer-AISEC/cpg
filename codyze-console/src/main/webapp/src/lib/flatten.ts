import { NodeJSON } from "@/types";

export interface FlattenedNode extends NodeJSON {
  depth: number;
}

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