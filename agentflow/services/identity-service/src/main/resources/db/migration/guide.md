# Database Schema Migration Guidelines

## Overview

This document outlines the guidelines for managing database schema migrations in the AgentFlow Identity Service using Flyway.

## Migration Basics

### Naming Convention

All migration scripts must follow this naming pattern:

```
V{version}__{description}.sql
```

- `{version}`: Migration version number (e.g., 1, 2, 3.1, 4.5)
- `{description}`: Brief description with underscores instead of spaces (e.g., Initial_Schema, Add_User_Fields)

Examples:
- `V1__Initial_Schema.sql`
- `V2__Add_Password_Reset_Tokens.sql`
- `V2.1__Alter_User_Fields.sql`

### File Location

All migration scripts should be placed in:

```
src/main/resources/db/migration/
```

## Migration Development Process

### 1. Create a New Migration Script

When you need to make changes to the database schema:

1. Determine the next version number based on existing migrations
2. Create a new SQL file following the naming convention
3. Write SQL statements for your changes (see Guidelines below)
4. Test the migration in development environment

### 2. Guidelines for Writing Migrations

- **Always use IF NOT EXISTS** when creating new tables/indexes to make scripts more resilient
- **Always use explicit names** for constraints and indexes
- **Always include statements to roll back** any failed changes if possible
- **Organize scripts by entity** and include clear comments

Example:
```sql
-- Create new table
CREATE TABLE IF NOT EXISTS example_table (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add index
CREATE INDEX idx_example_table_name ON example_table(name);
```

### 3. Testing Migrations

Before committing:

1. Run the migration on a clean database to ensure it works properly
2. Verify the changes match your intended database schema
3. Ensure application functionality works properly with the new schema

## Managing Migrations in Different Environments

### Development
- Profile: `dev`
- Behavior: Clean database and run all migrations on application start
- Use for local development only

### Testing
- Profile: `test`
- Behavior: Clean database and run all migrations for tests
- Used for automated tests

### Staging & Production
- Profile: `staging`, `prod`
- Behavior: Only apply new migrations, never clean database
- Always backup database before applying migrations

## Handling Common Scenarios

### Adding a new table
Create a new migration script with CREATE TABLE statements.

### Adding a new column to an existing table
```sql
ALTER TABLE table_name 
ADD COLUMN column_name VARCHAR(100) NULL;
```

### Modifying a column
```sql
ALTER TABLE table_name 
MODIFY COLUMN column_name VARCHAR(200) NOT NULL;
```

### Adding data
```sql
INSERT INTO table_name (column1, column2) 
VALUES ('value1', 'value2');
```

## Troubleshooting

### Migration checksum error
If you get a checksum error, it means a previously applied migration file has been changed. Never modify existing migration files that have been applied to any environment. Instead, create a new migration to make the needed changes.

### Failed migration
If a migration fails, Flyway will mark the schema as "failed". To fix:
1. Fix the issue in your migration script
2. Run `flyway repair` (through Spring Boot: `spring.flyway.repair=true`)
3. Restart the application

## Best Practices

1. **Never modify existing migrations** that have been applied to any environment
2. **Keep migrations small and focused** on a single change or closely related changes
3. **Always use transactions** for related changes that must succeed or fail together
4. **Add comments** to explain complex changes
5. **Coordinate migrations** with application code changes
6. **Test thoroughly** before applying to production