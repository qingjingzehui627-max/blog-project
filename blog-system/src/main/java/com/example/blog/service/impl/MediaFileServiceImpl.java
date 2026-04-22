package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.MediaFile;
import com.example.blog.mapper.MediaFileMapper;
import com.example.blog.service.MediaFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MediaFileServiceImpl extends ServiceImpl<MediaFileMapper, MediaFile> implements MediaFileService {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    public MediaFileServiceImpl() {
        // 创建上传目录
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public String uploadImage(MultipartFile file, Long userId) {
        return uploadFile(file, userId, "image");
    }

    @Override
    public String uploadVideo(MultipartFile file, Long userId) {
        return uploadFile(file, userId, "video");
    }

    private String uploadFile(MultipartFile file, Long userId, String type) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new RuntimeException("Original filename is null");
            }
            
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // 创建文件路径
            String filePath = UPLOAD_DIR + type + "/";
            File dir = new File(filePath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create directory: " + filePath);
                }
            }

            // 保存文件
            File dest = new File(filePath + filename);
            file.transferTo(dest);

            // 生成访问URL
            String url = "/uploads/" + type + "/" + filename;

            // 保存到数据库
            MediaFile mediaFile = new MediaFile();
            mediaFile.setUrl(url);
            mediaFile.setType(type);
            mediaFile.setSize(file.getSize());
            mediaFile.setUserId(userId);
            mediaFile.setCreatedAt(LocalDateTime.now());
            baseMapper.insert(mediaFile);

            return url;
        } catch (IOException e) {
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }
}