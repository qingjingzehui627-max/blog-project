import { computed, ref } from 'vue'

const THEME_KEY = 'hui-workspace-theme'
const initialTheme = localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light'
const theme = ref(initialTheme)

function applyTheme(value) {
  const root = document.documentElement
  root.classList.toggle('dark', value === 'dark')
  localStorage.setItem(THEME_KEY, value)
}

let initialized = false

export function useTheme() {
  if (!initialized) {
    applyTheme(theme.value)
    initialized = true
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    applyTheme(theme.value)
  }

  function setTheme(value) {
    theme.value = value === 'dark' ? 'dark' : 'light'
    applyTheme(theme.value)
  }

  return {
    theme,
    isDark: computed(() => theme.value === 'dark'),
    toggleTheme,
    setTheme
  }
}
