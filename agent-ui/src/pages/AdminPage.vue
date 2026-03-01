<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Eye, Send, Loader2, AlertTriangle, CheckCircle2,
  Users, Sun, Moon, ExternalLink, Mail, ShieldCheck,
  LayoutDashboard, ChevronRight, ArrowRight, ChevronLeft
} from 'lucide-vue-next'
import { api } from '../composables/useApi'

// ── Routing ─────────────────────────────────────────────────────
type AdminView = 'overview' | 'newsletter'
const activeView = ref<AdminView>('overview')

const navItems: { id: AdminView; label: string; icon: any; group: string }[] = [
  { id: 'overview',   label: 'Overview',   icon: LayoutDashboard, group: 'General' },
  { id: 'newsletter', label: 'Newsletter', icon: Mail,            group: 'Audience' },
]

const navGroups = computed(() => {
  const groups = new Map<string, typeof navItems>()
  for (const item of navItems) {
    if (!groups.has(item.group)) groups.set(item.group, [])
    groups.get(item.group)!.push(item)
  }
  return [...groups.entries()]
})

const activeItem = computed(() => navItems.find(i => i.id === activeView.value))

// ── Newsletter state ─────────────────────────────────────────────
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
  <div class="root">

    <!-- ── Sidebar ──────────────────────────────────────────────── -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <ShieldCheck :size="15" />
        </div>
        <div class="sidebar-title-group">
          <span class="brand">Esprito AI</span>
          <span class="brand-sub">Admin Panel</span>
        </div>
      </div>

      <nav class="nav">
        <template v-for="[group, items] in navGroups" :key="group">
          <div class="nav-group-label">{{ group }}</div>
          <button
            v-for="item in items"
            :key="item.id"
            @click="activeView = item.id"
            :class="['nav-item', activeView === item.id && 'is-active']"
          >
            <component :is="item.icon" :size="15" class="shrink-0" />
            <span>{{ item.label }}</span>
          </button>
        </template>
      </nav>

      <div class="sidebar-footer">
        <a href="/app" class="back-btn">
          <ChevronLeft :size="15" class="shrink-0" />
          <span>Back to App</span>
        </a>
      </div>
    </aside>

    <!-- ── Main content ──────────────────────────────────────────── -->
    <div class="content">

      <!-- Topbar -->
      <header class="topbar">
        <div class="breadcrumb">
          <span class="bc-root">Admin</span>
          <ChevronRight :size="12" class="bc-sep" />
          <span class="bc-current">{{ activeItem?.label }}</span>
        </div>
      </header>

      <!-- Views -->
      <main class="main">

        <!-- ── Overview ── -->
        <div v-if="activeView === 'overview'" class="overview">
          <div class="overview-header">
            <h1 class="overview-title">Dashboard</h1>
            <p class="overview-sub">Manage Esprito AI platform settings and operations.</p>
          </div>

          <div class="module-grid">
            <button class="module-card" @click="activeView = 'newsletter'">
              <div class="module-icon-wrap">
                <Mail :size="19" />
              </div>
              <div class="module-info">
                <div class="module-name">Newsletter</div>
                <div class="module-desc">Compose and broadcast emails to active subscribers</div>
              </div>
              <ArrowRight :size="15" class="module-arrow" />
            </button>
          </div>
        </div>

        <!-- ── Newsletter ── -->
        <div v-else-if="activeView === 'newsletter'" class="nl-wrap">
          <div class="nl-grid">

            <!-- Compose -->
            <section class="panel">
              <div class="panel-head">
                <span class="panel-title">Compose</span>
              </div>
              <div class="panel-body">

                <div class="field">
                  <label class="field-label">Subject line</label>
                  <input
                    v-model="subject"
                    type="text"
                    placeholder="Esprito AI — what's new in March"
                    class="inp"
                  />
                </div>

                <div class="field flex-1 flex flex-col">
                  <label class="field-label">HTML content</label>
                  <textarea
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
                  <button @click="preview" :disabled="previewLoading || sendLoading" class="btn btn-ghost">
                    <Loader2 v-if="previewLoading" :size="14" class="animate-spin" />
                    <Eye v-else :size="14" />
                    {{ previewLoading ? 'Loading…' : 'Preview' }}
                  </button>
                  <button @click="send" :disabled="previewLoading || sendLoading" class="btn btn-primary">
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
            <section class="panel panel-preview">
              <div class="panel-head panel-head-between">
                <span class="panel-title">Email Preview</span>
                <div v-if="previewHtml" class="preview-controls">
                  <div class="theme-toggle">
                    <button
                      @click="previewDark = false"
                      :class="['toggle-btn', !previewDark && 'is-active']"
                      title="Light background"
                    ><Sun :size="13" /></button>
                    <button
                      @click="previewDark = true"
                      :class="['toggle-btn', previewDark && 'is-active']"
                      title="Dark background"
                    ><Moon :size="13" /></button>
                  </div>
                  <button @click="openPreviewInTab" class="open-btn" title="Open in new tab">
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
                class="preview-frame"
                :style="{ background: previewDark ? '#1a1a2e' : '#fff' }"
                sandbox="allow-same-origin"
              />
            </section>

          </div>
        </div>

      </main>
    </div>

  </div>
</template>

<style scoped>
/* ── Design tokens ──────────────────────────────────────────────── */
.root {
  --bg:          #0e1825;
  --surface:     #152236;
  --surface-2:   #1d2f47;
  --surface-3:   #263c5c;
  --border:      #2c4260;
  --border-hi:   #3d5c88;
  --text:        #dde9f8;
  --text-2:      #8aabcf;
  --text-3:      #5c809f;
  --text-dim:    #3d5a78;
  --accent:      #5a72e8;
  --accent-hi:   #4a60d8;
  --accent-glow: rgba(90, 114, 232, 0.14);
  --success:     #34d399;
  --success-bg:  rgba(52, 211, 153, 0.08);
  --success-bdr: rgba(52, 211, 153, 0.2);
  --error:       #f87171;
  --error-bg:    rgba(248, 113, 113, 0.08);
  --error-bdr:   rgba(248, 113, 113, 0.2);
  --radius:      8px;
  --radius-sm:   5px;
  --font: 'DM Sans', system-ui, -apple-system, sans-serif;
  --mono: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
}

/* ── Root layout ────────────────────────────────────────────────── */
.root {
  min-height: 100vh;
  display: flex;
  background: var(--bg);
  color: var(--text-2);
  font-family: var(--font);
  font-size: 13px;
  line-height: 1.5;
}

/* ── Sidebar ────────────────────────────────────────────────────── */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  background: var(--surface);
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.sidebar-logo {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: var(--accent-glow);
  border: 1px solid rgba(90, 114, 232, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  flex-shrink: 0;
}

.sidebar-title-group {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.brand {
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: -0.01em;
  line-height: 1.2;
}

.brand-sub {
  font-size: 10px;
  color: var(--text-3);
  letter-spacing: 0.01em;
}

.nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow-y: auto;
}

.nav-group-label {
  padding: 12px 8px 4px;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-3);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 7px 10px;
  border-radius: 6px;
  border: none;
  background: none;
  color: var(--text-2);
  font-size: 13px;
  font-family: var(--font);
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s, color 0.12s;
}

.nav-item:hover {
  background: var(--surface-2);
  color: var(--text);
}

.nav-item.is-active {
  background: var(--accent-glow);
  color: var(--text);
  border: 1px solid rgba(90, 114, 232, 0.2);
}

/* ── Sidebar footer / back button ───────────────────────────────── */
.sidebar-footer {
  padding: 14px 10px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  color: var(--text-2);
  font-size: 13px;
  font-family: var(--font);
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
  box-sizing: border-box;
}

.back-btn:hover {
  background: var(--surface-3);
  border-color: var(--border-hi);
  color: var(--text);
}

/* ── Right content area ─────────────────────────────────────────── */
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.topbar {
  height: 48px;
  display: flex;
  align-items: center;
  padding: 0 22px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  background: var(--bg);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
}

.bc-root    { color: var(--text-3); font-weight: 500; }
.bc-sep     { color: var(--text-dim); }
.bc-current { color: var(--text-2); font-weight: 600; }

.main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  scrollbar-width: thin;
  scrollbar-color: var(--border) transparent;
}

/* ── Overview ───────────────────────────────────────────────────── */
.overview {
  padding: 32px 28px;
  max-width: 720px;
}

.overview-header { margin-bottom: 28px; }

.overview-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.overview-sub {
  font-size: 13px;
  color: var(--text-3);
  line-height: 1.6;
}

.module-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.module-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  text-align: left;
  width: 100%;
  font-family: var(--font);
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
}

.module-card:hover {
  background: var(--surface-2);
  border-color: var(--border-hi);
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.25);
}

.module-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 9px;
  background: var(--accent-glow);
  border: 1px solid rgba(90, 114, 232, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  flex-shrink: 0;
}

.module-info   { flex: 1; min-width: 0; }
.module-name   { font-size: 14px; font-weight: 600; color: var(--text); margin-bottom: 3px; }
.module-desc   { font-size: 12px; color: var(--text-3); }
.module-arrow  { color: var(--text-3); }

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

.panel-preview {
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

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700;1,9..40,400&family=JetBrains+Mono:wght@400;500&display=swap');
</style>
