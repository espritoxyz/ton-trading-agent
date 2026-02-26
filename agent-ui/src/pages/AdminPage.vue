<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Eye, Send, Loader2, AlertTriangle, CheckCircle2,
  Users, Sun, Moon, ExternalLink, Mail, ShieldCheck,
  LayoutDashboard, ChevronRight, ArrowRight
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
    const { data } = await api.post('/newsletter/admin/send', {
      subject: subject.value,
      htmlContent: htmlContent.value,
    })
    sendResult.value = data
  } catch (e: any) {
    nlError.value = e?.response?.data?.message ?? e?.message ?? 'Send failed'
  } finally {
    sendLoading.value = false
  }
}
</script>

<template>
  <div class="root">

    <!-- ── Sidebar ─────────────────────────────────────────────── -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <ShieldCheck :size="15" class="text-indigo-400 shrink-0" />
        <span class="brand">Admin</span>
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
            <component :is="item.icon" :size="14" class="shrink-0" />
            <span>{{ item.label }}</span>
          </button>
        </template>
      </nav>

      <div class="sidebar-footer">
        <a href="/app" class="back-link">← Back to app</a>
      </div>
    </aside>

    <!-- ── Main content ──────────────────────────────────────────── -->
    <div class="content">

      <!-- Topbar -->
      <header class="topbar">
        <div class="breadcrumb">
          <span>Admin</span>
          <ChevronRight :size="11" />
          <span class="bc-current">{{ activeItem?.label }}</span>
        </div>
      </header>

      <!-- Views -->
      <main class="main">

        <!-- ── Overview ── -->
        <div v-if="activeView === 'overview'" class="overview">
          <div class="overview-header">
            <h1 class="overview-title">Admin Dashboard</h1>
            <p class="overview-sub">Manage Esprito AI platform settings and operations.</p>
          </div>

          <div class="module-grid">
            <button class="module-card" @click="activeView = 'newsletter'">
              <div class="module-icon-wrap">
                <Mail :size="18" class="text-indigo-400" />
              </div>
              <div class="module-info">
                <div class="module-name">Newsletter</div>
                <div class="module-desc">Compose and broadcast emails to active subscribers</div>
              </div>
              <ArrowRight :size="14" class="text-slate-600 shrink-0" />
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
                  <AlertTriangle :size="13" class="shrink-0 mt-px" />
                  <span>{{ nlError }}</span>
                </div>

                <div class="flex gap-2">
                  <button @click="preview" :disabled="previewLoading || sendLoading" class="btn btn-ghost flex-1">
                    <Loader2 v-if="previewLoading" :size="13" class="animate-spin" />
                    <Eye v-else :size="13" />
                    {{ previewLoading ? 'Loading…' : 'Preview' }}
                  </button>
                  <button @click="send" :disabled="previewLoading || sendLoading" class="btn btn-primary flex-1">
                    <Loader2 v-if="sendLoading" :size="13" class="animate-spin" />
                    <Send v-else :size="13" />
                    {{ sendLoading ? 'Sending…' : 'Send to all' }}
                  </button>
                </div>

                <div v-if="sendResult" class="result-box">
                  <div class="flex items-center gap-2 mb-3">
                    <CheckCircle2 :size="14" class="text-emerald-400" />
                    <span class="text-sm font-medium text-emerald-300">Broadcast complete</span>
                  </div>
                  <div class="stats-grid">
                    <div class="stat">
                      <div class="stat-val text-slate-200">{{ sendResult.totalSubscribers }}</div>
                      <div class="stat-lbl"><Users :size="9" class="inline mr-0.5" />Total</div>
                    </div>
                    <div class="stat">
                      <div class="stat-val text-emerald-400">{{ sendResult.sent }}</div>
                      <div class="stat-lbl">Sent</div>
                    </div>
                    <div class="stat">
                      <div class="stat-val" :class="sendResult.failed > 0 ? 'text-red-400' : 'text-slate-600'">
                        {{ sendResult.failed }}
                      </div>
                      <div class="stat-lbl">Failed</div>
                    </div>
                  </div>
                </div>

              </div>
            </section>

            <!-- Preview -->
            <section class="panel overflow-hidden flex flex-col">
              <div class="panel-head justify-between">
                <span class="panel-title">Email Preview</span>
                <div v-if="previewHtml" class="flex items-center gap-1.5">
                  <div class="theme-toggle">
                    <button
                      @click="previewDark = false"
                      :class="['toggle-btn', !previewDark && 'is-active']"
                      title="Light background"
                    ><Sun :size="12" /></button>
                    <button
                      @click="previewDark = true"
                      :class="['toggle-btn', previewDark && 'is-active']"
                      title="Dark background"
                    ><Moon :size="12" /></button>
                  </div>
                  <button @click="openPreviewInTab" class="open-btn" title="Open in new tab">
                    <ExternalLink :size="12" />
                  </button>
                </div>
              </div>

              <div v-if="!previewHtml" class="preview-empty">
                <Eye :size="32" stroke-width="1.5" />
                <p>Click <strong>Preview</strong> to render the email</p>
              </div>

              <!-- :key forces full iframe recreation on theme switch so srcdoc reloads -->
              <iframe
                v-else
                :key="`preview-${previewDark}`"
                :srcdoc="previewSrcdoc"
                class="flex-1 w-full border-none"
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
/* ── Root ──────────────────────────────────────────────────────── */
.root {
  min-height: 100vh;
  display: flex;
  background: #080d18;
  color: #94a3b8;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-size: 13px;
}

/* ── Sidebar ───────────────────────────────────────────────────── */
.sidebar {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid #131c2e;
  display: flex;
  flex-direction: column;
  background: #0a0f1e;
}

.sidebar-header {
  height: 50px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border-bottom: 1px solid #131c2e;
  flex-shrink: 0;
}

.brand {
  font-size: 14px;
  font-weight: 700;
  color: #e2e8f0;
  letter-spacing: -0.01em;
}

.nav {
  flex: 1;
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow-y: auto;
}

.nav-group-label {
  padding: 10px 7px 3px;
  font-size: 10px;
  font-weight: 700;
  color: #253044;
  text-transform: uppercase;
  letter-spacing: 0.09em;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border-radius: 5px;
  border: none;
  background: none;
  color: #4b6080;
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s, color 0.12s;
}
.nav-item:hover  { background: #111c30; color: #7a94b0; }
.nav-item.is-active { background: #141f35; color: #e2e8f0; }

.sidebar-footer {
  padding: 12px 14px;
  border-top: 1px solid #131c2e;
}
.back-link {
  font-size: 11.5px;
  color: #2d3f57;
  text-decoration: none;
  transition: color 0.15s;
}
.back-link:hover { color: #4b6080; }

/* ── Right content ─────────────────────────────────────────────── */
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.topbar {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #131c2e;
  flex-shrink: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #2d3f57;
}
.bc-current { color: #64748b; }

.main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  scrollbar-width: thin;
  scrollbar-color: #131c2e transparent;
}

/* ── Overview ──────────────────────────────────────────────────── */
.overview {
  padding: 28px 24px;
  max-width: 680px;
}
.overview-header { margin-bottom: 24px; }
.overview-title { font-size: 17px; font-weight: 700; color: #e2e8f0; margin-bottom: 4px; }
.overview-sub   { font-size: 12.5px; color: #37506b; }

.module-grid { display: flex; flex-direction: column; gap: 8px; }

.module-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 15px;
  background: #0a0f1e;
  border: 1px solid #131c2e;
  border-radius: 7px;
  cursor: pointer;
  text-align: left;
  width: 100%;
  transition: background 0.15s, border-color 0.15s;
}
.module-card:hover { background: #0f172a; border-color: #1e2d45; }

.module-icon-wrap {
  width: 36px; height: 36px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.18);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.module-info  { flex: 1; min-width: 0; }
.module-name  { font-size: 13px; font-weight: 600; color: #c4d3e6; }
.module-desc  { font-size: 11.5px; color: #37506b; margin-top: 2px; }

/* ── Newsletter ────────────────────────────────────────────────── */
.nl-wrap {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.nl-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 12px;
  min-height: calc(100vh - 44px - 32px);
}

/* ── Panel ─────────────────────────────────────────────────────── */
.panel {
  background: #0a0f1e;
  border: 1px solid #131c2e;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  padding: 8px 13px;
  border-bottom: 1px solid #131c2e;
  flex-shrink: 0;
}

.panel-title {
  font-size: 10px;
  font-weight: 700;
  color: #2d3f57;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.panel-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #131c2e transparent;
}

/* ── Form ──────────────────────────────────────────────────────── */
.field { display: flex; flex-direction: column; gap: 5px; }

.field-label {
  font-size: 10px;
  font-weight: 700;
  color: #2d3f57;
  text-transform: uppercase;
  letter-spacing: 0.07em;
}

.inp {
  background: #060a14;
  border: 1px solid #131c2e;
  border-radius: 5px;
  padding: 7px 10px;
  font-size: 13px;
  color: #e2e8f0;
  outline: none;
  transition: border-color 0.15s;
  width: 100%;
}
.inp:focus { border-color: #4f46e5; }
.inp::placeholder { color: #1e2d45; }

.ta {
  background: #060a14;
  border: 1px solid #131c2e;
  border-radius: 5px;
  padding: 9px 10px;
  font-size: 11.5px;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  color: #c4d3e6;
  resize: none;
  outline: none;
  transition: border-color 0.15s;
  min-height: 200px;
  line-height: 1.65;
}
.ta:focus { border-color: #4f46e5; }
.ta::placeholder { color: #1a2540; }

/* ── Error ─────────────────────────────────────────────────────── */
.err-box {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 9px 11px;
  border-radius: 5px;
  background: rgba(239, 68, 68, 0.07);
  border: 1px solid rgba(239, 68, 68, 0.15);
  color: #fca5a5;
  font-size: 12px;
}

/* ── Buttons ───────────────────────────────────────────────────── */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  border: none;
  white-space: nowrap;
}
.btn:disabled { opacity: 0.3; cursor: not-allowed; }

.btn-ghost {
  background: transparent;
  border: 1px solid #131c2e;
  color: #37506b;
}
.btn-ghost:not(:disabled):hover { background: #0f172a; color: #64748b; border-color: #1e2d45; }

.btn-primary { background: #4f46e5; color: #fff; }
.btn-primary:not(:disabled):hover { background: #4338ca; }

/* ── Send result ───────────────────────────────────────────────── */
.result-box {
  padding: 12px;
  border-radius: 5px;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.15);
}
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7px; text-align: center; }
.stat {
  padding: 8px 6px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  border: 1px solid #131c2e;
}
.stat-val { font-size: 16px; font-weight: 700; line-height: 1.2; }
.stat-lbl { font-size: 10px; color: #2d3f57; margin-top: 2px; }

/* ── Preview ───────────────────────────────────────────────────── */
.preview-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #131c2e;
  font-size: 12px;
}
.preview-empty strong { color: #1e2d45; }

.theme-toggle {
  display: flex;
  background: #060a14;
  border: 1px solid #131c2e;
  border-radius: 4px;
  overflow: hidden;
}
.toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px 7px;
  background: none;
  border: none;
  color: #1e2d45;
  cursor: pointer;
  transition: all 0.15s;
}
.toggle-btn:hover { color: #37506b; }
.toggle-btn.is-active { background: #131c2e; color: #64748b; }

.open-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px 7px;
  border-radius: 4px;
  background: none;
  border: 1px solid #131c2e;
  color: #1e2d45;
  cursor: pointer;
  transition: all 0.15s;
}
.open-btn:hover { color: #37506b; border-color: #1e2d45; }
</style>
