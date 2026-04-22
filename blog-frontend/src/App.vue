<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/40 to-indigo-50/30">
    <NavBar />
    <main class="max-w-4xl mx-auto px-4 py-10">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
import { useAuthStore } from './stores/auth'
import { onMounted } from 'vue'

const auth = useAuthStore()
onMounted(() => auth.fetchUser())
</script>

<style>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
