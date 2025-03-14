import React, { useState } from "react";

interface GenerateCPGFormProps {
  onSubmit: (sourceDir: string, includeDir?: string, topLevel?: string) => void;
  loading: boolean;
  error: string | null;
}

const GenerateCPGForm: React.FC<GenerateCPGFormProps> = ({
  onSubmit,
  loading,
  error,
}) => {
  const [sourceDir, setSourceDir] = useState<string>("");
  const [includeDir, setIncludeDir] = useState<string | undefined>(undefined);
  const [topLevel, setTopLevel] = useState<string | undefined>(undefined);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(sourceDir, includeDir, topLevel);
  };

  return (
    <div className="mb-6 rounded bg-white p-6 shadow-md">
      <h2 className="mb-4 text-xl font-semibold">Generate CPG</h2>
      <form onSubmit={handleSubmit}>
        <div className="mb-4">
          <label
            htmlFor="sourceDir"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Source Directory
          </label>
          <input
            type="text"
            id="sourceDir"
            value={sourceDir}
            onChange={(e) => setSourceDir(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-indigo-500"
            placeholder="/path/to/source/code"
            required
          />
        </div>
        <div className="mb-4">
          <label
            htmlFor="includeDir"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Include Directory (optional)
          </label>
          <input
            type="text"
            id="includeDir"
            value={includeDir}
            onChange={(e) => setIncludeDir(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-indigo-500"
            placeholder="/path/to/source/code"
          />
        </div>
        <div className="mb-4">
          <label
            htmlFor="topLevel"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Top Level Directory (optional)
          </label>
          <input
            type="text"
            id="topLevel"
            value={topLevel}
            onChange={(e) => setTopLevel(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-indigo-500"
            placeholder="/path/to/source/code"
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="rounded bg-blue-600 px-4 py-2 font-medium text-white shadow-sm hover:bg-blue-700 disabled:opacity-70"
        >
          {loading ? "Generating..." : "Generate CPG"}
        </button>
      </form>

      {error && (
        <div className="mt-4 rounded bg-red-100 p-3 text-red-700">{error}</div>
      )}
    </div>
  );
};

export default GenerateCPGForm;
