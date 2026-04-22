import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../views/HomeView.vue') },
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/register', component: () => import('../views/RegisterView.vue') },
  { path: '/post/:id', component: () => import('../views/PostDetailView.vue') },
  { path: '/create', component: () => import('../views/CreatePostView.vue'), meta: { auth: true } },
  { path: '/edit/:id', component: () => import('../views/EditPostView.vue'), meta: { auth: true } },
  { path: '/favorites', component: () => import('../views/FavoritesView.vue'), meta: { auth: true } },
  { path: '/user/:id', component: () => import('../views/ProfileView.vue') }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  if (to.meta.auth && !localStorage.getItem('token')) {
    return '/login'
  }
})

export default router
