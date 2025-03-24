-- V5__AB_Test_Schema.sql
-- Add A/B testing tables and update existing structures

-- Update ab_tests table with missing columns
ALTER TABLE ab_tests 
ADD COLUMN IF NOT EXISTS sample_size INT DEFAULT 100,
ADD COLUMN IF NOT EXISTS confidence_threshold DOUBLE DEFAULT 95.0,
ADD COLUMN IF NOT EXISTS evaluation_metric VARCHAR(100) DEFAULT 'success_rate',
ADD COLUMN IF NOT EXISTS test_parameters TEXT,
ADD COLUMN IF NOT EXISTS success_criteria VARCHAR(255),
ADD COLUMN IF NOT EXISTS provider_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS model_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS started_at TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP NULL;

-- Create indexes for ab_tests if they don't exist
CREATE INDEX IF NOT EXISTS idx_test_created_by ON ab_tests(created_by);
CREATE INDEX IF NOT EXISTS idx_test_status ON ab_tests(status);
CREATE INDEX IF NOT EXISTS idx_test_name ON ab_tests(name);

-- Update ab_test_results table with missing columns
ALTER TABLE ab_test_results
ADD COLUMN IF NOT EXISTS version_id VARCHAR(36),
ADD COLUMN IF NOT EXISTS is_control BOOLEAN,
ADD COLUMN IF NOT EXISTS sample_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS success_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS success_rate DOUBLE,
ADD COLUMN IF NOT EXISTS avg_tokens DOUBLE,
ADD COLUMN IF NOT EXISTS total_cost DECIMAL(10,6),
ADD COLUMN IF NOT EXISTS p_value DOUBLE,
ADD COLUMN IF NOT EXISTS confidence_level DOUBLE,
ADD COLUMN IF NOT EXISTS metric_values TEXT,
ADD COLUMN IF NOT EXISTS execution_ids TEXT;

-- Add foreign key if it doesn't exist
ALTER TABLE ab_test_results 
ADD CONSTRAINT IF NOT EXISTS fk_result_version FOREIGN KEY (version_id) REFERENCES prompt_versions(id);

-- Create indexes for ab_test_results if they don't exist
CREATE INDEX IF NOT EXISTS idx_result_test ON ab_test_results(ab_test_id);
CREATE INDEX IF NOT EXISTS idx_result_version ON ab_test_results(version_id);
CREATE INDEX IF NOT EXISTS idx_result_control ON ab_test_results(is_control); 