<template>
  <div class="min-h-[78vh] flex items-center justify-center relative">
    <div class="absolute inset-0 overflow-hidden pointer-events-none -z-10">
      <div class="absolute top-1/3 left-1/3 w-72 h-72 bg-blue-200/25 rounded-full blur-3xl"></div>
      <div class="absolute bottom-1/4 right-1/3 w-64 h-64 bg-indigo-200/20 rounded-full blur-3xl"></div>
    </div>

    <div class="w-full max-w-sm">
      <div class="bg-white/80 backdrop-blur-sm border border-white/60 rounded-3xl p-8 shadow-[0_8px_40px_rgba(59,130,246,0.10)]">
        <div class="text-center mb-8">
          <div class="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 mb-3 shadow-lg shadow-blue-200/60">
            <span class="text-white text-xl">✦</span>
          </div>
          <h2 class="text-xl font-bold text-gray-800">欢迎回来</h2>
          <p class="text-gray-400 text-xs mt-1">登录以继续创作</p>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-xs font-medium text-gray-500 mb-1.5">用户名</label>
            <input
              v-model="form.username"
              type="text"
              required
              placeholder="输入用户名"
              class="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm bg-gray-50 placeholder-gray-300 focus:bg-white focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100/80 transition-all duration-200"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-500 mb-1.5">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              placeholder="输入密码"
              class="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm bg-gray-50 placeholder-gray-300 focus:bg-white focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100/80 transition-all duration-200"
            />
          </div>

          <Transition name="slide-fade">
            <p v-if="error" class="text-red-500 text-xs bg-red-50 border border-red-100 rounded-xl px-3 py-2.5">
              ⚠ {{ error }}
            </p>
          </Transition>

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-2.5 rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-blue-200/60 hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:transform-none transition-all duration-200 mt-2"
          >
            <span v-if="loading" class="flex items-center justify-center gap-2">
              <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>
              登录中...
            </span>
            <span v-else>登录</span>
          </button>
        </form>

        <p class="text-center text-xs text-gray-400 mt-6">
          没有账号？
          <RouterLink to="/register" class="text-blue-500 font-medium hover:text-blue-700 transition-colors">立即注册</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const form = ref({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(form.value.username, form.value.password)
    router.push('/')
  } catch {
    error.value = '用户名或密码错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.slide-fade-enter-active, .slide-fade-leave-active {
  transition: all 0.2s ease;
}
.slide-fade-enter-from, .slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
