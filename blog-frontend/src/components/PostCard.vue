<template>
  <RouterLink
    :to="`/post/${post.id}`"
    class="group block bg-white border border-gray-100 rounded-2xl p-6 hover:border-blue-200/80 hover:shadow-[0_8px_30px_rgba(59,130,246,0.10)] hover:scale-[1.02] transition-all duration-300 cursor-pointer"
  >
    <div class="flex gap-5 items-start">
      <div class="flex-1 min-w-0">
        <h2 class="text-base font-semibold text-gray-800 mb-2 group-hover:text-blue-700 transition-colors duration-200 line-clamp-2 leading-snug">
          {{ post.title }}
        </h2>
        <p class="text-gray-400 text-sm line-clamp-2 mb-4 leading-relaxed">{{ excerpt }}</p>
        <div class="flex items-center gap-3 flex-wrap text-xs text-gray-400">
          <span
            v-if="post.category"
            class="bg-gradient-to-r from-blue-50 to-indigo-50 text-blue-500 border border-blue-100 px-2.5 py-0.5 rounded-full font-medium"
          >
            {{ post.category }}
          </span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <span class="flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
            </svg>
            {{ post.viewCount ?? 0 }}
          </span>
          <span class="flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
            {{ post.likeCount ?? 0 }}
          </span>
          <span class="ml-auto text-blue-200 group-hover:text-blue-500 group-hover:translate-x-1 transition-all duration-200">→</span>
        </div>
      </div>
      <div v-if="post.cover" class="flex-shrink-0 w-24 h-18 rounded-xl overflow-hidden shadow-sm">
        <img :src="post.cover" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
      </div>
    </div>
  </RouterLink>
</template>

<script setup>
const props = defineProps({ post: Object })

const excerpt = props.post.content
  ?.replace(/[#*`>[\]!]/g, '')
  .replace(/\n+/g, ' ')
  .trim()
  .slice(0, 110) + '...'

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffDays = Math.floor((now - d) / 86400000)
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays} 天前`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)} 周前`
  return d.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}
</script>
