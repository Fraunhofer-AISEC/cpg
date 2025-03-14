import React from 'react';
import { NodeInfo } from '../types';

interface NodeTooltipProps {
    node: NodeInfo;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
}

function NodeTooltip({ node, lineHeight, charWidth, offsetTop, offsetLeft }) {
    return (
        <div
            className="absolute bg-white p-2 shadow-md rounded z-30 text-xs border border-gray-300"
            style={{
                top: `${(node.endLine) * lineHeight + offsetTop}px`,
                left: `${node.startColumn * charWidth + offsetLeft}px`,
                maxWidth: '300px'
            }}
        >
            <p className="font-bold">{node.type}</p>
            <p><strong>Name:</strong> {node.name}</p>
            <p><strong>Location:</strong> L{node.startLine}:C{node.startColumn} - L{node.endLine}:C{node.endColumn}</p>
            <p className="mt-1 truncate"><strong>Code:</strong> {node.code}</p>
        </div>
    );
};

export default NodeTooltip;