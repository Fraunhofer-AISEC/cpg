import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { TranslationResult } from '../types';
import { getTranslationResult, generateCPG } from '../services/api';

function DashboardPage() {
    const [result, setResult] = useState<TranslationResult | null>(null);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [sourceDir, setSourceDir] = useState<string>('');
    const [includeDir, setIncludeDir] = useState<string | null>(null);

    useEffect(() => {
        // Try to fetch any existing result when the component mounts
        const fetchResult = async () => {
            try {
                const data = await getTranslationResult();
                setResult(data);
                if (data && data.sourceDir) {
                    setSourceDir(data.sourceDir);
                }
            } catch (error) {
                // Ignore error if no result exists yet
            }
        };

        fetchResult();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            const data = await generateCPG(sourceDir, includeDir);
            setResult(data);
        } catch (err) {
            setError('Failed to generate CPG. Please check the path and try again.');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container mx-auto p-4">
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

            {result && (
                <div className="bg-white shadow-md rounded p-6">
                    <h2 className="text-xl font-semibold mb-4">Analysis Results</h2>
                    <div className="mb-4">
                        <p className="text-gray-700">
                            <span className="font-medium">Total Components:</span> {result.components.length}
                        </p>
                        <p className="text-gray-700">
                            <span className="font-medium">Total Nodes:</span> {result.totalNodes}
                        </p>
                    </div>

                    <h3 className="text-lg font-medium mb-2">Components</h3>
                    <ul className="divide-y divide-gray-200">
                        {result.components.map((component) => (
                            <li key={component.name} className="py-3">
                                <Link
                                    to={`/component/${component.name}`}
                                    className="text-blue-600 hover:underline text-lg font-medium"
                                >
                                    {component.name}
                                </Link>
                                <p className="text-gray-600 text-sm mt-1">
                                    {component.translationUnits.length} translation units
                                </p>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
}

export default DashboardPage;