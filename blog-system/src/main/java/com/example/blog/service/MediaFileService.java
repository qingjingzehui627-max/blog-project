package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

public interface MediaFileService extends IService<MediaFile> {
    String uploadImage(MultipartFile file, Long userId);
    String uploadVideo(MultipartFile file, Long userId);
}