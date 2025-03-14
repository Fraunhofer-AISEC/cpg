import React from 'react';
import {NodeInfo} from '../types';

interface ConceptOperationNodeTableProps {
    nodes: NodeInfo[];
    highlightedNode: NodeInfo | null;
    setHighlightedNode: (node: NodeInfo | null) => void;
}

function ConceptOperationNodeTable({ nodes, highlightedNode, setHighlightedNode }: ConceptOperationNodeTableProps) {
    return (
        <div className="bg-white shadow-md rounded p-6 mt-6">
            <h2 className="text-lg font-semibold mb-4">Concept & Operation Nodes ({nodes.length})</h2>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Location</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Code</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {nodes.map((node) => (
                            <tr
                                key={node.id}
                                className={`${highlightedNode?.id === node.id ? 'bg-gray-100' : ''} hover:bg-gray-50 cursor-pointer`}
                                onMouseEnter={() => setHighlightedNode(node)}
                                onMouseLeave={() => setHighlightedNode(null)}
                            >
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{node.type}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{node.name}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    L{node.startLine}:C{node.startColumn} - L{node.endLine}:C{node.endColumn}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 truncate max-w-xs">{node.code}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export default ConceptOperationNodeTable;