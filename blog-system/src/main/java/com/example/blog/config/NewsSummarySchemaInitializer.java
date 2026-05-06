package com.example.blog.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 新闻 AI 摘要表初始化器。
 */
@Component
public class NewsSummarySchemaInitializer implements ApplicationRunner {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 启动时自动补齐新闻 AI 摘要表结构，避免首次联调时因缺表报错。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS news_ai_summary (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "article_id BIGINT NOT NULL," +
                        "fetch_date DATE NOT NULL," +
                        "summary_text TEXT," +
                        "summary_status VARCHAR(20) NOT NULL," +
                        "trigger_type VARCHAR(20) NOT NULL," +
                        "model_name VARCHAR(100)," +
                        "prompt_version VARCHAR(50)," +
                        "generated_at DATETIME," +
                        "retry_count INT NOT NULL DEFAULT 0," +
                        "error_message VARCHAR(1000)," +
                        "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "UNIQUE KEY uk_article_id (article_id)," +
                        "KEY idx_fetch_date_status (fetch_date, summary_status)," +
                        "KEY idx_generated_at (generated_at)," +
                        "CONSTRAINT fk_news_ai_summary_article FOREIGN KEY (article_id) REFERENCES news_articles(id)" +
                        ")"
        );
    }
}
