package com.example.blog.controller;

import com.example.blog.service.MediaFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.security.Principal;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Resource
    private MediaFileService mediaFileService;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, Principal principal) {
        Long userId = principal != null ? Long.parseLong(principal.getName()) : 1L; // 默认用户ID
        String url = mediaFileService.uploadImage(file, userId);
        return ResponseEntity.ok(url);
    }

    @PostMapping("/video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file, Principal principal) {
        Long userId = principal != null ? Long.parseLong(principal.getName()) : 1L; // 默认用户ID
        String url = mediaFileService.uploadVideo(file, userId);
        return ResponseEntity.ok(url);
    }
}