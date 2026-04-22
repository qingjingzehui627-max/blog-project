<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold bg-gradient-to-r from-blue-700 to-indigo-600 bg-clip-text text-transparent">编辑文章</h1>
    </div>

    <div class="bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden">
      <div v-if="form.title === undefined" class="flex items-center justify-center py-24">
        <div class="flex items-center gap-3 text-gray-400 text-sm">
          <svg class="animate-spin w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
          </svg>
          加载中...
        </div>
      </div>

      <form v-else @submit.prevent="handleSubmit">
        <div class="px-8 pt-8 pb-5">
          <input
            v-model="form.title"
            placeholder="文章标题"
            required
            class="w-full text-2xl font-bold text-gray-800 placeholder-gray-200 focus:outline-none border-0 bg-transparent"
          />
        </div>

        <div class="border-t border-gray-100">
          <MdEditor
            v-model="form.content"
            :on-upload-img="handleImageUpload"
            style="height: 520px; border: none; border-radius: 0; box-shadow: none;"
          />
        </div>

        <div class="px-8 py-4 border-t border-gray-100 bg-gray-50/40 flex items-center justify-between">
          <Transition name="slide-err">
            <p v-if="error" class="text-red-400 text-xs flex items-center gap-1">⚠ {{ error }}</p>
          </Transition>
          <div class="flex items-center gap-3 ml-auto">
            <RouterLink
              :to="`/post/${route.params.id}`"
              class="px-4 py-2 text-sm text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-all duration-200"
            >
              取消
            </RouterLink>
            <button
              type="submit"
              :disabled="submitting"
              class="px-6 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-medium rounded-full shadow-sm hover:shadow-md hover:shadow-blue-200/60 hover:-translate-y-0.5 disabled:opacity-50 disabled:transform-none transition-all duration-200"
            >
              {{ submitting ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { getPostById, updatePost } from '../api/posts'
import { uploadImage } from '../api/upload'

const route = useRoute()
const router = useRouter()
const form = ref({})
const error = ref('')
const submitting = ref(false)

async function handleImageUpload(files, callback) {
  const urls = await Promise.all(files.map(f => uploadImage(f)))
  callback(urls)
}

async function handleSubmit() {
  submitting.value = true
  try {
    await updatePost(route.params.id, form.value)
    router.push(`/post/${route.params.id}`)
  } catch {
    error.value = '保存失败，请重试'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const post = await getPostById(route.params.id)
  form.value = { title: post.title, content: post.content, categoryId: post.categoryId, cover: post.cover }
})
</script>

<style scoped>
.slide-err-enter-active, .slide-err-leave-active {
  transition: all 0.2s ease;
}
.slide-err-enter-from, .slide-err-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}
</style>
