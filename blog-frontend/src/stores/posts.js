import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPosts } from '../api/posts'

export const usePostsStore = defineStore('posts', () => {
  const posts = ref([])
  const currentPage = ref(1)
  const loading = ref(false)

  async function fetchPosts(page = 1, size = 10) {
    loading.value = true
    try {
      const res = await getPosts(page, size)
      posts.value = res
      currentPage.value = page
    } finally {
      loading.value = false
    }
  }

  return { posts, currentPage, loading, fetchPosts }
})
