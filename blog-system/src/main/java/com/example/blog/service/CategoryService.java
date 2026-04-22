package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> getAllCategories();
    Category getCategoryById(Long id);
    void createCategory(Category category);
    void updateCategory(Category category);
    void deleteCategory(Long id);
}