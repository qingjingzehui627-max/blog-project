package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.Like;

public interface LikeService extends IService<Like> {
    void like(Long targetId, String type, Long userId);
    void unlike(Long targetId, String type, Long userId);
    boolean isLiked(Long targetId, String type, Long userId);
    Long getLikeCount(Long targetId, String type);
}