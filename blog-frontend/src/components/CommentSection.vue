<template>
  <div>
    <h3 class="text-base font-semibold text-gray-700 mb-5 flex items-center gap-2">
      <span>评论</span>
      <span class="bg-blue-100 text-blue-600 text-xs px-2 py-0.5 rounded-full font-semibold">{{ comments.length }}</span>
    </h3>

    <div v-if="auth.isLoggedIn" class="mb-6 bg-white rounded-2xl border border-gray-100 p-4 shadow-sm focus-within:border-blue-200 focus-within:shadow-[0_4px_20px_rgba(59,130,246,0.08)] transition-all duration-200">
      <textarea
        v-model="newComment"
        placeholder="写下你的想法..."
        rows="3"
        class="w-full text-sm text-gray-700 placeholder-gray-300 focus:outline-none resize-none bg-transparent leading-relaxed"
      />
      <div class="flex justify-end mt-3 pt-3 border-t border-gray-50">
        <button
          @click="submitComment"
          :disabled="!newComment.trim() || submitting"
          class="px-5 py-1.5 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-xs font-medium rounded-full hover:shadow-md hover:shadow-blue-200/60 hover:-translate-y-0.5 disabled:opacity-40 disabled:transform-none transition-all duration-200"
        >
          {{ submitting ? '发布中...' : '发布评论' }}
        </button>
      </div>
    </div>
    <div v-else class="mb-6 bg-gradient-to-r from-blue-50/60 to-indigo-50/40 rounded-2xl px-5 py-4 text-sm text-gray-400 border border-blue-100/60">
      <RouterLink to="/login" class="text-blue-500 font-medium hover:text-blue-700 transition-colors">登录</RouterLink>
      后参与讨论 💬
    </div>

    <div v-if="topLevel.length === 0" class="text-center py-10 text-gray-300 text-sm">
      还没有评论，快来抢沙发吧 🛋️
    </div>

    <TransitionGroup name="comment" tag="div" class="space-y-3">
      <div
        v-for="c in topLevel"
        :key="c.id"
        class="bg-white border border-gray-100 rounded-2xl p-4 hover:border-blue-100 hover:shadow-[0_4px_20px_rgba(59,130,246,0.06)] transition-all duration-200"
      >
        <div class="flex items-start gap-3">
          <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0 shadow-sm">
            {{ avatarLetter(c.userId) }}
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1.5">
              <span class="text-xs font-semibold text-gray-600">用户 {{ c.userId }}</span>
              <span class="text-gray-200">·</span>
              <span class="text-xs text-gray-400">{{ formatDate(c.createdAt) }}</span>
            </div>
            <p class="text-sm text-gray-700 leading-relaxed">{{ c.content }}</p>

            <div v-if="repliesOf(c.id).length > 0" class="mt-3 pl-4 border-l-2 border-blue-100/80 space-y-3">
              <div v-for="r in repliesOf(c.id)" :key="r.id" class="flex items-start gap-2">
                <div class="w-6 h-6 rounded-full bg-gradient-to-br from-indigo-300 to-purple-400 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                  {{ avatarLetter(r.userId) }}
                </div>
                <div>
                  <span class="text-xs font-semibold text-gray-500 mr-1.5">用户 {{ r.userId }}</span>
                  <span class="text-xs text-gray-600">{{ r.content }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getComments, createComment } from '../api/comments'

const props = defineProps({ postId: Number })
const auth = useAuthStore()
const comments = ref([])
const newComment = ref('')
const submitting = ref(false)

const topLevel = computed(() => comments.value.filter(c => !c.parentId))
const repliesOf = id => comments.value.filter(c => c.parentId === id)

const avatarLetter = userId => String(userId ?? '?').slice(-1).toUpperCase()

async function load() {
  comments.value = await getComments(props.postId)
}

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    await createComment({ postId: props.postId, content: newComment.value })
    newComment.value = ''
    await load()
  } finally {
    submitting.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const diff = Math.floor((new Date() - d) / 60000)
  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff} 分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)} 小时前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(load)
</script>

<style scoped>
.comment-enter-active {
  transition: all 0.3s ease;
}
.comment-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.comment-leave-active {
  transition: all 0.2s ease;
}
.comment-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
