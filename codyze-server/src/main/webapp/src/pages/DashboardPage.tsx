import React, {useEffect, useState} from 'react';
import {TranslationResult} from '../types';
import {generateCPG, getTranslationResult} from '../services/api';
import GenerateCPGForm from '../components/GenerateCPGForm';
import AnalysisResults from '../components/AnalysisResults';

function DashboardPage() {
    const [result, setResult] = useState<TranslationResult | null>(null);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchResult = async () => {
            try {
                const data = await getTranslationResult();
                setResult(data);
            } catch (error) {
                // Ignore error if no result exists yet
            }
        };

        fetchResult();
    }, []);

    const handleSubmit = async (sourceDir: string, includeDir?: string, topLevel?: string) => {
        setLoading(true);
        setError(null);

        try {
            const data = await generateCPG(sourceDir, includeDir, topLevel);
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
            <GenerateCPGForm onSubmit={handleSubmit} loading={loading} error={error} />
            {result && <AnalysisResults result={result} />}
        </div>
    );
}

export default DashboardPage;