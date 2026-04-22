package com.example.blog.controller;

import com.example.blog.entity.Tag;
import com.example.blog.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Resource
    private TagService tagService;

    @GetMapping
    public ResponseEntity<?> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @PostMapping
    public ResponseEntity<?> createTag(@RequestBody Tag tag) {
        tagService.createTag(tag);
        return ResponseEntity.ok("Tag created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTagById(@PathVariable Long id) {
        Tag tag = tagService.getTagById(id);
        if (tag == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tag);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        Tag existingTag = tagService.getTagById(id);
        if (existingTag == null) {
            return ResponseEntity.notFound().build();
        }
        tag.setId(id);
        tagService.updateTag(tag);
        return ResponseEntity.ok("Tag updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        Tag tag = tagService.getTagById(id);
        if (tag == null) {
            return ResponseEntity.notFound().build();
        }
        tagService.deleteTag(id);
        return ResponseEntity.ok("Tag deleted successfully");
    }
}