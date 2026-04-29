package com.example.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 博客系统启动入口。
 */
@SpringBootApplication
@MapperScan("com.example.blog.mapper")
@EnableScheduling
public class BlogSystemApplication {
    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BlogSystemApplication.class, args);
    }
}
