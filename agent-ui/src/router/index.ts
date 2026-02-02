import { createRouter, createWebHistory } from 'vue-router'
import LandingApp from '../../landing/src/App.vue'
import AppLayout from '../layouts/AppLayout.vue'
import Dashboard from '../pages/Dashboard.vue'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: LandingApp
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
