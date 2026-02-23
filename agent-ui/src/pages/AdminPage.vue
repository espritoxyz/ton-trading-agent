<script setup lang="ts">
import { ref, inject } from 'vue'
import { Newspaper, Eye, Send, Loader, AlertTriangle, CheckCircle2, Users } from 'lucide-vue-next'
import { api } from '../composables/useApi'

type AdminTab = 'newsletter'

const activeTab = ref<AdminTab>('newsletter')

const tabs = [
  { id: 'newsletter' as const, label: 'Newsletter', icon: Newspaper },
]

const setNavigationTabs = inject<any>('setNavigationTabs', null)
if (setNavigationTabs) {
  setNavigationTabs(tabs, activeTab)
}

// ── Newsletter state ────────────────────────────────────────────
const subject = ref('')
const htmlContent = ref('')
const previewHtml = ref('')
const previewLoading = ref(false)
const sendLoading = ref(false)
const error = ref('')
const sendResult = ref<{ totalSubscribers: number; sent: number; failed: number } | null>(null)

function openPreviewInTab() {
  const w = window.open()
  if (w) { w.document.write(previewHtml.value); w.document.close() }
}

async function preview() {
  if (!subject.value.trim() || !htmlContent.value.trim()) {
    error.value = 'Fill in subject and HTML content'
    return
  }
  previewLoading.value = true
  error.value = ''
  sendResult.value = null
  try {
    const resp = await api.post(
      '/newsletter/admin/preview',
      { subject: subject.value, htmlContent: htmlContent.value },
      { responseType: 'text' }
    )
    previewHtml.value = resp.data as string
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? e?.message ?? 'Preview failed'
  } finally {
    previewLoading.value = false
  }
}

async function send() {
  if (!subject.value.trim() || !htmlContent.value.trim()) {
    error.value = 'Fill in subject and HTML content'
    return
  }
  if (!confirm(`Send "${subject.value}" to all active subscribers?\n\nThis cannot be undone.`)) return

  sendLoading.value = true
  error.value = ''
  sendResult.value = null
  try {
    const { data } = await api.post('/newsletter/admin/send', {
      subject: subject.value,
      htmlContent: htmlContent.value,
    })
    sendResult.value = data
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? e?.message ?? 'Send failed'
  } finally {
    sendLoading.value = false
  }
}
</script>

<template>
  <div class="h-full overflow-auto page-scroller">

    <!-- Newsletter Tab -->
    <div v-if="activeTab === 'newsletter'" class="p-1">

      <!-- Two-column grid: compose | preview -->
      <div class="grid gap-4 lg:grid-cols-[400px_1fr]" style="min-height: calc(100vh - 120px);">

        <!-- ── Left: Compose ── -->
        <div class="glass-card p-6 flex flex-col gap-4">

          <!-- Header -->
          <div class="flex items-center gap-3 pb-4 border-b border-gray-200 dark:border-white/10">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-cosmic-500 to-purple-600 flex items-center justify-center shadow-lg shrink-0">
              <Newspaper :size="20" class="text-white" />
            </div>
            <div>
              <h2 class="text-lg font-semibold gradient-text">Compose Newsletter</h2>
              <p class="text-xs text-gray-500 dark:text-gray-400">Sent via Resend to all active subscribers</p>
            </div>
          </div>

          <!-- Subject -->
          <div>
            <label class="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1.5 block">Subject line</label>
            <input
              v-model="subject"
              type="text"
              placeholder="Esprito AI — what's new in March"
              class="w-full rounded-lg bg-gray-100 dark:bg-white/10 border border-gray-300 dark:border-white/20 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 transition"
            />
          </div>

          <!-- HTML Content -->
          <div class="flex-1 flex flex-col">
            <label class="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1.5 block">HTML content</label>
            <textarea
              v-model="htmlContent"
              placeholder="<h2>Hello!</h2>&#10;<p>This month we launched...</p>"
              class="flex-1 min-h-[240px] w-full rounded-lg bg-gray-100 dark:bg-white/10 border border-gray-300 dark:border-white/20 px-3 py-2.5 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-cosmic-500 transition resize-none font-mono leading-relaxed"
            />
          </div>

          <!-- Error -->
          <div v-if="error" class="flex items-start gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/30">
            <AlertTriangle :size="15" class="text-red-500 dark:text-red-400 mt-0.5 shrink-0" />
            <p class="text-xs text-red-700 dark:text-red-300">{{ error }}</p>
          </div>

          <!-- Actions -->
          <div class="flex gap-3">
            <button
              @click="preview"
              :disabled="previewLoading || sendLoading"
              class="flex-1 flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium bg-gray-100 dark:bg-white/10 hover:bg-gray-200 dark:hover:bg-white/20 border border-gray-300 dark:border-white/20 text-gray-900 dark:text-white transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Loader v-if="previewLoading" :size="15" class="animate-spin" />
              <Eye v-else :size="15" />
              <span>{{ previewLoading ? 'Loading…' : 'Preview' }}</span>
            </button>

            <button
              @click="send"
              :disabled="previewLoading || sendLoading"
              class="flex-1 flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold bg-gradient-to-r from-cosmic-500 to-purple-600 text-white hover:opacity-90 transition shadow-md shadow-cosmic-500/20 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Loader v-if="sendLoading" :size="15" class="animate-spin" />
              <Send v-else :size="15" />
              <span>{{ sendLoading ? 'Sending…' : 'Send to all' }}</span>
            </button>
          </div>

          <!-- Send Result -->
          <div v-if="sendResult" class="rounded-lg bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/30 p-4">
            <div class="flex items-center gap-2 mb-3">
              <CheckCircle2 :size="16" class="text-emerald-500" />
              <span class="text-sm font-semibold text-emerald-800 dark:text-emerald-300">Broadcast complete</span>
            </div>
            <div class="grid grid-cols-3 gap-2 text-center">
              <div class="rounded-lg bg-white/60 dark:bg-white/5 p-2 border border-gray-200 dark:border-white/10">
                <div class="text-lg font-bold text-gray-700 dark:text-gray-200">{{ sendResult.totalSubscribers }}</div>
                <div class="text-[10px] text-gray-500 dark:text-gray-400 flex items-center justify-center gap-1">
                  <Users :size="10" />Total
                </div>
              </div>
              <div class="rounded-lg bg-white/60 dark:bg-white/5 p-2 border border-gray-200 dark:border-white/10">
                <div class="text-lg font-bold text-emerald-600 dark:text-emerald-400">{{ sendResult.sent }}</div>
                <div class="text-[10px] text-gray-500 dark:text-gray-400">Sent</div>
              </div>
              <div class="rounded-lg bg-white/60 dark:bg-white/5 p-2 border border-gray-200 dark:border-white/10">
                <div class="text-lg font-bold" :class="sendResult.failed > 0 ? 'text-red-500' : 'text-gray-400 dark:text-gray-500'">
                  {{ sendResult.failed }}
                </div>
                <div class="text-[10px] text-gray-500 dark:text-gray-400">Failed</div>
              </div>
            </div>
          </div>
        </div>

        <!-- ── Right: Preview ── -->
        <div class="glass-card overflow-hidden flex flex-col">
          <div class="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-white/10 shrink-0">
            <span class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Email Preview</span>
            <button
              v-if="previewHtml"
              @click="openPreviewInTab"
              class="text-xs text-cosmic-500 hover:text-cosmic-400 transition font-medium"
            >
              Open in new tab ↗
            </button>
          </div>

          <!-- Placeholder -->
          <div v-if="!previewHtml" class="flex-1 flex flex-col items-center justify-center gap-3 text-gray-300 dark:text-gray-700">
            <Eye :size="40" stroke-width="1.5" />
            <p class="text-sm">Click <strong class="text-gray-500 dark:text-gray-500">Preview</strong> to render the email here</p>
          </div>

          <!-- Rendered email -->
          <iframe
            v-else
            :srcdoc="previewHtml"
            class="flex-1 w-full border-none bg-white"
            sandbox="allow-same-origin"
          />
        </div>

      </div>
    </div>

  </div>
</template>

<style scoped>
.page-scroller {
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.4) transparent;
  scrollbar-gutter: stable;
}
.page-scroller::-webkit-scrollbar { width: 6px; }
.page-scroller::-webkit-scrollbar-track { background: transparent; }
.page-scroller::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #6366f1, #a855f7);
  border-radius: 10px;
}
</style>
