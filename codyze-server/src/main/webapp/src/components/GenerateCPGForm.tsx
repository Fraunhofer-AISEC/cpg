import React, { useState } from 'react';

interface GenerateCPGFormProps {
    onSubmit: (sourceDir: string, includeDir?: string, topLevel?: string) => void;
    loading: boolean;
    error: string | null;
}

const GenerateCPGForm: React.FC<GenerateCPGFormProps> = ({ onSubmit, loading, error }) => {
    const [sourceDir, setSourceDir] = useState<string>('');
    const [includeDir, setIncludeDir] = useState<string | undefined>(undefined);
    const [topLevel, setTopLevel] = useState<string | undefined>(undefined);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit(sourceDir, includeDir, topLevel);
    };

    return (
        <div className="bg-white shadow-md rounded p-6 mb-6">
            <h2 className="text-xl font-semibold mb-4">Generate CPG</h2>
            <form onSubmit={handleSubmit}>
                <div className="mb-4">
                    <label htmlFor="sourceDir" className="block text-sm font-medium text-gray-700 mb-1">
                        Source Directory
                    </label>
                    <input
                        type="text"
                        id="sourceDir"
                        value={sourceDir}
                        onChange={(e) => setSourceDir(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                        placeholder="/path/to/source/code"
                        required
                    />
                </div>
                <div className="mb-4">
                    <label htmlFor="includeDir" className="block text-sm font-medium text-gray-700 mb-1">
                        Include Directory (optional)
                    </label>
                    <input
                        type="text"
                        id="includeDir"
                        value={includeDir}
                        onChange={(e) => setIncludeDir(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                        placeholder="/path/to/source/code"
                    />
                </div>
                <div className="mb-4">
                    <label htmlFor="topLevel" className="block text-sm font-medium text-gray-700 mb-1">
                        Top Level Directory (optional)
                    </label>
                    <input
                        type="text"
                        id="topLevel"
                        value={topLevel}
                        onChange={(e) => setTopLevel(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                        placeholder="/path/to/source/code"
                    />
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded shadow-sm disabled:opacity-70"
                >
                    {loading ? 'Generating...' : 'Generate CPG'}
                </button>
            </form>

            {error && (
                <div className="mt-4 p-3 bg-red-100 text-red-700 rounded">
                    {error}
                </div>
            )}
        </div>
    );
};

export default GenerateCPGForm;