import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import AdminLayout from '@/layouts/AdminLayout.vue'
import LoginView from '@/views/LoginView.vue'
import WorkspaceView from '@/views/WorkspaceView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { title: '管理端登录', public: true },
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/workspace',
    children: [
      {
        path: 'workspace',
        name: 'workspace',
        component: WorkspaceView,
        meta: { title: '工作台', eyebrow: '运营总览' },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/OrderView.vue'),
        meta: { title: '订单管理', eyebrow: '履约中心', description: '集中处理接单、配送与订单状态。' },
      },
      {
        path: 'dishes',
        name: 'dishes',
        component: () => import('@/views/DishView.vue'),
        meta: { title: '菜品管理', eyebrow: '商品中心', description: '维护菜品、价格、规格与售卖状态。' },
      },
      {
        path: 'setmeals',
        name: 'setmeals',
        component: () => import('@/views/SetmealView.vue'),
        meta: { title: '套餐管理', eyebrow: '商品中心', description: '组合菜品并管理套餐的售卖计划。' },
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/CategoryView.vue'),
        meta: { title: '分类管理', eyebrow: '商品中心', description: '管理菜品与套餐的展示分类。' },
      },
      {
        path: 'employees',
        name: 'employees',
        component: () => import('@/views/EmployeeView.vue'),
        meta: { title: '员工管理', eyebrow: '组织管理', description: '维护员工账号与在岗状态。', requiresAdmin: true },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '我的', eyebrow: '账号中心', description: '查看当前登录员工的身份与账号状态。' },
      },
      {
        path: 'reports',
        name: 'reports',
        component: () => import('@/views/ReportView.vue'),
        meta: { title: '数据统计', eyebrow: '经营分析', description: '查看营业、订单和用户趋势。' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/workspace' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia)
  document.title = `${String(to.meta.title || '管理端')} · 驿达点餐`

  if (!to.meta.public && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !authStore.isAdmin) return { name: 'workspace' }
  if (to.name === 'login' && authStore.isAuthenticated) return { name: 'workspace' }
  return true
})

export default router
