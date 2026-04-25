package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.Post;

import java.util.List;

public interface PostService extends IService<Post> {
    List<Post> getPosts(int page, int size, String keyword);
    List<Post> getPostsByUser(Long userId);
    Post getPostById(Long id);
    void createPost(Post post);
    void updatePost(Post post);
    void deletePost(Long id);
    void incrementViewCount(Long id);
}
