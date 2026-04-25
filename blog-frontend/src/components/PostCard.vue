<template>
  <RouterLink
    :to="`/post/${post.id}`"
    class="group flex items-start gap-4 border-b border-slate-100 px-5 py-4 transition hover:bg-slate-50/70"
  >
    <div class="min-w-0 flex-1">
      <div class="mb-2 flex items-center gap-2 text-xs text-slate-400">
        <span class="font-medium text-slate-500">{{ authorName }}</span>
        <span>·</span>
        <span>{{ formatDate(post.createdAt) }}</span>
      </div>

      <h2 class="line-clamp-2 text-[22px] font-semibold leading-tight text-slate-900 transition-colors group-hover:text-[#1e80ff]">
        {{ post.title || '未命名文章' }}
      </h2>

      <p class="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">
        {{ excerpt || '这篇文章还没有摘要内容。' }}
      </p>

      <div class="mt-3 flex flex-wrap items-center gap-3 text-xs text-slate-400">
        <span class="inline-flex items-center gap-1">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          {{ post.viewCount ?? 0 }}
        </span>

        <span class="inline-flex items-center gap-1">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
          </svg>
          {{ post.likeCount ?? 0 }}
        </span>

        <span
          v-if="post.category"
          class="rounded bg-slate-100 px-2 py-0.5 text-slate-500"
        >
          {{ post.category }}
        </span>
      </div>
    </div>

    <img
      :src="coverUrl"
      alt="cover"
      class="h-24 w-40 flex-shrink-0 rounded-lg border border-slate-100 object-cover"
    />
  </RouterLink>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
})

const defaultCover = `data:image/svg+xml;utf8,${encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180" viewBox="0 0 320 180"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#dbeafe"/><stop offset="1" stop-color="#e9d5ff"/></linearGradient></defs><rect width="320" height="180" fill="url(#g)"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#64748b" font-size="20" font-family="Arial">Blog Cover</text></svg>'
)}`

const coverUrl = computed(() => props.post.cover || defaultCover)

const excerpt = computed(() =>
  (props.post.content || '')
    .replace(/[#*`>[\]!]/g, '')
    .replace(/\n+/g, ' ')
    .trim()
    .slice(0, 120)
)

const authorName = computed(() => props.post.authorName || '未知作者')

function formatDate(dateStr) {
  if (!dateStr) return '刚刚'
  const d = new Date(dateStr)
  const now = new Date()
  const diffDays = Math.floor((now - d) / 86400000)
  if (diffDays <= 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays} 天前`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} 周前`
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}
</script>
