import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPosts } from '../api/posts'
import { getUserById } from '../api/users'

export const usePostsStore = defineStore('posts', () => {
  const posts = ref([])
  const currentPage = ref(1)
  const currentKeyword = ref('')
  const loading = ref(false)

  async function attachAuthorNames(list) {
    const userIds = [...new Set(list.map(post => post.userId).filter(Boolean))]
    const users = await Promise.all(
      userIds.map(async userId => {
        try {
          const user = await getUserById(userId)
          return [userId, user?.username || '未知作者']
        } catch (error) {
          return [userId, '未知作者']
        }
      })
    )
    const authorMap = new Map(users)
    return list.map(post => ({
      ...post,
      authorName: authorMap.get(post.userId) || '未知作者'
    }))
  }

  async function fetchPosts(page = 1, size = 10, keyword = '') {
    loading.value = true
    try {
      const res = await getPosts(page, size, keyword)
      posts.value = await attachAuthorNames(res)
      currentPage.value = page
      currentKeyword.value = keyword
    } finally {
      loading.value = false
    }
  }

  return { posts, currentPage, currentKeyword, loading, fetchPosts }
})
