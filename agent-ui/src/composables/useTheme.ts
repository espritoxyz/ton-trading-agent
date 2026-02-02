import { ref, onMounted, watch } from 'vue'

const THEME_STORAGE_KEY = 'app-theme'

function getSystemTheme(): 'light' | 'dark' {
  if (typeof window !== 'undefined' && window.matchMedia) {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return 'light'
}

export function applyTheme(newTheme: 'light' | 'dark') {
  const html = document.documentElement
  if (newTheme === 'dark') {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

export function loadTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light'
  
  const saved = localStorage.getItem(THEME_STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') {
    return saved
  }
  
  return getSystemTheme()
}

// New: synchronous loader used at import/runtime before mount
function loadThemeSync(): 'light' | 'dark' {
  try {
    const saved = (typeof window !== 'undefined') ? localStorage.getItem(THEME_STORAGE_KEY) : null
    if (saved === 'light' || saved === 'dark') return saved
  } catch (e) {
    // ignore
  }
  // Default to dark to match product requirement
  return 'dark'
}

function saveTheme(newTheme: 'light' | 'dark') {
  if (typeof window !== 'undefined') {
    localStorage.setItem(THEME_STORAGE_KEY, newTheme)
  }
}

// Initialize theme synchronously so the class is available ASAP
const theme = ref<'light' | 'dark'>(loadThemeSync())
// Ensure applied immediately (idempotent)
if (typeof document !== 'undefined') {
  applyTheme(theme.value)
}

function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
}

watch(theme, (newTheme) => {
  applyTheme(newTheme)
  saveTheme(newTheme)
})

// (functions are exported above)

export function useTheme() {
  onMounted(() => {
    const loadedTheme = loadTheme()
    theme.value = loadedTheme
    applyTheme(loadedTheme)
    
    if (typeof window !== 'undefined' && window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      const handler = (e: MediaQueryListEvent) => {
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
