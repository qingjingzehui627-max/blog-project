package com.example.blog.controller;

import com.example.blog.service.FavoriteService;
import com.example.blog.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @Resource
    private PostService postService;

    @PostMapping
    public ResponseEntity<?> addFavorite(@RequestParam Long postId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        favoriteService.addFavorite(postId, userId);
        return ResponseEntity.ok("Added to favorites");
    }

    @DeleteMapping
    public ResponseEntity<?> removeFavorite(@RequestParam Long postId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        favoriteService.removeFavorite(postId, userId);
        return ResponseEntity.ok("Removed from favorites");
    }

    @GetMapping("/status")
    public ResponseEntity<?> isFavorited(@RequestParam Long postId, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        boolean isFavorited = favoriteService.isFavorited(postId, userId);
        return ResponseEntity.ok(isFavorited);
    }

    @GetMapping
    public ResponseEntity<?> getFavorites(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        List<Long> postIds = favoriteService.getFavoritePostIds(userId);
        // 根据postIds获取文章列表
        return ResponseEntity.ok(postIds);
    }
}