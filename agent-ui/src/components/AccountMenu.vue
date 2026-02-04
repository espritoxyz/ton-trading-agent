<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { login, logout, loggingIn, accessToken, email, refreshProfile, authError } from '../composables/useAuth.ts'
import { refreshBalance } from '../composables/useBalance.ts'
import { APP_VERSION } from '../config'
import RegisterModal from './RegisterModal.vue'
import { User, LogOut, RefreshCw, AlertTriangle, Loader, LogIn } from 'lucide-vue-next'

const username = ref('')
const password = ref('')
const showDropdown = ref(false)
const showRegister = ref(false)
const containerRef = ref<HTMLElement>()
const buttonRef = ref<HTMLElement>()
const dropdownContentRef = ref<HTMLElement>()
const dropdownPosition = ref({ top: 0, right: 0 })

async function onLogin() {
  await login(username.value, password.value)
  password.value = ''
  if (accessToken.value) {
    await Promise.all([refreshProfile(), refreshBalance()])
    showDropdown.value = false
  }
}

function onLogout() {
  logout()
  showDropdown.value = false
}

async function onRefresh() {
  await Promise.all([refreshProfile(), refreshBalance()])
}

function openRegister() {
  showRegister.value = true
  showDropdown.value = false
}

function closeRegister() {
  showRegister.value = false
}

function onRegistered() {
  closeRegister()
}

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value && buttonRef.value) {
    updateDropdownPosition()
  }
}

function updateDropdownPosition() {
  if (buttonRef.value) {
    const rect = buttonRef.value.getBoundingClientRect()
    dropdownPosition.value = {
      top: rect.bottom + 8,
      right: window.innerWidth - rect.right
    }
  }
}

// Close dropdown when clicking outside
function handleClickOutside(event: MouseEvent) {
  const target = event.target as Node
  const isInsideButton = buttonRef.value?.contains(target)
  const isInsideDropdown = dropdownContentRef.value?.contains(target)

  if (!isInsideButton && !isInsideDropdown && showDropdown.value) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (accessToken.value) {
    Promise.all([refreshProfile(), refreshBalance()])
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="relative" ref="containerRef">
    <!-- Not Logged In: Sign In Button -->
    <button
      v-if="!accessToken"
      ref="buttonRef"
      @click="toggleDropdown"
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
      <!-- Online indicator -->
      <div class="absolute bottom-0.5 right-0.5 w-2.5 h-2.5 bg-emerald-500 rounded-full border-2 border-white dark:border-gray-900"></div>
    </button>

    <!-- Dropdown Menu (Teleported to body) -->
    <Teleport to="body">
      <div
        v-if="showDropdown"
        ref="dropdownContentRef"
        class="fixed w-80 glass-card p-4 shadow-xl border border-gray-200 dark:border-white/20 rounded-xl z-[9999]"
        :style="{ top: dropdownPosition.top + 'px', right: dropdownPosition.right + 'px' }"
      >
      <!-- Login Form (Not Logged In) -->
      <form v-if="!accessToken" class="space-y-3" @submit.prevent="onLogin">
        <div class="flex items-center gap-3 mb-4 pb-3 border-b border-gray-200 dark:border-white/10">
          <div class="w-10 h-10 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center shadow-lg">
            <User :size="20" class="text-white" />
          </div>
          <div>
            <div class="text-sm font-semibold gradient-text">Sign In</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">Access your account</div>
          </div>
        </div>

        <div>
          <label class="text-xs text-gray-600 dark:text-gray-400 mb-1 block">Username or Email</label>
          <input
            v-model="username"
            type="text"
            placeholder="your@email.com"
            autocomplete="username"
            class="w-full rounded-lg bg-gray-100 dark:bg-white/10 border border-gray-300 dark:border-white/20 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition"
          />
        </div>

        <div>
          <label class="text-xs text-gray-600 dark:text-gray-400 mb-1 block">Password</label>
          <input
            v-model="password"
            type="password"
            placeholder="••••••••"
            autocomplete="current-password"
            class="w-full rounded-lg bg-gray-100 dark:bg-white/10 border border-gray-300 dark:border-white/20 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-cosmic-500 focus:border-transparent transition"
          />
        </div>

        <div v-if="authError" class="flex items-center gap-2 p-2 rounded-lg bg-red-100 dark:bg-red-500/10 border border-red-300 dark:border-red-500/30">
          <AlertTriangle :size="16" class="text-red-600 dark:text-red-400" />
          <div class="text-xs text-red-700 dark:text-red-300">{{ authError }}</div>
        </div>

        <button
          type="submit"
          class="cosmic-button w-full rounded-lg px-4 py-2 text-sm font-semibold text-white disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="loggingIn || !username || !password"
        >
          <span v-if="loggingIn" class="flex items-center justify-center gap-2">
            <Loader :size="14" class="animate-spin" />
            <span>Signing in...</span>
          </span>
          <span v-else>Sign In</span>
        </button>

        <div class="text-center pt-2 border-t border-gray-200 dark:border-white/10">
          <span class="text-xs text-gray-600 dark:text-gray-400">Don't have an account? </span>
          <button type="button" class="text-xs text-cosmic-600 dark:text-cosmic-400 hover:text-cosmic-700 dark:hover:text-cosmic-300 transition font-semibold" @click="openRegister">
            Create one
          </button>
        </div>

        <div class="pt-3 mt-3 border-t border-gray-200 dark:border-white/10 text-center">
          <div class="text-xs text-gray-500 dark:text-gray-400">
            Version {{ APP_VERSION }}
          </div>
        </div>
      </form>

      <!-- User Menu (Logged In) -->
      <div v-else class="space-y-3">
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
          class="w-full rounded-lg bg-gradient-to-r from-red-500 to-pink-600 px-4 py-2 text-sm font-semibold text-white hover:opacity-90 transition flex items-center justify-center gap-2 shadow-md"
          @click="onLogout"
        >
          <LogOut :size="16" />
          <span>Logout</span>
        </button>

        <div class="pt-3 mt-3 border-t border-gray-200 dark:border-white/10 text-center">
          <div class="text-xs text-gray-500 dark:text-gray-400">
            Version {{ APP_VERSION }}
          </div>
        </div>
      </div>
      </div>
    </Teleport>

    <RegisterModal v-if="showRegister" @registered="onRegistered" @close="closeRegister" />
  </div>
</template>
