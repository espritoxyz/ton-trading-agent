<script setup lang="ts">
import Dashboard from './pages/Dashboard.vue'
import { useTheme } from './composables/useTheme.ts'
import { SunIcon, MoonIcon } from '@heroicons/vue/24/outline'
import { APP_VERSION } from './config.ts'

const { theme, toggleTheme } = useTheme()
</script>

<template>
  <div class="mx-auto flex h-screen max-w-6xl flex-col p-4">
    <header class="mb-4 flex shrink-0 items-center justify-between">
      <div class="flex items-center gap-3">
        <svg width="32" height="32" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg" class="h-8 w-8">
          <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" class="fill-indigo-600 dark:fill-indigo-400"/>
          <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
          <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
        </svg>
        <h1 class="text-xl font-medium text-gray-900 dark:text-gray-100">Esprito AI</h1>
      </div>
      <div class="flex items-center gap-4">
        <div class="text-xs text-gray-500 dark:text-gray-400">
          <div>Caution: Alpha Software</div>
          <div>Version {{ APP_VERSION }}</div>
        </div>
        <button
          @click="toggleTheme"
          class="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-200"
          aria-label="Toggle theme"
        >
          <SunIcon v-if="theme === 'dark'" class="h-5 w-5" />
          <MoonIcon v-else class="h-5 w-5" />
        </button>
      </div>
    </header>

    <div class="flex-1 overflow-hidden">
      <Dashboard />
    </div>
  </div>
</template>
