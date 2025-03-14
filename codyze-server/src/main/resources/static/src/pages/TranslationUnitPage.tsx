<<<<<<< HEAD
import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { TranslationUnit, NodeInfo } from '../types';
import { getTranslationUnit, getNodesForTranslationUnit } from '../services/api';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { docco } from 'react-syntax-highlighter/dist/esm/styles/hljs';

// Helper function to get color based on node type
const getColorForNodeType = (type: string): string => {
    const colorMap: Record<string, string> = {
        FunctionDeclaration: 'rgba(255, 99, 132, 0.3)',  // Red
        VariableDeclaration: 'rgba(54, 162, 235, 0.3)',  // Blue
        RecordDeclaration: 'rgba(255, 206, 86, 0.3)',    // Yellow
        Statement: 'rgba(75, 192, 192, 0.3)',            // Green
        Expression: 'rgba(153, 102, 255, 0.3)',          // Purple
        Literal: 'rgba(255, 159, 64, 0.3)',              // Orange
    };

    // Return a color from the map or a default color
    return colorMap[type] || 'rgba(128, 128, 128, 0.3)';
};

const TranslationUnitPage: React.FC = () => {
=======
import {useEffect, useState} from 'react';
import {Link, useSearchParams} from 'react-router-dom';
import {NodeInfo, TranslationUnit} from '../types';
import {getTranslationUnit} from '../services/api';
import SyntaxHighlighter from 'react-syntax-highlighter';
import {docco} from 'react-syntax-highlighter/dist/esm/styles/hljs';
import NodeOverlay from '../components/NodeOverlay';
import NodeTooltip from '../components/NodeTooltip';
import NodeTable from '../components/NodeTable';
import ConceptOperationNodeTable from '../components/ConceptOperationNodeTable';

function TranslationUnitPage() {
>>>>>>> origin/webconsole
    const [searchParams] = useSearchParams();
    const componentName = searchParams.get('component');
    const path = searchParams.get('path');

    const [translationUnit, setTranslationUnit] = useState<TranslationUnit | null>(null);
<<<<<<< HEAD
    const [nodes, setNodes] = useState<NodeInfo[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [highlightedNode, setHighlightedNode] = useState<NodeInfo | null>(null);

    // Constants for rendering node overlays
    const lineHeight = 24; // Adjust based on your actual line height
    const charWidth = 10;   // Adjust based on your actual character width
    const offsetLeft = 33;
    const offsetTop = 8;
=======
    const [astNodes, setAstNodes] = useState<NodeInfo[]>([]);
    const [overlayNodes, setOverlayNodes] = useState<NodeInfo[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [highlightedNode, setHighlightedNode] = useState<NodeInfo | null>(null);
    const [activeTab, setActiveTab] = useState<'astNodes' | 'overlayNodes'>('overlayNodes');

    const lineHeight = 24;
    const charWidth = 10;
    const offsetTop = 8;
    const baseOffsetLeft = 14;
>>>>>>> origin/webconsole

    useEffect(() => {
        const fetchData = async () => {
            if (!componentName || !path) {
                setError('Missing component name or path');
                setLoading(false);
                return;
            }

            try {
                console.log(`Fetching data for component: ${componentName}, path: ${path}`);
<<<<<<< HEAD
                const [unitData, nodesData] = await Promise.all([
                    getTranslationUnit(componentName, path),
                    getNodesForTranslationUnit(componentName, path)
                ]);

                setTranslationUnit(unitData);
                setNodes(nodesData);
                console.log(`Fetched ${nodesData.length} nodes`);
            } catch (err) {
                setError('Failed to load translation unit data');
=======
                const data = await getTranslationUnit(componentName, path);
                setTranslationUnit(data);
                setAstNodes(data.astNodes);
                setOverlayNodes(data.overlayNodes);
                console.log(`Fetched ${data.length} nodes`);
            } catch (err) {
                setError('Failed to load nodes');
>>>>>>> origin/webconsole
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [componentName, path]);

    if (loading) {
        return <div className="container mx-auto p-4">Loading translation unit...</div>;
    }

    if (error || !translationUnit) {
        return (
            <div className="container mx-auto p-4">
                <div className="p-3 bg-red-100 text-red-700 rounded">{error || 'Translation unit not found'}</div>
                <div className="mt-4">
                    <Link to="/" className="text-blue-600 hover:underline">Back to Dashboard</Link>
                </div>
            </div>
        );
    }

<<<<<<< HEAD
=======
    const totalLines = translationUnit.code.split('\n').length;
    const lineNumberWidth = Math.ceil(Math.log10(totalLines + 1));
    const offsetLeft = baseOffsetLeft + lineNumberWidth * charWidth;

    const nodes = activeTab === 'overlayNodes' ? overlayNodes : astNodes;

>>>>>>> origin/webconsole
    return (
        <div className="container mx-auto p-4">
            <div className="mb-4">
                {componentName && (
                    <Link to={`/component/${componentName}`} className="text-blue-600 hover:underline">
                        Back to Component
                    </Link>
                )}
            </div>

            <h1 className="text-2xl font-bold mb-6">{translationUnit.name}</h1>
            <p className="text-gray-500 mb-4">{translationUnit.path}</p>

            <div className="bg-white shadow-md rounded p-2 mb-6 relative">
                <div className="relative">
                    <SyntaxHighlighter language="python" style={docco} showLineNumbers={true}>
                        {translationUnit.code}
                    </SyntaxHighlighter>

<<<<<<< HEAD
                    {/* Improved Node overlays */}
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

                    {/* Node information tooltip */}
                    {highlightedNode && (
                        <div
                            className="absolute bg-white p-2 shadow-md rounded z-30 text-xs border border-gray-300"
                            style={{
                                top: `${(highlightedNode.endLine) * lineHeight + offsetTop}px`,
                                left: `${highlightedNode.startColumn * charWidth + offsetLeft}px`,
                                maxWidth: '300px'
                            }}
                        >
                            <p className="font-bold">{highlightedNode.type}</p>
                            <p><strong>Name:</strong> {highlightedNode.name}</p>
                            <p><strong>Location:</strong> L{highlightedNode.startLine}:C{highlightedNode.startColumn} - L{highlightedNode.endLine}:C{highlightedNode.endColumn}</p>
                            <p className="mt-1 truncate"><strong>Code:</strong> {highlightedNode.code}</p>
                        </div>
=======
                    <NodeOverlay
                        nodes={nodes}
                        highlightedNode={highlightedNode}
                        setHighlightedNode={setHighlightedNode}
                        lineHeight={lineHeight}
                        charWidth={charWidth}
                        offsetTop={offsetTop}
                        offsetLeft={offsetLeft}
                    />

                    {highlightedNode && (
                        <NodeTooltip
                            node={highlightedNode}
                            lineHeight={lineHeight}
                            charWidth={charWidth}
                            offsetTop={offsetTop}
                            offsetLeft={offsetLeft}
                        />
>>>>>>> origin/webconsole
                    )}
                </div>
            </div>

            <div className="bg-white shadow-md rounded p-6">
<<<<<<< HEAD
                <h2 className="text-lg font-semibold mb-4">AST Nodes ({nodes.length})</h2>
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
=======
                <div className="mb-4">
                    <button
                        className={`px-4 py-2 ml-2 ${activeTab === 'overlayNodes' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
                        onClick={() => setActiveTab('overlayNodes')}
                    >
                        Overlay Nodes
                    </button>
                    <button
                        className={`px-4 py-2 ${activeTab === 'astNodes' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
                        onClick={() => setActiveTab('astNodes')}
                    >
                        AST Nodes
                    </button>
                </div>

                {activeTab === 'astNodes' ? (
                    <NodeTable nodes={astNodes} highlightedNode={highlightedNode} setHighlightedNode={setHighlightedNode} />
                ) : (
                    <ConceptOperationNodeTable nodes={overlayNodes} highlightedNode={highlightedNode} setHighlightedNode={setHighlightedNode} />
                )}
>>>>>>> origin/webconsole
            </div>
        </div>
    );
};

export default TranslationUnitPage;