<template>
  <nav class="sticky top-0 z-50 bg-white/75 backdrop-blur-xl border-b border-gray-100/80 shadow-[0_1px_20px_rgba(0,0,0,0.04)]">
    <div class="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
      <RouterLink
        to="/"
        class="text-lg font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent tracking-tight hover:opacity-75 transition-opacity duration-200"
      >
        ✦ Blog
      </RouterLink>

      <div class="flex items-center gap-2 text-sm">
        <template v-if="auth.isLoggedIn">
          <RouterLink
            to="/create"
            class="inline-flex items-center gap-1.5 bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-4 py-1.5 rounded-full text-xs font-medium shadow-sm hover:shadow-md hover:shadow-blue-200/60 hover:-translate-y-0.5 transition-all duration-200"
          >
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4"/></svg>
            写文章
          </RouterLink>
          <RouterLink
            to="/favorites"
            class="text-gray-500 hover:text-blue-600 px-3 py-1.5 rounded-full hover:bg-blue-50 transition-all duration-200 text-xs"
          >
            收藏
          </RouterLink>
          <RouterLink
            :to="auth.user ? `/user/${auth.user.id}` : '/'"
            class="flex items-center gap-2 pl-2 border-l border-gray-100 hover:opacity-80 transition-opacity"
          >
            <div class="w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center text-white text-xs font-bold shadow-sm">
              {{ auth.user?.username?.[0]?.toUpperCase() ?? '?' }}
            </div>
            <span class="text-gray-500 text-xs hidden sm:block max-w-[80px] truncate">{{ auth.user?.username }}</span>
          </RouterLink>
          <button
            @click="handleLogout"
            class="text-gray-400 hover:text-red-500 text-xs px-2 py-1 rounded-lg hover:bg-red-50 transition-all duration-200"
          >
            退出
          </button>
        </template>
        <template v-else>
          <RouterLink
            to="/login"
            class="text-gray-600 hover:text-blue-600 px-3 py-1.5 rounded-full hover:bg-blue-50 transition-all duration-200 text-xs"
          >
            登录
          </RouterLink>
          <RouterLink
            to="/register"
            class="inline-flex items-center bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-4 py-1.5 rounded-full text-xs font-medium shadow-sm hover:shadow-md hover:shadow-blue-200/60 hover:-translate-y-0.5 transition-all duration-200"
          >
            注册
          </RouterLink>
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/')
}
</script>
