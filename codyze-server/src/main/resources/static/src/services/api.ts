// src/main/web/src/services/api.ts
import axios from 'axios';
import { TranslationResult, Component, TranslationUnit, NodeInfo } from '../types';

const API_BASE_URL = '/api';

<<<<<<< HEAD
export const generateCPG = async (sourceDir: string): Promise<TranslationResult> => {
    const response = await axios.post(`${API_BASE_URL}/generate`, { sourceDir });
=======
export const generateCPG = async (sourceDir: string, includeDir?: string): Promise<TranslationResult> => {
    const response = await axios.post(`${API_BASE_URL}/generate`, { sourceDir, includeDir });
>>>>>>> origin/webconsole
    return response.data;
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