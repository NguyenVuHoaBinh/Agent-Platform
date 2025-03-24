-- V1__Initial_Schema.sql
-- Initial database schema for Prompt Management Service

-- Create prompt_templates table
CREATE TABLE prompt_templates (
                                  id VARCHAR(36) NOT NULL PRIMARY KEY,
                                  name VARCHAR(255) NOT NULL,
                                  description VARCHAR(1000),
                                  project_id VARCHAR(36) NOT NULL,
                                  created_by VARCHAR(36) NOT NULL,
                                  created_at TIMESTAMP NOT NULL,
                                  updated_at TIMESTAMP NOT NULL,
                                  category VARCHAR(50),
                                  UNIQUE KEY uk_name_project (name, project_id)
);

-- Create prompt_versions table
CREATE TABLE prompt_versions (
                                 id VARCHAR(36) NOT NULL PRIMARY KEY,
                                 template_id VARCHAR(36) NOT NULL,
                                 version_number VARCHAR(20) NOT NULL,
                                 content TEXT NOT NULL,
                                 created_at TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 created_by VARCHAR(36) NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 parent_version_id VARCHAR(36),
                                 FOREIGN KEY (template_id) REFERENCES prompt_templates(id) ON DELETE CASCADE,
                                 FOREIGN KEY (parent_version_id) REFERENCES prompt_versions(id),
                                 UNIQUE KEY uk_template_version (template_id, version_number)
);

-- Create prompt_parameters table
CREATE TABLE prompt_parameters (
                                   id VARCHAR(36) NOT NULL PRIMARY KEY,
                                   version_id VARCHAR(36) NOT NULL,
                                   name VARCHAR(100) NOT NULL,
                                   description VARCHAR(500),
                                   parameter_type VARCHAR(20) NOT NULL,
                                   default_value TEXT,
                                   required BOOLEAN NOT NULL DEFAULT FALSE,
                                   validation_pattern VARCHAR(255),
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   FOREIGN KEY (version_id) REFERENCES prompt_versions(id) ON DELETE CASCADE,
                                   UNIQUE KEY uk_version_param_name (version_id, name)
);

-- Create prompt_executions table
CREATE TABLE prompt_executions (
                                   id VARCHAR(36) NOT NULL PRIMARY KEY,
                                   version_id VARCHAR(36) NOT NULL,
                                   provider_id VARCHAR(100) NOT NULL,
                                   model_id VARCHAR(100) NOT NULL,
                                   input_parameters TEXT,
                                   raw_response TEXT,
                                   token_count INT,
                                   input_tokens INT,
                                   output_tokens INT,
                                   cost DECIMAL(10,6),
                                   response_time_ms BIGINT,
                                   executed_at TIMESTAMP NOT NULL,
                                   executed_by VARCHAR(36) NOT NULL,
                                   status VARCHAR(20) NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   FOREIGN KEY (version_id) REFERENCES prompt_versions(id)
);

-- Create version_audit_logs table
CREATE TABLE version_audit_logs (
                                    id VARCHAR(36) NOT NULL PRIMARY KEY,
                                    version_id VARCHAR(36) NOT NULL,
                                    action_type VARCHAR(50) NOT NULL,
                                    performed_by VARCHAR(100) NOT NULL,
                                    performed_at TIMESTAMP NOT NULL,
                                    details TEXT,
                                    previous_status VARCHAR(50),
                                    new_status VARCHAR(50),
                                    reference_version_id VARCHAR(36),
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    FOREIGN KEY (version_id) REFERENCES prompt_versions(id) ON DELETE CASCADE
);

ALTER TABLE prompt_versions ADD COLUMN system_prompt TEXT AFTER content;