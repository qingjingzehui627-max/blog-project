<template>
  <div class="space-y-5">
    <section class="rounded-[32px] border border-[#d9dcff] bg-[linear-gradient(135deg,#ffffff_0%,#f7f8ff_100%)] p-6 shadow-[0_18px_42px_rgba(99,102,241,0.1)]">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#6366f1]">News Admin</p>
          <h1 class="mt-2 text-[30px] font-black text-slate-900">新闻后台管理</h1>
          <p class="mt-3 text-sm leading-7 text-slate-600">
            管理新闻抓取、AI 摘要、重复归并与分类规则，所有改动都在独立模块内完成，不影响原有博客功能。
          </p>
        </div>
        <RouterLink
          to="/news"
          class="inline-flex items-center rounded-2xl border border-[#d9dcff] bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
        >
          返回新闻列表
        </RouterLink>
      </div>
    </section>

    <section class="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
      <div class="space-y-5">
        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <h2 class="text-lg font-bold text-slate-900">手动抓取</h2>
          <p class="mt-2 text-sm leading-7 text-slate-500">
            支持按日期、按数据源手动抓取新闻。默认抓取前一天数据，适合日常补跑和联调。
          </p>

          <label class="mt-4 block text-sm font-medium text-slate-600">
            抓取日期
            <input
              v-model="fetchDate"
              type="date"
              class="mt-2 h-11 w-full rounded-2xl border border-[#d9dcff] bg-[#fbfbff] px-4 text-sm text-slate-700 outline-none transition focus:border-[#6366f1]"
            />
          </label>

          <label class="mt-4 block text-sm font-medium text-slate-600">
            抓取数据源
            <select
              v-model="selectedProvider"
              class="mt-2 h-11 w-full rounded-2xl border border-[#d9dcff] bg-[#fbfbff] px-4 text-sm text-slate-700 outline-none transition focus:border-[#6366f1]"
            >
              <option value="">全部启用数据源</option>
              <option v-for="item in providers" :key="item.providerCode" :value="item.providerCode">
                {{ item.displayName }}
              </option>
            </select>
          </label>

          <button
            type="button"
            class="mt-5 inline-flex w-full items-center justify-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-3 text-sm font-semibold text-white shadow-[0_12px_24px_rgba(99,102,241,0.18)] transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="fetching"
            @click="handleFetch"
          >
            {{ fetching ? '抓取中...' : '开始抓取新闻' }}
          </button>

          <p v-if="fetchMessage" class="mt-4 rounded-2xl bg-[#f7f8ff] px-4 py-3 text-sm text-slate-600">
            {{ fetchMessage }}
          </p>

          <div v-if="lastFetchProviders.length > 0" class="mt-4 space-y-3">
            <div
              v-for="item in lastFetchProviders"
              :key="`${item.source}-${item.jobId}`"
              class="rounded-2xl border border-[#e6e9ff] bg-[#fbfbff] p-4"
            >
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-sm font-semibold text-slate-800">{{ item.source }}</p>
                  <p class="mt-1 text-xs text-slate-400">{{ item.fetchDate || fetchDate }}</p>
                </div>
                <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="statusClass(item.status)">
                  {{ item.status }}
                </span>
              </div>
              <div class="mt-3 grid grid-cols-2 gap-2 text-xs text-slate-500">
                <span>请求数：{{ item.requestCount }}</span>
                <span>抓取数：{{ item.fetchedCount }}</span>
                <span>新增：{{ item.insertedCount }}</span>
                <span>更新：{{ item.updatedCount }}</span>
              </div>
              <p v-if="item.message" class="mt-3 text-xs leading-6 text-amber-600">
                {{ item.message }}
              </p>
            </div>
          </div>
        </section>

        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-bold text-slate-900">AI 摘要</h2>
              <p class="mt-1 text-sm text-slate-500">
                自动摘要每天只处理热度前 10 条，总额度每天 50 条。单条摘要可在新闻详情页手动生成。
              </p>
            </div>
            <button
              type="button"
              class="text-sm font-medium text-[#4f46e5]"
              @click="loadSummaryQuota"
            >
              刷新额度
            </button>
          </div>

          <div class="mt-4 grid grid-cols-3 gap-3">
            <div class="rounded-2xl bg-[#f8faff] px-4 py-4">
              <p class="text-xs text-slate-400">每日额度</p>
              <p class="mt-2 text-2xl font-black text-slate-900">{{ summaryQuota.limit ?? 50 }}</p>
            </div>
            <div class="rounded-2xl bg-[#f8faff] px-4 py-4">
              <p class="text-xs text-slate-400">已使用</p>
              <p class="mt-2 text-2xl font-black text-slate-900">{{ summaryQuota.used ?? 0 }}</p>
            </div>
            <div class="rounded-2xl bg-[#f8faff] px-4 py-4">
              <p class="text-xs text-slate-400">剩余额度</p>
              <p class="mt-2 text-2xl font-black text-slate-900">{{ summaryQuota.remaining ?? 0 }}</p>
            </div>
          </div>

          <label class="mt-4 block text-sm font-medium text-slate-600">
            摘要日期
            <input
              v-model="summaryDate"
              type="date"
              class="mt-2 h-11 w-full rounded-2xl border border-[#d9dcff] bg-[#fbfbff] px-4 text-sm text-slate-700 outline-none transition focus:border-[#6366f1]"
            />
          </label>

          <button
            type="button"
            class="mt-5 inline-flex w-full items-center justify-center rounded-2xl border border-[#cfd7ff] bg-white px-4 py-3 text-sm font-semibold text-[#4f46e5] transition hover:bg-[#f7f8ff] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="summaryGenerating"
            @click="handleGenerateTopSummaries"
          >
            {{ summaryGenerating ? '生成中...' : '手动生成当日前10条 AI 摘要' }}
          </button>

          <p v-if="summaryMessage" class="mt-4 rounded-2xl bg-[#f7f8ff] px-4 py-3 text-sm text-slate-600">
            {{ summaryMessage }}
          </p>

          <div v-if="summaryItems.length > 0" class="mt-4 space-y-3">
            <div
              v-for="item in summaryItems"
              :key="item.articleId"
              class="rounded-2xl border border-[#e6e9ff] bg-[#fbfbff] p-4"
            >
              <div class="flex items-center justify-between gap-3">
                <div class="min-w-0">
                  <p class="truncate text-sm font-semibold text-slate-800">
                    Top {{ item.rankOrder || '-' }} · {{ item.title }}
                  </p>
                  <p class="mt-1 text-xs text-slate-400">文章 ID：{{ item.articleId }}</p>
                </div>
                <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="statusClass(item.status)">
                  {{ item.status }}
                </span>
              </div>
              <p v-if="item.message" class="mt-3 text-xs leading-6 text-slate-500">
                {{ item.message }}
              </p>
            </div>
          </div>
        </section>

        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-bold text-slate-900">数据源状态</h2>
            <button type="button" class="text-sm font-medium text-[#4f46e5]" @click="loadProviders">
              刷新
            </button>
          </div>

          <div class="mt-4 space-y-3">
            <div
              v-for="item in providers"
              :key="item.providerCode"
              class="rounded-2xl border border-[#e6e9ff] bg-[#fbfbff] p-4"
            >
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-sm font-semibold text-slate-800">{{ item.displayName }}</p>
                  <p class="mt-1 text-xs uppercase tracking-[0.16em] text-slate-400">{{ item.providerCode }}</p>
                </div>
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="item.enabled && item.configValid ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'"
                >
                  {{ item.enabled ? (item.configValid ? '可用' : '待配置') : '未启用' }}
                </span>
              </div>

              <p class="mt-3 text-xs leading-6 text-slate-500">{{ item.message }}</p>

              <div class="mt-3 grid grid-cols-2 gap-2 text-xs text-slate-500">
                <span>单页大小：{{ item.config?.pageSize ?? '-' }}</span>
                <span>Top 限制：{{ item.config?.topLimit ?? '-' }}</span>
                <span>最大页数：{{ item.config?.maxPagesPerQuery ?? '-' }}</span>
                <span>请求间隔：{{ item.config?.requestIntervalMs ?? '-' }} ms</span>
              </div>
            </div>

            <div v-if="providers.length === 0" class="rounded-2xl bg-[#fbfbff] px-4 py-6 text-sm text-slate-400">
              暂无数据源配置
            </div>
          </div>
        </section>

        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-bold text-slate-900">最近任务</h2>
            <button type="button" class="text-sm font-medium text-[#4f46e5]" @click="loadJobs">
              刷新
            </button>
          </div>

          <div class="mt-4 space-y-3">
            <div
              v-for="job in jobs"
              :key="job.id"
              class="rounded-2xl border border-[#e6e9ff] bg-[#fbfbff] p-4"
            >
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-sm font-semibold text-slate-800">
                    {{ job.jobDate }} / {{ job.source }} / {{ job.triggerType }}
                  </p>
                  <p class="mt-1 text-xs text-slate-400">{{ formatDateTime(job.startedAt) }}</p>
                </div>
                <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="statusClass(job.status)">
                  {{ job.status }}
                </span>
              </div>

              <div class="mt-3 grid grid-cols-2 gap-2 text-xs text-slate-500">
                <span>请求数：{{ job.requestCount }}</span>
                <span>抓取数：{{ job.fetchedCount }}</span>
                <span>新增：{{ job.insertedCount }}</span>
                <span>更新：{{ job.updatedCount }}</span>
              </div>

              <p v-if="job.errorMessage" class="mt-3 text-xs leading-6 text-amber-600">
                {{ job.errorMessage }}
              </p>
            </div>

            <div v-if="jobs.length === 0" class="rounded-2xl bg-[#fbfbff] px-4 py-6 text-sm text-slate-400">
              暂无抓取任务记录
            </div>
          </div>
        </section>
      </div>

      <div class="space-y-5">
        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-bold text-slate-900">重复新闻归并结果</h2>
              <p class="mt-1 text-sm text-slate-500">
                用于核对多数据源新闻的去重效果和归并来源明细。
              </p>
            </div>
            <div class="flex items-center gap-2">
              <input
                v-model="duplicateDate"
                type="date"
                class="h-10 rounded-2xl border border-[#d9dcff] bg-[#fbfbff] px-4 text-sm text-slate-700 outline-none transition focus:border-[#6366f1]"
              />
              <button
                type="button"
                class="inline-flex items-center rounded-2xl border border-[#d9dcff] bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-[#6366f1] hover:text-[#4f46e5]"
                @click="loadDuplicates"
              >
                刷新
              </button>
            </div>
          </div>

          <div class="mt-5 space-y-4">
            <div
              v-for="item in duplicates"
              :key="item.articleId"
              class="rounded-[24px] border border-[#e6e9ff] bg-[#fbfbff] p-4"
            >
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-base font-bold text-slate-900">{{ item.title }}</p>
                  <div class="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                    <span class="rounded-full bg-white px-3 py-1">主来源：{{ item.primaryProvider }}</span>
                    <span class="rounded-full bg-white px-3 py-1">来源数：{{ item.sourceCount }}</span>
                    <span class="rounded-full bg-white px-3 py-1">{{ item.categoryName }}</span>
                  </div>
                </div>
                <a
                  v-if="item.webUrl"
                  :href="item.webUrl"
                  target="_blank"
                  rel="noreferrer"
                  class="text-sm font-medium text-[#4f46e5]"
                >
                  查看原文
                </a>
              </div>

              <p class="mt-3 text-xs text-slate-400">
                抓取日期：{{ item.fetchDate || '-' }} / 发布时间：{{ formatDateTime(item.publishedAt) }}
              </p>

              <div class="mt-4 grid gap-3 md:grid-cols-2">
                <div
                  v-for="source in item.sources"
                  :key="source.id"
                  class="rounded-2xl border border-white/80 bg-white p-3"
                >
                  <div class="flex items-center justify-between gap-3">
                    <p class="text-sm font-semibold text-slate-800">{{ source.providerCode }}</p>
                    <span class="text-xs text-slate-400">{{ formatDateTime(source.publishedAt) }}</span>
                  </div>
                  <p class="mt-2 text-sm leading-6 text-slate-600">{{ source.title }}</p>
                  <a
                    v-if="source.sourceUrl"
                    :href="source.sourceUrl"
                    target="_blank"
                    rel="noreferrer"
                    class="mt-2 inline-flex text-xs font-medium text-[#4f46e5]"
                  >
                    来源链接
                  </a>
                </div>
              </div>
            </div>

            <div v-if="duplicates.length === 0" class="rounded-2xl bg-[#fbfbff] px-4 py-6 text-sm text-slate-400">
              当前日期暂无重复新闻归并结果
            </div>
          </div>
        </section>

        <section class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-lg font-bold text-slate-900">分类规则</h2>
              <p class="mt-1 text-sm text-slate-500">
                保存后会作用于后续新闻抓取和分类。
              </p>
            </div>
            <button type="button" class="text-sm font-medium text-[#4f46e5]" @click="loadRules">
              刷新
            </button>
          </div>

          <div class="mt-5 space-y-4">
            <form
              v-for="rule in editableRules"
              :key="rule.id"
              class="rounded-[24px] border border-[#e6e9ff] bg-[#fbfbff] p-4"
              @submit.prevent="saveRule(rule)"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p class="text-base font-bold text-slate-900">{{ rule.categoryName }}</p>
                  <p class="mt-1 text-xs uppercase tracking-[0.16em] text-slate-400">{{ rule.categoryCode }}</p>
                </div>
                <div class="flex items-center gap-2">
                  <label class="text-xs text-slate-500">优先级</label>
                  <input
                    v-model.number="rule.priority"
                    type="number"
                    class="h-9 w-20 rounded-xl border border-[#d9dcff] bg-white px-3 text-sm text-slate-700 outline-none"
                  />
                  <select
                    v-model.number="rule.enabled"
                    class="h-9 rounded-xl border border-[#d9dcff] bg-white px-3 text-sm text-slate-700 outline-none"
                  >
                    <option :value="1">启用</option>
                    <option :value="0">停用</option>
                  </select>
                </div>
              </div>

              <label class="mt-4 block text-sm font-medium text-slate-600">
                命中关键词
                <textarea
                  v-model="rule.includeKeywords"
                  rows="3"
                  class="mt-2 w-full rounded-2xl border border-[#d9dcff] bg-white px-4 py-3 text-sm leading-7 text-slate-700 outline-none transition focus:border-[#6366f1]"
                ></textarea>
              </label>

              <label class="mt-4 block text-sm font-medium text-slate-600">
                排除关键词
                <textarea
                  v-model="rule.excludeKeywords"
                  rows="2"
                  class="mt-2 w-full rounded-2xl border border-[#d9dcff] bg-white px-4 py-3 text-sm leading-7 text-slate-700 outline-none transition focus:border-[#6366f1]"
                ></textarea>
              </label>

              <div class="mt-4 flex items-center justify-between gap-3">
                <p class="text-xs text-slate-400">
                  使用英文逗号分隔关键词，保存后对后续抓取生效。
                </p>
                <button
                  type="submit"
                  class="inline-flex items-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 py-2.5 text-sm font-semibold text-white shadow-[0_10px_20px_rgba(99,102,241,0.18)] transition hover:opacity-90"
                >
                  保存规则
                </button>
              </div>
            </form>

            <div v-if="editableRules.length === 0" class="rounded-2xl bg-[#fbfbff] px-4 py-6 text-sm text-slate-400">
              暂无分类规则
            </div>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  fetchNewsNow,
  generateTopNewsSummaries,
  getNewsDuplicates,
  getNewsJobLogs,
  getNewsProviders,
  getNewsRules,
  getNewsSummaryQuota,
  updateNewsRule
} from '../api/news'

const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10)
const fetchDate = ref(yesterday)
const duplicateDate = ref(yesterday)
const summaryDate = ref(yesterday)
const selectedProvider = ref('')
const fetching = ref(false)
const summaryGenerating = ref(false)
const fetchMessage = ref('')
const summaryMessage = ref('')
const providers = ref([])
const jobs = ref([])
const duplicates = ref([])
const editableRules = ref([])
const lastFetchProviders = ref([])
const summaryItems = ref([])
const summaryQuota = ref({ limit: 50, used: 0, remaining: 50 })

/**
 * 手动抓取新闻。
 */
async function handleFetch() {
  fetching.value = true
  fetchMessage.value = ''
  lastFetchProviders.value = []
  try {
    const params = {}
    if (fetchDate.value) params.date = fetchDate.value
    if (selectedProvider.value) params.source = selectedProvider.value
    const result = await fetchNewsNow(params)
    fetchMessage.value = `任务状态：${result.status}，抓取日期：${result.fetchDate || fetchDate.value}`
    lastFetchProviders.value = Array.isArray(result?.providerResults) ? result.providerResults : []
    await Promise.all([loadJobs(), loadDuplicates(), loadProviders()])
  } catch (error) {
    fetchMessage.value = error.response?.data?.message || '抓取失败，请稍后重试'
  } finally {
    fetching.value = false
  }
}

/**
 * 手动生成前十条热度新闻摘要。
 */
async function handleGenerateTopSummaries() {
  summaryGenerating.value = true
  summaryMessage.value = ''
  summaryItems.value = []
  try {
    const result = await generateTopNewsSummaries({ date: summaryDate.value })
    summaryQuota.value = result?.quota || summaryQuota.value
    summaryItems.value = Array.isArray(result?.items) ? result.items : []
    summaryMessage.value = `已生成 ${result.generatedCount || 0} 条，跳过 ${result.skippedCount || 0} 条，失败 ${result.failedCount || 0} 条`
  } catch (error) {
    summaryMessage.value = error.response?.data?.message || '批量生成摘要失败'
  } finally {
    summaryGenerating.value = false
  }
}

/**
 * 查询任务日志。
 */
async function loadJobs() {
  const result = await getNewsJobLogs(1, 10)
  jobs.value = Array.isArray(result?.list) ? result.list : []
}

/**
 * 查询数据源状态。
 */
async function loadProviders() {
  const result = await getNewsProviders()
  providers.value = Array.isArray(result) ? result : []
}

/**
 * 查询重复新闻。
 */
async function loadDuplicates() {
  const params = duplicateDate.value ? { date: duplicateDate.value } : {}
  const result = await getNewsDuplicates(1, 10, params)
  duplicates.value = Array.isArray(result?.list) ? result.list : []
}

/**
 * 查询分类规则。
 */
async function loadRules() {
  const result = await getNewsRules()
  editableRules.value = Array.isArray(result) ? result.map(item => ({ ...item })) : []
}

/**
 * 查询摘要额度。
 */
async function loadSummaryQuota() {
  const result = await getNewsSummaryQuota({ date: summaryDate.value })
  summaryQuota.value = result || { limit: 50, used: 0, remaining: 50 }
}

/**
 * 保存单条规则。
 *
 * @param {object} rule 当前规则
 */
async function saveRule(rule) {
  await updateNewsRule(rule.id, {
    categoryCode: rule.categoryCode,
    categoryName: rule.categoryName,
    includeKeywords: rule.includeKeywords,
    excludeKeywords: rule.excludeKeywords,
    priority: rule.priority,
    enabled: rule.enabled
  })
  fetchMessage.value = `规则 ${rule.categoryCode} 已保存`
  await loadRules()
}

/**
 * 返回状态样式。
 *
 * @param {string} status 状态值
 * @returns {string}
 */
function statusClass(status) {
  if (status === 'SUCCESS') return 'bg-emerald-50 text-emerald-600'
  if (status === 'FAILED') return 'bg-red-50 text-red-500'
  if (status === 'SKIPPED') return 'bg-amber-50 text-amber-600'
  return 'bg-slate-100 text-slate-500'
}

/**
 * 格式化时间文本。
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

onMounted(async () => {
  await Promise.all([loadProviders(), loadJobs(), loadDuplicates(), loadRules(), loadSummaryQuota()])
})
</script>
