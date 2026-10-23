/**
 * 路由配置 + 全局守卫（对应 design-web.html 左侧菜单）。
 * 守卫规则：无 token 访问受保护页 → /login?redirect=...；已登录访问 /login → /dashboard。
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/token'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '数据看板', icon: 'i-grid' },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/orders/OrderListView.vue'),
        meta: { title: '工单列表', icon: 'i-clipboard' },
      },
      {
        path: 'orders/:id',
        name: 'order-detail',
        component: () => import('@/views/orders/OrderDetailView.vue'),
        meta: { title: '工单详情' },
      },
      {
        path: 'dispatch',
        name: 'dispatch',
        component: () => import('@/views/dispatch/DispatchView.vue'),
        meta: { title: '派单中心', icon: 'i-send' },
      },
      {
        path: 'repairers',
        name: 'repairers',
        component: () => import('@/views/repairer/RepairerView.vue'),
        meta: { title: '维修师傅', icon: 'i-users' },
      },
      {
        path: 'repairer-audit',
        name: 'repairer-audit',
        component: () => import('@/views/repairerAudit/RepairerAuditView.vue'),
        meta: { title: '师傅审核', icon: 'i-audit' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/user/UserView.vue'),
        meta: { title: '用户管理', icon: 'i-user' },
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/category/CategoryView.vue'),
        meta: { title: '故障分类', icon: 'i-tags' },
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('@/views/notice/NoticeView.vue'),
        meta: { title: '通知中心', icon: 'i-bell' },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/setting/SettingView.vue'),
        meta: { title: '系统设置', icon: 'i-sliders' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

/* ---------- 全局守卫 ---------- */
router.beforeEach((to) => {
  const hasToken = getToken() !== ''
  document.title = `${String(to.meta.title ?? '校园易修')} · 校园易修`

  if (!hasToken && to.path !== '/login') {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (hasToken && to.path === '/login') {
    return { path: '/dashboard' }
  }
  return true
})

export default router
