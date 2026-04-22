package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("post_tags")
public class PostTag {
    @TableField("post_id")
    private Long postId;
    @TableField("tag_id")
    private Long tagId;
}