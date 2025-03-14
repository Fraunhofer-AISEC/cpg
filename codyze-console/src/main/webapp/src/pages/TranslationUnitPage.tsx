import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import SyntaxHighlighter from "react-syntax-highlighter";
import { docco } from "react-syntax-highlighter/dist/esm/styles/hljs";
import NodeOverlay from "../components/NodeOverlay";
import NodeTable from "../components/NodeTable";
import NodeTooltip from "../components/NodeTooltip";
import {
  getAstNodesForTranslationUnit,
  getOverlayNodesForTranslationUnit,
  getTranslationUnit,
} from "../services/api";
import { NodeJSON, TranslationUnitJSON } from "../types";

function TranslationUnitPage() {
  const [searchParams] = useSearchParams();
  const componentName = searchParams.get("component");
  const path = searchParams.get("path");
  const line = searchParams.get("line")
    ? parseInt(searchParams.get("line")!, 10)
    : null;
  const findingText = searchParams.get("findingText") || "";
  const kind = searchParams.get("kind") || "info";

  const [translationUnit, setTranslationUnit] =
    useState<TranslationUnitJSON | null>(null);
  const [astNodes, setAstNodes] = useState<NodeJSON[]>([]);
  const [overlayNodes, setOverlayNodes] = useState<NodeJSON[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [highlightedNode, setHighlightedNode] = useState<NodeJSON | null>(null);
  const [activeTab, setActiveTab] = useState<"astNodes" | "overlayNodes">(
    "overlayNodes",
  );

  const lineHeight = 24;
  const charWidth = 10;
  const offsetTop = 8;
  const baseOffsetLeft = 14;

  useEffect(() => {
    const fetchData = async () => {
      if (!componentName || !path) {
        setError("Missing component name or path");
        setLoading(false);
        return;
      }

      try {
        console.log(
          `Fetching data for component: ${componentName}, path: ${path}`,
        );
        const data = await getTranslationUnit(componentName, path);
        setTranslationUnit(data);
        setAstNodes(await getAstNodesForTranslationUnit(componentName, path));
        setOverlayNodes(
          await getOverlayNodesForTranslationUnit(componentName, path),
        );
      } catch (err) {
        setError("Failed to load nodes");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [componentName, path]);

  if (loading) {
    return (
      <div className="container mx-auto p-4">Loading translation unit...</div>
    );
  }

  if (error || !translationUnit) {
    return (
      <div className="container mx-auto p-4">
        <div className="rounded bg-red-100 p-3 text-red-700">
          {error || "Translation unit not found"}
        </div>
        <div className="mt-4">
          <Link to="/" className="text-blue-600 hover:underline">
            Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  const totalLines = translationUnit.code.split("\n").length;
  const lineNumberWidth = Math.ceil(Math.log10(totalLines + 1));
  const offsetLeft = baseOffsetLeft + lineNumberWidth * charWidth;

  const nodes = activeTab === "overlayNodes" ? overlayNodes : astNodes;

  return (
    <div className="container mx-auto p-4">
      <div className="mb-4">
        {componentName && (
          <Link
            to={`/component/${componentName}`}
            className="text-blue-600 hover:underline"
          >
            Back to Component
          </Link>
        )}
      </div>

      <h1 className="mb-6 text-2xl font-bold">{translationUnit.name}</h1>
      <p className="mb-4 text-gray-500">{translationUnit.path}</p>

      <div className="relative mb-6 rounded bg-white p-2 shadow-md">
        <div className="relative">
          <SyntaxHighlighter
            language="python"
            style={docco}
            showLineNumbers={true}
          >
            {translationUnit.code}
          </SyntaxHighlighter>

          <NodeOverlay
            nodes={nodes}
            highlightedNode={highlightedNode}
            setHighlightedNode={setHighlightedNode}
            lineHeight={lineHeight}
            charWidth={charWidth}
            offsetTop={offsetTop}
            offsetLeft={offsetLeft}
            highlightLine={line}
            findingText={findingText}
            kind={kind}
          />

          {highlightedNode && (
            <NodeTooltip
              node={highlightedNode}
              lineHeight={lineHeight}
              charWidth={charWidth}
              offsetTop={offsetTop}
              offsetLeft={offsetLeft}
            />
          )}
        </div>
      </div>

      <div className="rounded bg-white p-6 shadow-md">
        <div className="mb-4">
          <button
            className={`ml-2 px-4 py-2 ${
              activeTab === "overlayNodes"
                ? "bg-blue-600 text-white"
                : "bg-gray-200"
            }`}
            onClick={() => setActiveTab("overlayNodes")}
          >
            Overlay Nodes
          </button>
          <button
            className={`px-4 py-2 ${
              activeTab === "astNodes"
                ? "bg-blue-600 text-white"
                : "bg-gray-200"
            }`}
            onClick={() => setActiveTab("astNodes")}
          >
            AST Nodes
          </button>
        </div>

        {activeTab === "astNodes" ? (
          <NodeTable
            title={"AST"}
            nodes={astNodes}
            highlightedNode={highlightedNode}
            setHighlightedNode={setHighlightedNode}
          />
        ) : (
          <NodeTable
            title={"Overlay Nodes"}
            nodes={overlayNodes}
            highlightedNode={highlightedNode}
            setHighlightedNode={setHighlightedNode}
          />
        )}
      </div>
    </div>
  );
}

export default TranslationUnitPage;
