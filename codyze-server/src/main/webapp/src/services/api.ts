// src/main/web/src/services/api.ts
import axios from 'axios';
import { TranslationResult, Component, TranslationUnit, NodeInfo } from '../types';

const API_BASE_URL = '/api';

export const generateCPG = async (sourceDir: string, includeDir?: string, topLevel?: string): Promise<TranslationResult> => {
    const response = await axios.post(`${API_BASE_URL}/generate`, { sourceDir, includeDir, topLevel });
    return response.data;
};

export const regenerateCPG = async (): Promise<TranslationResult> => {
    const response = await fetch('/api/regenerate', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to regenerate CPG');
    }

    return response.json();
};

export const getTranslationResult = async (): Promise<TranslationResult> => {
    const response = await axios.get(`${API_BASE_URL}/result`);
    return response.data;
};

export const getComponent = async (name: string): Promise<Component> => {
    const response = await axios.get(`${API_BASE_URL}/component/${name}`);
    return response.data;
};

export const getTranslationUnit = async (componentName: string, path: string): Promise<TranslationUnit> => {
    const response = await axios.get(`${API_BASE_URL}/translationUnit`, {
        params: { component: componentName, path }
    });
    return response.data;
};

export const getNodesForTranslationUnit = async (componentName: string, path: string): Promise<NodeInfo[]> => {
    const response = await axios.get(`${API_BASE_URL}/nodes`, {
        params: { component: componentName, path }
    });
    return response.data;
};