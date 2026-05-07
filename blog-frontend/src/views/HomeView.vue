<template>
  <div class="grid grid-cols-1 items-start gap-4 lg:grid-cols-[220px_minmax(0,1fr)_300px] xl:grid-cols-[240px_minmax(0,1fr)_320px] xl:gap-5">
    <aside class="hidden lg:block">
      <div class="sticky top-20 space-y-4">
        <section class="rounded-xl border border-slate-200 bg-white p-3">
          <h3 class="mb-2 px-2 text-base font-semibold text-slate-700">{{ isNewsMode ? '新闻分类' : '博客分类' }}</h3>
          <ul class="space-y-1">
            <li
              class="cursor-pointer rounded-lg px-3 py-2 transition"
              :class="selectedFilter === '' ? 'bg-[#f4f7ff] font-medium text-[#1e80ff]' : 'text-slate-600 hover:bg-[#f4f7ff] hover:text-[#1e80ff]'"
              @click="handleFilterChange('')"
            >
              全部
            </li>
            <li
              v-for="item in sidebarItems"
              :key="item.key"
              class="cursor-pointer rounded-lg px-3 py-2 transition"
              :class="selectedFilter === item.key ? 'bg-[#f4f7ff] font-medium text-[#1e80ff]' : 'text-slate-600 hover:bg-[#f4f7ff] hover:text-[#1e80ff]'"
              @click="handleFilterChange(item.key)"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="truncate">{{ item.label }}</span>
                <span v-if="item.count !== null" class="text-xs text-slate-400">{{ item.count }}</span>
              </div>
            </li>
          </ul>
        </section>

        <div class="relative">
          <button
            type="button"
            class="block w-full overflow-hidden rounded-xl border border-[#d9dcff] bg-[linear-gradient(180deg,#ffffff_0%,#f8f8ff_100%)] p-4 text-left shadow-[0_12px_30px_rgba(99,102,241,0.08)] transition hover:-translate-y-0.5 hover:shadow-[0_18px_36px_rgba(99,102,241,0.14)]"
            @click="openAiDialog"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="text-sm font-semibold uppercase tracking-[0.18em] text-[#6366f1]">AI</p>
                <h3 class="mt-2 text-[18px] font-bold text-slate-900">智能助手</h3>
              </div>
              <div class="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-[linear-gradient(135deg,#4f46e5_0%,#8b5cf6_100%)] text-lg font-bold text-white shadow-[0_12px_24px_rgba(99,102,241,0.22)]">
                AI
              </div>
            </div>

            <div class="mt-4 rounded-2xl bg-white/80 p-3">
              <div class="flex items-center justify-between text-sm">
                <span class="text-slate-400">当前模型</span>
                <span class="ml-3 truncate font-medium text-slate-700">{{ aiCurrentModel }}</span>
              </div>
              <div class="mt-2 flex items-center justify-between text-sm">
                <span class="text-slate-400">今日剩余</span>
                <span class="font-semibold text-[#4f46e5]">{{ aiRemainingText }}</span>
              </div>
            </div>

            <div class="mt-4 flex items-center justify-center text-center text-sm font-semibold text-[#4f46e5]">
              点击开始提问
            </div>
          </button>

          <div
            v-if="showAiDialog"
            class="absolute bottom-0 left-0 z-[70] w-[380px] overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-[0_20px_48px_rgba(15,23,42,0.12)]"
          >
            <div class="flex items-center justify-between border-b border-slate-100 bg-[linear-gradient(180deg,#ffffff_0%,#f8f9ff_100%)] px-4 py-4">
              <div class="flex items-center gap-3">
                <div class="grid h-10 w-10 place-items-center rounded-2xl bg-[linear-gradient(135deg,#4f46e5_0%,#8b5cf6_100%)] text-sm font-bold text-white shadow-[0_10px_20px_rgba(99,102,241,0.2)]">
                  AI
                </div>
                <div>
                  <h3 class="text-sm font-bold text-slate-900">风中有诗</h3>
                  <p class="text-xs text-slate-500">简洁对话，随时提问</p>
                </div>
              </div>
              <button
                type="button"
                class="grid h-8 w-8 place-items-center rounded-full border border-slate-200 text-slate-400 transition hover:border-slate-300 hover:text-slate-600"
                @click.stop="closeAiDialog"
              >
                ×
              </button>
            </div>

            <div v-if="!aiConfig.enabled" class="px-4 py-10 text-center">
              <p class="text-sm font-semibold text-slate-800">AI 未开启</p>
              <p class="mt-2 text-xs text-slate-500">请先在后端配置中启用 AI 功能。</p>
            </div>

            <div v-else-if="!auth.isLoggedIn" class="px-4 py-10 text-center">
              <p class="text-sm font-semibold text-slate-800">登录后即可使用 AI</p>
              <p class="mt-2 text-xs text-slate-500">普通用户每日提问次数受系统配置限制。</p>
              <RouterLink
                to="/login"
                class="mt-4 inline-flex items-center rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-2 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(99,102,241,0.18)]"
                @click="closeAiDialog"
              >
                去登录
              </RouterLink>
            </div>

            <template v-else>
              <div
                ref="messageBoxRef"
                class="h-[220px] space-y-3 overflow-y-auto bg-[linear-gradient(180deg,#f8fafc_0%,#f5f7ff_100%)] px-3 py-3"
              >
                <div
                  v-for="(item, index) in messages"
                  :key="`${item.role}-${index}`"
                  class="flex"
                  :class="item.role === 'user' ? 'justify-end' : 'justify-start'"
                >
                  <div class="flex max-w-[88%] items-end gap-2" :class="item.role === 'user' ? 'flex-row-reverse' : 'flex-row'">
                    <div
                      class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-[11px] font-semibold"
                      :class="item.role === 'user'
                        ? 'border border-slate-200 bg-white text-slate-500'
                        : 'bg-[linear-gradient(135deg,#4f46e5_0%,#8b5cf6_100%)] text-white shadow-[0_8px_18px_rgba(99,102,241,0.18)]'"
                    >
                      {{ item.role === 'user' ? '我' : 'AI' }}
                    </div>
                    <div
                      class="rounded-2xl px-3.5 py-3 text-[13px] leading-6 shadow-sm"
                      :class="item.role === 'user'
                        ? 'border border-slate-200 bg-white text-slate-700'
                        : 'border border-[#d9dcff] bg-[linear-gradient(135deg,#eef2ff_0%,#f5f3ff_100%)] text-slate-800'"
                    >
                      <p class="whitespace-pre-wrap break-words">{{ item.content }}</p>
                      <div
                        v-if="item.role === 'assistant' && Array.isArray(item.sources) && item.sources.length > 0"
                        class="mt-3 rounded-xl border border-[#d9dcff] bg-white/80 p-3"
                      >
                        <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#6366f1]">参考来源</p>
                        <div class="mt-2 space-y-2">
                          <a
                            v-for="(source, sourceIndex) in item.sources"
                            :key="`${source.title}-${sourceIndex}`"
                            :href="source.url || '#'"
                            class="block rounded-lg border border-slate-100 bg-slate-50 px-2.5 py-2 transition hover:border-[#c7cbff] hover:bg-white"
                          >
                            <p class="text-[12px] font-semibold text-slate-700">{{ source.type }} · {{ source.title }}</p>
                            <p v-if="source.snippet" class="mt-1 line-clamp-2 text-[11px] leading-5 text-slate-500">{{ source.snippet }}</p>
                          </a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="sending" class="flex justify-start">
                  <div class="flex max-w-[88%] items-end gap-2">
                    <div class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-[linear-gradient(135deg,#4f46e5_0%,#8b5cf6_100%)] text-[11px] font-semibold text-white shadow-[0_8px_18px_rgba(99,102,241,0.18)]">
                      AI
                    </div>
                    <div class="rounded-2xl border border-[#d9dcff] bg-[linear-gradient(135deg,#eef2ff_0%,#f5f3ff_100%)] px-3.5 py-3 text-[13px] text-slate-600 shadow-sm">
                      正在思考中...
                    </div>
                  </div>
                </div>
              </div>

              <div class="border-t border-slate-100 bg-white px-4 py-4">
                <div class="mb-2 flex flex-wrap items-center justify-between gap-2 text-xs">
                  <p v-if="errorText" class="text-red-500">{{ errorText }}</p>
                </div>

                <form class="rounded-2xl border border-slate-200 bg-slate-50 p-3" @submit.prevent="handleSend">
                  <input
                    v-model="draft"
                    type="text"
                    maxlength="3000"
                    placeholder="请输入你想咨询的问题..."
                    class="h-10 w-full bg-transparent text-xs text-slate-700 outline-none placeholder:text-slate-400"
                  />
                  <div class="mt-3 flex items-center justify-between gap-2">
                    <div class="flex min-w-0 items-center gap-2 text-xs">
                      <span class="shrink-0 text-slate-400">剩余</span>
                      <span class="shrink-0 font-semibold text-[#4f46e5]">{{ quota?.remaining ?? aiConfig.dailyLimit ?? 0 }} 次</span>
                    </div>
                    <div class="flex items-center gap-2">
                      <select
                        v-model="selectedModel"
                        class="h-8 min-w-0 rounded-lg border border-slate-200 bg-white px-2 text-xs text-slate-700 outline-none transition focus:border-[#6366f1]"
                      >
                        <option v-for="item in aiModels" :key="item" :value="item">{{ item }}</option>
                      </select>
                      <button
                        type="submit"
                        :disabled="sending || !canSend"
                        class="inline-flex items-center rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-2 text-xs font-semibold text-white shadow-[0_10px_24px_rgba(99,102,241,0.18)] transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        发送
                      </button>
                    </div>
                  </div>
                </form>
              </div>
            </template>
          </div>
        </div>
      </div>
    </aside>

    <section class="min-w-0 overflow-hidden rounded-xl border border-slate-200 bg-white">
      <div class="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
        <div>
          <h2 class="text-[22px] font-bold text-slate-900">{{ centerTitle }}</h2>
          <p class="mt-1 text-sm text-slate-400">{{ centerSubtitle }}</p>
        </div>
        <span v-if="hasKeyword" class="text-sm text-slate-400">关键词：{{ keyword }}</span>
      </div>

      <div v-if="loading" class="space-y-4 p-5">
        <div
          v-for="index in 4"
          :key="index"
          class="animate-pulse rounded-[24px] border border-[#d9dcff] bg-white p-5"
        >
          <div class="h-4 w-28 rounded bg-slate-200"></div>
          <div class="mt-4 h-7 w-3/4 rounded bg-slate-200"></div>
          <div class="mt-3 h-4 w-full rounded bg-slate-100"></div>
          <div class="mt-2 h-4 w-5/6 rounded bg-slate-100"></div>
        </div>
      </div>

      <div v-else-if="displayItems.length === 0" class="px-6 py-14 text-center">
        <div class="text-4xl">暂无</div>
        <p class="mt-3 text-slate-500">{{ emptyText }}</p>
      </div>

      <div v-else-if="isNewsMode" class="space-y-4 p-5">
        <NewsCard v-for="item in displayItems" :key="item.id" :article="item" />
      </div>

      <div v-else>
        <PostCard v-for="post in displayItems" :key="post.id" :post="post" />
      </div>

      <div
        v-if="!loading && displayItems.length > 0"
        class="flex items-center justify-center gap-2 border-t border-slate-100 px-5 py-4"
      >
        <button
          :disabled="page === 1"
          class="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-500 transition hover:border-[#1e80ff]/35 hover:text-[#1e80ff] disabled:cursor-not-allowed disabled:opacity-40"
          @click="changePage(page - 1)"
        >
          上一页
        </button>
        <span class="inline-flex h-9 min-w-9 items-center justify-center rounded-lg bg-[#1e80ff] px-3 text-sm font-semibold text-white">
          {{ page }}
        </span>
        <button
          :disabled="page >= totalPages"
          class="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-500 transition hover:border-[#1e80ff]/35 hover:text-[#1e80ff] disabled:cursor-not-allowed disabled:opacity-40"
          @click="changePage(page + 1)"
        >
          下一页
        </button>
      </div>
    </section>

    <aside class="hidden lg:block">
      <div class="sticky top-20 space-y-4">
        <section class="rounded-xl border border-slate-200 bg-white p-4">
          <h3 class="mb-3 text-lg font-semibold text-slate-800">用户资料</h3>
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="h-12 w-12 overflow-hidden rounded-full bg-slate-200">
                <img :src="profileAvatar" alt="avatar" class="h-full w-full object-cover" />
              </div>
              <div>
                <p class="text-base font-semibold text-slate-800">{{ profileName }}</p>
                <p class="text-sm text-slate-400">{{ userSignature }}</p>
              </div>
            </div>
            <RouterLink
              to="/create"
              class="inline-flex h-10 shrink-0 items-center rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(99,102,241,0.18)] transition hover:opacity-90"
            >
              创建文章
            </RouterLink>
          </div>
          <div class="mt-4 rounded-2xl border border-[#eef1ff] bg-[#fafbff] px-4 py-4 text-center">
            <p class="text-2xl font-semibold text-slate-900">{{ userPostCount }}</p>
            <p class="mt-1 text-xs text-slate-400">发表博客数量</p>
          </div>
          <div class="mt-4 space-y-2 rounded-2xl border border-[#d9dcff] bg-[linear-gradient(180deg,#f8f9ff_0%,#ffffff_100%)] p-3">
            <button
              type="button"
              class="inline-flex w-full items-center justify-center rounded-xl border border-[#d9dcff] bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
              @click="toggleHomeMode"
            >
              {{ switchButtonText }}
            </button>
          </div>
        </section>

        <section class="rounded-xl border border-slate-200 bg-white p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="text-lg font-semibold text-slate-800">{{ rankingTitle }}</h3>
            <button class="text-xs text-slate-400 hover:text-[#1e80ff]" @click="refreshRanking">刷新</button>
          </div>
          <ol class="space-y-2 border-t border-slate-100 pt-3">
            <li v-for="item in rankingItems" :key="item.id" class="text-sm text-slate-600">
              <RouterLink :to="rankingLink(item.id)" class="line-clamp-1 hover:text-[#1e80ff]">
                <span class="mr-2 font-semibold" :class="isNewsMode ? 'text-[#f53f3f]' : 'text-[#4f46e5]'">
                  {{ rankingIndex(item) }}
                </span>
                {{ item.title || defaultRankingTitle }}
              </RouterLink>
            </li>
            <li v-if="rankingItems.length === 0" class="text-sm text-slate-400">暂无数据</li>
          </ol>
        </section>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { chatWithAi, getAiConfig, getAiQuota } from '../api/ai'
import { getCategories } from '../api/categories'
import { getNews, getNewsCategories, getTopNews } from '../api/news'
import { getPosts } from '../api/posts'
import { getPostsByUser } from '../api/users'
import NewsCard from '../components/NewsCard.vue'
import PostCard from '../components/PostCard.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const HOME_MODE_KEY = 'home_view_mode'

const page = ref(1)
const pageSize = 10
const homeMode = ref(localStorage.getItem(HOME_MODE_KEY) || 'news')
const selectedFilter = ref('')
const newsCategories = ref([])
const blogCategories = ref([])
const newsItems = ref([])
const blogItems = ref([])
const newsTotal = ref(0)
const blogTotal = ref(0)
const newsFetchDate = ref('')
const topNews = ref([])
const topPosts = ref([])
const userPosts = ref([])
const loading = ref(false)
const showAiDialog = ref(false)
const selectedModel = ref('')
const quota = ref(null)
const draft = ref('')
const sending = ref(false)
const errorText = ref('')
const messageBoxRef = ref(null)
const aiConfig = ref({
  enabled: false,
  provider: '',
  model: '',
  models: [],
  dailyLimit: 0,
  ragEnabled: false
})
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是风中有诗。你可以直接问我写作润色、技术方案、博客总结，或者结合站内文章和新闻来提问。'
  }
])

const isNewsMode = computed(() => homeMode.value !== 'blog')
const keyword = computed(() => (typeof route.query.q === 'string' ? route.query.q.trim() : ''))
const hasKeyword = computed(() => keyword.value.length > 0)
const aiModels = computed(() => {
  if (Array.isArray(aiConfig.value.models) && aiConfig.value.models.length > 0) {
    return aiConfig.value.models
  }
  return aiConfig.value.model ? [aiConfig.value.model] : []
})
const aiCurrentModel = computed(() => selectedModel.value || aiConfig.value.model || '-')
const sidebarItems = computed(() => {
  if (isNewsMode.value) {
    return newsCategories.value.map(item => ({
      key: item.categoryCode || '',
      label: item.categoryName || '未分类',
      count: item.articleCount ?? 0
    }))
  }
  return blogCategories.value.map(item => ({
    key: String(item.id),
    label: item.name || '未分类',
    count: null
  }))
})
const displayItems = computed(() => (isNewsMode.value ? newsItems.value : blogItems.value))
const totalPages = computed(() => {
  const total = isNewsMode.value ? newsTotal.value : blogTotal.value
  return Math.max(1, Math.ceil((total || 0) / pageSize))
})
const resolvedFetchDate = computed(() => formatDateOnly(newsFetchDate.value))
const centerTitle = computed(() => (isNewsMode.value ? '热度新闻' : '博客文章'))
const centerSubtitle = computed(() => {
  if (isNewsMode.value) {
    return `按热度排序展示最新聚合新闻${resolvedFetchDate.value ? ` · ${resolvedFetchDate.value}` : ''}`
  }
  return '按照发布时间展示站内博客内容'
})
const emptyText = computed(() => {
  if (isNewsMode.value) {
    return hasKeyword.value ? '未找到匹配新闻' : '还没有新闻数据'
  }
  return hasKeyword.value ? '未找到匹配文章' : '还没有博客文章'
})
const profileName = computed(() => auth.user?.username || '请登录')
const profileAvatar = computed(() => auth.user?.avatar || 'https://dummyimage.com/100x100/e2e8f0/64748b&text=U')
const userSignature = computed(() => auth.user?.bio || '这个人很神秘，什么都没留下')
const userPostCount = computed(() => userPosts.value.length)
const switchButtonText = computed(() => (isNewsMode.value ? '浏览博客' : '浏览新闻'))
const rankingTitle = computed(() => (isNewsMode.value ? '热度新闻 Top 10' : '最新博客'))
const rankingItems = computed(() => (isNewsMode.value ? topNews.value : topPosts.value))
const defaultRankingTitle = computed(() => (isNewsMode.value ? '未命名新闻' : '未命名文章'))
const aiRemainingText = computed(() => {
  if (!aiConfig.value.enabled) return '未开启'
  if (!auth.isLoggedIn) return `登录后可用 ${aiConfig.value.dailyLimit ?? 0} 次`
  return `${quota.value?.remaining ?? aiConfig.value.dailyLimit ?? 0} 次`
})
const canSend = computed(() => {
  if (!auth.isLoggedIn || !aiConfig.value.enabled) return false
  if (!draft.value.trim()) return false
  if (quota.value && quota.value.remaining <= 0) return false
  return true
})

function tryDecodeUtf8Mojibake(value) {
  if (typeof value !== 'string' || !value) {
    return value || ''
  }
  if (!/[ÃÂÆÐæçèéêëìíîïðñòóôõöøùúûüýþÿ]/.test(value) && !/[�]/.test(value)) {
    return value
  }
  try {
    return decodeURIComponent(escape(value))
  } catch (error) {
    return value
  }
}

function normalizeDisplayText(value) {
  return tryDecodeUtf8Mojibake(value || '')
}

function normalizeSources(sources) {
  if (!Array.isArray(sources)) {
    return []
  }
  return sources.map(source => ({
    ...source,
    type: normalizeDisplayText(source?.type || ''),
    title: normalizeDisplayText(source?.title || ''),
    snippet: normalizeDisplayText(source?.snippet || '')
  }))
}

function normalizeAiConfig(data) {
  return {
    enabled: data?.enabled ?? false,
    provider: data?.provider || '',
    model: data?.model || '',
    models: Array.isArray(data?.models) ? data.models : [],
    dailyLimit: data?.dailyLimit ?? 0,
    ragEnabled: data?.ragEnabled ?? false
  }
}

function formatDateOnly(dateText) {
  if (!dateText) return ''
  return new Date(dateText).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function rankingIndex(item) {
  return isNewsMode.value ? item.rankOrder || '-' : topPosts.value.findIndex(post => post.id === item.id) + 1
}

function rankingLink(id) {
  return isNewsMode.value ? `/news/${id}` : `/post/${id}`
}

function persistHomeMode() {
  localStorage.setItem(HOME_MODE_KEY, homeMode.value)
}

async function loadNews(targetPage = 1) {
  const result = await getNews({
    page: targetPage,
    size: pageSize,
    ...(selectedFilter.value ? { category: selectedFilter.value } : {}),
    ...(hasKeyword.value ? { keyword: keyword.value } : {})
  })
  newsItems.value = Array.isArray(result?.list) ? result.list : []
  newsTotal.value = Number(result?.total || 0)
  newsFetchDate.value = result?.fetchDate || ''
  page.value = Number(result?.page || targetPage)
}

async function loadBlogs(targetPage = 1) {
  const posts = await getPosts(targetPage, pageSize, keyword.value)
  const filtered = selectedFilter.value
    ? posts.filter(post => String(post.categoryId) === selectedFilter.value)
    : posts
  blogItems.value = filtered
  blogTotal.value = filtered.length < pageSize && targetPage === 1 ? filtered.length : Math.max(filtered.length, targetPage * pageSize)
  page.value = targetPage
}

async function loadNewsCategoriesList() {
  try {
    const result = await getNewsCategories()
    newsCategories.value = Array.isArray(result) ? result : []
  } catch (error) {
    newsCategories.value = []
  }
}

async function loadBlogCategoriesList() {
  try {
    const result = await getCategories()
    blogCategories.value = Array.isArray(result) ? result : []
  } catch (error) {
    blogCategories.value = []
  }
}

async function loadTopNewsList() {
  try {
    const result = await getTopNews({
      limit: 10,
      ...(selectedFilter.value && isNewsMode.value ? { category: selectedFilter.value } : {})
    })
    topNews.value = Array.isArray(result?.list) ? result.list : []
  } catch (error) {
    topNews.value = []
  }
}

async function loadTopPostsList() {
  try {
    const posts = await getPosts(1, 10, '')
    topPosts.value = [...posts]
      .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
      .slice(0, 10)
  } catch (error) {
    topPosts.value = []
  }
}

async function loadUserPosts() {
  if (!auth.user?.id) {
    userPosts.value = []
    return
  }
  try {
    const result = await getPostsByUser(auth.user.id)
    userPosts.value = Array.isArray(result) ? result : []
  } catch (error) {
    userPosts.value = []
  }
}

async function loadAiPanel() {
  try {
    aiConfig.value = normalizeAiConfig(await getAiConfig())
  } catch (error) {
    aiConfig.value = normalizeAiConfig({})
  }

  if (!selectedModel.value || !aiModels.value.includes(selectedModel.value)) {
    selectedModel.value = aiConfig.value.model || aiModels.value[0] || ''
  }

  if (!auth.isLoggedIn || !aiConfig.value.enabled) {
    quota.value = null
    return
  }

  try {
    quota.value = await getAiQuota()
  } catch (error) {
    quota.value = null
  }
}

async function loadCurrentMode(targetPage = 1) {
  loading.value = true
  try {
    if (isNewsMode.value) {
      await loadNews(targetPage)
    } else {
      await loadBlogs(targetPage)
    }
  } catch (error) {
    if (isNewsMode.value) {
      newsItems.value = []
      newsTotal.value = 0
    } else {
      blogItems.value = []
      blogTotal.value = 0
    }
    page.value = targetPage
  } finally {
    loading.value = false
  }
}

async function openAiDialog() {
  showAiDialog.value = true
  await loadAiPanel()
  await scrollToBottom()
}

function closeAiDialog() {
  showAiDialog.value = false
  errorText.value = ''
}

async function handleFilterChange(filterKey) {
  selectedFilter.value = filterKey
  await loadCurrentMode(1)
  await refreshRanking()
}

async function toggleHomeMode() {
  homeMode.value = isNewsMode.value ? 'blog' : 'news'
  selectedFilter.value = ''
  persistHomeMode()
  await loadCurrentMode(1)
  await refreshRanking()
}

async function refreshRanking() {
  if (isNewsMode.value) {
    await loadTopNewsList()
  } else {
    await loadTopPostsList()
  }
}

async function handleSend() {
  if (!canSend.value) return

  const content = draft.value.trim()
  const history = messages.value
    .filter(item => item.role === 'user' || item.role === 'assistant')
    .slice(-10)
    .map(item => ({ role: item.role, content: item.content }))

  messages.value.push({ role: 'user', content })
  draft.value = ''
  errorText.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const response = await chatWithAi({
      message: content,
      model: selectedModel.value || aiConfig.value.model,
      history
    })
    messages.value.push({
      role: 'assistant',
      content: normalizeDisplayText(response.answer || '暂时没有生成内容。'),
      sources: normalizeSources(response.sources)
    })
    if (response.model) {
      selectedModel.value = response.model
    }
    quota.value = {
      dailyLimit: response.dailyLimit,
      used: response.used,
      remaining: response.remaining
    }
  } catch (error) {
    errorText.value = normalizeDisplayText(error.response?.data?.message || error.response?.data || '发送失败，请稍后再试。')
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messageBoxRef.value) {
    messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
  }
}

async function changePage(targetPage) {
  if (targetPage < 1 || targetPage > totalPages.value) return
  await loadCurrentMode(targetPage)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(
  () => route.query.q,
  async () => {
    selectedFilter.value = ''
    await loadCurrentMode(1)
    await refreshRanking()
  }
)

watch(
  () => auth.isLoggedIn,
  async () => {
    await loadAiPanel()
    await loadUserPosts()
  }
)

watch(
  () => auth.user?.id,
  async () => {
    await loadUserPosts()
  }
)

onMounted(async () => {
  persistHomeMode()
  await Promise.all([loadNewsCategoriesList(), loadBlogCategoriesList(), loadAiPanel(), loadUserPosts()])
  await loadCurrentMode(1)
  await refreshRanking()
})
</script>
