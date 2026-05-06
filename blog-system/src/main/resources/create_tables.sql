-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 创建文章表
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    cover VARCHAR(255),
    category_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    view_count INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 创建评论表
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES comments(id)
);

-- 创建点赞表
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 创建收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

-- 创建标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 创建文章标签关联表
CREATE TABLE IF NOT EXISTS post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);

-- 创建媒体文件表
CREATE TABLE IF NOT EXISTS media_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    type VARCHAR(10) NOT NULL,
    size BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 插入默认分类
INSERT INTO categories (name)
VALUES ('技术'), ('生活'), ('工作'), ('学习')
ON DUPLICATE KEY UPDATE name = name;

-- 插入默认标签
INSERT INTO tags (name)
VALUES ('Java'), ('Spring Boot'), ('Vue'), ('MySQL'), ('Redis')
ON DUPLICATE KEY UPDATE name = name;

-- 聚合新闻主表
CREATE TABLE IF NOT EXISTS news_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(32) NOT NULL DEFAULT 'guardian',
    source_content_id VARCHAR(255) NOT NULL,
    primary_provider VARCHAR(32) NOT NULL,
    section_id VARCHAR(100),
    section_name VARCHAR(100),
    pillar_id VARCHAR(100),
    pillar_name VARCHAR(100),
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500),
    title_hash VARCHAR(64),
    summary TEXT,
    content MEDIUMTEXT,
    author VARCHAR(255),
    web_url VARCHAR(500) NOT NULL,
    normalized_url VARCHAR(500),
    url_hash VARCHAR(64),
    api_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    published_at DATETIME NOT NULL,
    fetch_date DATE NOT NULL,
    rank_order INT,
    source_count INT NOT NULL DEFAULT 1,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    keyword_tags VARCHAR(500),
    lang VARCHAR(20) DEFAULT 'en',
    status TINYINT NOT NULL DEFAULT 1,
    raw_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_fetch_date_rank (fetch_date, rank_order),
    KEY idx_category_date (category_code, fetch_date),
    KEY idx_published_at (published_at),
    KEY idx_status_published (status, published_at),
    KEY idx_fetch_url_hash (fetch_date, url_hash),
    KEY idx_fetch_title_hash (fetch_date, title_hash)
);

-- 新闻来源明细表
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

-- 新闻抓取任务日志
CREATE TABLE IF NOT EXISTS news_fetch_job_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(32) NOT NULL,
    job_date DATE NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    fetched_count INT NOT NULL DEFAULT 0,
    inserted_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    finished_at DATETIME,
    error_message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_source_job_date (source, job_date),
    KEY idx_status_started_at (status, started_at)
);

-- 新闻 AI 摘要表
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

-- 新闻分类规则表
CREATE TABLE IF NOT EXISTS news_category_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(50) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    include_keywords TEXT NOT NULL,
    exclude_keywords TEXT,
    priority INT NOT NULL DEFAULT 100,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_code (category_code)
);

INSERT INTO news_category_rules (
    category_code,
    category_name,
    include_keywords,
    exclude_keywords,
    priority,
    enabled
)
VALUES
    ('MODEL', '大模型', 'openai,gpt,anthropic,claude,gemini,llama,large language model,llm,foundation model', '', 10, 1),
    ('RESEARCH', 'AI研究', 'research,paper,benchmark,multimodal,reasoning,agentic,training,alignment', '', 20, 1),
    ('CHIP', '算力芯片', 'nvidia,gpu,semiconductor,chip,cuda,datacenter,inference chip', '', 30, 1),
    ('PRODUCT', 'AI产品', 'copilot,assistant,chatbot,ai tool,product launch,release', '', 40, 1),
    ('POLICY', '政策监管', 'regulation,regulator,law,privacy,copyright,safety,governance', '', 50, 1),
    ('STARTUP', '投融资', 'startup,funding,investment,venture capital,acquisition,merger', '', 60, 1),
    ('GENERAL_TECH', '科技综合', 'technology,software,cloud,developer,platform,app', '', 90, 1)
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    include_keywords = VALUES(include_keywords),
    exclude_keywords = VALUES(exclude_keywords),
    priority = VALUES(priority),
    enabled = VALUES(enabled);
