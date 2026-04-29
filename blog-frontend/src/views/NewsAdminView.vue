<template>
  <div class="space-y-5">
    <section class="rounded-[32px] border border-[#d9dcff] bg-[linear-gradient(135deg,#ffffff_0%,#f7f8ff_100%)] p-6 shadow-[0_18px_42px_rgba(99,102,241,0.1)]">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-[#6366f1]">News Admin</p>
          <h1 class="mt-2 text-[30px] font-black text-slate-900">新闻后台管理</h1>
          <p class="mt-3 text-sm leading-7 text-slate-600">
            手动抓取 Guardian 新闻、查看任务日志，并在线调整关键词分类规则。
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
        <div class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <h2 class="text-lg font-bold text-slate-900">手动抓取</h2>
          <p class="mt-2 text-sm leading-7 text-slate-500">
            默认建议抓取前一天新闻，也可以手动指定历史日期补数。
          </p>

          <label class="mt-4 block text-sm font-medium text-slate-600">
            抓取日期
            <input
              v-model="fetchDate"
              type="date"
              class="mt-2 h-11 w-full rounded-2xl border border-[#d9dcff] bg-[#fbfbff] px-4 text-sm text-slate-700 outline-none transition focus:border-[#6366f1]"
            />
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
        </div>

        <div class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-bold text-slate-900">最近任务</h2>
            <button
              type="button"
              class="text-sm font-medium text-[#4f46e5]"
              @click="loadJobs"
            >
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
                    {{ job.jobDate }} · {{ job.triggerType }}
                  </p>
                  <p class="mt-1 text-xs text-slate-400">
                    {{ formatDateTime(job.startedAt) }}
                  </p>
                </div>
                <span
                  class="rounded-full px-3 py-1 text-xs font-semibold"
                  :class="statusClass(job.status)"
                >
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
        </div>
      </div>

      <div class="rounded-[28px] border border-[#d9dcff] bg-white p-5 shadow-[0_16px_38px_rgba(99,102,241,0.08)]">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-lg font-bold text-slate-900">分类规则</h2>
            <p class="mt-1 text-sm text-slate-500">
              调整关键词命中规则后，后续抓取会按最新规则分类。
            </p>
          </div>
          <button
            type="button"
            class="text-sm font-medium text-[#4f46e5]"
            @click="loadRules"
          >
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
                  <option :value="0">禁用</option>
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
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchNewsNow, getNewsJobLogs, getNewsRules, updateNewsRule } from '../api/news'

const fetchDate = ref(new Date(Date.now() - 86400000).toISOString().slice(0, 10))
const fetching = ref(false)
const fetchMessage = ref('')
const jobs = ref([])
const editableRules = ref([])

/**
 * 手动抓取新闻。
 */
async function handleFetch() {
  fetching.value = true
  fetchMessage.value = ''
  try {
    const result = await fetchNewsNow(fetchDate.value ? { date: fetchDate.value } : undefined)
    fetchMessage.value = `任务状态：${result.status}，抓取日期：${result.fetchDate || fetchDate.value}`
    await loadJobs()
  } catch (error) {
    fetchMessage.value = error.response?.data?.message || '抓取失败，请稍后重试'
  } finally {
    fetching.value = false
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
 * 查询分类规则。
 */
async function loadRules() {
  const result = await getNewsRules()
  editableRules.value = Array.isArray(result)
    ? result.map(item => ({ ...item }))
    : []
}

/**
 * 保存单条规则。
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
 * 返回状态标签样式。
 */
function statusClass(status) {
  if (status === 'SUCCESS') return 'bg-emerald-50 text-emerald-600'
  if (status === 'FAILED') return 'bg-red-50 text-red-500'
  if (status === 'SKIPPED') return 'bg-amber-50 text-amber-600'
  return 'bg-slate-100 text-slate-500'
}

/**
 * 格式化日志时间。
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
  await Promise.all([loadJobs(), loadRules()])
})
</script>
