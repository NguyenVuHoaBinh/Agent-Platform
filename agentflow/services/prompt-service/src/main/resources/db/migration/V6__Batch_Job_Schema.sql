-- V6__Batch_Job_Schema.sql
-- Add Batch Job tables

-- Create batch_jobs table if it doesn't exist
CREATE TABLE IF NOT EXISTS batch_jobs (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    scheduled_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    priority INT NOT NULL DEFAULT 5,
    max_retries INT NOT NULL DEFAULT 3,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    completion_percentage INT DEFAULT 0,
    template_id VARCHAR(36),
    version_id VARCHAR(36),
    parameters TEXT,
    configuration TEXT,
    result TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES prompt_templates(id),
    FOREIGN KEY (version_id) REFERENCES prompt_versions(id)
);

-- Create indexes for batch_jobs
CREATE INDEX IF NOT EXISTS idx_job_status ON batch_jobs(status);
CREATE INDEX IF NOT EXISTS idx_job_type ON batch_jobs(job_type);
CREATE INDEX IF NOT EXISTS idx_job_created_by ON batch_jobs(created_by);
CREATE INDEX IF NOT EXISTS idx_job_scheduled_at ON batch_jobs(scheduled_at);

-- Create batch_job_executions table if it doesn't exist
CREATE TABLE IF NOT EXISTS batch_job_executions (
    id VARCHAR(36) PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    duration_ms BIGINT,
    worker_id VARCHAR(255),
    execution_parameters TEXT,
    execution_result TEXT,
    execution_log TEXT,
    error_message VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES batch_jobs(id) ON DELETE CASCADE
);

-- Create indexes for batch_job_executions
CREATE INDEX IF NOT EXISTS idx_execution_job ON batch_job_executions(job_id);
CREATE INDEX IF NOT EXISTS idx_execution_status ON batch_job_executions(status);
CREATE INDEX IF NOT EXISTS idx_execution_started ON batch_job_executions(started_at); 