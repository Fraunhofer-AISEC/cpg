import { NodeJSON } from "@/types";

interface NodeTableProps {
  title: string;
  nodes: NodeJSON[];
  highlightedNode: NodeJSON | null;
  setHighlightedNode: (node: NodeJSON | null) => void;
}

function NodeTable({
  title,
  nodes,
  highlightedNode,
  setHighlightedNode,
}: NodeTableProps) {
  return (
    <div className="rounded bg-white p-6 shadow-md">
      <h2 className="mb-4 text-lg font-semibold">
        {title} ({nodes.length})
      </h2>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Name
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Location
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                Code
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {nodes.map((node) => (
              <tr
                key={node.id}
                className={`${highlightedNode?.id === node.id ? "bg-gray-100" : ""} cursor-pointer hover:bg-gray-50`}
                onMouseEnter={() => setHighlightedNode(node)}
                onMouseLeave={() => setHighlightedNode(null)}
              >
                <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-gray-900">
                  {node.type}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                  {node.name}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                  L{node.startLine}:C{node.startColumn} - L{node.endLine}:C
                  {node.endColumn}
                </td>
                <td className="max-w-xs truncate whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                  {node.code}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default NodeTable;
