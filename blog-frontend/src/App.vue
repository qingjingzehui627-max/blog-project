<template>
  <div :class="appShellClass">
    <NavBar />
    <main class="mx-auto w-full max-w-[1520px] px-2 pb-10 pt-6 sm:px-3 lg:px-4">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import NavBar from './components/NavBar.vue'
import { useTheme } from './composables/useTheme'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const { isDark } = useTheme()

const appShellClass = computed(() =>
  isDark.value
    ? 'min-h-screen bg-zinc-950 text-zinc-100 transition-colors duration-300'
    : 'min-h-screen bg-transparent text-slate-900 transition-colors duration-300'
)

onMounted(() => auth.fetchUser())
</script>

<style>
.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
