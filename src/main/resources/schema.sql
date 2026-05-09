CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(255) NOT NULL ,
    hashed_password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) UNIQUE,
    is_guest   BOOL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS saved_result (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url  VARCHAR(1024),
    json_data  CLOB,
    user_id BIGINT,
    category   VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);