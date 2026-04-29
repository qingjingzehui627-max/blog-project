# 📝 Blog System（全栈博客系统）

一个基于 **Vue3 + Spring Boot + MySQL + Redis** 的现代博客系统，支持文章发布、评论互动、图片上传等核心功能。

---

## 🚀 项目简介

本项目对标 Medium / CSDN / 掘金，旨在实现一个 **可上线、可扩展的博客平台**，具备完整的前后端架构。

---

## 🧩 技术栈

### 🎨 前端

* Vue3
* Pinia
* Tailwind CSS
* Ant Design Vue
* Markdown 编辑器

### ⚙️ 后端

* Spring Boot
* MyBatis-Plus
* Spring Security + JWT
* OpenAI API / Spring AI
* 定时任务 @Scheduled

### 🗄️ 数据库

* MySQL
* Redis

### ☁️ 文件存储

* 本地存储（开发）
* 支持扩展 OSS / S3（生产）

---

## ✅ 已实现功能

### 👤 用户系统

* 用户注册
* 用户登录（JWT认证）
* 获取用户信息

### 📝 文章系统

* 发布文章（Markdown）
* 编辑文章
* 删除文章
* 获取文章列表
* 查看文章详情

### 🖼️ 图片上传

* 支持图片上传
* 返回访问 URL

### 💬 评论系统

* 文章评论
* 二级回复

### 👍 点赞功能

* 支持文章点赞
* 支持评论点赞

### ⭐ 收藏功能

* 收藏文章
* 查看收藏列表

### 🏷️ 分类 & 标签

* 文章分类
* 标签管理

### 🔍 AI智能助手
* 智能对话（Chat）
* 博客内容问答（RAG）
* 搜索增强回答
* 内容辅助生成（可扩展）

### 🔍 全球热点新闻资讯
* 多数据源AI新闻抓取
* 新闻分类，去重
* 新闻管理后台
* 新闻抓取定时任务
---

## 📡 核心接口示例

### 👤 用户

```
POST /api/auth/register
POST /api/auth/login
GET  /api/user/info
```

### 📝 文章

```
GET    /api/posts
POST   /api/posts
GET    /api/posts/{id}
PUT    /api/posts/{id}
DELETE /api/posts/{id}
```

### 📤 上传

```
POST /api/upload/image
```

### 📤 AI聊天

```
POST /api/ai/chat
```

### 📤 新闻资讯接口

```
GET /api/news
GET /api/news/top
GET /api/news/categories
GET /api/admin/news/rules
POST /api/admin/news/fetch
```
---

## 📁 项目结构

```
blog-project
├── blog-frontend   # 前端项目（Vue3）
└── blog-system     # 后端项目（Spring Boot）
```

---

## ⚙️ 本地运行

### 🔧 后端

```bash
cd blog-system
mvn spring-boot:run
```

### 🎨 前端

```bash
cd blog-frontend
npm install
npm run dev
```

---

## 📊 功能进度

| 模块   | 状态 |
| ---- | -- |
| 用户系统 | ✅  |
| 文章系统 | ✅  |
| 图片上传 | ✅  |
| 评论系统 | ✅  |
| 点赞功能 | ✅  |
| 收藏功能 | ✅  |
| 分类标签 | ✅  |
| AI智能体 | ✅  | 
| 全球新闻资讯 | ✅  | 
| 搜索功能 | ⏳  |
| 视频上传 | ⏳  |

---
## 📊 新增功能

| 模块   | 状态 |
| ---- | -- |
| 全球新闻资讯 | ✅  | 

## 🔥 后续规划

* 🔍 搜索优化（Elasticsearch）
* 🎬 视频上传（分片上传）
* 📊 推荐系统
* ☁️ 接入云存储（OSS / S3）
* 🚀 Docker 部署
* 🌐 HTTPS + 域名上线

---

## 📌 项目亮点

* ✅ 前后端分离架构
* 🔐 JWT 无状态认证
* ☁️ 可扩展文件存储设计
* ⚡ Redis 提升性能（缓存 / 计数）
* 🧩 模块清晰，易扩展
* 🔍 AI智能助手博客内容问答
* 🔍 多数据源定时抓取全球热点新闻资讯

---

## ⭐ 如果对你有帮助

欢迎 Star ⭐ & Fork 🍴
