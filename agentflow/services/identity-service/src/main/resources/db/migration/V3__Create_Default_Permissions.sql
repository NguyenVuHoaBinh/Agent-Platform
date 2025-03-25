-- Create default permissions
INSERT INTO permissions (id, name, description, resource, action)
VALUES
    -- User permissions
    (UUID(), 'user:create', 'Create new users', 'user', 'create'),
    (UUID(), 'user:read', 'Read user information', 'user', 'read'),
    (UUID(), 'user:update', 'Update user information', 'user', 'update'),
    (UUID(), 'user:delete', 'Delete users', 'user', 'delete'),
    (UUID(), 'user:list', 'List users', 'user', 'list'),

    -- Organization permissions
    (UUID(), 'organization:create', 'Create organizations', 'organization', 'create'),
    (UUID(), 'organization:read', 'View organization details', 'organization', 'read'),
    (UUID(), 'organization:update', 'Update organization information', 'organization', 'update'),
    (UUID(), 'organization:delete', 'Delete organizations', 'organization', 'delete'),
    (UUID(), 'organization:list', 'List organizations', 'organization', 'list'),

    -- Project permissions
    (UUID(), 'project:create', 'Create projects', 'project', 'create'),
    (UUID(), 'project:read', 'View project details', 'project', 'read'),
    (UUID(), 'project:update', 'Update project information', 'project', 'update'),
    (UUID(), 'project:delete', 'Delete projects', 'project', 'delete'),
    (UUID(), 'project:list', 'List projects', 'project', 'list'),

    -- Role permissions
    (UUID(), 'role:create', 'Create roles', 'role', 'create'),
    (UUID(), 'role:read', 'View role details', 'role', 'read'),
    (UUID(), 'role:update', 'Update roles', 'role', 'update'),
    (UUID(), 'role:delete', 'Delete roles', 'role', 'delete'),
    (UUID(), 'role:list', 'List roles', 'role', 'list'),

    -- Team permissions
    (UUID(), 'team:create', 'Create teams', 'team', 'create'),
    (UUID(), 'team:read', 'View team details', 'team', 'read'),
    (UUID(), 'team:update', 'Update team information', 'team', 'update'),
    (UUID(), 'team:delete', 'Delete teams', 'team', 'delete'),
    (UUID(), 'team:list', 'List teams', 'team', 'list'),

    -- Invitation permissions
    (UUID(), 'invitation:create', 'Create invitations', 'invitation', 'create'),
    (UUID(), 'invitation:read', 'View invitation details', 'invitation', 'read'),
    (UUID(), 'invitation:delete', 'Cancel invitations', 'invitation', 'delete'),
    (UUID(), 'invitation:list', 'List invitations', 'invitation', 'list'),

    -- Audit permissions
    (UUID(), 'audit:read', 'View audit logs', 'audit', 'read'),
    (UUID(), 'audit:list', 'List audit logs', 'audit', 'list');

-- Assign permissions to roles

-- Get role IDs
SET @super_admin_id = (SELECT id FROM roles WHERE name = 'SUPER_ADMIN');
SET @system_admin_id = (SELECT id FROM roles WHERE name = 'SYSTEM_ADMIN');
SET @org_admin_id = (SELECT id FROM roles WHERE name = 'ORGANIZATION_ADMIN');
SET @org_member_id = (SELECT id FROM roles WHERE name = 'ORGANIZATION_MEMBER');
SET @project_admin_id = (SELECT id FROM roles WHERE name = 'PROJECT_ADMIN');
SET @project_member_id = (SELECT id FROM roles WHERE name = 'PROJECT_MEMBER');
SET @guest_id = (SELECT id FROM roles WHERE name = 'GUEST');

-- Super Admin gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @super_admin_id, id FROM permissions;

-- System Admin gets most permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @system_admin_id, id FROM permissions WHERE name NOT IN ('role:delete', 'role:create');

-- Organization Admin permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @org_admin_id, id FROM permissions WHERE
    name IN (
             'user:read', 'user:list',
             'organization:read', 'organization:update',
             'project:create', 'project:read', 'project:update', 'project:delete', 'project:list',
             'team:create', 'team:read', 'team:update', 'team:delete', 'team:list',
             'invitation:create', 'invitation:read', 'invitation:delete', 'invitation:list',
             'audit:read', 'audit:list'
        );

-- Organization Member permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @org_member_id, id FROM permissions WHERE
    name IN (
             'user:read',
             'organization:read',
             'project:read', 'project:list',
             'team:read', 'team:list'
        );

-- Project Admin permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @project_admin_id, id FROM permissions WHERE
    name IN (
             'user:read', 'user:list',
             'project:read', 'project:update',
             'team:create', 'team:read', 'team:update', 'team:list',
             'invitation:create', 'invitation:read', 'invitation:list'
        );

-- Project Member permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @project_member_id, id FROM permissions WHERE
    name IN (
             'user:read',
             'project:read',
             'team:read', 'team:list'
        );

-- Guest permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @guest_id, id FROM permissions WHERE
    name IN (
             'organization:read',
             'project:read'
        );