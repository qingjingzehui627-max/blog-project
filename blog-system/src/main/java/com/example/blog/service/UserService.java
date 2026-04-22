package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.User;

public interface UserService extends IService<User> {
    User findByUsername(String username);
    void register(User user);
}