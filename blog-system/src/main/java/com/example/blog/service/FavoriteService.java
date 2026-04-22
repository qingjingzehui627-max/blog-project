package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.Favorite;

import java.util.List;

public interface FavoriteService extends IService<Favorite> {
    void addFavorite(Long postId, Long userId);
    void removeFavorite(Long postId, Long userId);
    boolean isFavorited(Long postId, Long userId);
    List<Long> getFavoritePostIds(Long userId);
}