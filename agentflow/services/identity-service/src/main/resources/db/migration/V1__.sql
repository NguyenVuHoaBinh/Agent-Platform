
-- Permissions Table
CREATE TABLE IF NOT EXISTS permissions (
                                           id VARCHAR(36) PRIMARY KEY,
                                           name VARCHAR(50) NOT NULL UNIQUE,
                                           description TEXT,
                                           created_at TIMESTAMP,
                                           updated_at TIMESTAMP
);

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
                                     id VARCHAR(36) PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE,
                                     description TEXT,
                                     created_at TIMESTAMP,
                                     updated_at TIMESTAMP
);

-- Role Permissions Junction Table
CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id VARCHAR(36) NOT NULL,
                                                permission_id VARCHAR(36) NOT NULL,
                                                PRIMARY KEY (role_id, permission_id),
                                                FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                                                FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- Users Table
CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(36) PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
                                     email VARCHAR(100) NOT NULL UNIQUE,
                                     password_hash VARCHAR(255) NOT NULL,
                                     first_name VARCHAR(50),
                                     last_name VARCHAR(50),
                                     enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                     account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
                                     account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
                                     credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
                                     created_at TIMESTAMP,
                                     updated_at TIMESTAMP
);

-- User Roles Junction Table
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id VARCHAR(36) NOT NULL,
                                          role_id VARCHAR(36) NOT NULL,
                                          PRIMARY KEY (user_id, role_id),
                                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                          FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Organizations Table
CREATE TABLE IF NOT EXISTS organizations (
                                             id VARCHAR(36) PRIMARY KEY,
                                             name VARCHAR(100) NOT NULL,
                                             description TEXT,
                                             created_at TIMESTAMP,
                                             updated_at TIMESTAMP
);

-- User Organizations Junction Table
CREATE TABLE IF NOT EXISTS user_organizations (
                                                  user_id VARCHAR(36) NOT NULL,
                                                  organization_id VARCHAR(36) NOT NULL,
                                                  PRIMARY KEY (user_id, organization_id),
                                                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                                  FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Projects Table
CREATE TABLE IF NOT EXISTS projects (
                                        id VARCHAR(36) PRIMARY KEY,
                                        name VARCHAR(100) NOT NULL,
                                        description TEXT,
                                        organization_id VARCHAR(36) NOT NULL,
                                        created_at TIMESTAMP,
                                        updated_at TIMESTAMP,
                                        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- User Projects Junction Table
CREATE TABLE IF NOT EXISTS user_projects (
                                             user_id VARCHAR(36) NOT NULL,
                                             project_id VARCHAR(36) NOT NULL,
                                             PRIMARY KEY (user_id, project_id),
                                             FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                             FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Insert default roles
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES
    (UUID(), 'ROLE_ADMIN', 'Administrator role with full system access', NOW(), NOW()),
    (UUID(), 'ROLE_MANAGER', 'Manager role with organization management access', NOW(), NOW()),
    (UUID(), 'ROLE_USER', 'Standard user role with basic access', NOW(), NOW());

-- Insert default permissions
INSERT INTO permissions (id, name, description, created_at, updated_at)
VALUES
    (UUID(), 'READ_USERS', 'Permission to read user information', NOW(), NOW()),
    (UUID(), 'WRITE_USERS', 'Permission to create and update users', NOW(), NOW()),
    (UUID(), 'READ_ORGANIZATIONS', 'Permission to read organization information', NOW(), NOW()),
    (UUID(), 'WRITE_ORGANIZATIONS', 'Permission to create and update organizations', NOW(), NOW()),
    (UUID(), 'READ_PROJECTS', 'Permission to read project information', NOW(), NOW()),
    (UUID(), 'WRITE_PROJECTS', 'Permission to create and update projects', NOW(), NOW());

-- Assign permissions to admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN';

-- Assign read permissions to manager role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_MANAGER' AND (p.name LIKE 'READ_%' OR p.name LIKE 'WRITE_%');

-- Assign basic permissions to user role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name LIKE 'READ_%';