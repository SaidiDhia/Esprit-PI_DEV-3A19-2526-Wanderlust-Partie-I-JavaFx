-- User Table
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone_number VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    role VARCHAR(50) NOT NULL, -- ADMIN, HOST, PARTICIPANT
    tfa_method VARCHAR(50), -- FACE, QR, EMAIL
    created_at DATETIME,
    profile_picture VARCHAR(255)
);

-- Password Reset Token Table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- TFA Secret Table
CREATE TABLE IF NOT EXISTS tfa_secrets (
    user_id VARCHAR(36) PRIMARY KEY,
    secret_key VARCHAR(255) NOT NULL,
    qr_code TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Activity Log Table
CREATE TABLE IF NOT EXISTS activity_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
