<template>
  <div class="grid grid-cols-1 items-start gap-4 lg:grid-cols-[220px_minmax(0,1fr)_300px] xl:gap-5 xl:grid-cols-[240px_minmax(0,1fr)_320px]">
      <aside class="hidden lg:block">
        <div class="sticky top-20 space-y-4">
          <section class="rounded-xl border border-slate-200 bg-white p-3">
            <h3 class="mb-2 px-2 text-base font-semibold text-slate-700">分类</h3>
            <ul class="space-y-1">
              <li
                class="cursor-pointer rounded-lg px-3 py-2 transition"
                :class="selectedCategoryId === null ? 'bg-[#f4f7ff] font-medium text-[#1e80ff]' : 'text-slate-600 hover:bg-[#f4f7ff] hover:text-[#1e80ff]'"
                @click="selectedCategoryId = null"
              >
                全部
              </li>
              <li
                v-for="item in categories"
                :key="item.id"
                class="cursor-pointer rounded-lg px-3 py-2 transition"
                :class="selectedCategoryId === item.id ? 'bg-[#f4f7ff] font-medium text-[#1e80ff]' : 'text-slate-600 hover:bg-[#f4f7ff] hover:text-[#1e80ff]'"
                @click="selectedCategoryId = item.id"
              >
                {{ item.name }}
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
                  <p class="text-sm font-semibold uppercase tracking-[0.18em] text-[#6366f1]">风</p>
                  <h3 class="mt-2 text-[18px] font-bold text-slate-900">智能体助手</h3>
                </div>
                <div class="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-[linear-gradient(135deg,#4f46e5_0%,#8b5cf6_100%)] text-lg font-bold text-white shadow-[0_12px_24px_rgba(99,102,241,0.22)]">
                  风
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
                点击我开始提问
              </div>
            </button>

            <div
              v-if="showAiDialog"
              class="absolute bottom-0 left-0 z-[70] w-[380px] overflow-hidden rounded-[24px] border border-slate-200 bg-white shadow-[0_20px_48px_rgba(15,23,42,0.12)]"
            >
              <div class="flex items-center justify-between border-b border-slate-100 bg-[linear-gradient(180deg,#ffffff_0%,#f8f9ff_100%)] px-4 py-4">
                <div class="flex items-center gap-3">
                  <div class="grid h-10 w-10 place-items-center rounded-2xl bg-[linear-gradient(135deg,#4f46e5_0%,#8b5cf6_100%)] text-sm font-bold text-white shadow-[0_10px_20px_rgba(99,102,241,0.2)]">风</div>
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
                <p class="text-sm font-semibold text-slate-800">风 未开启</p>
                <p class="mt-2 text-xs text-slate-500">请先在后端配置里开启智能体。</p>
              </div>

              <div v-else-if="!auth.isLoggedIn" class="px-4 py-10 text-center">
                <p class="text-sm font-semibold text-slate-800">登录后即可使用 风</p>
                <p class="mt-2 text-xs text-slate-500">普通用户每日提问次数受配置限制。</p>
                <RouterLink
                  to="/login"
                  class="mt-4 inline-flex items-center rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-2 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(99,102,241,0.18)]"
                  @click="closeAiDialog"
                >
                  去登录
                </RouterLink>
              </div>

              <template v-else>
                <div ref="messageBoxRef" class="h-[220px] space-y-3 overflow-y-auto bg-[linear-gradient(180deg,#f8fafc_0%,#f5f7ff_100%)] px-3 py-3">
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
                        {{ item.role === 'user' ? '我' : '风' }}
                      </div>
                      <div
                        class="rounded-2xl px-3.5 py-3 text-[13px] leading-6 shadow-sm"
                        :class="item.role === 'user'
                          ? 'border border-slate-200 bg-white text-slate-700'
                          : 'border border-[#d9dcff] bg-[linear-gradient(135deg,#eef2ff_0%,#f5f3ff_100%)] text-slate-800'"
                      >
                        <p class="whitespace-pre-wrap break-words">{{ item.content }}</p>
                      </div>
                    </div>
                  </div>

                  <div v-if="sending" class="flex justify-start">
                    <div class="flex max-w-[88%] items-end gap-2">
                      <div class="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-[linear-gradient(135deg,#4f46e5_0%,#8b5cf6_100%)] text-[11px] font-semibold text-white shadow-[0_8px_18px_rgba(99,102,241,0.18)]">
                        风
                      </div>
                      <div class="rounded-2xl border border-[#d9dcff] bg-[linear-gradient(135deg,#eef2ff_0%,#f5f3ff_100%)] px-3.5 py-3 text-[13px] text-slate-600 shadow-sm">
                        风 正在思考中...
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
        <div class="flex items-center border-b border-slate-100 px-5">
          <button
            type="button"
            class="relative py-4 text-[19px] font-medium"
            :class="activeTab === 'recommend' ? 'text-[#1e80ff]' : 'text-slate-500'"
            @click="activeTab = 'recommend'"
          >
            推荐
            <span
              v-if="activeTab === 'recommend'"
              class="absolute bottom-0 left-0 h-[3px] w-full rounded bg-[#1e80ff]"
            ></span>
          </button>
          <button
            type="button"
            class="relative ml-6 py-4 text-[19px] font-medium"
            :class="activeTab === 'latest' ? 'text-[#1e80ff]' : 'text-slate-500'"
            @click="activeTab = 'latest'"
          >
            最新
            <span
              v-if="activeTab === 'latest'"
              class="absolute bottom-0 left-0 h-[3px] w-full rounded bg-[#1e80ff]"
            ></span>
          </button>
          <span v-if="hasKeyword" class="ml-auto text-sm text-slate-400">关键词：{{ keyword }}</span>
        </div>

        <div v-if="store.loading" class="space-y-2 p-5">
          <div v-for="i in 4" :key="i" class="animate-pulse rounded-lg border border-slate-100 p-4">
            <div class="mb-3 h-5 w-2/3 rounded bg-slate-200"></div>
            <div class="mb-2 h-4 w-full rounded bg-slate-100"></div>
            <div class="h-4 w-4/5 rounded bg-slate-100"></div>
          </div>
        </div>

        <div v-else-if="displayPosts.length === 0" class="px-6 py-14 text-center">
          <div class="text-4xl">🗂</div>
          <p class="mt-3 text-slate-500">{{ hasKeyword ? '未找到匹配文章' : '还没有文章' }}</p>
        </div>

        <div v-else>
          <PostCard v-for="post in displayPosts" :key="post.id" :post="post" />
        </div>

        <div v-if="!store.loading && displayPosts.length > 0" class="flex items-center justify-center gap-2 border-t border-slate-100 px-5 py-4">
          <button
            @click="changePage(page - 1)"
            :disabled="page === 1"
            class="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-500 transition hover:border-[#1e80ff]/35 hover:text-[#1e80ff] disabled:cursor-not-allowed disabled:opacity-40"
          >
            上一页
          </button>
          <span class="inline-flex h-9 min-w-9 items-center justify-center rounded-lg bg-[#1e80ff] px-3 text-sm font-semibold text-white">
            {{ page }}
          </span>
          <button
            @click="changePage(page + 1)"
            :disabled="store.posts.length < pageSize"
            class="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm text-slate-500 transition hover:border-[#1e80ff]/35 hover:text-[#1e80ff] disabled:cursor-not-allowed disabled:opacity-40"
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
                  <p class="text-sm text-slate-400">{{ profileDesc }}</p>
                </div>
              </div>
              <RouterLink
                to="/create"
                class="inline-flex h-10 shrink-0 items-center rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(99,102,241,0.18)] transition hover:opacity-90"
              >
                创建文章
              </RouterLink>
            </div>
            <div class="mt-3 grid grid-cols-3 gap-2 text-center">
              <div>
                <p class="text-lg font-semibold text-slate-800">{{ store.posts.length }}</p>
                <p class="text-xs text-slate-400">当前页文章</p>
              </div>
              <div>
                <p class="text-lg font-semibold text-slate-800">{{ totalViews }}</p>
                <p class="text-xs text-slate-400">阅读量</p>
              </div>
              <div>
                <p class="text-lg font-semibold text-slate-800">{{ totalLikes }}</p>
                <p class="text-xs text-slate-400">点赞量</p>
              </div>
            </div>
            <div class="mt-4 space-y-2 rounded-2xl border border-[#d9dcff] bg-[linear-gradient(180deg,#f8f9ff_0%,#ffffff_100%)] p-3">
              <RouterLink
                to="/news"
                class="inline-flex w-full items-center justify-center rounded-xl border border-[#d9dcff] bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
              >
                浏览 AI 新闻
              </RouterLink>
              <RouterLink
                v-if="auth.isLoggedIn"
                to="/news-admin"
                class="inline-flex w-full items-center justify-center rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-2.5 text-sm font-semibold text-white shadow-[0_10px_24px_rgba(99,102,241,0.18)] transition hover:opacity-90"
              >
                进入新闻后台
              </RouterLink>
            </div>
          </section>

          <section class="rounded-xl border border-slate-200 bg-white p-4">
            <h3 class="mb-3 text-lg font-semibold text-slate-800">日历信息</h3>
            <div class="mb-2 text-sm text-slate-500">{{ calendarTitle }}</div>
            <div class="grid grid-cols-7 gap-1 text-center text-xs text-slate-400">
              <span v-for="day in weekDays" :key="day">{{ day }}</span>
            </div>
            <div class="mt-1 grid grid-cols-7 gap-1 text-center text-xs">
              <span
                v-for="(cell, idx) in calendarCells"
                :key="idx"
                class="inline-flex h-7 items-center justify-center rounded"
                :class="cell === today ? 'bg-[#1e80ff] text-white' : 'text-slate-500'"
              >
                {{ cell || '' }}
              </span>
            </div>
          </section>

          <section class="rounded-xl border border-slate-200 bg-white p-4">
            <div class="mb-3 flex items-center justify-between">
              <h3 class="text-lg font-semibold text-slate-800">点赞 Top 10</h3>
              <button class="text-xs text-slate-400 hover:text-[#1e80ff]" @click="loadTopLikedPosts">刷新</button>
            </div>
            <ol class="space-y-2 border-t border-slate-100 pt-3">
              <li v-for="(item, idx) in topLikedPosts" :key="item.id" class="text-sm text-slate-600">
                <RouterLink :to="`/post/${item.id}`" class="line-clamp-1 hover:text-[#1e80ff]">
                  <span class="mr-2 font-semibold" :class="idx < 3 ? 'text-[#f53f3f]' : 'text-slate-400'">{{ idx + 1 }}</span>
                  {{ item.title || '未命名文章' }}
                </RouterLink>
              </li>
              <li v-if="topLikedPosts.length === 0" class="text-sm text-slate-400">暂无数据</li>
            </ol>
          </section>
        </div>
      </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { chatWithAi, getAiConfig, getAiQuota } from '../api/ai'
import { getCategories } from '../api/categories'
import { getPosts } from '../api/posts'
import PostCard from '../components/PostCard.vue'
import { usePostsStore } from '../stores/posts'

const route = useRoute()
const auth = useAuthStore()
const store = usePostsStore()

const page = ref(1)
const pageSize = 10
const activeTab = ref('recommend')
const categories = ref([])
const selectedCategoryId = ref(null)
const topLikedPosts = ref([])
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
  dailyLimit: 0
})
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是风中有诗。你可以直接问我写作润色、技术方案、博客总结或其他任务。'
  }
])

const keyword = computed(() => (typeof route.query.q === 'string' ? route.query.q.trim() : ''))
const hasKeyword = computed(() => keyword.value.length > 0)
const aiModels = computed(() => {
  if (Array.isArray(aiConfig.value.models) && aiConfig.value.models.length > 0) {
    return aiConfig.value.models
  }
  return aiConfig.value.model ? [aiConfig.value.model] : []
})
const aiCurrentModel = computed(() => selectedModel.value || aiConfig.value.model || '-')

const filteredPosts = computed(() => {
  if (selectedCategoryId.value == null) return store.posts
  return store.posts.filter(post => Number(post.categoryId) === Number(selectedCategoryId.value))
})

const displayPosts = computed(() => {
  const list = [...filteredPosts.value]
  if (activeTab.value === 'recommend') {
    return list.sort((a, b) => Number(b.viewCount || 0) - Number(a.viewCount || 0))
  }
  return list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
})

const profileName = computed(() => auth.user?.username || '请登录')
const profileAvatar = computed(() => auth.user?.avatar || 'https://dummyimage.com/100x100/e2e8f0/64748b&text=U')
const profileDesc = computed(() => (auth.isLoggedIn ? '社区活跃用户' : '登录后可同步个人资料'))
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

const totalViews = computed(() =>
  displayPosts.value.reduce((sum, post) => sum + Number(post.viewCount || 0), 0)
)

const totalLikes = computed(() =>
  displayPosts.value.reduce((sum, post) => sum + Number(post.likeCount || 0), 0)
)

const now = new Date()
const year = now.getFullYear()
const month = now.getMonth() + 1
const today = now.getDate()
const calendarTitle = `${year} 年 ${month} 月`
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const calendarCells = computed(() => {
  const firstDay = new Date(year, month - 1, 1).getDay()
  const monthDays = new Date(year, month, 0).getDate()
  const cells = Array(firstDay).fill(0)
  for (let i = 1; i <= monthDays; i += 1) {
    cells.push(i)
  }
  while (cells.length % 7 !== 0) {
    cells.push(0)
  }
  return cells
})

function normalizeAiConfig(data) {
  return {
    enabled: data?.enabled ?? false,
    provider: data?.provider || '',
    model: data?.model || '',
    models: Array.isArray(data?.models) ? data.models : [],
    dailyLimit: data?.dailyLimit ?? 0
  }
}

async function loadPosts(targetPage = 1) {
  if (targetPage < 1) return
  page.value = targetPage
  await store.fetchPosts(targetPage, pageSize, keyword.value)
}

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch (error) {
    categories.value = []
  }
}

async function loadTopLikedPosts() {
  const posts = await getPosts(1, 100, '')
  topLikedPosts.value = [...posts]
    .sort((a, b) => Number(b.likeCount || 0) - Number(a.likeCount || 0))
    .slice(0, 10)
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

async function openAiDialog() {
  showAiDialog.value = true
  await loadAiPanel()
  await scrollToBottom()
}

function closeAiDialog() {
  showAiDialog.value = false
  errorText.value = ''
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
      content: response.answer || '暂时没有生成内容。'
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
    errorText.value = error.response?.data?.message || error.response?.data || '发送失败，请稍后再试'
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

async function changePage(n) {
  await loadPosts(n)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(
  () => route.query.q,
  async () => {
    await loadPosts(1)
  }
)

watch(
  () => auth.isLoggedIn,
  async () => {
    await loadAiPanel()
  }
)

onMounted(async () => {
  await Promise.all([loadCategories(), loadPosts(1), loadTopLikedPosts(), loadAiPanel()])
})
</script>
