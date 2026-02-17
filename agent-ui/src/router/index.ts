import {createRouter, createWebHistory} from 'vue-router'
import LandingApp from '../../landing/src/App.vue'
import AppLayout from '../layouts/AppLayout.vue'
import Dashboard from '../pages/Dashboard.vue'
import EmailVerificationPage from '../components/EmailVerificationPage.vue'
import PrivacyPage from '../../landing/src/PrivacyPage.vue'
import TermsPage from '../../landing/src/TermsPage.vue'
import RoadmapPage from '../../landing/src/RoadmapPage.vue'
import BlogPage from '../../landing/src/BlogPage.vue'
import AboutPage from '../../landing/src/AboutPage.vue'
import CareersPage from '../../landing/src/CareersPage.vue'

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
    component: AppLayout,
    children: [
      { path: '', name: 'Dashboard', component: Dashboard }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
