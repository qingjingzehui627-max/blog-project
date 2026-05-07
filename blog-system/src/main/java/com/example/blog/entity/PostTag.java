package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章与标签的关联实体。
 * 该表在数据库中使用联合主键，MyBatis-Plus 启动阶段仍需要识别出一个主键字段，
 * 因此这里将 postId 标记为输入型主键，避免元数据初始化时报错。
 */
@Data
@TableName("post_tags")
public class PostTag {

    /**
     * 文章 ID。
     */
    @TableId(value = "post_id", type = IdType.INPUT)
    private Long postId;

    /**
     * 标签 ID。
     */
    @TableField("tag_id")
    private Long tagId;
}
