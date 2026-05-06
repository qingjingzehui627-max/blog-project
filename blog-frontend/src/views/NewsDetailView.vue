<template>
  <div v-if="loading" class="space-y-4">
    <div class="h-8 w-40 animate-pulse rounded bg-slate-200"></div>
    <div class="h-12 w-3/4 animate-pulse rounded bg-slate-200"></div>
    <div class="h-64 animate-pulse rounded-[32px] bg-slate-100"></div>
  </div>

  <div v-else-if="article" class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
    <article class="rounded-[32px] border border-[#d9dcff] bg-white p-6 shadow-[0_18px_44px_rgba(99,102,241,0.1)] sm:p-8">
      <RouterLink
        to="/news"
        class="inline-flex items-center rounded-full border border-[#d9dcff] px-4 py-2 text-sm font-medium text-slate-600 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
      >
        返回新闻列表
      </RouterLink>

      <div class="mt-6 flex flex-wrap items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[#6366f1]">
        <span>{{ article.categoryName }}</span>
        <span class="text-slate-300">/</span>
        <span class="text-slate-400 normal-case tracking-normal">{{ formatDateTime(article.publishedAt) }}</span>
      </div>

      <h1 class="mt-4 text-[34px] font-black leading-tight text-slate-900">
        {{ article.title }}
      </h1>

      <div class="mt-4 flex flex-wrap items-center gap-4 text-sm text-slate-500">
        <span>{{ article.author || article.primaryProvider || '新闻来源' }}</span>
        <span>{{ article.sectionName || '-' }}</span>
        <span>Top {{ article.rankOrder || '-' }}</span>
      </div>

      <div
        v-if="article.thumbnailUrl"
        class="mt-8 overflow-hidden rounded-[28px] border border-[#e6e9ff] bg-[#f7f8ff]"
      >
        <img :src="article.thumbnailUrl" :alt="article.title" class="max-h-[420px] w-full object-cover" />
      </div>

      <section
        v-if="aiSummary?.summaryText"
        class="mt-8 rounded-[28px] border border-[#d7e3ff] bg-[linear-gradient(135deg,#eef4ff_0%,#f8faff_100%)] px-5 py-5"
      >
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#4f46e5]">AI Summary</p>
            <h2 class="mt-2 text-lg font-bold text-slate-900">AI 摘要</h2>
          </div>
          <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-500">
            {{ aiSummary.summaryStatus || 'SUCCESS' }}
          </span>
        </div>
        <p class="mt-4 text-[16px] leading-8 text-slate-700">
          {{ aiSummary.summaryText }}
        </p>
        <p class="mt-3 text-xs text-slate-400">
          生成时间：{{ formatDateTime(aiSummary.generatedAt) }} / 模型：{{ aiSummary.modelName || '-' }}
        </p>
      </section>

      <section v-if="article.summary" class="mt-8 rounded-[24px] bg-[#f7f8ff] px-5 py-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Source Summary</p>
        <p class="mt-3 text-[16px] leading-8 text-slate-700">
          {{ article.summary }}
        </p>
      </section>

      <div class="mt-8 space-y-5 text-[16px] leading-8 text-slate-700">
        <p v-for="(paragraph, index) in contentParagraphs" :key="index">
          {{ paragraph }}
        </p>
      </div>

      <div class="mt-8 flex flex-wrap items-center gap-3 border-t border-slate-100 pt-6">
        <a
          :href="article.webUrl"
          target="_blank"
          rel="noreferrer"
          class="inline-flex items-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-3 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(99,102,241,0.2)] transition hover:opacity-90"
        >
          打开原文
        </a>
        <button
          v-if="auth.isLoggedIn"
          type="button"
          class="inline-flex items-center rounded-2xl border border-[#d9dcff] bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5] disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="summarySubmitting"
          @click="handleGenerateSummary"
        >
          {{ summarySubmitting ? '生成中...' : (aiSummary?.summaryText ? '重新生成 AI 摘要' : '生成 AI 摘要') }}
        </button>
        <RouterLink
          v-if="auth.isLoggedIn"
          to="/news-admin"
          class="inline-flex items-center rounded-2xl border border-[#d9dcff] bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
        >
          进入新闻后台
        </RouterLink>
      </div>

      <p v-if="summaryMessage" class="mt-4 rounded-2xl bg-[#f8fafc] px-4 py-3 text-sm text-slate-600">
        {{ summaryMessage }}
      </p>
    </article>

    <aside class="space-y-4">
      <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
        <h2 class="text-lg font-bold text-slate-900">新闻信息</h2>
        <dl class="mt-4 space-y-3 text-sm">
          <div class="flex items-start justify-between gap-4">
            <dt class="text-slate-400">分类</dt>
            <dd class="text-right font-medium text-slate-700">{{ article.categoryName }}</dd>
          </div>
          <div class="flex items-start justify-between gap-4">
            <dt class="text-slate-400">抓取日期</dt>
            <dd class="text-right font-medium text-slate-700">{{ formatDateOnly(article.fetchDate) }}</dd>
          </div>
          <div class="flex items-start justify-between gap-4">
            <dt class="text-slate-400">栏目</dt>
            <dd class="text-right font-medium text-slate-700">{{ article.sectionName || '-' }}</dd>
          </div>
          <div class="flex items-start justify-between gap-4">
            <dt class="text-slate-400">作者</dt>
            <dd class="text-right font-medium text-slate-700">{{ article.author || '-' }}</dd>
          </div>
          <div class="flex items-start justify-between gap-4">
            <dt class="text-slate-400">AI 状态</dt>
            <dd class="text-right font-medium text-slate-700">{{ aiSummary?.summaryStatus || '未生成' }}</dd>
          </div>
        </dl>
      </section>

      <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
        <h2 class="text-lg font-bold text-slate-900">命中关键词</h2>
        <div class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="item in keywordTags"
            :key="item"
            class="rounded-full bg-[#eef2ff] px-3 py-2 text-sm font-medium text-[#4f46e5]"
          >
            {{ item }}
          </span>
          <span v-if="keywordTags.length === 0" class="text-sm text-slate-400">暂无规则命中关键词</span>
        </div>
      </section>
    </aside>
  </div>

  <div v-else class="rounded-[28px] border border-[#d9dcff] bg-white px-6 py-16 text-center shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
    <p class="text-lg font-semibold text-slate-800">新闻不存在或已下线</p>
    <p v-if="errorText" class="mt-2 text-sm text-slate-400">{{ errorText }}</p>
    <RouterLink to="/news" class="mt-4 inline-flex items-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-3 text-sm font-semibold text-white">
      返回新闻列表
    </RouterLink>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { generateNewsSummary, getNewsDetail } from '../api/news'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const summarySubmitting = ref(false)
const detail = ref(null)
const errorText = ref('')
const summaryMessage = ref('')

const article = computed(() => detail.value?.article || null)
const aiSummary = computed(() => detail.value?.aiSummary || null)

const keywordTags = computed(() =>
  ((article.value?.keywordTags || ''))
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
)

const contentParagraphs = computed(() => {
  const text = article.value?.content || ''
  return text
    .split(/\n{2,}/)
    .map(item => item.trim())
    .filter(Boolean)
})

/**
 * 加载新闻详情。
 */
async function loadDetail() {
  loading.value = true
  errorText.value = ''
  try {
    detail.value = await getNewsDetail(route.params.id)
  } catch (error) {
    detail.value = null
    errorText.value = error.response?.data?.message || '新闻详情加载失败'
  } finally {
    loading.value = false
  }
}

/**
 * 手动生成单条新闻摘要。
 */
async function handleGenerateSummary() {
  if (!article.value) return
  summarySubmitting.value = true
  summaryMessage.value = ''
  try {
    const result = await generateNewsSummary(article.value.id, {
      force: aiSummary.value?.summaryText ? true : false
    })
    summaryMessage.value = result?.message || '摘要生成完成'
    await loadDetail()
  } catch (error) {
    summaryMessage.value = error.response?.data?.message || '摘要生成失败'
  } finally {
    summarySubmitting.value = false
  }
}

/**
 * 格式化完整时间。
 *
 * @param {string} dateText 时间文本
 * @returns {string}
 */
function formatDateTime(dateText) {
  if (!dateText) return '-'
  return new Date(dateText).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 格式化日期。
 *
 * @param {string} dateText 日期文本
 * @returns {string}
 */
function formatDateOnly(dateText) {
  if (!dateText) return '-'
  return new Date(dateText).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

watch(
  () => route.params.id,
  async () => {
    await loadDetail()
  }
)

onMounted(loadDetail)
</script>
