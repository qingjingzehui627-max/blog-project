package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Category;
import com.example.blog.mapper.CategoryMapper;
import com.example.blog.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> getAllCategories() {
        return baseMapper.selectList(null);
    }

    @Override
    public Category getCategoryById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public void createCategory(Category category) {
        baseMapper.insert(category);
    }

    @Override
    public void updateCategory(Category category) {
        baseMapper.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        baseMapper.deleteById(id);
    }
}