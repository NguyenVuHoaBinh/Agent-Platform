
-- Password Reset Tokens Table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Create index for token lookups
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);

-- Create index for user lookups
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);