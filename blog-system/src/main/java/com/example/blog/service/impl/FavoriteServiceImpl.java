package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Favorite;
import com.example.blog.mapper.FavoriteMapper;
import com.example.blog.service.FavoriteService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Override
    public void addFavorite(Long postId, Long userId) {
        // 检查是否已经收藏
        if (!isFavorited(postId, userId)) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setPostId(postId);
            favorite.setCreatedAt(LocalDateTime.now());
            baseMapper.insert(favorite);
        }
    }

    @Override
    public void removeFavorite(Long postId, Long userId) {
        baseMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getPostId, postId)
        );
    }

    @Override
    public boolean isFavorited(Long postId, Long userId) {
        Favorite favorite = baseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getPostId, postId)
        );
        return favorite != null;
    }

    @Override
    public List<Long> getFavoritePostIds(Long userId) {
        return baseMapper.findPostIdsByUserId(userId);
    }
}