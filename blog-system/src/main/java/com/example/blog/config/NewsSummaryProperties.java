package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 新闻 AI 摘要配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.news-summary")
public class NewsSummaryProperties {

    /** 是否启用新闻 AI 摘要。 */
    private boolean enabled = true;

    /** 是否启用定时自动摘要。 */
    private boolean autoEnabled = true;

    /** 每天自动生成的热度新闻数量。 */
    private int autoTopLimit = 10;

    /** 每天允许生成的摘要总数。 */
    private int dailyLimit = 50;

    /** 摘要任务定时表达式。 */
    private String cron = "0 40 6 * * ?";

    /** 摘要任务执行时区。 */
    private String zone = "Asia/Shanghai";

    /** 摘要模型温度。 */
    private double temperature = 0.3D;

    /** 正文截断长度，避免请求过大。 */
    private int contentMaxLength = 2000;

    /** 失败后的最大重试次数。 */
    private int maxRetries = 1;

    /** 提示词版本号，方便后续迭代。 */
    private String promptVersion = "news-summary-v1";
}
