package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Post;
import com.example.blog.mapper.PostMapper;
import com.example.blog.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Override
    public List<Post> getPosts(int page, int size, String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                        .orderByDesc(Post::getCreatedAt);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Post::getTitle, keyword)
                    .or()
                    .like(Post::getContent, keyword));
        }

        return baseMapper.selectList(
                queryWrapper.last("LIMIT " + (page - 1) * size + ", " + size)
        );
    }

    @Override
    public Post getPostById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public void createPost(Post post) {
        post.setCreatedAt(LocalDateTime.now());
        post.setViewCount(0);
        baseMapper.insert(post);
    }

    @Override
    public void updatePost(Post post) {
        baseMapper.updateById(post);
    }

    @Override
    public void deletePost(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public List<Post> getPostsByUser(Long userId) {
        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .orderByDesc(Post::getCreatedAt)
        );
    }

    @Override
    public void incrementViewCount(Long id) {
        Post post = baseMapper.selectById(id);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            baseMapper.updateById(post);
        }
    }
}
