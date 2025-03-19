-- V2__Performance_Indexes.sql
-- Add performance indexes to improve query performance

-- Indexes for prompt_templates
CREATE INDEX idx_template_project ON prompt_templates(project_id);
CREATE INDEX idx_template_category ON prompt_templates(category);
CREATE INDEX idx_template_created_by ON prompt_templates(created_by);
CREATE INDEX idx_template_updated_at ON prompt_templates(updated_at);

-- Indexes for prompt_versions
CREATE INDEX idx_version_template ON prompt_versions(template_id);
CREATE INDEX idx_version_status ON prompt_versions(status);
CREATE INDEX idx_version_created_at ON prompt_versions(created_at);
CREATE INDEX idx_version_created_by ON prompt_versions(created_by);

-- Indexes for prompt_parameters
CREATE INDEX idx_parameter_version ON prompt_parameters(version_id);
CREATE INDEX idx_parameter_type ON prompt_parameters(parameter_type);
CREATE INDEX idx_parameter_required ON prompt_parameters(required);

-- Indexes for prompt_executions
CREATE INDEX idx_execution_version ON prompt_executions(version_id);
CREATE INDEX idx_execution_provider ON prompt_executions(provider_id);
CREATE INDEX idx_execution_model ON prompt_executions(model_id);
CREATE INDEX idx_execution_status ON prompt_executions(status);
CREATE INDEX idx_execution_executed_at ON prompt_executions(executed_at);
CREATE INDEX idx_execution_executed_by ON prompt_executions(executed_by);