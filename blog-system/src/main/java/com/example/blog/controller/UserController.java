package com.example.blog.controller;

import com.example.blog.entity.User;
import com.example.blog.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        Long userId = Long.parseLong(principal.getName());
        User user = userService.getById(userId);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) return ResponseEntity.notFound().build();
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User user = userService.getById(userId);
        if (user == null) return ResponseEntity.notFound().build();
        if (body.containsKey("bio")) user.setBio(body.get("bio"));
        if (body.containsKey("avatar")) user.setAvatar(body.get("avatar"));
        userService.updateById(user);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}
