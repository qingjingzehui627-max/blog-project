package com.example.blog.controller;

import com.example.blog.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;

@RestController
@RequestMapping("/api/like")
public class LikeController {

    @Resource
    private LikeService likeService;

    @PostMapping
    public ResponseEntity<?> like(@RequestParam Long targetId, @RequestParam String type, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        likeService.like(targetId, type, userId);
        return ResponseEntity.ok("Liked successfully");
    }

    @DeleteMapping
    public ResponseEntity<?> unlike(@RequestParam Long targetId, @RequestParam String type, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        likeService.unlike(targetId, type, userId);
        return ResponseEntity.ok("Unliked successfully");
    }

    @GetMapping("/status")
    public ResponseEntity<?> isLiked(@RequestParam Long targetId, @RequestParam String type, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        boolean isLiked = likeService.isLiked(targetId, type, userId);
        return ResponseEntity.ok(isLiked);
    }

    @GetMapping("/count")
    public ResponseEntity<?> getLikeCount(@RequestParam Long targetId, @RequestParam String type) {
        Long count = likeService.getLikeCount(targetId, type);
        return ResponseEntity.ok(count);
    }
}