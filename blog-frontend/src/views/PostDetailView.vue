<template>
  <div v-if="loading" class="space-y-4">
    <div class="h-10 w-2/3 animate-pulse rounded bg-slate-200"></div>
    <div class="h-6 w-1/3 animate-pulse rounded bg-slate-100"></div>
    <div class="h-64 animate-pulse rounded-xl bg-slate-100"></div>
  </div>

  <div v-else-if="post" class="relative">
    <div class="hidden xl:block">
      <div class="fixed left-6 top-[180px] z-20 space-y-4">
        <button class="grid h-10 w-10 place-items-center rounded-full border border-slate-200 bg-white text-slate-500 shadow-sm">
          👍
        </button>
        <button class="grid h-10 w-10 place-items-center rounded-full border border-slate-200 bg-white text-slate-500 shadow-sm">
          💬
        </button>
        <button class="grid h-10 w-10 place-items-center rounded-full border border-slate-200 bg-white text-slate-500 shadow-sm">
          ⭐
        </button>
      </div>
    </div>

    <div>
      <div>
        <article class="rounded-xl border border-slate-200 bg-white p-6 sm:p-8">
          <h1 class="text-4xl font-black leading-tight text-slate-900">
            {{ post.title }}
          </h1>

          <div class="mt-4 flex flex-wrap items-center gap-4 text-sm text-slate-500">
            <span class="font-medium text-slate-700">{{ authorName }}</span>
            <span>{{ formatDate(post.createdAt) }}</span>
            <span class="inline-flex items-center gap-1">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              {{ post.viewCount ?? 0 }}
            </span>
            <span>阅读 {{ readMinutes }} 分钟</span>
          </div>

          <div class="article-content prose prose-slate mt-8 max-w-none prose-headings:text-slate-900 prose-a:text-[#1e80ff] prose-pre:rounded-xl prose-pre:bg-slate-900">
            <MdPreview editorId="post-preview" :modelValue="post.content || ''" previewTheme="github" />
          </div>

          <div class="mt-8 flex flex-wrap items-center gap-3 border-t border-slate-100 pt-5">
            <button
              @click="toggleLike"
              class="inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition"
              :class="liked ? 'border-red-200 bg-red-50 text-red-500' : 'border-slate-200 bg-white text-slate-500 hover:border-red-200 hover:text-red-500'"
            >
              👍 {{ likeCount }}
            </button>

            <button
              @click="toggleFav"
              class="inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition"
              :class="favorited ? 'border-amber-200 bg-amber-50 text-amber-600' : 'border-slate-200 bg-white text-slate-500 hover:border-amber-200 hover:text-amber-600'"
            >
              {{ favorited ? '已收藏' : '收藏' }}
            </button>

            <div v-if="isOwner" class="ml-auto flex items-center gap-2">
              <RouterLink
                :to="`/edit/${post.id}`"
                class="rounded-full border border-[#1e80ff]/30 px-3 py-1.5 text-sm text-[#1e80ff] transition hover:bg-[#1e80ff]/5"
              >
                编辑
              </RouterLink>
              <button
                @click="handleDelete"
                class="rounded-full border border-red-200 px-3 py-1.5 text-sm text-red-500 transition hover:bg-red-50"
              >
                删除
              </button>
            </div>
          </div>
        </article>

        <div class="mt-4 rounded-xl border border-slate-200 bg-white p-6">
          <CommentSection :postId="post.id" />
        </div>
      </div>
    </div>
  </div>

  <div v-else class="py-20 text-center">
    <div class="text-5xl">📰</div>
    <p class="mt-3 text-slate-500">文章不存在</p>
    <RouterLink to="/" class="mt-4 inline-block text-[#1e80ff] hover:underline">返回首页</RouterLink>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { getPostById, deletePost } from '../api/posts'
import { getLikeStatus, getLikeCount, like, unlike } from '../api/like'
import { getFavoriteStatus, addFavorite, removeFavorite } from '../api/favorites'
import { getUserById } from '../api/users'
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
const authorName = computed(() => post.value?.authorName || '未知作者')

const readMinutes = computed(() => {
  const words = (post.value?.content || '').replace(/\s+/g, '').length
  return Math.max(1, Math.round(words / 300))
})

async function load() {
  loading.value = true
  const id = Number(route.params.id)
  const postData = await getPostById(id)
  let author = '未知作者'
  if (postData?.userId) {
    try {
      const user = await getUserById(postData.userId)
      author = user?.username || author
    } catch (error) {
      author = '未知作者'
    }
  }
  post.value = {
    ...postData,
    authorName: author
  }
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
    likeCount.value -= 1
  } else {
    await like(id, 'post')
    likeCount.value += 1
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
  if (!dateStr) return '未知时间'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

onMounted(load)
</script>

<style scoped>
.article-content :deep(.md-editor-preview) {
  font-size: 1.125rem;
  line-height: 2rem;
  font-weight: 600;
}

.article-content :deep(.md-editor-preview p),
.article-content :deep(.md-editor-preview li),
.article-content :deep(.md-editor-preview blockquote) {
  font-size: 1.125rem;
  line-height: 2rem;
  font-weight: 600;
}

.article-content :deep(.md-editor-preview h1) {
  font-size: 2.5rem;
  font-weight: 800;
}

.article-content :deep(.md-editor-preview h2) {
  font-size: 2rem;
  font-weight: 800;
}

.article-content :deep(.md-editor-preview h3) {
  font-size: 1.75rem;
  font-weight: 700;
}
</style>
