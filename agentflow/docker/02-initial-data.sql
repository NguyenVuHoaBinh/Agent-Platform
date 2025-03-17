USE agentflow;

-- Insert initial roles
INSERT INTO roles (id, name, description)
VALUES
    ('1', 'ROLE_ADMIN', 'Administrator with full access'),
    ('2', 'ROLE_USER', 'Regular user with limited access'),
    ('3', 'ROLE_MANAGER', 'Project manager with team access')
    ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert initial permissions
INSERT INTO permissions (id, name, description)
VALUES
    ('1', 'READ_USERS', 'Can view user information'),
    ('2', 'WRITE_USERS', 'Can create and modify users'),
    ('3', 'READ_PROJECTS', 'Can view projects'),
    ('4', 'WRITE_PROJECTS', 'Can create and modify projects'),
    ('5', 'READ_ORGANIZATIONS', 'Can view organizations'),
    ('6', 'WRITE_ORGANIZATIONS', 'Can create and modify organizations'),
    ('7', 'READ_PROMPTS', 'Can view prompts'),
    ('8', 'WRITE_PROMPTS', 'Can create and modify prompts'),
    ('9', 'READ_FLOWS', 'Can view flows'),
    ('10', 'WRITE_FLOWS', 'Can create and modify flows'),
    ('11', 'READ_AGENTS', 'Can view agents'),
    ('12', 'WRITE_AGENTS', 'Can create and modify agents'),
    ('13', 'READ_INTEGRATIONS', 'Can view integrations'),
    ('14', 'WRITE_INTEGRATIONS', 'Can create and modify integrations')
    ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Associate permissions with roles
INSERT INTO role_permissions (role_id, permission_id)
VALUES
-- Admin has all permissions
('1', '1'), ('1', '2'), ('1', '3'), ('1', '4'), ('1', '5'), ('1', '6'),
('1', '7'), ('1', '8'), ('1', '9'), ('1', '10'), ('1', '11'), ('1', '12'),
('1', '13'), ('1', '14'),
-- User has read permissions
('2', '1'), ('2', '3'), ('2', '5'), ('2', '7'), ('2', '9'), ('2', '11'), ('2', '13'),
-- Manager has read all and write some
('3', '1'), ('3', '3'), ('3', '4'), ('3', '5'), ('3', '7'), ('3', '8'),
('3', '9'), ('3', '10'), ('3', '11'), ('3', '12'), ('3', '13')
    ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Create default admin user (password: admin123)
INSERT INTO users (id, username, email, password_hash, first_name, last_name)
VALUES ('1', 'admin', 'admin@agentflow.com', '$2a$10$MuLvMeh/VfTvfNqzvM.UVeSJQHUBThhxr0jVtFk/ZoLA7o6d1JaCW', 'Admin', 'User')
    ON DUPLICATE KEY UPDATE username = VALUES(username);

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id)
VALUES ('1', '1')
    ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- Create default organization
INSERT INTO organizations (id, name, description)
VALUES ('1', 'Default Organization', 'Default organization for AgentFlow platform')
    ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Associate admin with default organization
INSERT INTO user_organizations (user_id, organization_id)
VALUES ('1', '1')
    ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- Create default project
INSERT INTO projects (id, name, description, organization_id)
VALUES ('1', 'Default Project', 'Default project for AgentFlow platform', '1')
    ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Associate admin with default project
INSERT INTO user_projects (user_id, project_id)
VALUES ('1', '1')
    ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);