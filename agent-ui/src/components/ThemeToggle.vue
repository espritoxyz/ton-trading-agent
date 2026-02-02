<script setup lang="ts">
import { useTheme } from '../composables/useTheme'
import { SunIcon, MoonIcon } from '@heroicons/vue/24/outline'
const { theme, toggleTheme } = useTheme()
</script>

<template>
  <button
    @click="toggleTheme"
    class="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-200"
    aria-label="Toggle theme"
  >
    <SunIcon v-if="theme === 'dark'" class="h-5 w-5" />
    <MoonIcon v-else class="h-5 w-5" />
  </button>
</template>
