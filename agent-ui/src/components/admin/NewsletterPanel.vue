<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Eye, Send, Loader2, AlertTriangle, CheckCircle2,
  Users, Sun, Moon, ExternalLink,
} from 'lucide-vue-next'
import { api } from '../../composables/useApi'

const subject = ref('')
const htmlContent = ref('')
const previewHtml = ref('')
const previewLoading = ref(false)
const sendLoading = ref(false)
const nlError = ref('')
const sendResult = ref<{ totalSubscribers: number; sent: number; failed: number } | null>(null)
const previewDark = ref(false)

// CSS mirrors the exact class rules in newsletter-email.html's
// @media (prefers-color-scheme: dark) block. We always inject (for both
// light and dark) so the preview is deterministic regardless of the
// admin's own OS theme. Injection goes before </head> — our <style>
// tag appears after the template's own <style>, so equal-specificity
// !important rules resolve in our favour via source order.
const PREVIEW_CSS: Record<'light' | 'dark', string> = {
  light: `<style>
    body         { background-color: #f5f7fa !important; color-scheme: light; }
    .bg-light    { background-color: #f5f7fa !important; }
    .bg-white    { background-color: #ffffff !important; }
    .text-dark   { color: #1a1a1a !important; }
    .text-gray   { color: #374151 !important; }
    .text-muted  { color: #64748b !important; }
    .footer-bg   { background-color: #f8fafc !important; }
    .border-gray { border-color: #e2e8f0 !important; }
  </style>`,
  dark: `<style>
    body         { background-color: #1a1a1a !important; color-scheme: dark; }
    .bg-light    { background-color: #1a1a1a !important; }
    .bg-white    { background-color: #2d2d2d !important; }
    .text-dark   { color: #f1f5f9 !important; }
    .text-gray   { color: #cbd5e1 !important; }
    .text-muted  { color: #94a3b8 !important; }
    .footer-bg   { background-color: #1e1e1e !important; }
    .border-gray { border-color: #334155 !important; }
  </style>`,
}

// :key on the iframe forces a full DOM recreate on theme switch —
// browsers ignore srcdoc attribute changes on an existing iframe.
const previewSrcdoc = computed(() => {
  if (!previewHtml.value) return ''
  const css = PREVIEW_CSS[previewDark.value ? 'dark' : 'light']
  return previewHtml.value.includes('</head>')
    ? previewHtml.value.replace('</head>', `${css}</head>`)
    : css + previewHtml.value
})

function openPreviewInTab() {
  const w = window.open()
  if (w) { w.document.write(previewHtml.value); w.document.close() }
}

async function preview() {
  if (!subject.value.trim() || !htmlContent.value.trim()) {
    nlError.value = 'Fill in subject and HTML content'
    return
  }
  previewLoading.value = true
  nlError.value = ''
  sendResult.value = null
  try {
    const resp = await api.post(
      '/newsletter/admin/preview',
      { subject: subject.value, htmlContent: htmlContent.value },
      { responseType: 'text' }
    )
    previewHtml.value = resp.data as string
  } catch (e: any) {
    nlError.value = e?.response?.data?.message ?? e?.message ?? 'Preview failed'
  } finally {
    previewLoading.value = false
  }
}

async function send() {
  if (!subject.value.trim() || !htmlContent.value.trim()) {
    nlError.value = 'Fill in subject and HTML content'
    return
  }
  if (!confirm(`Send "${subject.value}" to all active subscribers?\n\nThis cannot be undone.`)) return
  sendLoading.value = true
  nlError.value = ''
  sendResult.value = null
  try {
    const { data: started } = await api.post('/newsletter/admin/send', {
      subject: subject.value,
      htmlContent: htmlContent.value,
    })
    await pollBroadcastStatus(started.jobId)
  } catch (e: any) {
    nlError.value = e?.response?.data?.message ?? e?.message ?? 'Send failed'
  } finally {
    sendLoading.value = false
  }
}

async function pollBroadcastStatus(jobId: string) {
  const POLL_INTERVAL_MS = 2_000
  const MAX_POLLS = 300 // bail out after ~10 minutes
  for (let i = 0; i < MAX_POLLS; i++) {
    await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL_MS))
    const { data } = await api.get(`/newsletter/admin/status/${jobId}`)
    if (data.state === 'COMPLETED') {
      sendResult.value = data.result
      return
    }
    if (data.state === 'FAILED') {
      throw new Error('Broadcast failed on the server. Check backend logs for details.')
    }
    // RUNNING — keep polling
  }
  throw new Error('Broadcast is taking too long. It may still be running — check backend logs.')
}
</script>

<template>
  <div class="nl-wrap">
    <div class="nl-grid">

      <!-- Compose -->
      <section class="panel">
        <div class="panel-head">
          <span class="panel-title">Compose</span>
        </div>
        <div class="panel-body">

          <div class="field">
            <label for="nl-subject" class="field-label">Subject line</label>
            <input
              id="nl-subject"
              v-model="subject"
              type="text"
              placeholder="Esprito AI — what's new in March"
              class="inp"
            />
          </div>

          <div class="field flex-1">
            <label for="nl-html" class="field-label">HTML content</label>
            <textarea
              id="nl-html"
              v-model="htmlContent"
              placeholder="<h2>Hello!</h2>&#10;<p>This month we launched...</p>"
              class="ta flex-1"
            />
          </div>

          <div v-if="nlError" class="err-box">
            <AlertTriangle :size="14" class="shrink-0 mt-px" />
            <span>{{ nlError }}</span>
          </div>

          <div class="btn-row">
            <button type="button" @click="preview" :disabled="previewLoading || sendLoading" class="btn btn-ghost">
              <Loader2 v-if="previewLoading" :size="14" class="animate-spin" />
              <Eye v-else :size="14" />
              {{ previewLoading ? 'Loading…' : 'Preview' }}
            </button>
            <button type="button" @click="send" :disabled="previewLoading || sendLoading" class="btn btn-primary">
              <Loader2 v-if="sendLoading" :size="14" class="animate-spin" />
              <Send v-else :size="14" />
              {{ sendLoading ? 'Sending…' : 'Send to all' }}
            </button>
          </div>

          <div v-if="sendResult" class="result-box">
            <div class="result-header">
              <CheckCircle2 :size="15" class="result-icon" />
              <span class="result-title">Broadcast complete</span>
            </div>
            <div class="stats-grid">
              <div class="stat">
                <div class="stat-val">{{ sendResult.totalSubscribers }}</div>
                <div class="stat-lbl"><Users :size="10" class="inline mr-0.5" />Total</div>
              </div>
              <div class="stat">
                <div class="stat-val stat-sent">{{ sendResult.sent }}</div>
                <div class="stat-lbl">Sent</div>
              </div>
              <div class="stat">
                <div class="stat-val" :class="sendResult.failed > 0 ? 'stat-failed' : 'stat-zero'">
                  {{ sendResult.failed }}
                </div>
                <div class="stat-lbl">Failed</div>
              </div>
            </div>
          </div>

        </div>
      </section>

      <!-- Preview -->
      <section class="panel">
        <div class="panel-head panel-head-between">
          <span class="panel-title">Email Preview</span>
          <div v-if="previewHtml" class="preview-controls">
            <div class="theme-toggle">
              <button
                type="button"
                @click="previewDark = false"
                :class="['toggle-btn', !previewDark && 'is-active']"
                title="Light background"
              ><Sun :size="13" /></button>
              <button
                type="button"
                @click="previewDark = true"
                :class="['toggle-btn', previewDark && 'is-active']"
                title="Dark background"
              ><Moon :size="13" /></button>
            </div>
            <button type="button" @click="openPreviewInTab" class="open-btn" title="Open in new tab">
              <ExternalLink :size="13" />
            </button>
          </div>
        </div>

        <div v-if="!previewHtml" class="preview-empty">
          <Eye :size="36" stroke-width="1.3" class="preview-empty-icon" />
          <p>Click <strong>Preview</strong> to render the email</p>
        </div>

        <!-- :key forces full iframe recreation on theme switch so srcdoc reloads -->
        <iframe
          v-else
          :key="`preview-${previewDark}`"
          :srcdoc="previewSrcdoc"
          title="Email preview"
          class="preview-frame"
          :style="{ background: previewDark ? '#1a1a2e' : '#fff' }"
          sandbox="allow-same-origin"
        />
      </section>

    </div>
  </div>
</template>

<style scoped>
/* ── Newsletter layout ──────────────────────────────────────────── */
.nl-wrap {
  padding: 18px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.nl-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 14px;
  min-height: calc(100vh - 48px - 36px);
}

/* ── Panel ──────────────────────────────────────────────────────── */
.panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  min-height: 40px;
}

.panel-head-between {
  justify-content: space-between;
}

.panel-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-3);
  text-transform: uppercase;
  letter-spacing: 0.07em;
}

.panel-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--border) transparent;
}

/* ── Form fields ────────────────────────────────────────────────── */
.field { display: flex; flex-direction: column; gap: 6px; }

.field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-3);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.inp {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 8px 11px;
  font-size: 13px;
  font-family: var(--font);
  color: var(--text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  width: 100%;
  box-sizing: border-box;
}

.inp:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.inp::placeholder { color: var(--text-dim); }

.ta {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 11px;
  font-size: 12px;
  font-family: var(--mono);
  color: var(--text-2);
  resize: none;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  min-height: 200px;
  line-height: 1.7;
  box-sizing: border-box;
  width: 100%;
}

.ta:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.ta::placeholder { color: var(--text-dim); }

/* ── Error ──────────────────────────────────────────────────────── */
.err-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--error-bg);
  border: 1px solid var(--error-bdr);
  color: var(--error);
  font-size: 12.5px;
  line-height: 1.5;
}

/* ── Buttons ────────────────────────────────────────────────────── */
.btn-row {
  display: flex;
  gap: 8px;
}

.btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-family: var(--font);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.btn:disabled { opacity: 0.35; cursor: not-allowed; }

.btn-ghost {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-2);
}

.btn-ghost:not(:disabled):hover {
  background: var(--surface-2);
  border-color: var(--border-hi);
  color: var(--text);
}

.btn-primary {
  background: var(--accent);
  border: 1px solid transparent;
  color: #fff;
}

.btn-primary:not(:disabled):hover {
  background: var(--accent-hi);
  box-shadow: 0 2px 12px rgba(90, 114, 232, 0.35);
}

/* ── Send result ────────────────────────────────────────────────── */
.result-box {
  padding: 14px;
  border-radius: var(--radius-sm);
  background: var(--success-bg);
  border: 1px solid var(--success-bdr);
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.result-icon  { color: var(--success); }
.result-title { font-size: 13px; font-weight: 600; color: var(--success); }

.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; text-align: center; }

.stat {
  padding: 10px 8px;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 5px;
  border: 1px solid var(--border);
}

.stat-val { font-size: 18px; font-weight: 700; line-height: 1.2; color: var(--text); }
.stat-lbl { font-size: 10px; color: var(--text-3); margin-top: 3px; }

.stat-sent   { color: var(--success); }
.stat-failed { color: var(--error); }
.stat-zero   { color: var(--text-dim); }

/* ── Preview ────────────────────────────────────────────────────── */
.preview-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-3);
  font-size: 13px;
}

.preview-empty-icon { color: var(--text-dim); }
.preview-empty strong { color: var(--text-2); }

.preview-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.theme-toggle {
  display: flex;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 5px;
  overflow: hidden;
}

.toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px 8px;
  background: none;
  border: none;
  color: var(--text-3);
  cursor: pointer;
  transition: all 0.15s;
}

.toggle-btn:hover { color: var(--text-2); }

.toggle-btn.is-active {
  background: var(--surface-2);
  color: var(--text);
}

.open-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px 8px;
  border-radius: 5px;
  background: none;
  border: 1px solid var(--border);
  color: var(--text-3);
  cursor: pointer;
  transition: all 0.15s;
}

.open-btn:hover {
  color: var(--text-2);
  border-color: var(--border-hi);
  background: var(--surface-2);
}

.preview-frame {
  flex: 1;
  width: 100%;
  border: none;
  display: block;
}
</style>
