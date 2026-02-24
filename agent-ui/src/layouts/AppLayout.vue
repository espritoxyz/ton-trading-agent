<script setup lang="ts">
import { ref, provide } from 'vue'
import { APP_VERSION } from '../config'
import { useTheme } from '../composables/useTheme'
import { SunIcon, MoonIcon } from '@heroicons/vue/24/outline'
import ThemeToggle from '../components/ThemeToggle.vue'
import AccountMenu from '../components/AccountMenu.vue'
import NotificationBell from '../components/NotificationBell.vue'
import NotificationToast from '../components/NotificationToast.vue'

const navigationTabs = ref<any[]>([])
const activeTab = ref<any>(null)

// Provide a function for child components to set navigation tabs
provide('setNavigationTabs', (tabs: any[], activeTabRef: any) => {
  navigationTabs.value = tabs
  activeTab.value = activeTabRef
})
</script>

<style scoped>
.star {
  animation: twinkle 3s infinite;
}
</style>

<template>
  <div class="relative min-h-screen overflow-hidden">
    <!-- Light Theme Background -->
    <div class="light-bg fixed inset-0 bg-gradient-to-br from-gray-50 via-blue-50 to-purple-50"></div>

    <!-- Dark Theme Cosmic Background -->
    <div class="dark-bg fixed inset-0 bg-space-darker hidden dark:block" style="
      background-image:
        radial-gradient(circle at 20% 50%, rgba(99, 102, 241, 0.15) 0%, transparent 50%),
        radial-gradient(circle at 80% 80%, rgba(139, 92, 246, 0.15) 0%, transparent 50%),
        radial-gradient(circle at 40% 20%, rgba(59, 130, 246, 0.1) 0%, transparent 50%);
    "></div>

    <!-- Animated Stars (Dark Theme Only) -->
    <div class="stars fixed inset-0 pointer-events-none hidden dark:block">
      <div v-for="i in 100" :key="i" class="star absolute w-0.5 h-0.5 bg-white rounded-full"
        :style="{
          left: `${Math.random() * 100}%`,
          top: `${Math.random() * 100}%`,
          animationDelay: `${Math.random() * 3}s`
        }"
      />
    </div>

    <!-- Main Content -->
    <div class="relative z-10 mx-auto flex h-screen max-w-6xl flex-col p-4">
      <header class="mb-4 flex shrink-0 items-center justify-between glass-card p-4">
        <a
          href="/"
          class="flex items-center gap-3 group no-underline"
          title="Return to home"
        >
          <svg width="32" height="32" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 transition-transform group-hover:scale-105">
            <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
            <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
            <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
          </svg>
          <div class="relative">
            <h1 class="text-xl font-semibold gradient-text group-hover:opacity-80 transition">Esprito AI</h1>
            <!-- Alpha Badge Superscript -->
            <span class="absolute -top-1 -right-10 px-1.5 py-0.5 text-[9px] font-bold rounded bg-gradient-to-r from-violet-500 to-purple-600 text-white shadow-sm shadow-violet-500/40">
              BETA
            </span>
          </div>
        </a>

        <!-- Navigation Tabs -->
        <div class="flex-1 flex justify-center">
          <div v-if="navigationTabs.length > 0 && activeTab" class="flex gap-2">
            <button
              v-for="tab in navigationTabs"
              :key="tab.id"
              @click="activeTab.value = tab.id"
              :class="[
                'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 shrink-0',
                activeTab.value === tab.id
                  ? 'bg-gradient-to-r from-cosmic-500 to-purple-600 text-white shadow-lg shadow-cosmic-500/30'
                  : 'bg-gray-200 dark:bg-transparent text-gray-800 dark:text-gray-400 hover:bg-gray-300 dark:hover:bg-white/5 border border-gray-300 dark:border-transparent shadow-sm'
              ]"
            >
              <component :is="tab.icon" :size="18" />
              <span>{{ tab.label }}</span>
            </button>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <NotificationBell />
          <AccountMenu />
          <ThemeToggle />
        </div>
      </header>

      <div class="flex-1 overflow-hidden min-h-0">
        <slot />
      </div>
    </div>

    <!-- Notification Toast Container -->
    <NotificationToast />
  </div>
</template>
