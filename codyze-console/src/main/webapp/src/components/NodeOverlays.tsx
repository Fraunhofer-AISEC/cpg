import { NodeJSON } from "@/types";
import { useEffect } from "react";
import { getFindingStyle } from "../lib/colors";
import { FlattenedNode } from "../lib/flatten";
import NodeOverlay from "./NodeOverlay";

interface NodeOverlayProps {
  nodes: FlattenedNode[];
  codeLines: string[];
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

function NodeOverlays({
  nodes,
  codeLines,
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
        <NodeOverlay
          key={node.id}
          node={node}
          codeLines={codeLines}
          highlightedNode={highlightedNode}
          setHighlightedNode={setHighlightedNode}
          lineHeight={lineHeight}
          charWidth={charWidth}
          offsetTop={offsetTop}
          offsetLeft={offsetLeft}
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

export default NodeOverlays;
