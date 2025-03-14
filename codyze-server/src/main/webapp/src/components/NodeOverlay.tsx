import { useEffect } from 'react';
import { NodeInfo } from '../types';

interface NodeOverlayProps {
    nodes: NodeInfo[];
    highlightedNode: NodeInfo | null;
    setHighlightedNode: (node: NodeInfo | null) => void;
    lineHeight: number;
    charWidth: number;
    offsetTop: number;
    offsetLeft: number;
    highlightLine: number | null;
    findingText?: string;
    kind?: string
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
                         kind = 'info'
                     }: NodeOverlayProps) {
    const getFindingStyle = () => {
        switch (kind.toLowerCase()) {
            case 'fail':
            case 'error':
                return {
                    backgroundColor: 'rgba(254, 226, 226, 1)', // bg-red-100
                    borderColor: 'rgba(239, 68, 68, 1)',      // border-red-500
                    color: 'rgba(185, 28, 28, 1)'             // text-red-700
                };
            case 'pass':
            case 'success':
                return {
                    backgroundColor: 'rgba(220, 252, 231, 1)', // bg-green-100
                    borderColor: 'rgba(34, 197, 94, 1)',       // border-green-500
                    color: 'rgba(21, 128, 61, 1)'              // text-green-700
                };
            default:
                return {
                    backgroundColor: 'rgba(243, 244, 246, 1)', // bg-gray-100
                    borderColor: 'rgba(107, 114, 128, 1)',     // border-gray-500
                    color: 'rgba(55, 65, 81, 1)'               // text-gray-700
                };
        }
    };

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

    // Effect to scroll to highlighted line when the component mounts or highlightLine changes
    useEffect(() => {
        if (highlightLine !== null) {
            const yPosition = (highlightLine - 1) * lineHeight + offsetTop;

            // Scroll to position the line in the middle of the viewport
            window.scrollTo({
                top: yPosition - window.innerHeight / 3,
                behavior: 'smooth'
            });
        }
    }, [highlightLine, lineHeight, offsetTop]);

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
            {highlightLine !== null && (
                <>
                    <div
                        className="absolute w-full"
                        style={{
                            top: `${(highlightLine - 1) * lineHeight + offsetTop}px`,
                            height: `${lineHeight}px`,
                            backgroundColor: 'rgba(255, 255, 0, 0.3)',
                            zIndex: 5
                        }}
                    />
                    {findingText && (
                        <div
                            className="absolute left-0 right-0 px-4 py-2 border-l-4"
                            style={{
                                top: `${highlightLine * lineHeight + offsetTop}px`,
                                zIndex: 30,
                                maxWidth: '100%',
                                overflowWrap: 'break-word',
                                ...getFindingStyle()
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