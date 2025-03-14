import React from 'react';
import { NodeInfo } from '../types';

interface NodeOverlayProps {
    nodes: NodeInfo[];
    highlightedNode: NodeInfo | null;
    setHighlightedNode: (node: NodeInfo | null) => void;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
}

function NodeOverlay({ nodes, highlightedNode, setHighlightedNode, lineHeight, charWidth, offsetTop, offsetLeft }) {
    const getColorForNodeType = (type: string): string => {
        const colorMap: Record<string, string> = {
            FunctionDeclaration: 'rgba(255, 99, 132, 0.3)',  // Red
            VariableDeclaration: 'rgba(54, 162, 235, 0.3)',  // Blue
            RecordDeclaration: 'rgba(255, 206, 86, 0.3)',    // Yellow
            Statement: 'rgba(75, 192, 192, 0.3)',            // Green
            Expression: 'rgba(153, 102, 255, 0.3)',          // Purple
            Literal: 'rgba(255, 159, 64, 0.3)',              // Orange
            SetFileFlags: 'rgba(54, 162, 235, 0.3)',         // Dark Blue for file operations
            SetFileMask: 'rgba(54, 162, 235, 0.3)',          // Dark Blue for file operations
            CloseFile: 'rgba(54, 162, 235, 0.3)',            // Dark Blue for file operations
            DeleteFile: 'rgba(54, 162, 235, 0.3)',           // Dark Blue for file operations
            OpenFile: 'rgba(54, 162, 235, 0.3)',             // Dark Blue for file operations
            ReadFile: 'rgba(54, 162, 235, 0.3)',             // Dark Blue for file operations
            WriteFile: 'rgba(54, 162, 235, 0.3)',            // Dark Blue for file operations
            Configuration: 'rgba(0, 0, 255, 0.3)',           // Bright Blue for configuration
            ConfigurationGroup: 'rgba(0, 0, 255, 0.3)',      // Bright Blue for configuration
            ConfigurationOption: 'rgba(0, 0, 255, 0.3)',     // Bright Blue for configuration
            LoadConfiguration: 'rgba(0, 0, 255, 0.3)',       // Bright Blue for configuration
            ReadConfigurationGroup: 'rgba(0, 0, 255, 0.3)',  // Bright Blue for configuration
            ReadConfigurationOption: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
            RegisterConfigurationGroup: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
            RegisterConfigurationOption: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
            ProvideConfiguration: 'rgba(0, 0, 255, 0.3)',    // Bright Blue for configuration
            ProvideConfigurationGroup: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
            ProvideConfigurationOption: 'rgba(0, 0, 255, 0.3)', // Bright Blue for configuration
        };

        return colorMap[type] || 'rgba(128, 128, 128, 0.3)';
    };

    return (
        <div className="absolute top-0 left-0 w-full h-full">
            {nodes.map((node) => (
                <div
                    key={node.id}
                    className="absolute transition-all duration-200 cursor-pointer"
                    style={{
                        top: `${(node.startLine - 1) * lineHeight + offsetTop}px`,
                        left: `${node.startColumn * charWidth + offsetLeft}px`,
                        height: `${(node.endLine - node.startLine + 1) * lineHeight}px`,
                        width: `${(node.endColumn - node.startColumn) * charWidth}px`,
                        backgroundColor: getColorForNodeType(node.type),
                        border: highlightedNode?.id === node.id ? '2px solid black' : '1px solid transparent',
                        opacity: highlightedNode?.id === node.id ? 0.6 : 0.3,
                        minWidth: '4px',
                        minHeight: '4px',
                        zIndex: highlightedNode?.id === node.id ? 20 : 10
                    }}
                    onMouseEnter={() => setHighlightedNode(node)}
                    onMouseLeave={() => setHighlightedNode(null)}
                />
            ))}
        </div>
    );
};

export default NodeOverlay;