-- V3__Reference_Data.sql
-- Reference data for the Prompt Management Service

-- Create table for parameter types
CREATE TABLE parameter_types (
                                 id VARCHAR(20) NOT NULL PRIMARY KEY,
                                 description VARCHAR(200) NOT NULL,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert standard parameter types
INSERT INTO parameter_types (id, description) VALUES
                                                  ('STRING', 'Text string parameter'),
                                                  ('NUMBER', 'Numeric parameter'),
                                                  ('BOOLEAN', 'Boolean parameter (true/false)'),
                                                  ('ARRAY', 'Array/list of values'),
                                                  ('OBJECT', 'JSON object parameter'),
                                                  ('DATE', 'Date parameter'),
                                                  ('DATETIME', 'Date and time parameter');

-- Create table for version statuses
CREATE TABLE version_statuses (
                                  id VARCHAR(20) NOT NULL PRIMARY KEY,
                                  description VARCHAR(200) NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert standard version statuses
INSERT INTO version_statuses (id, description) VALUES
                                                   ('DRAFT', 'Work in progress version'),
                                                   ('REVIEW', 'Version under review'),
                                                   ('APPROVED', 'Approved but not published'),
                                                   ('PUBLISHED', 'Active published version'),
                                                   ('DEPRECATED', 'Version that should no longer be used'),
                                                   ('ARCHIVED', 'Old version that is archived for reference');

-- Create table for execution statuses
CREATE TABLE execution_statuses (
                                    id VARCHAR(20) NOT NULL PRIMARY KEY,
                                    description VARCHAR(200) NOT NULL,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert standard execution statuses
INSERT INTO execution_statuses (id, description) VALUES
                                                     ('SUCCESS', 'Successful execution'),
                                                     ('ERROR', 'Failed with error'),
                                                     ('TIMEOUT', 'Execution timed out'),
                                                     ('INVALID_PARAMS', 'Failed due to invalid parameters'),
                                                     ('PROVIDER_ERROR', 'Provider service returned an error'),
                                                     ('RATE_LIMITED', 'Request was rate limited');