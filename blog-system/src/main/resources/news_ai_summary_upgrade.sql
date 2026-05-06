CREATE TABLE IF NOT EXISTS news_ai_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    fetch_date DATE NOT NULL,
    summary_text TEXT,
    summary_status VARCHAR(20) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    model_name VARCHAR(100),
    prompt_version VARCHAR(50),
    generated_at DATETIME,
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_id (article_id),
    KEY idx_fetch_date_status (fetch_date, summary_status),
    KEY idx_generated_at (generated_at),
    CONSTRAINT fk_news_ai_summary_article
        FOREIGN KEY (article_id) REFERENCES news_articles(id)
);
