package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Like;
import com.example.blog.mapper.LikeMapper;
import com.example.blog.service.LikeService;
import org.springframework.stereotype.Service;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    @Override
    public void like(Long targetId, String type, Long userId) {
        // 检查是否已经点赞
        if (!isLiked(targetId, type, userId)) {
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetId(targetId);
            like.setType(type);
            baseMapper.insert(like);
        }
    }

    @Override
    public void unlike(Long targetId, String type, Long userId) {
        baseMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Like>()
                        .eq(Like::getUserId, userId)
                        .eq(Like::getTargetId, targetId)
                        .eq(Like::getType, type)
        );
    }

    @Override
    public boolean isLiked(Long targetId, String type, Long userId) {
        Like like = baseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Like>()
                        .eq(Like::getUserId, userId)
                        .eq(Like::getTargetId, targetId)
                        .eq(Like::getType, type)
        );
        return like != null;
    }

    @Override
    public Long getLikeCount(Long targetId, String type) {
        return baseMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Like>()
                        .eq(Like::getTargetId, targetId)
                        .eq(Like::getType, type)
        );
    }
}