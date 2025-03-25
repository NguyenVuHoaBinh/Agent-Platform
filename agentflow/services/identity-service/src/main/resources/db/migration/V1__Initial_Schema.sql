-- users table
CREATE TABLE users (
                       id VARCHAR(36) NOT NULL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       status VARCHAR(20) NOT NULL,
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                       mfa_secret VARCHAR(255),
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,
                       last_login_at TIMESTAMP,
                       UNIQUE KEY uk_email (email),
                       UNIQUE KEY uk_username (username)
);

-- organizations table
CREATE TABLE organizations (
                               id VARCHAR(36) NOT NULL PRIMARY KEY,
                               name VARCHAR(255) NOT NULL,
                               display_name VARCHAR(255) NOT NULL,
                               description VARCHAR(1000),
                               status VARCHAR(20) NOT NULL,
                               parent_id VARCHAR(36),
                               created_at TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP NOT NULL,
                               FOREIGN KEY (parent_id) REFERENCES organizations(id),
                               UNIQUE KEY uk_name (name)
);

-- projects table
CREATE TABLE projects (
                          id VARCHAR(36) NOT NULL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          display_name VARCHAR(255) NOT NULL,
                          description VARCHAR(1000),
                          status VARCHAR(20) NOT NULL,
                          organization_id VARCHAR(36) NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,
                          FOREIGN KEY (organization_id) REFERENCES organizations(id),
                          UNIQUE KEY uk_org_name (organization_id, name)
);

-- roles table
CREATE TABLE roles (
                       id VARCHAR(36) NOT NULL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description VARCHAR(1000),
                       scope VARCHAR(20) NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,
                       UNIQUE KEY uk_name (name)
);

-- permissions table
CREATE TABLE permissions (
                             id VARCHAR(36) NOT NULL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             description VARCHAR(1000),
                             resource VARCHAR(255) NOT NULL,
                             action VARCHAR(255) NOT NULL,
                             UNIQUE KEY uk_name (name)
);

-- role_permissions table
CREATE TABLE role_permissions (
                                  role_id VARCHAR(36) NOT NULL,
                                  permission_id VARCHAR(36) NOT NULL,
                                  PRIMARY KEY (role_id, permission_id),
                                  FOREIGN KEY (role_id) REFERENCES roles(id),
                                  FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- user_roles table
CREATE TABLE user_roles (
                            id VARCHAR(36) NOT NULL PRIMARY KEY,
                            user_id VARCHAR(36) NOT NULL,
                            role_id VARCHAR(36) NOT NULL,
                            organization_id VARCHAR(36),
                            project_id VARCHAR(36),
                            assigned_at TIMESTAMP NOT NULL,
                            expires_at TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id),
                            FOREIGN KEY (role_id) REFERENCES roles(id),
                            FOREIGN KEY (organization_id) REFERENCES organizations(id),
                            FOREIGN KEY (project_id) REFERENCES projects(id),
                            UNIQUE KEY uk_user_role_org_proj (user_id, role_id, organization_id, project_id)
);

-- teams table
CREATE TABLE teams (
                       id VARCHAR(36) NOT NULL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description VARCHAR(1000),
                       organization_id VARCHAR(36) NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,
                       FOREIGN KEY (organization_id) REFERENCES organizations(id),
                       UNIQUE KEY uk_org_name (organization_id, name)
);

-- team_members table
CREATE TABLE team_members (
                              team_id VARCHAR(36) NOT NULL,
                              user_id VARCHAR(36) NOT NULL,
                              role VARCHAR(50) NOT NULL,
                              joined_at TIMESTAMP NOT NULL,
                              PRIMARY KEY (team_id, user_id),
                              FOREIGN KEY (team_id) REFERENCES teams(id),
                              FOREIGN KEY (user_id) REFERENCES users(id)
);

-- invitations table
CREATE TABLE invitations (
                             id VARCHAR(36) NOT NULL PRIMARY KEY,
                             email VARCHAR(255) NOT NULL,
                             organization_id VARCHAR(36) NOT NULL,
                             project_id VARCHAR(36),
                             team_id VARCHAR(36),
                             role_id VARCHAR(36) NOT NULL,
                             token VARCHAR(255) NOT NULL,
                             status VARCHAR(20) NOT NULL,
                             created_by VARCHAR(36) NOT NULL,
                             created_at TIMESTAMP NOT NULL,
                             expires_at TIMESTAMP NOT NULL,
                             FOREIGN KEY (organization_id) REFERENCES organizations(id),
                             FOREIGN KEY (project_id) REFERENCES projects(id),
                             FOREIGN KEY (team_id) REFERENCES teams(id),
                             FOREIGN KEY (role_id) REFERENCES roles(id),
                             FOREIGN KEY (created_by) REFERENCES users(id),
                             UNIQUE KEY uk_token (token)
);

-- audit_logs table
CREATE TABLE audit_logs (
                            id VARCHAR(36) NOT NULL PRIMARY KEY,
                            user_id VARCHAR(36),
                            event_type VARCHAR(100) NOT NULL,
                            resource_type VARCHAR(100),
                            resource_id VARCHAR(255),
                            action VARCHAR(100) NOT NULL,
                            ip_address VARCHAR(50),
                            user_agent VARCHAR(500),
                            additional_data JSON,
                            timestamp TIMESTAMP NOT NULL,
                            INDEX idx_user_id (user_id),
                            INDEX idx_event_type (event_type),
                            INDEX idx_timestamp (timestamp)
);

-- oauth_clients table
CREATE TABLE oauth_clients (
                               id VARCHAR(36) NOT NULL PRIMARY KEY,
                               client_id VARCHAR(255) NOT NULL,
                               client_secret VARCHAR(255) NOT NULL,
                               redirect_uris TEXT NOT NULL,
                               grant_types VARCHAR(255) NOT NULL,
                               scopes VARCHAR(255) NOT NULL,
                               user_id VARCHAR(36),
                               created_at TIMESTAMP NOT NULL,
                               updated_at TIMESTAMP NOT NULL,
                               UNIQUE KEY uk_client_id (client_id),
                               FOREIGN KEY (user_id) REFERENCES users(id)
);

-- refresh_tokens table
CREATE TABLE refresh_tokens (
                                id VARCHAR(36) NOT NULL PRIMARY KEY,
                                token_id VARCHAR(255) NOT NULL,
                                user_id VARCHAR(36) NOT NULL,
                                client_id VARCHAR(255),
                                issued_at TIMESTAMP NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                UNIQUE KEY uk_token_id (token_id),
                                FOREIGN KEY (user_id) REFERENCES users(id)
);

-- token_blacklist table
CREATE TABLE token_blacklist (
                                 token_id VARCHAR(255) NOT NULL PRIMARY KEY,
                                 expires_at TIMESTAMP NOT NULL,
                                 INDEX idx_expires_at (expires_at)
);

-- backup_codes table
CREATE TABLE backup_codes (
                              id VARCHAR(36) NOT NULL PRIMARY KEY,
                              user_id VARCHAR(36) NOT NULL,
                              code VARCHAR(255) NOT NULL,
                              used BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMP NOT NULL,
                              used_at TIMESTAMP,
                              FOREIGN KEY (user_id) REFERENCES users(id),
                              UNIQUE KEY uk_user_code (user_id, code)
);

-- user_consents table
CREATE TABLE user_consents (
                               id VARCHAR(36) NOT NULL PRIMARY KEY,
                               user_id VARCHAR(36) NOT NULL,
                               consent_type VARCHAR(100) NOT NULL,
                               consent_version VARCHAR(50) NOT NULL,
                               granted BOOLEAN NOT NULL,
                               granted_at TIMESTAMP NOT NULL,
                               expires_at TIMESTAMP,
                               data_categories TEXT,
                               FOREIGN KEY (user_id) REFERENCES users(id),
                               UNIQUE KEY uk_user_consent_type (user_id, consent_type, consent_version)
);