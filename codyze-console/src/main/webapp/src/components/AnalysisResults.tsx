import { TranslationResultJSON } from "../types";
import ComponentsList from "../components/ComponentsList";
import FindingsList from "../components/FindingsList";

interface AnalysisResultsProps {
  result: TranslationResultJSON;
}

function AnalysisResults({ result }: AnalysisResultsProps) {
  return (
    <div className="rounded bg-white p-6 shadow-md">
      <h2 className="mb-4 text-xl font-semibold">Analysis Results</h2>
      <div className="mb-4">
        <p className="text-gray-700">
          <span className="font-medium">Total Components:</span>{" "}
          {result.components.length}
        </p>
        <p className="text-gray-700">
          <span className="font-medium">Total Nodes:</span> {result.totalNodes}
        </p>
      </div>

      <ComponentsList components={result.components} />
      <FindingsList findings={result.findings} />
    </div>
  );
}

export default AnalysisResults;
