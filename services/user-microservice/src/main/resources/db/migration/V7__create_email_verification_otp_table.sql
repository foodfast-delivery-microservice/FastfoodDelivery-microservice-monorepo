CREATE TABLE IF NOT EXISTS email_verification_otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    attempts INT NOT NULL,
    max_attempts INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_verification_otp_email_type_status
    ON email_verification_otp (email, type, status);

CREATE INDEX IF NOT EXISTS idx_email_verification_otp_user_type
    ON email_verification_otp (user_id, type);

