<script setup lang="ts">
import { ref, onMounted } from 'vue'
import LandingApp from '../landing/src/App.vue'
import AppLayout from './layouts/AppLayout.vue'
import Dashboard from './pages/Dashboard.vue'

const route = ref(window.location.pathname || '/')

function navigate(to: string) {
  if (to === route.value) return
  history.pushState({}, '', to)
  route.value = to
}

onMounted(() => {
  window.addEventListener('popstate', () => {
    route.value = window.location.pathname
  })

  // Intercept clicks on internal <a> links to enable SPA navigation
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (!target) return
    const anchor = target.closest('a') as HTMLAnchorElement | null
    if (!anchor) return
    const href = anchor.getAttribute('href')
    if (!href) return
    // only intercept internal links
    if (href.startsWith('/') && !href.startsWith('//') && anchor.target !== '_blank') {
      e.preventDefault()
      navigate(href)
    }
  })
})
</script>

<template>
  <div>
    <component v-if="route === '/' || route.startsWith('/') && !route.startsWith('/app')" :is="LandingApp" />
    <component v-else :is="AppLayout">
      <template v-slot>
        <Dashboard />
      </template>
    </component>
  </div>
</template>
