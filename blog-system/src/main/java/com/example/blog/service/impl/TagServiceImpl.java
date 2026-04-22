package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Tag;
import com.example.blog.mapper.TagMapper;
import com.example.blog.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public List<Tag> getAllTags() {
        return baseMapper.selectList(null);
    }

    @Override
    public Tag getTagById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public void createTag(Tag tag) {
        baseMapper.insert(tag);
    }

    @Override
    public void updateTag(Tag tag) {
        baseMapper.updateById(tag);
    }

    @Override
    public void deleteTag(Long id) {
        baseMapper.deleteById(id);
    }
}