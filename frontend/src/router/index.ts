import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
        { path: 'login', name: 'Login', component: () => import('@/views/Login.vue') },
        { path: 'register', name: 'Register', component: () => import('@/views/Register.vue') },
        { path: 'parse', name: 'VideoParse', component: () => import('@/views/VideoParse.vue') },
        { path: 'clip', name: 'AIClip', component: () => import('@/views/AIClip.vue'), meta: { requiresAuth: true } },
        { path: 'community', name: 'Community', component: () => import('@/views/Community.vue') },
        { path: 'upload', name: 'Upload', component: () => import('@/views/Upload.vue'), meta: { requiresAuth: true } },
        { path: 'tasks', name: 'TaskList', component: () => import('@/views/TaskList.vue'), meta: { requiresAuth: true } },
        { path: 'tasks/:id', name: 'TaskDetail', component: () => import('@/views/TaskDetail.vue'), meta: { requiresAuth: true } },
        { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
        { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue') },
        { path: 'tasks', name: 'AdminTasks', component: () => import('@/views/admin/TaskManage.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Home' })
    return
  }
  // 已登录用户访问登录/注册页时跳转首页
  if ((to.name === 'Login' || to.name === 'Register') && authStore.isLoggedIn) {
    next({ name: 'Home' })
    return
  }
  next()
})

export default router
