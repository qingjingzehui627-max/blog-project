<template>
  <div class="grid gap-5 xl:grid-cols-[280px_minmax(0,1fr)]">
    <aside class="space-y-4">
      <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#6366f1]">AI News</p>
        <h1 class="mt-3 text-[28px] font-black leading-tight text-slate-900">
          AI 科技新闻聚合
        </h1>
        <p class="mt-3 text-sm leading-7 text-slate-600">
          每日聚合 Guardian 的 AI 与科技新闻，支持分类浏览、日期切换、关键词筛选和后台手动抓取。
        </p>

        <div class="mt-5 flex flex-wrap gap-3">
          <RouterLink
            v-if="auth.isLoggedIn"
            to="/news-admin"
            class="inline-flex items-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-2.5 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(99,102,241,0.18)] transition hover:opacity-90"
          >
            进入新闻后台
          </RouterLink>
          <button
            type="button"
            class="inline-flex items-center rounded-2xl border border-[#d9dcff] bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
            @click="refreshPage"
          >
            刷新列表
          </button>
        </div>
      </section>

      <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
        <h2 class="text-lg font-bold text-slate-900">筛选条件</h2>

        <label class="mt-4 block text-sm font-medium text-slate-600">
          关键词
          <input
            v-model.trim="filters.keyword"
            type="text"
            placeholder="例如 OpenAI / Nvidia"
            class="mt-2 h-11 w-full rounded-2xl border border-[#d9dcff] bg-[#fbfbff] px-4 text-sm text-slate-700 outline-none transition focus:border-[#6366f1]"
            @keyup.enter="applyFilters"
          />
        </label>

        <label class="mt-4 block text-sm font-medium text-slate-600">
          抓取日期
          <input
            v-model="filters.date"
            type="date"
            class="mt-2 h-11 w-full rounded-2xl border border-[#d9dcff] bg-[#fbfbff] px-4 text-sm text-slate-700 outline-none transition focus:border-[#6366f1]"
          />
        </label>

        <div class="mt-4 rounded-2xl border border-[#eef1ff] bg-[#fafbff] p-3">
          <div class="flex items-center justify-between gap-3">
            <p class="text-sm font-medium text-slate-600">按日期切换</p>
            <span class="text-xs text-slate-400">{{ quickDateLabel }}</span>
          </div>
          <div class="mt-3 grid grid-cols-3 gap-2">
            <button
              type="button"
              class="rounded-xl border border-[#d9dcff] bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
              @click="shiftDate(-1)"
            >
              前一天
            </button>
            <button
              type="button"
              class="rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-3 py-2 text-sm font-semibold text-white shadow-[0_10px_20px_rgba(99,102,241,0.18)] transition hover:opacity-90"
              @click="useLatestFetchDate"
            >
              最新抓取
            </button>
            <button
              type="button"
              class="rounded-xl border border-[#d9dcff] bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5] disabled:cursor-not-allowed disabled:opacity-40"
              :disabled="!canGoNextDate"
              @click="shiftDate(1)"
            >
              后一天
            </button>
          </div>
        </div>

        <div class="mt-4">
          <p class="text-sm font-medium text-slate-600">新闻分类</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <button
              type="button"
              class="rounded-full px-3 py-2 text-sm font-medium transition"
              :class="!filters.category ? activePillClass : idlePillClass"
              @click="setCategory('')"
            >
              全部
            </button>
            <button
              v-for="item in categoryOptions"
              :key="item.categoryCode"
              type="button"
              class="rounded-full px-3 py-2 text-sm font-medium transition"
              :class="filters.category === item.categoryCode ? activePillClass : idlePillClass"
              @click="setCategory(item.categoryCode)"
            >
              {{ item.categoryName }}<span class="ml-1 text-xs opacity-70">({{ item.articleCount }})</span>
            </button>
          </div>
        </div>

        <div class="mt-5 flex gap-3">
          <button
            type="button"
            class="inline-flex flex-1 items-center justify-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-3 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(99,102,241,0.18)] transition hover:opacity-90"
            @click="applyFilters"
          >
            应用筛选
          </button>
          <button
            type="button"
            class="inline-flex flex-1 items-center justify-center rounded-2xl border border-[#d9dcff] bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
            @click="resetFilters"
          >
            重置
          </button>
        </div>
      </section>

      <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold text-slate-900">今日 Top 新闻</h2>
          <span class="text-xs font-medium text-slate-400">{{ fetchDateLabel }}</span>
        </div>
        <ol class="mt-4 space-y-3">
          <li v-for="item in topNews" :key="item.id">
            <RouterLink
              :to="`/news/${item.id}`"
              class="flex items-start gap-3 rounded-2xl border border-transparent px-3 py-2 transition hover:border-[#d9dcff] hover:bg-[#fafbff]"
            >
              <span class="mt-0.5 inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#eef2ff] text-xs font-bold text-[#4f46e5]">
                {{ item.rankOrder }}
              </span>
              <span class="line-clamp-2 text-sm font-medium leading-6 text-slate-700">
                {{ item.title }}
              </span>
            </RouterLink>
          </li>
          <li v-if="topNews.length === 0" class="rounded-2xl bg-[#fafbff] px-4 py-6 text-sm text-slate-400">
            暂无 Top 新闻数据
          </li>
        </ol>
      </section>
    </aside>

    <section class="space-y-4">
      <div class="rounded-[30px] border border-[#d9dcff] bg-[linear-gradient(135deg,#ffffff_0%,#f7f8ff_100%)] p-6 shadow-[0_18px_42px_rgba(99,102,241,0.1)]">
        <div class="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#6366f1]">Overview</p>
            <h2 class="mt-2 text-[26px] font-black text-slate-900">
              {{ statsTitle }}
            </h2>
            <p class="mt-2 text-sm text-slate-600">
              当前展示 {{ total }} 条新闻，日期范围聚焦于 {{ fetchDateLabel }}。
            </p>
          </div>
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <div class="rounded-2xl border border-white/70 bg-white/80 px-4 py-3">
              <p class="text-xs text-slate-400">当前页</p>
              <p class="mt-1 text-xl font-black text-slate-900">{{ page }}</p>
            </div>
            <div class="rounded-2xl border border-white/70 bg-white/80 px-4 py-3">
              <p class="text-xs text-slate-400">每页数量</p>
              <p class="mt-1 text-xl font-black text-slate-900">{{ size }}</p>
            </div>
            <div class="rounded-2xl border border-white/70 bg-white/80 px-4 py-3">
              <p class="text-xs text-slate-400">总量</p>
              <p class="mt-1 text-xl font-black text-slate-900">{{ total }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-[#6366f1]">Date Switch</p>
              <h3 class="mt-2 text-xl font-black text-slate-900">按日期切换</h3>
            </div>
            <span class="rounded-full bg-[#eef2ff] px-3 py-1 text-xs font-semibold text-[#4f46e5]">
              {{ resolvedDateLabel }}
            </span>
          </div>

          <div class="mt-4 rounded-2xl border border-[#eef1ff] bg-[#fafbff] p-4">
            <p class="text-sm text-slate-500">当前筛选日期</p>
            <p class="mt-2 text-2xl font-black text-slate-900">{{ resolvedDateLabel }}</p>
            <p class="mt-2 text-sm leading-6 text-slate-500">
              可快速回看前一天新闻，或一键回到最近一次抓取结果。
            </p>
          </div>

          <div class="mt-4 grid grid-cols-3 gap-2">
            <button
              type="button"
              class="rounded-xl border border-[#d9dcff] bg-white px-3 py-2.5 text-sm font-medium text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
              @click="shiftDate(-1)"
            >
              前一天
            </button>
            <button
              type="button"
              class="rounded-xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-3 py-2.5 text-sm font-semibold text-white shadow-[0_10px_20px_rgba(99,102,241,0.18)] transition hover:opacity-90"
              @click="useLatestFetchDate"
            >
              最新抓取
            </button>
            <button
              type="button"
              class="rounded-xl border border-[#d9dcff] bg-white px-3 py-2.5 text-sm font-medium text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5] disabled:cursor-not-allowed disabled:opacity-40"
              :disabled="!canGoNextDate"
              @click="shiftDate(1)"
            >
              后一天
            </button>
          </div>
        </section>

        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex items-start justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-[#6366f1]">Category Chart</p>
              <h3 class="mt-2 text-xl font-black text-slate-900">分类统计图</h3>
            </div>
            <span class="rounded-full bg-[#eef2ff] px-3 py-1 text-xs font-semibold text-[#4f46e5]">
              共 {{ chartTotal }} 条
            </span>
          </div>

          <div v-if="chartItems.length > 0" class="mt-4 space-y-4">
            <div v-for="item in chartItems" :key="item.categoryCode" class="space-y-2">
              <div class="flex items-center justify-between gap-3 text-sm">
                <span class="font-medium text-slate-700">{{ item.categoryName }}</span>
                <span class="text-slate-400">{{ item.articleCount }} 条 / {{ item.percentLabel }}</span>
              </div>
              <div class="h-3 overflow-hidden rounded-full bg-[#eef1ff]">
                <div
                  class="h-full rounded-full bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)]"
                  :style="{ width: `${item.barWidth}%` }"
                ></div>
              </div>
            </div>
          </div>
          <div v-else class="mt-4 rounded-2xl bg-[#fafbff] px-4 py-10 text-center text-sm text-slate-400">
            当前日期暂无分类统计数据
          </div>
        </section>
      </div>

      <div
        v-if="errorText"
        class="rounded-[24px] border border-red-100 bg-red-50 px-5 py-4 text-sm text-red-500"
      >
        {{ errorText }}
      </div>

      <div v-if="loading" class="space-y-4">
        <div
          v-for="index in 4"
          :key="index"
          class="animate-pulse rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]"
        >
          <div class="h-4 w-32 rounded bg-slate-200"></div>
          <div class="mt-4 h-7 w-3/4 rounded bg-slate-200"></div>
          <div class="mt-3 h-4 w-full rounded bg-slate-100"></div>
          <div class="mt-2 h-4 w-5/6 rounded bg-slate-100"></div>
        </div>
      </div>

      <div
        v-else-if="articles.length === 0"
        class="rounded-[28px] border border-[#d9dcff] bg-white px-6 py-16 text-center shadow-[0_16px_38px_rgba(99,102,241,0.08)]"
      >
        <div class="text-5xl">📰</div>
        <p class="mt-4 text-lg font-semibold text-slate-800">暂无符合条件的新闻</p>
        <p class="mt-2 text-sm text-slate-500">可以尝试切换日期、分类或清空关键词重新查询。</p>
      </div>

      <div v-else class="space-y-4">
        <NewsCard v-for="item in articles" :key="item.id" :article="item" />
      </div>

      <div
        v-if="total > 0"
        class="flex flex-wrap items-center justify-between gap-3 rounded-[24px] border border-[#d9dcff] bg-white px-5 py-4 shadow-[0_12px_28px_rgba(99,102,241,0.08)]"
      >
        <p class="text-sm text-slate-500">
          第 {{ page }} 页，共 {{ totalPages }} 页
        </p>
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="rounded-2xl border border-[#d9dcff] bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page <= 1"
            @click="changePage(page - 1)"
          >
            上一页
          </button>
          <button
            type="button"
            class="rounded-2xl border border-[#d9dcff] bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page >= totalPages"
            @click="changePage(page + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNews, getNewsCategories, getTopNews } from '../api/news'
import NewsCard from '../components/NewsCard.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const articles = ref([])
const topNews = ref([])
const categoryOptions = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const fetchDate = ref('')
const errorText = ref('')

const filters = reactive({
  keyword: '',
  category: '',
  date: ''
})

const activePillClass = 'bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] text-white shadow-[0_10px_20px_rgba(99,102,241,0.18)]'
const idlePillClass = 'border border-[#d9dcff] bg-white text-slate-600 hover:border-[#6366f1] hover:text-[#4f46e5]'

const totalPages = computed(() => Math.max(1, Math.ceil((total.value || 0) / size.value)))
const resolvedDateValue = computed(() => filters.date || fetchDate.value || '')
const fetchDateLabel = computed(() => formatDateOnly(fetchDate.value || filters.date))
const resolvedDateLabel = computed(() => formatDateOnly(resolvedDateValue.value))
const quickDateLabel = computed(() => resolvedDateValue.value ? `当前 ${resolvedDateLabel.value}` : '未指定日期')
const statsTitle = computed(() => {
  if (filters.category && categoryOptions.value.length > 0) {
    const category = categoryOptions.value.find(item => item.categoryCode === filters.category)
    return category ? `${category.categoryName} 新闻列表` : '新闻列表'
  }
  return '全部新闻列表'
})
const chartTotal = computed(() =>
  categoryOptions.value.reduce((sum, item) => sum + Number(item.articleCount || 0), 0)
)
const chartItems = computed(() => {
  if (!Array.isArray(categoryOptions.value) || categoryOptions.value.length === 0) {
    return []
  }

  const totalCount = chartTotal.value || 1
  return [...categoryOptions.value]
    .map(item => {
      const articleCount = Number(item.articleCount || 0)
      const rawPercent = (articleCount / totalCount) * 100
      return {
        ...item,
        articleCount,
        barWidth: articleCount > 0 ? Math.max(8, Number(rawPercent.toFixed(2))) : 0,
        percentLabel: `${rawPercent.toFixed(1)}%`
      }
    })
    .sort((a, b) => b.articleCount - a.articleCount)
})
const canGoNextDate = computed(() => {
  const current = parseDateValue(resolvedDateValue.value)
  if (!current) return false

  const today = parseDateValue(formatDateInput(new Date()))
  if (!today) return false
  return current.getTime() < today.getTime()
})

/**
 * 从路由初始化筛选条件。
 */
function syncFromRoute() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.category = typeof route.query.category === 'string' ? route.query.category : ''
  filters.date = typeof route.query.date === 'string' ? route.query.date : ''
  page.value = Number(route.query.page || 1)
}

/**
 * 将筛选条件同步回路由。
 */
function syncToRoute(targetPage = 1) {
  const query = {}
  if (filters.keyword) query.keyword = filters.keyword
  if (filters.category) query.category = filters.category
  if (filters.date) query.date = filters.date
  if (targetPage > 1) query.page = String(targetPage)
  router.push({ path: '/news', query })
}

/**
 * 拉取新闻列表。
 */
async function loadNews() {
  loading.value = true
  errorText.value = ''
  try {
    const result = await getNews({
      page: page.value,
      size: size.value,
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.category ? { category: filters.category } : {}),
      ...(filters.date ? { date: filters.date } : {})
    })

    articles.value = Array.isArray(result?.list) ? result.list : []
    total.value = Number(result?.total || 0)
    size.value = Number(result?.size || 10)
    page.value = Number(result?.page || 1)
    fetchDate.value = result?.fetchDate || filters.date || ''
  } catch (error) {
    articles.value = []
    total.value = 0
    errorText.value = error.response?.data?.message || '新闻列表加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/**
 * 拉取分类统计。
 */
async function loadCategories() {
  try {
    categoryOptions.value = await getNewsCategories(
      filters.date ? { date: filters.date } : undefined
    )
  } catch (error) {
    categoryOptions.value = []
  }
}

/**
 * 拉取 Top 新闻。
 */
async function loadTopList() {
  try {
    const result = await getTopNews({
      limit: 10,
      ...(filters.date ? { date: filters.date } : {}),
      ...(filters.category ? { category: filters.category } : {})
    })
    topNews.value = Array.isArray(result?.list) ? result.list : []
  } catch (error) {
    topNews.value = []
  }
}

/**
 * 统一刷新页面数据。
 */
async function refreshPage() {
  await Promise.all([loadNews(), loadCategories(), loadTopList()])
}

/**
 * 应用筛选条件。
 */
function applyFilters() {
  syncToRoute(1)
}

/**
 * 切换分类。
 */
function setCategory(categoryCode) {
  filters.category = categoryCode
  applyFilters()
}

/**
 * 重置筛选条件。
 */
function resetFilters() {
  filters.keyword = ''
  filters.category = ''
  filters.date = ''
  syncToRoute(1)
}

/**
 * 切换分页。
 */
function changePage(targetPage) {
  if (targetPage < 1 || targetPage > totalPages.value) return
  syncToRoute(targetPage)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/**
 * 按天偏移当前日期，并重新查询。
 */
function shiftDate(offset) {
  const baseDate = parseDateValue(resolvedDateValue.value) || new Date()
  const target = new Date(baseDate)
  target.setDate(target.getDate() + offset)
  filters.date = formatDateInput(target)
  applyFilters()
}

/**
 * 切回最近一次抓取日期。
 */
function useLatestFetchDate() {
  filters.date = fetchDate.value || ''
  applyFilters()
}

/**
 * 解析 yyyy-MM-dd 字符串，避免时区带来的日期偏移。
 */
function parseDateValue(dateText) {
  if (!dateText || typeof dateText !== 'string') return null
  const parts = dateText.split('-').map(Number)
  if (parts.length !== 3 || parts.some(Number.isNaN)) return null
  return new Date(parts[0], parts[1] - 1, parts[2])
}

/**
 * 将日期对象格式化为 input[type=date] 可识别的值。
 */
function formatDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 格式化日期展示文案。
 */
function formatDateOnly(dateText) {
  const date = parseDateValue(dateText)
  if (!date) return '最近一次抓取'
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

watch(
  () => route.fullPath,
  async () => {
    syncFromRoute()
    await refreshPage()
  }
)

onMounted(async () => {
  syncFromRoute()
  await refreshPage()
})
</script>
