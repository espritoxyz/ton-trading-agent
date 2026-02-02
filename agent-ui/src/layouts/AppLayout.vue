<script setup lang="ts">
import { APP_VERSION } from '../config'
import { useTheme } from '../composables/useTheme'
import { SunIcon, MoonIcon } from '@heroicons/vue/24/outline'
import ThemeToggle from '../components/ThemeToggle.vue'
</script>

<template>
  <div class="mx-auto flex h-screen max-w-6xl flex-col p-4">
    <header class="mb-4 flex shrink-0 items-center justify-between">
      <div class="flex items-center gap-3">
        <!-- logo -->
        <svg width="32" height="32" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg" class="h-8 w-8">
          <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" class="fill-indigo-600 dark:fill-indigo-400"/>
          <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
        </svg>
        <h1 class="text-xl font-medium text-gray-900 dark:text-gray-100">Esprito AI</h1>
      </div>
      <div class="flex items-center gap-4">
        <div class="text-xs text-gray-500 dark:text-gray-400">
          <div>Caution: Alpha Software</div>
          <div>Version {{ APP_VERSION }}</div>
        </div>
        <ThemeToggle />
      </div>
    </header>

    <div class="flex-1 overflow-hidden min-h-0">
      <slot />
    </div>
  </div>
</template>
