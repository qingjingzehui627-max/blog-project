package com.example.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.entity.Favorite;

import java.util.List;

public interface FavoriteMapper extends BaseMapper<Favorite> {
    List<Long> findPostIdsByUserId(Long userId);
}