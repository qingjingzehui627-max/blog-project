<template>
  <div>
    <div class="mb-8">
      <h1 class="text-3xl font-bold bg-gradient-to-r from-blue-700 to-indigo-600 bg-clip-text text-transparent mb-1.5">我的收藏</h1>
      <p class="text-gray-400 text-sm">
        {{ loading ? '加载中...' : posts.length ? `共收藏 ${posts.length} 篇文章` : '收藏你喜欢的文章' }}
      </p>
    </div>

    <div v-if="loading" class="space-y-4">
      <div v-for="i in 3" :key="i" class="bg-white border border-gray-100 rounded-2xl p-6 animate-pulse">
        <div class="h-5 bg-gradient-to-r from-blue-50 to-gray-50 rounded-lg w-3/4 mb-3"></div>
        <div class="h-3.5 bg-gray-50 rounded w-full mb-2"></div>
        <div class="h-3.5 bg-gray-50 rounded w-2/3 mb-4"></div>
        <div class="flex gap-3">
          <div class="h-3 bg-blue-50 rounded-full w-12"></div>
          <div class="h-3 bg-gray-50 rounded w-16"></div>
        </div>
      </div>
    </div>

    <div v-else-if="posts.length === 0" class="text-center py-28">
      <div class="text-5xl mb-4 opacity-60">⭐</div>
      <p class="text-gray-400 mb-2">还没有收藏任何文章</p>
      <RouterLink to="/" class="text-sm text-blue-500 hover:text-blue-700 transition-colors">去发现好文章 →</RouterLink>
    </div>

    <div v-else class="space-y-4">
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavorites } from '../api/favorites'
import { getPostById } from '../api/posts'
import PostCard from '../components/PostCard.vue'

const posts = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const ids = await getFavorites()
    posts.value = await Promise.all(ids.map(id => getPostById(id)))
  } finally {
    loading.value = false
  }
})
</script>
