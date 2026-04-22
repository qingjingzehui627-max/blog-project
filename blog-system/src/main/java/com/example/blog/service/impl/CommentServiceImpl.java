package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Comment;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.service.CommentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        return baseMapper.findByPostId(postId);
    }

    @Override
    public void createComment(Comment comment) {
        comment.setCreatedAt(LocalDateTime.now());
        baseMapper.insert(comment);
    }
}