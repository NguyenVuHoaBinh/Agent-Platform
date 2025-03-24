import axiosInstance from './axiosConfig';
import { AxiosResponse } from 'axios';
import { PageResponse, PromptTemplate, PromptTemplateRequest, PromptTemplateSearchCriteria } from './promptService';

// API service for prompt templates
const templateService = {
  // Get all templates with pagination
  getTemplates: async (page: number = 0, size: number = 10): Promise<PageResponse<PromptTemplate>> => {
    const response: AxiosResponse<PageResponse<PromptTemplate>> = await axiosInstance.get(
      `/api/v1/templates?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Search templates with criteria
  searchTemplates: async (
    criteria: PromptTemplateSearchCriteria,
    page: number = 0,
    size: number = 10
  ): Promise<PageResponse<PromptTemplate>> => {
    const response: AxiosResponse<PageResponse<PromptTemplate>> = await axiosInstance.post(
      `/api/v1/templates/search?page=${page}&size=${size}`,
      criteria
    );
    return response.data;
  },

  // Get a template by ID
  getTemplateById: async (id: string): Promise<PromptTemplate> => {
    const response: AxiosResponse<PromptTemplate> = await axiosInstance.get(`/api/v1/templates/${id}`);
    return response.data;
  },

  // Create a new template
  createTemplate: async (template: PromptTemplateRequest): Promise<PromptTemplate> => {
    const response: AxiosResponse<PromptTemplate> = await axiosInstance.post('/api/v1/templates', template);
    return response.data;
  },

  // Update an existing template
  updateTemplate: async (id: string, template: PromptTemplateRequest): Promise<PromptTemplate> => {
    const response: AxiosResponse<PromptTemplate> = await axiosInstance.put(`/api/v1/templates/${id}`, template);
    return response.data;
  },

  // Delete a template
  deleteTemplate: async (id: string): Promise<void> => {
    await axiosInstance.delete(`/api/v1/templates/${id}`);
  },

  // Get all categories
  getCategories: async (): Promise<string[]> => {
    const response: AxiosResponse<string[]> = await axiosInstance.get('/api/v1/templates/categories');
    return response.data;
  },

  // Check if template name exists
  checkNameExists: async (name: string): Promise<boolean> => {
    const response: AxiosResponse<boolean> = await axiosInstance.get(`/api/v1/templates/check-name?name=${encodeURIComponent(name)}`);
    return response.data;
  }
};

export default templateService; 