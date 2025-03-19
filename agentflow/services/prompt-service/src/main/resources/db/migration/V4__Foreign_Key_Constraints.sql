-- V4__Foreign_Key_Constraints.sql
-- Add foreign key constraints between main tables and reference data

-- Add foreign key constraints for the reference data
ALTER TABLE prompt_versions ADD CONSTRAINT fk_version_status
    FOREIGN KEY (status) REFERENCES version_statuses(id);

ALTER TABLE prompt_parameters ADD CONSTRAINT fk_parameter_type
    FOREIGN KEY (parameter_type) REFERENCES parameter_types(id);

ALTER TABLE prompt_executions ADD CONSTRAINT fk_execution_status
    FOREIGN KEY (status) REFERENCES execution_statuses(id);