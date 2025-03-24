import axiosInstance from './axiosConfig';
import { AxiosResponse } from 'axios';
import { PageResponse } from './promptService';

// Define types for API responses and requests
export interface PromptParameter {
  id: string;
  name: string;
  description: string;
  parameterType: string;
  defaultValue?: string;
  required: boolean;
  validationPattern?: string;
}

export interface PromptVersion {
  id: string;
  templateId: string;
  templateName: string;
  versionNumber: string;
  content: string;
  systemPrompt?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  status: 'DRAFT' | 'REVIEW' | 'PUBLISHED' | 'ARCHIVED' | 'REJECTED';
  parentVersionId?: string;
  parameters: PromptParameter[];
}

export interface PromptVersionRequest {
  templateId: string;
  versionNumber: string;
  content: string;
  systemPrompt?: string;
  parentVersionId?: string;
  parameters?: PromptParameterRequest[];
}

export interface PromptParameterRequest {
  name: string;
  description: string;
  parameterType: string;
  defaultValue?: string;
  required: boolean;
  validationPattern?: string;
}

export interface VersionDiff {
  type: 'ADDED' | 'REMOVED' | 'CHANGED';
  content: string;
  lineNumber: number;
}

export interface VersionComparisonResult {
  version1: PromptVersion;
  version2: PromptVersion;
  contentDiff: VersionDiff[];
  parameterChanges: {
    added: PromptParameter[];
    removed: PromptParameter[];
    changed: PromptParameter[];
  };
  systemPromptDiff?: VersionDiff[];
}

// API service for prompt versions
const versionService = {
  // Get version by ID
  getVersionById: async (id: string): Promise<PromptVersion> => {
    const response: AxiosResponse<PromptVersion> = await axiosInstance.get(`/api/v1/versions/${id}`);
    return response.data;
  },

  // Create new version
  createVersion: async (version: PromptVersionRequest): Promise<PromptVersion> => {
    const response: AxiosResponse<PromptVersion> = await axiosInstance.post('/api/v1/versions', version);
    return response.data;
  },

  // Get versions by template ID
  getVersionsByTemplate: async (templateId: string, page: number = 0, size: number = 10): Promise<PageResponse<PromptVersion>> => {
    const response: AxiosResponse<PageResponse<PromptVersion>> = await axiosInstance.get(
      `/api/v1/templates/${templateId}/versions?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Get all versions
  getAllVersions: async (page: number = 0, size: number = 10): Promise<PageResponse<PromptVersion>> => {
    const response: AxiosResponse<PageResponse<PromptVersion>> = await axiosInstance.get(
      `/api/v1/versions?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Update existing version
  updateVersion: async (id: string, version: Partial<PromptVersionRequest>): Promise<PromptVersion> => {
    const response: AxiosResponse<PromptVersion> = await axiosInstance.put(`/api/v1/versions/${id}`, version);
    return response.data;
  },

  // Create branch from existing version
  createVersionBranch: async (parentId: string, version: Partial<PromptVersionRequest>): Promise<PromptVersion> => {
    const response: AxiosResponse<PromptVersion> = await axiosInstance.post(
      `/api/v1/versions/${parentId}/branch`, 
      version
    );
    return response.data;
  },

  // Get version status transitions
  getVersionStatusTransitions: async (id: string): Promise<{ [key: string]: string[] }> => {
    const response: AxiosResponse<{ [key: string]: string[] }> = await axiosInstance.get(`/api/v1/versions/${id}/status-transitions`);
    return response.data;
  },

  // Update version status
  updateVersionStatus: async (id: string, status: PromptVersion['status'], comment?: string): Promise<PromptVersion> => {
    const response: AxiosResponse<PromptVersion> = await axiosInstance.post(
      `/api/v1/versions/${id}/status`,
      { status, comment }
    );
    return response.data;
  },

  // Compare versions
  compareVersions: async (sourceId: string, targetId: string): Promise<VersionComparisonResult> => {
    const response: AxiosResponse<VersionComparisonResult> = await axiosInstance.get(
      `/api/v1/versions/compare?sourceId=${sourceId}&targetId=${targetId}`
    );
    return response.data;
  },

  // Rollback to a previous version
  rollbackVersion: async (id: string, comment: string): Promise<PromptVersion> => {
    const response: AxiosResponse<PromptVersion> = await axiosInstance.post(
      `/api/v1/versions/${id}/rollback`,
      { comment }
    );
    return response.data;
  },

  // Get version lineage
  getVersionLineage: async (id: string): Promise<any> => {
    const response: AxiosResponse<any> = await axiosInstance.get(`/api/v1/versions/${id}/lineage`);
    return response.data;
  },

  // Get version audit trail
  getVersionAuditTrail: async (id: string): Promise<any> => {
    const response: AxiosResponse<any> = await axiosInstance.get(`/api/v1/versions/${id}/audit-trail`);
    return response.data;
  },

  // Check if version can transition to a specific status
  canTransitionToStatus: async (versionId: string, status: PromptVersion['status']): Promise<boolean> => {
    const response: AxiosResponse<boolean> = await axiosInstance.get(
      `/api/v1/versions/${versionId}/can-transition?status=${status}`
    );
    return response.data;
  }
};

export default versionService; 