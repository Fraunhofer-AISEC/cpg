import { useEffect } from "react";
import { NodeJSON } from "@/types";
import { getColorForNodeType, getFindingStyle } from "../lib/colors";

interface NodeOverlayProps {
  nodes: NodeJSON[];
  highlightedNode: NodeJSON | null;
  setHighlightedNode: (node: NodeJSON | null) => void;
  lineHeight: number;
  charWidth: number;
  offsetTop: number;
  offsetLeft: number;
  highlightLine: number | null;
  findingText?: string;
  kind?: string;
}

function NodeOverlay({
  nodes,
  highlightedNode,
  setHighlightedNode,
  lineHeight,
  charWidth,
  offsetTop,
  offsetLeft,
  highlightLine,
  findingText,
  kind = "info",
}: NodeOverlayProps) {
  // Effect to scroll to highlighted line when the component mounts or highlightLine changes
  useEffect(() => {
    if (highlightLine !== null) {
      const yPosition = (highlightLine - 1) * lineHeight + offsetTop;

      // Scroll to position the line in the middle of the viewport
      window.scrollTo({
        top: yPosition - window.innerHeight / 3,
        behavior: "smooth",
      });
    }
  }, [highlightLine, lineHeight, offsetTop]);

  return (
    <div className="absolute left-0 top-0 h-full w-full">
      {nodes.map((node) => (
        <div
          key={node.id}
          className="absolute cursor-pointer transition-all duration-200"
          style={{
            top: `${(node.startLine - 1) * lineHeight + offsetTop}px`,
            left: `${node.startColumn * charWidth + offsetLeft}px`,
            height: `${(node.endLine - node.startLine + 1) * lineHeight}px`,
            width: `${(node.endColumn - node.startColumn) * charWidth}px`,
            backgroundColor: getColorForNodeType(node.type),
            border:
              highlightedNode?.id === node.id
                ? "2px solid black"
                : "1px solid transparent",
            opacity: highlightedNode?.id === node.id ? 0.6 : 0.3,
            minWidth: "4px",
            minHeight: "4px",
            zIndex: highlightedNode?.id === node.id ? 20 : 10,
          }}
          onMouseEnter={() => setHighlightedNode(node)}
          onMouseLeave={() => setHighlightedNode(null)}
        />
      ))}
      {highlightLine !== null && (
        <>
          <div
            className="absolute w-full"
            style={{
              top: `${(highlightLine - 1) * lineHeight + offsetTop}px`,
              height: `${lineHeight}px`,
              backgroundColor: "rgba(255, 255, 0, 0.3)",
              zIndex: 5,
            }}
          />
          {findingText && (
            <div
              className="absolute left-0 right-0 border-l-4 px-4 py-2"
              style={{
                top: `${highlightLine * lineHeight + offsetTop}px`,
                zIndex: 30,
                maxWidth: "100%",
                overflowWrap: "break-word",
                ...getFindingStyle(kind),
              }}
            >
              {findingText}
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default NodeOverlay;
