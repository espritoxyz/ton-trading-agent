import {createRouter, createWebHistory} from 'vue-router'
import LandingApp from '../pages/LandingPage.vue'
import AppLayout from '../layouts/AppLayout.vue'
import ChatPanel from '../components/ChatPanel.vue'
import EmailVerificationPage from '../components/EmailVerificationPage.vue'
import PrivacyPage from '../pages/PrivacyPage.vue'
import TermsPage from '../pages/TermsPage.vue'
import RoadmapPage from '../pages/RoadmapPage.vue'
import BlogPage from '../pages/BlogPage.vue'
import AboutPage from '../pages/AboutPage.vue'
import CareersPage from '../pages/CareersPage.vue'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: LandingApp
  },
  {
    path: '/privacy',
    name: 'Privacy',
    component: PrivacyPage
  },
  {
    path: '/terms',
    name: 'Terms',
    component: TermsPage
  },
  {
    path: '/roadmap',
    name: 'Roadmap',
    component: RoadmapPage
  },
  {
    path: '/blog',
    name: 'Blog',
    component: BlogPage
  },
  {
    path: '/about',
    name: 'About',
    component: AboutPage
  },
  {
    path: '/careers',
    name: 'Careers',
    component: CareersPage
  },
  {
    path: '/verify-email/:token',
    name: 'EmailVerification',
    component: EmailVerificationPage,
    props: true
  },
  {
    path: '/app',
    name: 'App',
    component: AppLayout,
    children: [
      { path: '', name: 'Chat', component: ChatPanel }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
