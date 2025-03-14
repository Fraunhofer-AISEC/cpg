import React from 'react';
import { TranslationResult } from '../types';
import ComponentsList from './ComponentsList';
import FindingsList from './FindingsList';

interface AnalysisResultsProps {
    result: TranslationResult;
}

const AnalysisResults: React.FC<AnalysisResultsProps> = ({ result }) => {
    return (
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

            <ComponentsList components={result.components} />
            <FindingsList findings={result.findings} />
        </div>
    );
};

export default AnalysisResults;