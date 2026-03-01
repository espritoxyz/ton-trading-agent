<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { logout, accessToken, email, refreshProfile, userId, isAdmin } from '../composables/useAuth.ts'
import { useWalletState } from '../composables/useWalletState.ts'
import { APP_VERSION } from '../config'
import AccountSettingsModal from './AccountSettingsModal.vue'
import { User, LogOut, RefreshCw, LogIn, ShieldCheck, Settings } from 'lucide-vue-next'

const { refreshWalletState, clearWalletState } = useWalletState()

const showDropdown = ref(false)
const showSettings = ref(false)
const buttonRef = ref<HTMLElement>()
const dropdownContentRef = ref<HTMLElement>()
const dropdownPosition = ref({ top: 0, right: 0 })

function navigateTo(path: string) {
  history.pushState({}, '', path)
  window.dispatchEvent(new PopStateEvent('popstate'))
}

function onLogout() {
  logout()
  clearWalletState()
  showDropdown.value = false
}

async function onRefresh() {
  if (userId.value) {
    await Promise.all([refreshProfile(), refreshWalletState(userId.value)])
  }
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value && buttonRef.value) {
    const rect = buttonRef.value.getBoundingClientRect()
    dropdownPosition.value = {
      top: rect.bottom + 8,
      right: window.innerWidth - rect.right
    }
  }
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as Node
  if (!buttonRef.value?.contains(target) && !dropdownContentRef.value?.contains(target) && showDropdown.value) {
    showDropdown.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  if (accessToken.value) {
    await refreshProfile()
    if (userId.value) {
      await refreshWalletState(userId.value)
    }
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="relative">
    <!-- Not Logged In: Sign In Button -->
    <button
      v-if="!accessToken"
      @click="navigateTo('/login')"
      class="flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium bg-gradient-to-r from-cosmic-500 to-purple-600 text-white hover:opacity-90 transition shadow-md"
    >
      <LogIn :size="16" />
      <span>Sign In</span>
    </button>

    <!-- Logged In: User Avatar -->
    <button
      v-else
      ref="buttonRef"
      @click="toggleDropdown"
      class="relative rounded-full p-1 hover:bg-gray-200 dark:hover:bg-white/10 transition"
      title="Account Menu"
    >
      <div class="w-8 h-8 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center shadow-md">
        <User :size="18" class="text-white" />
      </div>
      <div class="absolute bottom-0.5 right-0.5 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-white dark:border-gray-900"></div>
    </button>

    <!-- Dropdown Menu (Logged In) -->
    <Teleport to="body">
      <div
        v-if="showDropdown && accessToken"
        ref="dropdownContentRef"
        class="fixed w-72 glass-card p-4 shadow-xl border border-gray-200 dark:border-white/20 rounded-xl z-[9999]"
        :style="{ top: dropdownPosition.top + 'px', right: dropdownPosition.right + 'px' }"
      >
        <div class="space-y-3">
          <div class="flex items-center gap-3 pb-3 border-b border-gray-200 dark:border-white/10">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center shadow-lg">
              <User :size="20" class="text-white" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-xs text-gray-600 dark:text-gray-400 mb-0.5">Logged in as</div>
              <div class="text-sm text-gray-900 dark:text-white font-medium truncate">{{ email ?? '—' }}</div>
            </div>
          </div>

          <button
            class="w-full rounded-lg bg-gray-100 dark:bg-white/10 px-4 py-2 text-sm font-medium text-gray-900 dark:text-white hover:bg-gray-200 dark:hover:bg-white/20 transition border border-gray-300 dark:border-white/20 flex items-center justify-center gap-2"
            @click="onRefresh"
          >
            <RefreshCw :size="16" />
            <span>Refresh Profile</span>
          </button>

          <button
            class="w-full rounded-lg bg-gray-100 dark:bg-white/10 px-4 py-2 text-sm font-medium text-gray-900 dark:text-white hover:bg-gray-200 dark:hover:bg-white/20 transition border border-gray-300 dark:border-white/20 flex items-center justify-center gap-2"
            @click="showSettings = true; showDropdown = false"
          >
            <Settings :size="16" />
            <span>Account Settings</span>
          </button>

          <a
            v-if="isAdmin"
            href="/app/admin"
            @click="showDropdown = false"
            class="w-full rounded-lg bg-cosmic-500/10 dark:bg-cosmic-500/20 px-4 py-2 text-sm font-medium text-cosmic-700 dark:text-cosmic-300 hover:bg-cosmic-500/20 dark:hover:bg-cosmic-500/30 transition border border-cosmic-500/30 flex items-center justify-center gap-2 no-underline"
          >
            <ShieldCheck :size="16" />
            <span>Admin Panel</span>
          </a>

          <button
            class="w-full rounded-lg bg-gradient-to-r from-red-500 to-pink-600 px-4 py-2 text-sm font-semibold text-white hover:opacity-90 transition flex items-center justify-center gap-2 shadow-md"
            @click="onLogout"
          >
            <LogOut :size="16" />
            <span>Logout</span>
          </button>

          <div class="pt-2 mt-1 border-t border-gray-200 dark:border-white/10 text-center">
            <div class="text-xs text-gray-500 dark:text-gray-400">Version {{ APP_VERSION }}</div>
          </div>
        </div>
      </div>
    </Teleport>

    <AccountSettingsModal v-if="showSettings" @close="showSettings = false" />
  </div>
</template>
