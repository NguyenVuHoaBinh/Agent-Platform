import axiosInstance from './axiosConfig';
import { AxiosResponse } from 'axios';
import { PromptVersion } from './versionService';

// Define types for API responses and requests
export enum SuggestionType {
  TOKEN_EFFICIENCY = 'TOKEN_EFFICIENCY',
  CLARITY = 'CLARITY',
  ERROR_HANDLING = 'ERROR_HANDLING',
  PARAMETER_USAGE = 'PARAMETER_USAGE'
}

export enum OptimizationGoal {
  RESPONSE_QUALITY = 'RESPONSE_QUALITY',
  TOKEN_EFFICIENCY = 'TOKEN_EFFICIENCY',
  RELIABILITY = 'RELIABILITY',
  CLARITY = 'CLARITY'
}

export interface OptimizationSuggestion {
  type: SuggestionType;
  description: string;
  impact: 'HIGH' | 'MEDIUM' | 'LOW';
  suggestedChange: string;
  originalText?: string;
  lineNumbers?: number[];
}

export interface OptimizationResult {
  originalText: string;
  optimizedText?: string;
  suggestions: OptimizationSuggestion[];
  score: number;
  potentialImpact?: Record<string, any>;
  recommendation?: string;
  currentMetrics?: Record<string, any>;
  projectedMetrics?: Record<string, any>;
  automaticallyOptimized: boolean;
}

export interface PromptOptimizationRequest {
  suggestionTypes: SuggestionType[];
  applyAutomatically: boolean;
  createNewVersion: boolean;
  modelId?: string;
  providerId?: string;
  strategies?: Record<string, boolean>;
  parameters?: Record<string, any>;
  maxTokens?: number;
  goals?: OptimizationGoal[];
  measureImpact?: boolean;
  testSampleCount?: number;
  async?: boolean;
}

// API service for prompt optimization
const optimizationService = {
  // Analyze a prompt version
  analyzePrompt: async (versionId: string): Promise<OptimizationResult> => {
    const response: AxiosResponse<OptimizationResult> = await axiosInstance.get(
      `/api/v1/optimization/analyze/${versionId}`
    );
    return response.data;
  },

  // Generate optimized variations of a prompt
  generateOptimizedVariations: async (
    versionId: string,
    suggestionTypes?: SuggestionType[],
    count: number = 3
  ): Promise<Record<string, Record<string, any>>> => {
    let url = `/api/v1/optimization/variations/${versionId}?count=${count}`;
    
    if (suggestionTypes && suggestionTypes.length > 0) {
      const typeParams = suggestionTypes.map(type => `suggestionTypes=${type}`).join('&');
      url += `&${typeParams}`;
    }
    
    const response: AxiosResponse<Record<string, Record<string, any>>> = await axiosInstance.post(url);
    return response.data;
  },

  // Run a complete optimization process
  optimizePrompt: async (
    versionId: string,
    request: PromptOptimizationRequest
  ): Promise<OptimizationResult> => {
    const response: AxiosResponse<OptimizationResult> = await axiosInstance.post(
      `/api/v1/optimization/optimize/${versionId}`,
      request
    );
    return response.data;
  },

  // Start an asynchronous optimization process
  optimizePromptAsync: async (
    versionId: string,
    request: PromptOptimizationRequest
  ): Promise<void> => {
    await axiosInstance.post(
      `/api/v1/optimization/optimize-async/${versionId}`,
      request
    );
  },

  // Create a new version with optimized prompt text
  createOptimizedVersion: async (
    sourceVersionId: string,
    optimizedText: string
  ): Promise<PromptVersion> => {
    const response: AxiosResponse<PromptVersion> = await axiosInstance.post(
      `/api/v1/optimization/create-version/${sourceVersionId}`,
      optimizedText,
      {
        headers: {
          'Content-Type': 'text/plain'
        }
      }
    );
    return response.data;
  },

  // Create an optimization batch job
  createOptimizationJob: async (
    versionId: string,
    request: PromptOptimizationRequest
  ): Promise<string> => {
    const response: AxiosResponse<string> = await axiosInstance.post(
      `/api/v1/optimization/job/${versionId}`,
      request
    );
    return response.data;
  },

  // Apply specific optimization strategies
  applyOptimizationStrategies: async (
    versionId: string,
    strategies: Record<string, boolean>
  ): Promise<string> => {
    const response: AxiosResponse<string> = await axiosInstance.post(
      `/api/v1/optimization/strategies/${versionId}`,
      strategies
    );
    return response.data;
  },

  // Suggest token efficiency improvements
  suggestTokenEfficiencyImprovements: async (versionId: string): Promise<OptimizationResult> => {
    const response: AxiosResponse<OptimizationResult> = await axiosInstance.get(
      `/api/v1/optimization/suggest/token-efficiency/${versionId}`
    );
    return response.data;
  },

  // Suggest clarity improvements
  suggestClarityImprovements: async (versionId: string): Promise<OptimizationResult> => {
    const response: AxiosResponse<OptimizationResult> = await axiosInstance.get(
      `/api/v1/optimization/suggest/clarity/${versionId}`
    );
    return response.data;
  },

  // Suggest error handling improvements
  suggestErrorHandlingImprovements: async (versionId: string): Promise<OptimizationResult> => {
    const response: AxiosResponse<OptimizationResult> = await axiosInstance.get(
      `/api/v1/optimization/suggest/error-handling/${versionId}`
    );
    return response.data;
  },

  // Suggest parameter improvements
  suggestParameterImprovements: async (versionId: string): Promise<OptimizationResult> => {
    const response: AxiosResponse<OptimizationResult> = await axiosInstance.get(
      `/api/v1/optimization/suggest/parameters/${versionId}`
    );
    return response.data;
  }
};

export default optimizationService; 