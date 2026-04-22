<template>
  <div>
    <!-- Loading skeleton -->
    <div v-if="loading" class="animate-pulse space-y-6">
      <div class="bg-white rounded-2xl border border-gray-100 p-8 flex items-start gap-6">
        <div class="w-20 h-20 rounded-full bg-blue-50 flex-shrink-0"></div>
        <div class="flex-1 space-y-3">
          <div class="h-6 bg-gray-100 rounded w-32"></div>
          <div class="h-4 bg-gray-50 rounded w-64"></div>
          <div class="h-4 bg-gray-50 rounded w-40"></div>
        </div>
      </div>
    </div>

    <div v-else-if="user">
      <!-- Profile Header Card -->
      <div class="bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden mb-6">
        <!-- Gradient banner -->
        <div class="h-24 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500"></div>

        <div class="px-8 pb-6 -mt-10">
          <!-- Avatar -->
          <div class="flex items-end justify-between mb-4">
            <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-blue-400 to-indigo-600 flex items-center justify-center text-white text-3xl font-bold shadow-lg border-4 border-white">
              {{ user.username?.[0]?.toUpperCase() }}
            </div>
            <div v-if="isOwnProfile" class="mb-1">
              <button
                v-if="!editing"
                @click="startEdit"
                class="px-4 py-1.5 text-xs text-gray-500 border border-gray-200 rounded-full hover:border-blue-300 hover:text-blue-500 hover:bg-blue-50 transition-all duration-200"
              >
                编辑资料
              </button>
              <div v-else class="flex gap-2">
                <button
                  @click="cancelEdit"
                  class="px-4 py-1.5 text-xs text-gray-400 border border-gray-200 rounded-full hover:bg-gray-50 transition-all duration-200"
                >
                  取消
                </button>
                <button
                  @click="saveEdit"
                  :disabled="saving"
                  class="px-4 py-1.5 text-xs text-white bg-gradient-to-r from-blue-600 to-indigo-600 rounded-full hover:shadow-md hover:shadow-blue-200/60 hover:-translate-y-0.5 disabled:opacity-50 disabled:transform-none transition-all duration-200"
                >
                  {{ saving ? '保存中...' : '保存' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Name & bio -->
          <h1 class="text-xl font-bold text-gray-900 mb-1">{{ user.username }}</h1>

          <div v-if="!editing">
            <p v-if="user.bio" class="text-gray-500 text-sm mb-3 leading-relaxed">{{ user.bio }}</p>
            <p v-else-if="isOwnProfile" class="text-gray-300 text-sm mb-3 italic cursor-pointer hover:text-blue-400 transition-colors" @click="startEdit">
              点击添加个人简介...
            </p>
            <p v-else class="text-gray-300 text-sm mb-3 italic">暂无简介</p>
          </div>
          <div v-else class="mb-3">
            <textarea
              v-model="editBio"
              placeholder="介绍一下你自己..."
              rows="3"
              maxlength="500"
              class="w-full text-sm text-gray-700 placeholder-gray-300 border border-blue-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100/80 resize-none transition-all duration-200"
            />
            <div class="text-right text-xs text-gray-300 mt-1">{{ editBio.length }}/500</div>
          </div>

          <!-- Meta -->
          <div class="flex items-center gap-4 text-xs text-gray-400">
            <span class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
              </svg>
              {{ formatJoinDate(user.createdAt) }} 加入
            </span>
            <span class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              {{ posts.length }} 篇文章
            </span>
          </div>
        </div>
      </div>

      <!-- Posts Section -->
      <div>
        <h2 class="text-base font-semibold text-gray-700 mb-4 flex items-center gap-2">
          <span>发布的文章</span>
          <span class="bg-blue-100 text-blue-600 text-xs px-2 py-0.5 rounded-full font-semibold">{{ posts.length }}</span>
        </h2>

        <div v-if="postsLoading" class="space-y-4">
          <div v-for="i in 3" :key="i" class="bg-white border border-gray-100 rounded-2xl p-6 animate-pulse">
            <div class="h-5 bg-blue-50 rounded w-3/4 mb-3"></div>
            <div class="h-3.5 bg-gray-50 rounded w-full mb-2"></div>
            <div class="h-3.5 bg-gray-50 rounded w-1/2"></div>
          </div>
        </div>
        <div v-else-if="posts.length === 0" class="text-center py-16 text-gray-300 text-sm">
          <div class="text-4xl mb-3 opacity-50">📝</div>
          <p>还没有发布任何文章</p>
        </div>
        <div v-else class="space-y-4">
          <PostCard v-for="post in posts" :key="post.id" :post="post" />
        </div>
      </div>
    </div>

    <div v-else class="text-center py-28">
      <div class="text-5xl mb-4 opacity-50">👤</div>
      <p class="text-gray-400">用户不存在</p>
      <RouterLink to="/" class="text-sm text-blue-500 hover:text-blue-700 mt-3 inline-block transition-colors">← 返回首页</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getUserById, updateProfile, getPostsByUser } from '../api/users'
import PostCard from '../components/PostCard.vue'

const route = useRoute()
const auth = useAuthStore()

const user = ref(null)
const posts = ref([])
const loading = ref(true)
const postsLoading = ref(true)
const editing = ref(false)
const saving = ref(false)
const editBio = ref('')

const isOwnProfile = computed(() =>
  auth.isLoggedIn && auth.user && auth.user.id === user.value?.id
)

async function load(id) {
  loading.value = true
  postsLoading.value = true
  try {
    user.value = await getUserById(id)
  } finally {
    loading.value = false
  }
  try {
    posts.value = await getPostsByUser(id)
  } finally {
    postsLoading.value = false
  }
}

function startEdit() {
  editBio.value = user.value.bio ?? ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveEdit() {
  saving.value = true
  try {
    const updated = await updateProfile({ bio: editBio.value })
    user.value = updated
    if (auth.user) auth.user.bio = updated.bio
    editing.value = false
  } finally {
    saving.value = false
  }
}

function formatJoinDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
}

watch(() => route.params.id, id => id && load(id))
onMounted(() => load(route.params.id))
</script>
