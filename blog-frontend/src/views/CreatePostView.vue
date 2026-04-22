<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold bg-gradient-to-r from-blue-700 to-indigo-600 bg-clip-text text-transparent">写文章</h1>
    </div>

    <div class="bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden">
      <form @submit.prevent="handleSubmit">
        <div class="px-8 pt-8 pb-5">
          <input
            v-model="form.title"
            placeholder="输入文章标题..."
            required
            class="w-full text-2xl font-bold text-gray-800 placeholder-gray-200 focus:outline-none border-0 bg-transparent"
          />
        </div>

        <div class="px-8 pb-4 flex items-center gap-3 flex-wrap border-b border-gray-100">
          <select
            v-model="form.categoryId"
            class="text-xs text-gray-500 border border-gray-200 rounded-full px-3 py-1.5 bg-gray-50 hover:bg-white focus:outline-none focus:border-blue-300 transition-all duration-200"
          >
            <option value="">选择分类</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>

          <label class="flex items-center gap-1.5 text-xs text-gray-400 cursor-pointer border border-gray-200 rounded-full px-3 py-1.5 hover:border-blue-300 hover:text-blue-500 hover:bg-blue-50/50 transition-all duration-200">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
            上传封面
            <input type="file" accept="image/*" class="hidden" @change="handleCover" />
          </label>

          <Transition name="badge">
            <span v-if="form.cover" class="text-xs text-green-600 bg-green-50 border border-green-100 px-2.5 py-0.5 rounded-full">
              ✓ 封面已上传
            </span>
          </Transition>
        </div>

        <MdEditor
          v-model="form.content"
          :on-upload-img="handleImageUpload"
          style="height: 520px; border: none; border-radius: 0; box-shadow: none;"
        />

        <div class="px-8 py-4 border-t border-gray-100 bg-gray-50/40 flex items-center justify-between">
          <Transition name="slide-err">
            <p v-if="error" class="text-red-400 text-xs flex items-center gap-1">⚠ {{ error }}</p>
          </Transition>
          <div class="flex items-center gap-3 ml-auto">
            <RouterLink
              to="/"
              class="px-4 py-2 text-sm text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-all duration-200"
            >
              取消
            </RouterLink>
            <button
              type="submit"
              :disabled="submitting"
              class="px-6 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white text-sm font-medium rounded-full shadow-sm hover:shadow-md hover:shadow-blue-200/60 hover:-translate-y-0.5 disabled:opacity-50 disabled:transform-none transition-all duration-200"
            >
              {{ submitting ? '发布中...' : '发布文章' }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createPost } from '../api/posts'
import { uploadImage } from '../api/upload'
import { getCategories } from '../api/categories'

const router = useRouter()
const categories = ref([])
const form = ref({ title: '', content: '', categoryId: '', cover: '' })
const error = ref('')
const submitting = ref(false)

async function handleCover(e) {
  const file = e.target.files[0]
  if (!file) return
  form.value.cover = await uploadImage(file)
}

async function handleImageUpload(files, callback) {
  const urls = await Promise.all(files.map(f => uploadImage(f)))
  callback(urls)
}

async function handleSubmit() {
  if (!form.value.content.trim()) { error.value = '内容不能为空'; return }
  submitting.value = true
  try {
    await createPost(form.value)
    router.push('/')
  } catch {
    error.value = '发布失败，请重试'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => { categories.value = await getCategories() })
</script>

<style scoped>
.badge-enter-active, .badge-leave-active {
  transition: all 0.2s ease;
}
.badge-enter-from, .badge-leave-to {
  opacity: 0;
  transform: scale(0.85);
}
.slide-err-enter-active, .slide-err-leave-active {
  transition: all 0.2s ease;
}
.slide-err-enter-from, .slide-err-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}
</style>
