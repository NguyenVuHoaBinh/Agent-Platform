import axiosInstance from './axiosConfig';
import { AxiosResponse } from 'axios';

// Define types for API responses and requests
export interface PromptTestRequest {
  versionId: string;
  providerId: string;
  modelId: string;
  parameters: Record<string, any>;
  validationCriteria?: Record<string, any>;
  storeResult: boolean;
}

export interface PromptBatchTestRequest {
  versionId: string;
  providerId: string;
  modelId: string;
  parameterSets: Array<Record<string, any>>;
  validationCriteria?: Record<string, any>;
  storeResults: boolean;
}

export interface LlmResponseMetrics {
  tokenCount: number;
  promptTokens: number;
  completionTokens: number;
  latency: number;
  provider: string;
  model: string;
  timestamp: string;
}

export interface ValidationIssue {
  type: string;
  message: string;
  severity: 'ERROR' | 'WARNING' | 'INFO';
  location?: string;
}

export interface ValidationResult {
  passed: boolean;
  score?: number;
  issues: ValidationIssue[];
}

export interface PromptExecutionResult {
  executionId?: string;
  versionId: string;
  templateId: string;
  templateName: string;
  versionNumber: string;
  providerId: string;
  modelId: string;
  parameters: Record<string, any>;
  inputPrompt: string;
  responseText: string;
  metrics: LlmResponseMetrics;
  status: 'SUCCESS' | 'ERROR' | 'INVALID_PARAMS' | 'TIMEOUT';
  errorMessage?: string;
  validationResult?: ValidationResult;
  executedAt: string;
  executedBy: string;
}

// API service for prompt testing
const testingService = {
  // Test a prompt
  testPrompt: async (request: PromptTestRequest): Promise<PromptExecutionResult> => {
    const response: AxiosResponse<PromptExecutionResult> = await axiosInstance.post(
      '/api/v1/testing/test',
      request
    );
    return response.data;
  },

  // Batch test a prompt
  batchTestPrompt: async (request: PromptBatchTestRequest): Promise<PromptExecutionResult[]> => {
    const response: AxiosResponse<PromptExecutionResult[]> = await axiosInstance.post(
      '/api/v1/testing/batch',
      request
    );
    return response.data;
  },

  // Validate a response against criteria
  validateResponse: async (
    response: string,
    validationCriteria: Record<string, any>
  ): Promise<ValidationResult> => {
    const validationResponse: AxiosResponse<ValidationResult> = await axiosInstance.post(
      `/api/v1/testing/validate?response=${encodeURIComponent(response)}`,
      validationCriteria
    );
    return validationResponse.data;
  },

  // Compare two test executions
  compareResponses: async (executionId1: string, executionId2: string): Promise<Record<string, any>> => {
    const response: AxiosResponse<Record<string, any>> = await axiosInstance.get(
      `/api/v1/testing/compare?executionId1=${executionId1}&executionId2=${executionId2}`
    );
    return response.data;
  },

  // Get test history for a version
  getTestHistory: async (versionId: string, limit: number = 10): Promise<PromptExecutionResult[]> => {
    const response: AxiosResponse<PromptExecutionResult[]> = await axiosInstance.get(
      `/api/v1/testing/history/${versionId}?limit=${limit}`
    );
    return response.data;
  },

  // Get available LLM providers
  getAvailableProviders: async (): Promise<Record<string, string[]>> => {
    const response: AxiosResponse<Record<string, string[]>> = await axiosInstance.get(
      '/api/v1/testing/providers'
    );
    return response.data;
  },

  // Get available models for a provider
  getAvailableModels: async (providerId: string): Promise<Record<string, string>> => {
    const response: AxiosResponse<Record<string, string>> = await axiosInstance.get(
      `/api/v1/testing/providers/${providerId}/models`
    );
    return response.data;
  }
};

export default testingService; 