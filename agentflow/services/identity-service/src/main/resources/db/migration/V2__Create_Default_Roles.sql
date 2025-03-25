-- Insert default roles
INSERT INTO roles (id, name, description, scope, created_at, updated_at)
VALUES
    (UUID(), 'SUPER_ADMIN', 'Super administrator with full system access', 'GLOBAL', NOW(), NOW()),
    (UUID(), 'SYSTEM_ADMIN', 'System administrator with platform-wide administrative permissions', 'GLOBAL', NOW(), NOW()),
    (UUID(), 'ORGANIZATION_ADMIN', 'Organization administrator with full access to organization resources', 'ORGANIZATION', NOW(), NOW()),
    (UUID(), 'ORGANIZATION_MEMBER', 'Regular organization member', 'ORGANIZATION', NOW(), NOW()),
    (UUID(), 'PROJECT_ADMIN', 'Project administrator with full access to project resources', 'PROJECT', NOW(), NOW()),
    (UUID(), 'PROJECT_MEMBER', 'Regular project member', 'PROJECT', NOW(), NOW()),
    (UUID(), 'API_CLIENT', 'Machine-to-machine API client', 'GLOBAL', NOW(), NOW()),
    (UUID(), 'GUEST', 'Guest with limited read access', 'ORGANIZATION', NOW(), NOW());