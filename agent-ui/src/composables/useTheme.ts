import { ref, onMounted, watch } from 'vue'

const theme = ref<'light' | 'dark'>('light')
const THEME_STORAGE_KEY = 'app-theme'

function getSystemTheme(): 'light' | 'dark' {
  if (typeof window !== 'undefined' && window.matchMedia) {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return 'light'
}

function applyTheme(newTheme: 'light' | 'dark') {
  const html = document.documentElement
  if (newTheme === 'dark') {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

function loadTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light'
  
  const saved = localStorage.getItem(THEME_STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') {
    return saved
  }
  
  return getSystemTheme()
}

function saveTheme(newTheme: 'light' | 'dark') {
  if (typeof window !== 'undefined') {
    localStorage.setItem(THEME_STORAGE_KEY, newTheme)
  }
}

function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
}

watch(theme, (newTheme) => {
  applyTheme(newTheme)
  saveTheme(newTheme)
})

export function useTheme() {
  onMounted(() => {
    const loadedTheme = loadTheme()
    theme.value = loadedTheme
    applyTheme(loadedTheme)
    
    // Listen for system theme changes if no manual preference
    if (typeof window !== 'undefined' && window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      const handler = (e: MediaQueryListEvent) => {
        // Only auto-update if user hasn't set a preference
        if (!localStorage.getItem(THEME_STORAGE_KEY)) {
          theme.value = e.matches ? 'dark' : 'light'
        }
      }
      mediaQuery.addEventListener('change', handler)
    }
  })

  return {
    theme,
    toggleTheme,
    isDark: () => theme.value === 'dark'
  }
}
