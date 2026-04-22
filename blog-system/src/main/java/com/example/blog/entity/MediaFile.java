package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media_files")
public class MediaFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String type;
    private Long size;
    private Long userId;
    private LocalDateTime createdAt;
}