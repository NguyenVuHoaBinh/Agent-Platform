import axiosInstance from './axiosConfig';
import { AxiosResponse } from 'axios';

// Define types for API responses and requests
export interface PromptTemplate {
  id: string;
  name: string;
  description: string;
  projectId: string;
  category: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  versionCount?: number;
  hasPublishedVersion?: boolean;
}

export interface PromptTemplateRequest {
  name: string;
  description: string;
  projectId: string;
  category: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface PromptTemplateSearchCriteria {
  searchText?: string;
  projectId?: string;
  category?: string;
  createdBy?: string;
  hasPublishedVersion?: boolean;
  minVersionCount?: number;
  useExactMatch?: boolean;
  useFuzzyMatch?: boolean;
}

// API service for prompt templates
const promptTemplateService = {
  // Get all templates with pagination
  getTemplates: async (page: number = 0, size: number = 10): Promise<PageResponse<PromptTemplate>> => {
    const response: AxiosResponse<PageResponse<PromptTemplate>> = await axiosInstance.get(
      `/api/v1/templates?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Get template by ID
  getTemplateById: async (id: string, includeVersions: boolean = false): Promise<PromptTemplate> => {
    const response: AxiosResponse<PromptTemplate> = await axiosInstance.get(
      `/api/v1/templates/${id}?includeVersions=${includeVersions}`
    );
    return response.data;
  },

  // Create new template
  createTemplate: async (template: PromptTemplateRequest): Promise<PromptTemplate> => {
    const response: AxiosResponse<PromptTemplate> = await axiosInstance.post(
      '/api/v1/templates',
      template
    );
    return response.data;
  },

  // Update template
  updateTemplate: async (id: string, template: PromptTemplateRequest): Promise<PromptTemplate> => {
    const response: AxiosResponse<PromptTemplate> = await axiosInstance.put(
      `/api/v1/templates/${id}`,
      template
    );
    return response.data;
  },

  // Delete template
  deleteTemplate: async (id: string): Promise<void> => {
    await axiosInstance.delete(`/api/v1/templates/${id}`);
  },

  // Get templates by project
  getTemplatesByProject: async (projectId: string, page: number = 0, size: number = 10): Promise<PageResponse<PromptTemplate>> => {
    const response: AxiosResponse<PageResponse<PromptTemplate>> = await axiosInstance.get(
      `/api/v1/templates/by-project/${projectId}?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Get templates by category
  getTemplatesByCategory: async (category: string, page: number = 0, size: number = 10): Promise<PageResponse<PromptTemplate>> => {
    const response: AxiosResponse<PageResponse<PromptTemplate>> = await axiosInstance.get(
      `/api/v1/templates/by-category/${category}?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Search templates
  searchTemplates: async (
    criteria: PromptTemplateSearchCriteria,
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<PromptTemplate>> => {
    // Convert criteria to query parameters
    const params = new URLSearchParams();
    
    if (criteria.searchText) params.append('searchText', criteria.searchText);
    if (criteria.projectId) params.append('projectId', criteria.projectId);
    if (criteria.category) params.append('category', criteria.category);
    if (criteria.createdBy) params.append('createdBy', criteria.createdBy);
    if (criteria.hasPublishedVersion !== undefined) params.append('hasPublishedVersion', criteria.hasPublishedVersion.toString());
    if (criteria.minVersionCount !== undefined) params.append('minVersionCount', criteria.minVersionCount.toString());
    if (criteria.useExactMatch !== undefined) params.append('useExactMatch', criteria.useExactMatch.toString());
    if (criteria.useFuzzyMatch !== undefined) params.append('useFuzzyMatch', criteria.useFuzzyMatch.toString());
    
    params.append('page', page.toString());
    params.append('size', size.toString());
    
    const response: AxiosResponse<PageResponse<PromptTemplate>> = await axiosInstance.get(
      `/api/v1/templates/search?${params.toString()}`
    );
    return response.data;
  },

  // Get all categories
  getAllCategories: async (): Promise<string[]> => {
    const response: AxiosResponse<string[]> = await axiosInstance.get('/api/v1/templates/categories');
    return response.data;
  }
};

export default promptTemplateService; 