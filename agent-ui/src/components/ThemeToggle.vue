<script setup lang="ts">
import { useTheme } from '../composables/useTheme'
import { SunIcon, MoonIcon } from '@heroicons/vue/24/outline'
const { theme, toggleTheme } = useTheme()
</script>

<template>
  <button
    @click="toggleTheme"
    class="rounded-xl p-2 bg-gray-200 dark:bg-white/10 border border-gray-300 dark:border-white/20 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-white/20 hover:text-gray-900 dark:hover:text-white transition group"
    aria-label="Toggle theme"
  >
    <SunIcon v-if="theme === 'dark'" class="h-5 w-5 group-hover:rotate-180 transition-transform duration-500" />
    <MoonIcon v-else class="h-5 w-5 group-hover:-rotate-12 transition-transform duration-300" />
  </button>
</template>
