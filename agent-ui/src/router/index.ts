import {createRouter, createWebHistory} from 'vue-router'
import LandingApp from '../../landing/src/App.vue'
import AppLayout from '../layouts/AppLayout.vue'
import Dashboard from '../pages/Dashboard.vue'
import EmailVerificationPage from '../components/EmailVerificationPage.vue'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: LandingApp
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
