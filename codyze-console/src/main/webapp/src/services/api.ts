// src/main/web/src/services/api.ts
import axios from "axios";
import {
  ComponentJSON,
  NodeJSON,
  TranslationResultJSON,
  TranslationUnitJSON,
} from "../types";

const API_BASE_URL = "/api";

export const generateCPG = async (
  sourceDir: string,
  includeDir?: string,
  topLevel?: string,
): Promise<TranslationResultJSON> => {
  const response = await axios.post(`${API_BASE_URL}/analyze`, {
    sourceDir,
    includeDir,
    topLevel,
  });
  return response.data;
};

export const regenerateCPG = async (): Promise<TranslationResultJSON> => {
  const response = await fetch("/api/reanalyze", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || "Failed to regenerate CPG");
  }

  return response.json();
};

export const getTranslationResult =
  async (): Promise<TranslationResultJSON> => {
    const response = await axios.get(`${API_BASE_URL}/result`);
    return response.data;
  };

export const getComponent = async (name: string): Promise<ComponentJSON> => {
  const response = await axios.get(`${API_BASE_URL}/component/${name}`);
  return response.data;
};

export const getTranslationUnit = async (
  componentName: string,
  id: string,
): Promise<TranslationUnitJSON> => {
  const response = await axios.get(`${API_BASE_URL}/component/${componentName}/translation-unit/${id}`);
  return response.data;
};

export const getAstNodesForTranslationUnit = async (
  componentName: string,
  id: string,
): Promise<NodeJSON[]> => {
  const response = await axios.get(`${API_BASE_URL}/component/${componentName}/translation-unit/${id}/ast-nodes`);
  return response.data;
};

export const getOverlayNodesForTranslationUnit = async (
  componentName: string,
  name: string,
): Promise<NodeJSON[]> => {
  const response = await axios.get(`${API_BASE_URL}/component/${componentName}/translation-unit/${name}/overlay-nodes`);
  return response.data;
};
