<script setup lang="ts">
import { ref, computed, type Component } from 'vue'
import {
  Mail, ShieldCheck, LayoutDashboard,
  ChevronRight, ArrowRight, ChevronLeft,
} from 'lucide-vue-next'
import NewsletterPanel from '../components/admin/NewsletterPanel.vue'

// ── Routing ─────────────────────────────────────────────────────
type AdminView = 'overview' | 'newsletter'
const activeView = ref<AdminView>('overview')

const navItems: { id: AdminView; label: string; icon: Component; group: string }[] = [
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
            type="button"
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
            <button type="button" class="module-card" @click="activeView = 'newsletter'">
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
        <NewsletterPanel v-else-if="activeView === 'newsletter'" />

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
</style>

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700;1,9..40,400&family=JetBrains+Mono:wght@400;500&display=swap');
</style>
