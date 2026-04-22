<template>
  <div v-if="loading" class="space-y-5 animate-pulse">
    <div class="h-8 bg-gradient-to-r from-blue-50 to-gray-50 rounded-xl w-3/4"></div>
    <div class="flex gap-3">
      <div class="h-5 bg-blue-50 rounded-full w-20"></div>
      <div class="h-5 bg-gray-50 rounded-full w-16"></div>
    </div>
    <div class="h-px bg-gradient-to-r from-blue-100 to-transparent"></div>
    <div class="bg-white rounded-2xl border border-gray-100 p-8 space-y-3">
      <div v-for="i in 6" :key="i" class="h-4 bg-gray-50 rounded" :class="i % 3 === 0 ? 'w-2/3' : 'w-full'"></div>
    </div>
  </div>

  <div v-else-if="post">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-gray-900 mb-4 leading-tight">{{ post.title }}</h1>
      <div class="flex items-center gap-3 flex-wrap text-xs text-gray-400">
        <span class="bg-blue-50 text-blue-500 border border-blue-100 px-2.5 py-1 rounded-full font-medium">
          {{ formatDate(post.createdAt) }}
        </span>
        <span class="flex items-center gap-1.5">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
          </svg>
          {{ post.viewCount ?? 0 }} 阅读
        </span>
      </div>
      <div class="mt-5 h-px bg-gradient-to-r from-blue-300 via-indigo-200 to-transparent"></div>
    </div>

    <div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden mb-8">
      <MdEditor v-model="post.content" previewOnly />
    </div>

    <div class="flex items-center gap-3 py-5 border-t border-b border-gray-100 mb-8">
      <button
        @click="toggleLike"
        class="flex items-center gap-2 px-5 py-2 rounded-full text-sm font-medium transition-all duration-200"
        :class="liked
          ? 'bg-red-50 text-red-500 border border-red-100 hover:bg-red-100 shadow-sm'
          : 'bg-gray-50 text-gray-500 border border-gray-200 hover:bg-red-50 hover:text-red-400 hover:border-red-100'"
      >
        <span
          class="text-base transition-all duration-300"
          :class="liked ? 'scale-125' : 'scale-100'"
        >♥</span>
        <span>{{ likeCount }}</span>
      </button>

      <button
        @click="toggleFav"
        class="flex items-center gap-2 px-5 py-2 rounded-full text-sm font-medium transition-all duration-200"
        :class="favorited
          ? 'bg-amber-50 text-amber-600 border border-amber-100 hover:bg-amber-100 shadow-sm'
          : 'bg-gray-50 text-gray-500 border border-gray-200 hover:bg-amber-50 hover:text-amber-500 hover:border-amber-100'"
      >
        <span class="text-base transition-all duration-200" :class="favorited ? 'scale-110' : 'scale-100'">
          {{ favorited ? '★' : '☆' }}
        </span>
        <span>{{ favorited ? '已收藏' : '收藏' }}</span>
      </button>

      <div v-if="isOwner" class="ml-auto flex items-center gap-2">
        <RouterLink
          :to="`/edit/${post.id}`"
          class="px-3.5 py-1.5 text-xs text-blue-500 border border-blue-200 rounded-full hover:bg-blue-50 hover:border-blue-300 transition-all duration-200"
        >
          编辑
        </RouterLink>
        <button
          @click="handleDelete"
          class="px-3.5 py-1.5 text-xs text-red-400 border border-red-100 rounded-full hover:bg-red-50 hover:border-red-200 transition-all duration-200"
        >
          删除
        </button>
      </div>
    </div>

    <CommentSection :postId="post.id" />
  </div>

  <div v-else class="text-center py-28">
    <div class="text-5xl mb-4 opacity-50">🔍</div>
    <p class="text-gray-400">文章不存在</p>
    <RouterLink to="/" class="text-sm text-blue-500 hover:text-blue-700 mt-3 inline-block transition-colors">← 返回首页</RouterLink>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getPostById, deletePost } from '../api/posts'
import { getLikeStatus, getLikeCount, like, unlike } from '../api/like'
import { getFavoriteStatus, addFavorite, removeFavorite } from '../api/favorites'
import { useAuthStore } from '../stores/auth'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const post = ref(null)
const loading = ref(true)
const liked = ref(false)
const likeCount = ref(0)
const favorited = ref(false)

const isOwner = computed(() => auth.user && post.value && auth.user.id === post.value.userId)

async function load() {
  const id = Number(route.params.id)
  post.value = await getPostById(id)
  if (auth.isLoggedIn) {
    liked.value = await getLikeStatus(id, 'post')
    favorited.value = await getFavoriteStatus(id)
  }
  likeCount.value = await getLikeCount(id, 'post')
  loading.value = false
}

async function toggleLike() {
  if (!auth.isLoggedIn) return router.push('/login')
  const id = post.value.id
  if (liked.value) {
    await unlike(id, 'post')
    likeCount.value--
  } else {
    await like(id, 'post')
    likeCount.value++
  }
  liked.value = !liked.value
}

async function toggleFav() {
  if (!auth.isLoggedIn) return router.push('/login')
  const id = post.value.id
  if (favorited.value) {
    await removeFavorite(id)
  } else {
    await addFavorite(id)
  }
  favorited.value = !favorited.value
}

async function handleDelete() {
  if (!confirm('确认删除这篇文章？')) return
  await deletePost(post.value.id)
  router.push('/')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

onMounted(load)
</script>
