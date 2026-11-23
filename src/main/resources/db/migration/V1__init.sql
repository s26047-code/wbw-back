CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    google_sub   VARCHAR(255) NOT NULL,
    email        VARCHAR(320) NOT NULL,
    name         VARCHAR(100) NOT NULL,
    picture_url  VARCHAR(1000),
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_google_sub UNIQUE (google_sub)
);

CREATE TABLE user_settings (
    user_id            BIGINT      NOT NULL,
    theme              VARCHAR(20) NOT NULL,
    background_color   VARCHAR(7),
    text_color         VARCHAR(7),
    font_family        VARCHAR(50) NOT NULL,
    font_size          INT         NOT NULL,
    letter_spacing     DOUBLE      NOT NULL,
    line_height        DOUBLE      NOT NULL,
    paragraph_spacing  DOUBLE      NOT NULL,
    editor_width       INT         NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE folders (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    name        VARCHAR(50) NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_folders_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_folders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE documents (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id               BIGINT       NOT NULL,
    folder_id             BIGINT,
    title                 VARCHAR(200) NOT NULL,
    content               LONGTEXT     NOT NULL,
    plain_text            LONGTEXT     NOT NULL,
    preview               VARCHAR(200) NOT NULL,
    chars_with_spaces     INT          NOT NULL,
    chars_without_spaces  INT          NOT NULL,
    version               BIGINT       NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_documents_folder FOREIGN KEY (folder_id) REFERENCES folders (id) ON DELETE CASCADE
);

CREATE INDEX idx_documents_user_updated ON documents (user_id, updated_at);
CREATE INDEX idx_documents_user_folder ON documents (user_id, folder_id);

CREATE TABLE shares (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    document_id    BIGINT       NOT NULL,
    token          VARCHAR(64)  NOT NULL,
    password_hash  VARCHAR(100),
    enabled        BOOLEAN      NOT NULL,
    expires_at     DATETIME(6),
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shares_token UNIQUE (token),
    CONSTRAINT fk_shares_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE
);
