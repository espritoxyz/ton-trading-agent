<script setup lang="ts">
import { ref, onMounted, watch, getCurrentInstance, computed } from 'vue'
import LandingApp from '../landing/src/App.vue'
import AppLayout from './layouts/AppLayout.vue'
import Dashboard from './pages/Dashboard.vue'
import RoadmapPage from '../landing/src/RoadmapPage.vue'
import PrivacyPage from '../landing/src/PrivacyPage.vue'
import BlogPage from '../landing/src/BlogPage.vue'
import TermsPage from '../landing/src/TermsPage.vue'
import EmailVerificationPage from './components/EmailVerificationPage.vue'

const route = ref(window.location.pathname || '/')
const instance = getCurrentInstance()
const metrika = instance?.appContext.config.globalProperties.$metrika

// Extract verification token from URL
const verificationToken = computed(() => {
  const match = route.value.match(/^\/verify-email\/(.+)$/)
  return match ? match[1] : null
})

// Track page views in Yandex Metrika
watch(route, (newRoute) => {
  if (metrika) {
    metrika.hit(newRoute)
  }
})

function navigate(to: string) {
  if (to === route.value) return
  history.pushState({}, '', to)
  route.value = to
}

onMounted(() => {
  // Track initial page view
  if (metrika) {
    metrika.hit(route.value)
  }

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
    <component v-if="route === '/'" :is="LandingApp" />
    <component v-else-if="route === '/roadmap'" :is="RoadmapPage" />
    <component v-else-if="route === '/privacy'" :is="PrivacyPage" />
    <component v-else-if="route === '/blog'" :is="BlogPage" />
    <component v-else-if="route === '/terms'" :is="TermsPage" />
    <component v-else-if="verificationToken" :is="EmailVerificationPage" :token="verificationToken" />
    <component v-else-if="route.startsWith('/app')" :is="AppLayout">
      <template v-slot>
        <Dashboard />
      </template>
    </component>
    <component v-else :is="LandingApp" />
  </div>
</template>
