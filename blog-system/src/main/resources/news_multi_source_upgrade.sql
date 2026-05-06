-- 将原有单来源新闻表升级为多数据源聚合结构
ALTER TABLE news_articles
    ADD COLUMN primary_provider VARCHAR(32) NOT NULL DEFAULT 'guardian' AFTER source_content_id,
    ADD COLUMN normalized_title VARCHAR(500) NULL AFTER title,
    ADD COLUMN title_hash VARCHAR(64) NULL AFTER normalized_title,
    ADD COLUMN normalized_url VARCHAR(500) NULL AFTER web_url,
    ADD COLUMN url_hash VARCHAR(64) NULL AFTER normalized_url,
    ADD COLUMN source_count INT NOT NULL DEFAULT 1 AFTER rank_order;

ALTER TABLE news_articles
    DROP INDEX uk_source_content;

CREATE INDEX idx_fetch_url_hash ON news_articles (fetch_date, url_hash);
CREATE INDEX idx_fetch_title_hash ON news_articles (fetch_date, title_hash);

CREATE TABLE IF NOT EXISTS news_article_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    provider_code VARCHAR(32) NOT NULL,
    source_content_id VARCHAR(255) NOT NULL,
    source_url VARCHAR(500),
    normalized_url VARCHAR(500),
    url_hash VARCHAR(64),
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500),
    title_hash VARCHAR(64),
    published_at DATETIME NOT NULL,
    fetch_date DATE NOT NULL,
    raw_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider_source_date (provider_code, source_content_id, fetch_date),
    KEY idx_article_id (article_id),
    KEY idx_fetch_url_hash (fetch_date, url_hash),
    KEY idx_fetch_title_hash (fetch_date, title_hash),
    CONSTRAINT fk_news_article_sources_article
        FOREIGN KEY (article_id) REFERENCES news_articles(id)
);
