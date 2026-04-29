package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 新闻抓取任务日志实体。
 */
@Data
@TableName("news_fetch_job_log")
public class NewsFetchJobLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String source;
    private LocalDate jobDate;
    private String triggerType;
    private String status;
    private Integer requestCount;
    private Integer fetchedCount;
    private Integer insertedCount;
    private Integer updatedCount;
    private Integer failedCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
}
