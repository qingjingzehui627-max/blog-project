<template>
  <article
    class="group rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)] transition duration-300 hover:-translate-y-1 hover:shadow-[0_22px_48px_rgba(99,102,241,0.16)]"
  >
    <div class="flex items-start gap-4">
      <div class="min-w-0 flex-1">
        <div class="flex flex-wrap items-center gap-2 text-xs font-semibold uppercase tracking-[0.16em] text-[#6366f1]">
          <span>{{ article.categoryName || '科技综合' }}</span>
          <span class="text-slate-300">•</span>
          <span class="text-slate-400 normal-case tracking-normal">{{ formatDate(article.publishedAt) }}</span>
        </div>

        <RouterLink :to="`/news/${article.id}`" class="mt-3 block">
          <h3 class="line-clamp-2 text-[22px] font-black leading-tight text-slate-900 transition group-hover:text-[#4f46e5]">
            {{ article.title }}
          </h3>
        </RouterLink>

        <p class="mt-3 line-clamp-3 text-[15px] leading-7 text-slate-600">
          {{ article.summary || article.content || '暂无摘要' }}
        </p>

        <div class="mt-4 flex flex-wrap items-center gap-3 text-sm text-slate-500">
          <span>{{ article.author || 'Guardian' }}</span>
          <span class="rounded-full bg-[#eef2ff] px-3 py-1 font-medium text-[#4f46e5]">
            Top {{ article.rankOrder || '-' }}
          </span>
        </div>

        <div class="mt-5 flex flex-wrap items-center gap-3">
          <RouterLink
            :to="`/news/${article.id}`"
            class="inline-flex items-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-2 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(99,102,241,0.2)] transition hover:opacity-90"
          >
            查看详情
          </RouterLink>
          <a
            :href="article.webUrl"
            target="_blank"
            rel="noreferrer"
            class="inline-flex items-center rounded-2xl border border-[#d9dcff] bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
          >
            原文链接
          </a>
        </div>
      </div>

      <div
        v-if="article.thumbnailUrl"
        class="hidden h-[140px] w-[180px] shrink-0 overflow-hidden rounded-[24px] border border-[#e6e9ff] bg-[#f7f8ff] md:block"
      >
        <img :src="article.thumbnailUrl" :alt="article.title" class="h-full w-full object-cover" />
      </div>
    </div>
  </article>
</template>

<script setup>
/**
 * 新闻卡片组件。
 */
defineProps({
  article: {
    type: Object,
    required: true
  }
})

/**
 * 格式化发布时间。
 */
function formatDate(dateText) {
  if (!dateText) return '未知时间'
  return new Date(dateText).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>
