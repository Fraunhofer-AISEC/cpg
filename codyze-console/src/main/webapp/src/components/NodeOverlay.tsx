import { NodeJSON } from "@/types";
import { getColorForNodeType } from "../lib/colors";
import { FlattenedNode } from "../lib/flatten";

interface NodeOverlayProps {
  node: FlattenedNode;
  codeLines: string[];
  highlightedNode: NodeJSON | null;
  setHighlightedNode: (node: NodeJSON | null) => void;
  lineHeight: number;
  charWidth: number;
  offsetTop: number;
  offsetLeft: number;
}

function NodeOverlay({
  node,
  codeLines,
  highlightedNode,
  setHighlightedNode,
  lineHeight,
  charWidth,
  offsetTop,
  offsetLeft,
}: NodeOverlayProps) {
  const calculateWidth = (node: NodeJSON, codeLines: string[]): number => {
    if (node.startLine === node.endLine) {
      return (node.endColumn - node.startColumn) * charWidth;
    } else {
      return (
        codeLines
          .slice(node.startLine - 1, node.endLine - 1)
          .reduce((maxWidth, line) => Math.max(maxWidth, line.length), 0) *
        charWidth
      );
    }
  };

  return (
    <div
      key={node.id}
      className="absolute cursor-pointer transition-all duration-200"
      style={{
        top: `${(node.startLine - 1) * lineHeight + offsetTop}px`,
        left: `${node.startColumn * charWidth + offsetLeft}px`,
        height: `${(node.endLine - node.startLine + 1) * lineHeight}px`,
        width: `${calculateWidth(node, codeLines)}px`,
        backgroundColor: getColorForNodeType(node.type),
        border:
          highlightedNode?.id === node.id
            ? "2px solid black"
            : "1px solid transparent",
        opacity: highlightedNode?.id === node.id ? 0.6 : 0.3,
        minWidth: "4px",
        minHeight: "4px",
        zIndex: node.depth,
      }}
      onMouseEnter={() => setHighlightedNode(node)}
      onMouseLeave={() => setHighlightedNode(null)}
    />
  );
}

export default NodeOverlay;
