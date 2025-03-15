import { useEffect, useState } from "react";
import AnalysisResults from "../components/AnalysisResults";
import GenerateCPGForm from "../components/GenerateCPGForm";
import {
  generateCPG,
  getTranslationResult,
  regenerateCPG,
} from "../services/api";
import { TranslationResultJSON } from "../types";

function DashboardPage() {
  const [result, setResult] = useState<TranslationResultJSON | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [regenerateEnabled, setRegenerateEnabled] = useState<boolean>(false);

  useEffect(() => {
    const fetchResult = async () => {
      try {
        const data = await getTranslationResult();
        setResult(data);
        setRegenerateEnabled(true);
      } catch (error) {
        // Ignore error if no result exists yet
        setRegenerateEnabled(false);
      }
    };

    fetchResult();
  }, []);

  const handleSubmit = async (
    sourceDir: string,
    includeDir?: string,
    topLevel?: string,
  ) => {
    setLoading(true);
    setError(null);

    try {
      const data = await generateCPG(sourceDir, includeDir, topLevel);
      setResult(data);
      setRegenerateEnabled(true);
    } catch (err) {
      setError("Failed to generate CPG. Please check the path and try again.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerate = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await regenerateCPG();
      setResult(data);
    } catch (err) {
      setError("Failed to regenerate CPG.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto p-4">
      <GenerateCPGForm
        onSubmit={handleSubmit}
        loading={loading}
        error={error}
      />

      {regenerateEnabled && (
        <div className="mb-6">
          <button
            className="mt-4 rounded bg-green-600 px-4 py-2 font-bold text-white hover:bg-green-700"
            onClick={handleRegenerate}
            disabled={loading}
          >
            {loading ? "Working..." : "Re-Generate CPG"}
          </button>
          <p className="mt-1 text-sm text-gray-600">
            Re-run analysis with the current configuration
          </p>
        </div>
      )}

      {result && <AnalysisResults result={result} />}
    </div>
  );
}

export default DashboardPage;
