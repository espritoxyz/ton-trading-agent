<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Settings, Mail, Bell, BellOff, Loader, CheckCircle, X } from 'lucide-vue-next'
import { api } from '../composables/useApi.ts'
import { email } from '../composables/useAuth.ts'

const emits = defineEmits(['close'])

const loadingStatus = ref(true)
const subscribed = ref(false)
const saving = ref(false)
const saveSuccess = ref(false)
const saveError = ref<string | null>(null)

onMounted(async () => {
  loadingStatus.value = true
  try {
    const { data } = await api.get('/newsletter/subscription')
    subscribed.value = data.subscribed ?? false
  } catch {
    subscribed.value = false
  } finally {
    loadingStatus.value = false
  }
})

async function toggleSubscription() {
  saving.value = true
  saveSuccess.value = false
  saveError.value = null
  const next = !subscribed.value
  try {
    await api.put('/newsletter/subscription', { subscribed: next })
    subscribed.value = next
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 3000)
  } catch (e: any) {
    saveError.value = e?.response?.data?.message ?? e?.message ?? 'Failed to update subscription'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div class="absolute inset-0 bg-black/50 dark:bg-black/70" @click="emits('close')"></div>

      <div class="relative w-full max-w-md bg-white dark:bg-white/5 backdrop-blur-lg border-2 border-gray-300 dark:border-white/10 rounded-2xl p-8 shadow-2xl cosmic-glow">

        <!-- Header -->
        <div class="flex items-center justify-between mb-6 pb-4 border-b-2 border-gray-200 dark:border-white/10">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center shadow-lg">
              <Settings :size="20" class="text-white" />
            </div>
            <div>
              <h3 class="text-xl font-bold text-gray-900 dark:text-white">
                <span class="gradient-text">Account Settings</span>
              </h3>
              <p class="text-xs text-gray-600 dark:text-gray-400 mt-0.5">Manage your preferences</p>
            </div>
          </div>
          <button
            class="w-8 h-8 rounded-lg flex items-center justify-center text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-white/10 hover:text-gray-900 dark:hover:text-white transition"
            @click="emits('close')"
            aria-label="Close"
          >
            <X :size="18" />
          </button>
        </div>

        <div class="space-y-5">
          <!-- Email (read-only) -->
          <div>
            <label class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 block flex items-center gap-1.5">
              <Mail :size="13" />
              <span>Email</span>
            </label>
            <div class="w-full rounded-xl bg-gray-100 dark:bg-white/5 border-2 border-gray-200 dark:border-white/10 px-4 py-3 text-sm font-medium text-gray-600 dark:text-gray-400 select-all">
              {{ email ?? '—' }}
            </div>
          </div>

          <!-- Newsletter subscription -->
          <div>
            <div class="text-xs font-semibold text-gray-800 dark:text-gray-300 mb-2 flex items-center gap-1.5">
              <Bell :size="13" />
              <span>Newsletter</span>
            </div>

            <!-- Loading skeleton -->
            <div v-if="loadingStatus" class="flex items-center gap-3 p-4 rounded-xl bg-gray-50 dark:bg-white/5 border-2 border-gray-200 dark:border-white/10">
              <Loader :size="16" class="animate-spin text-gray-400" />
              <span class="text-sm text-gray-500 dark:text-gray-400">Loading subscription status...</span>
            </div>

            <!-- Toggle row -->
            <button
              v-else
              type="button"
              @click="toggleSubscription"
              :disabled="saving"
              class="w-full flex items-center justify-between gap-4 p-4 rounded-xl border-2 transition-all disabled:opacity-60 disabled:cursor-not-allowed"
              :class="subscribed
                ? 'bg-cosmic-50 dark:bg-cosmic-500/10 border-cosmic-300 dark:border-cosmic-500/40 hover:border-cosmic-400 dark:hover:border-cosmic-500/60'
                : 'bg-gray-50 dark:bg-white/5 border-gray-200 dark:border-white/10 hover:border-gray-300 dark:hover:border-white/20'"
            >
              <div class="flex items-center gap-3 min-w-0">
                <div
                  class="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0"
                  :class="subscribed ? 'bg-cosmic-100 dark:bg-cosmic-500/20' : 'bg-gray-100 dark:bg-white/10'"
                >
                  <Bell v-if="subscribed" :size="18" class="text-cosmic-600 dark:text-cosmic-400" />
                  <BellOff v-else :size="18" class="text-gray-500 dark:text-gray-400" />
                </div>
                <div class="text-left min-w-0">
                  <div class="text-sm font-semibold" :class="subscribed ? 'text-cosmic-700 dark:text-cosmic-300' : 'text-gray-700 dark:text-gray-300'">
                    {{ subscribed ? 'Subscribed to newsletter' : 'Not subscribed' }}
                  </div>
                  <div class="text-xs mt-0.5" :class="subscribed ? 'text-cosmic-600/70 dark:text-cosmic-400/70' : 'text-gray-500 dark:text-gray-400'">
                    {{ subscribed ? 'Click to unsubscribe' : 'Click to subscribe to updates' }}
                  </div>
                </div>
              </div>

              <!-- Toggle visual -->
              <div
                class="relative w-11 h-6 rounded-full flex-shrink-0 transition-colors duration-200"
                :class="subscribed ? 'bg-cosmic-500' : 'bg-gray-300 dark:bg-white/20'"
              >
                <div
                  class="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform duration-200"
                  :class="subscribed ? 'translate-x-5' : 'translate-x-0'"
                >
                  <Loader v-if="saving" :size="12" class="animate-spin text-gray-400 absolute inset-0 m-auto" />
                </div>
              </div>
            </button>

            <!-- Success feedback -->
            <div
              v-if="saveSuccess"
              class="mt-2 flex items-center gap-2 px-3 py-2 rounded-lg bg-green-50 dark:bg-green-500/10 border border-green-300 dark:border-green-500/30"
            >
              <CheckCircle :size="14" class="text-green-600 dark:text-green-400 flex-shrink-0" />
              <span class="text-xs text-green-700 dark:text-green-300 font-medium">
                {{ subscribed ? 'Subscribed successfully.' : 'Unsubscribed successfully.' }}
              </span>
            </div>

            <!-- Error feedback -->
            <div
              v-if="saveError"
              class="mt-2 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-300 dark:border-red-500/30"
            >
              <span class="text-xs text-red-700 dark:text-red-300">{{ saveError }}</span>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="mt-6 pt-4 border-t-2 border-gray-200 dark:border-white/10 flex justify-end">
          <button
            type="button"
            class="rounded-xl bg-gray-200 dark:bg-white/10 px-5 py-2.5 text-sm font-bold text-gray-800 dark:text-white hover:bg-gray-300 dark:hover:bg-white/20 transition border-2 border-gray-400 dark:border-white/20"
            @click="emits('close')"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
