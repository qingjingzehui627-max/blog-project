package com.example.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.entity.Comment;

import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {
    List<Comment> findByPostId(Long postId);
}