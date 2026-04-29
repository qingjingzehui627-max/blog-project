<template>
  <nav class="sticky top-0 z-50 border-b border-[#d9dcff] bg-[linear-gradient(180deg,rgba(246,247,255,0.96)_0%,rgba(250,252,255,0.96)_100%)] backdrop-blur">
    <div class="mx-auto flex h-[76px] w-full max-w-[1240px] items-center gap-4 px-3 pl-12 sm:px-4 sm:pl-14 lg:px-5 lg:pl-16">
      <RouterLink
        to="/"
        class="inline-flex shrink-0 items-center rounded-2xl px-1 py-1"
      >
        <span class="bg-gradient-to-r from-[#4f46e5] via-[#6366f1] to-[#8b5cf6] bg-clip-text text-[20px] font-black italic leading-none tracking-tight text-transparent transition duration-300 hover:from-[#2563eb] hover:via-[#7c3aed] hover:to-[#ec4899] sm:text-[22px]">
          QingJingZe
        </span>
      </RouterLink>

      <div class="hidden items-center gap-2 pl-1 lg:flex">
        <RouterLink to="/news" class="inline-flex h-11 items-center gap-2 whitespace-nowrap rounded-2xl px-4 text-[15px] font-semibold text-slate-700 transition hover:bg-white hover:text-[#6366f1] hover:shadow-[0_10px_24px_rgba(99,102,241,0.14)]">
          <svg class="h-4 w-4 text-slate-500 transition" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M19 5H5a2 2 0 00-2 2v10a2 2 0 002 2h14a2 2 0 002-2V7a2 2 0 00-2-2zm0 4-7 5-7-5" />
          </svg>
          <span>AI 新闻</span>
        </RouterLink>
        <RouterLink to="/links" class="inline-flex h-11 items-center gap-2 whitespace-nowrap rounded-2xl px-4 text-[15px] font-semibold text-slate-700 transition hover:bg-white hover:text-[#6366f1] hover:shadow-[0_10px_24px_rgba(99,102,241,0.14)]">
          <svg class="h-4 w-4 text-slate-500 transition" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M10 14L8 16a3 3 0 11-4-4l2-2m8-4l2-2a3 3 0 114 4l-2 2M9 15l6-6" />
          </svg>
          <span>友情链接</span>
        </RouterLink>
        <RouterLink to="/about" class="inline-flex h-11 items-center gap-2 whitespace-nowrap rounded-2xl px-4 text-[15px] font-semibold text-slate-700 transition hover:bg-white hover:text-[#6366f1] hover:shadow-[0_10px_24px_rgba(99,102,241,0.14)]">
          <svg class="h-4 w-4 text-slate-500" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M12 2a10 10 0 100 20 10 10 0 000-20zm0 6a1.25 1.25 0 110 2.5A1.25 1.25 0 0112 8zm1.25 8h-2.5v-5h2.5v5z" />
          </svg>
          <span>关于我</span>
        </RouterLink>
      </div>

      <form
        class="ml-auto hidden h-12 w-[332px] items-center overflow-hidden rounded-[22px] border border-[#d9dcff] bg-white shadow-[0_10px_30px_rgba(99,102,241,0.08)] md:flex"
        @submit.prevent="submitSearch"
      >
        <div class="grid h-full w-12 place-items-center text-slate-400">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.2-4.2m1.2-5.1a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="请输入内容"
          class="h-full flex-1 bg-transparent pr-3 text-[15px] text-slate-700 outline-none placeholder:text-slate-400"
        />
        <button
          type="submit"
          class="mr-1.5 inline-flex h-9 items-center justify-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-4 text-sm font-medium text-white transition hover:opacity-90"
          aria-label="搜索"
        >
          搜索
        </button>
      </form>

      <div class="flex items-center gap-2">
        <RouterLink
          v-if="auth.isLoggedIn"
          to="/news-admin"
          class="hidden h-10 items-center rounded-[18px] border-2 border-[#c7cbff] bg-white px-3.5 text-sm font-bold text-slate-700 shadow-[0_10px_24px_rgba(99,102,241,0.08)] transition hover:border-[#6366f1] hover:text-[#4f46e5] md:inline-flex"
        >
          新闻后台
        </RouterLink>
        <button
          type="button"
          @click="toggleTheme"
          class="inline-flex h-12 w-12 items-center justify-center rounded-2xl border border-[#d9dcff] bg-white text-slate-600 shadow-[0_10px_24px_rgba(99,102,241,0.08)] transition hover:-translate-y-0.5 hover:text-[#6366f1]"
          :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'"
        >
          <svg v-if="isDark" class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.36 6.36-1.41-1.41M7.05 7.05 5.64 5.64m12.72 0-1.41 1.41M7.05 16.95l-1.41 1.41M12 8a4 4 0 100 8 4 4 0 000-8z" />
          </svg>
          <svg v-else class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12.8A9 9 0 1111.2 3a7 7 0 009.8 9.8z" />
          </svg>
        </button>

        <template v-if="showUserInfo">
          <div class="group relative">
            <RouterLink
              :to="`/user/${auth.user.id}`"
              class="inline-flex h-12 w-12 items-center justify-center overflow-hidden rounded-full border border-[#d9dcff] bg-white text-xs font-semibold text-white shadow-[0_10px_24px_rgba(99,102,241,0.08)] transition hover:-translate-y-0.5 hover:border-[#6366f1]/35"
            >
              <img v-if="auth.user.avatar" :src="auth.user.avatar" alt="avatar" class="h-full w-full object-cover" />
              <span v-else class="text-slate-900">{{ auth.user.username?.[0]?.toUpperCase() ?? '?' }}</span>
            </RouterLink>
            <div class="pointer-events-none absolute right-0 top-[58px] z-30 w-56 translate-y-2 rounded-3xl border border-[#d9dcff] bg-white p-4 opacity-0 shadow-[0_18px_40px_rgba(99,102,241,0.12)] transition duration-200 group-hover:pointer-events-auto group-hover:translate-y-0 group-hover:opacity-100">
              <div class="flex items-center gap-3">
                <div class="flex h-12 w-12 items-center justify-center overflow-hidden rounded-full bg-slate-100 text-sm font-semibold text-slate-700">
                  <img v-if="auth.user.avatar" :src="auth.user.avatar" alt="avatar" class="h-full w-full object-cover" />
                  <span v-else>{{ auth.user.username?.[0]?.toUpperCase() ?? '?' }}</span>
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-bold text-slate-900">{{ auth.user.username }}</p>
                  <p class="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">{{ userBio }}</p>
                </div>
              </div>
            </div>
          </div>
          <button
            type="button"
            @click="handleLogout"
            class="inline-flex h-10 items-center gap-2 rounded-[18px] border-2 border-[#c7cbff] bg-white px-3.5 text-sm font-bold text-slate-700 shadow-[0_10px_24px_rgba(99,102,241,0.08)] transition hover:border-[#6366f1] hover:text-[#4f46e5]"
          >
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.2" d="M17 16l4-4m0 0-4-4m4 4H9m4 8H7a2 2 0 01-2-2V6a2 2 0 012-2h6" />
            </svg>
            退出
          </button>
        </template>

        <template v-else>
          <RouterLink
            to="/login"
            class="inline-flex h-12 w-12 items-center justify-center overflow-hidden rounded-full border border-[#d9dcff] bg-white text-[11px] font-bold text-slate-900 shadow-[0_10px_24px_rgba(99,102,241,0.08)] transition hover:-translate-y-0.5 hover:border-[#6366f1]/35"
          >
            未登录
          </RouterLink>
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTheme } from '../composables/useTheme'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()
const showUserInfo = computed(() => auth.isLoggedIn && !!auth.user)
const userBio = computed(() => auth.user?.bio || '这个人很神秘，什么都没留下')

const searchKeyword = ref(typeof route.query.q === 'string' ? route.query.q : '')

watch(
  () => route.query.q,
  value => {
    searchKeyword.value = typeof value === 'string' ? value : ''
  }
)

function submitSearch() {
  const q = searchKeyword.value.trim()
  router.push({ path: '/', query: q ? { q } : {} })
}

function handleLogout() {
  auth.logout()
  router.push('/')
}
</script>
