import { NodeJSON } from "@/types";

interface NodeTooltipProps {
  node: NodeJSON;
  lineHeight: number;
  charWidth: number;
  offsetTop: number;
  offsetLeft: number;
}

/**
 * Component to display a tooltip for a node.
 * 
 * @param node Node to display the tooltip for
 * @param lineHeight Height of a line in pixels
 * @param charWidth Width of a character in pixels
 * @param offsetTop Offset from the top in pixels
 * @param offsetLeft Offset from the left in pixels
 */
function NodeTooltip({ node, lineHeight, charWidth, offsetTop, offsetLeft }: NodeTooltipProps) {
  return (
    <div
      className="absolute z-30 rounded border border-gray-300 bg-white p-2 text-xs shadow-md"
      style={{
        top: `${node.endLine * lineHeight + offsetTop}px`,
        left: `${node.startColumn * charWidth + offsetLeft}px`,
        maxWidth: "300px",
      }}
    >
      <p className="font-bold">{node.type}</p>
      <p>
        <strong>Name:</strong> {node.name}
      </p>
      <p>
        <strong>Location:</strong> L{node.startLine}:C{node.startColumn} - L
        {node.endLine}:C{node.endColumn}
      </p>
      <p className="mt-1 truncate">
        <strong>Code:</strong> {node.code}
      </p>
    </div>
  );
}

export default NodeTooltip;
