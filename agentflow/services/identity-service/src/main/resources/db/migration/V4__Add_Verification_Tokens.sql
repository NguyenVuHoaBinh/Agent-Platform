-- Create verification tokens table
CREATE TABLE verification_tokens (
                                     id VARCHAR(36) NOT NULL PRIMARY KEY,
                                     token VARCHAR(255) NOT NULL,
                                     user_id VARCHAR(36) NOT NULL,
                                     token_type VARCHAR(50) NOT NULL,
                                     created_at TIMESTAMP NOT NULL,
                                     expires_at TIMESTAMP NOT NULL,
                                     used BOOLEAN NOT NULL DEFAULT FALSE,
                                     used_at TIMESTAMP,
                                     FOREIGN KEY (user_id) REFERENCES users(id),
                                     UNIQUE KEY uk_token (token)
);

-- Create index for faster token lookups
CREATE INDEX idx_verification_token ON verification_tokens(token);

-- Create index for finding tokens by user and type
CREATE INDEX idx_verification_user_type ON verification_tokens(user_id, token_type);