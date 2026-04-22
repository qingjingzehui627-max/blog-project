<template>
  <div>
    <div class="mb-8">
      <h1 class="text-3xl font-bold bg-gradient-to-r from-blue-700 to-indigo-600 bg-clip-text text-transparent mb-1.5">最新文章</h1>
      <p class="text-gray-400 text-sm">探索优质内容，开拓思维边界</p>
    </div>

    <div v-if="store.loading" class="space-y-4">
      <div v-for="i in 4" :key="i" class="bg-white border border-gray-100 rounded-2xl p-6 animate-pulse">
        <div class="flex gap-5">
          <div class="flex-1">
            <div class="h-5 bg-gradient-to-r from-blue-50 to-gray-50 rounded-lg w-3/4 mb-3"></div>
            <div class="h-3.5 bg-gray-50 rounded w-full mb-2"></div>
            <div class="h-3.5 bg-gray-50 rounded w-2/3 mb-4"></div>
            <div class="flex gap-3">
              <div class="h-3 bg-blue-50 rounded-full w-12"></div>
              <div class="h-3 bg-gray-50 rounded w-16"></div>
            </div>
          </div>
          <div class="w-24 h-18 bg-gray-50 rounded-xl flex-shrink-0"></div>
        </div>
      </div>
    </div>

    <div v-else-if="store.posts.length === 0" class="text-center py-28">
      <div class="text-5xl mb-4 opacity-60">📝</div>
      <p class="text-gray-400 mb-2">还没有文章</p>
      <RouterLink to="/create" class="text-sm text-blue-500 hover:text-blue-700 transition-colors">来发布第一篇 →</RouterLink>
    </div>

    <div v-else class="space-y-4">
      <PostCard v-for="post in store.posts" :key="post.id" :post="post" />
    </div>

    <div v-if="!store.loading && store.posts.length > 0" class="flex justify-center items-center gap-3 mt-10">
      <button
        @click="changePage(page - 1)"
        :disabled="page === 1"
        class="inline-flex items-center gap-1.5 px-5 py-2 text-sm border border-gray-200 rounded-full text-gray-500 hover:bg-blue-600 hover:text-white hover:border-blue-600 disabled:opacity-30 disabled:cursor-not-allowed transition-all duration-200"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        上一页
      </button>
      <span class="w-9 h-9 flex items-center justify-center bg-gradient-to-br from-blue-600 to-indigo-600 text-white text-sm font-semibold rounded-full shadow-sm">
        {{ page }}
      </span>
      <button
        @click="changePage(page + 1)"
        :disabled="store.posts.length < pageSize"
        class="inline-flex items-center gap-1.5 px-5 py-2 text-sm border border-gray-200 rounded-full text-gray-500 hover:bg-blue-600 hover:text-white hover:border-blue-600 disabled:opacity-30 disabled:cursor-not-allowed transition-all duration-200"
      >
        下一页
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePostsStore } from '../stores/posts'
import PostCard from '../components/PostCard.vue'

const store = usePostsStore()
const page = ref(1)
const pageSize = 10

async function changePage(n) {
  if (n < 1) return
  page.value = n
  await store.fetchPosts(n, pageSize)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => store.fetchPosts(1, pageSize))
</script>
