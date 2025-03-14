// src/main/web/src/pages/ComponentPage.tsx
import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Component } from '../types';
import { getComponent } from '../services/api';

function ComponentPage() {
    const { name } = useParams<{ name: string }>();
    const [component, setComponent] = useState<Component | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchComponent = async () => {
            if (!name) {
                setError('Component name is missing');
                setLoading(false);
                return;
            }

            try {
                const data = await getComponent(name);
                setComponent(data);
            } catch (err) {
                setError('Failed to load component data');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchComponent();
    }, [name]);

    if (loading) {
        return <div className="container mx-auto p-4">Loading component data...</div>;
    }

    if (error || !component) {
        return (
            <div className="container mx-auto p-4">
                <div className="p-3 bg-red-100 text-red-700 rounded">{error || 'Component not found'}</div>
                <div className="mt-4">
                    <Link to="/" className="text-blue-600 hover:underline">Back to Dashboard</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="container mx-auto p-4">
            <div className="mb-4">
                <Link to="/" className="text-blue-600 hover:underline">
                    Back to Dashboard
                </Link>
            </div>

            <h1 className="text-2xl font-bold mb-6">{component.name}</h1>
            <p className="text-gray-500 mb-4">Top Level: {component.topLevel}</p>

            <div className="bg-white shadow-md rounded p-6">
                <h2 className="text-xl font-semibold mb-4">Translation Units</h2>
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Path</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                        {component.translationUnits.map((unit) => (
                            <tr key={unit.path}>
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{unit.path}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    <Link
                                        to={`/translation-unit?component=${component.name}&path=${encodeURIComponent(unit.path)}`}
                                        className="text-blue-600 hover:underline"
                                    >
                                        View Details
                                    </Link>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default ComponentPage;