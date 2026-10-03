import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/cs',
    name: 'Cs',
    component: () => import('@/views/Cs.vue'),
    meta: { title: 'CS', requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      // 工作台
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Odometer', roles: ['ADMIN', 'CUSTOMER', 'AIR_CONTROL', 'OPERATOR', 'PILOT', 'SUPERVISOR', 'MAINTENANCE'] }
      },

      // 账号与权限管理
      {
        path: 'account',
        name: 'Account',
        meta: { title: '账号管理', icon: 'User', roles: ['ADMIN'] },
        children: [
          {
            path: 'users',
            name: 'UserManagement',
            component: () => import('@/views/Account/UserManagement.vue'),
            meta: { title: '用户管理', roles: ['ADMIN'] }
          },
          {
            path: 'roles',
            name: 'RoleManagement',
            component: () => import('@/views/Account/RoleManagement.vue'),
            meta: { title: '角色权限', roles: ['ADMIN'] }
          }
        ]
      },

      // 订单管理
      {
        path: 'order',
        name: 'Order',
        meta: { title: '订单管理', icon: 'Document', roles: ['ADMIN', 'CUSTOMER', 'OPERATOR'] },
        children: [
          {
            path: 'list',
            name: 'OrderList',
            component: () => import('@/views/Order/OrderList.vue'),
            meta: { title: '订单列表', roles: ['ADMIN', 'CUSTOMER', 'OPERATOR'] }
          },
          {
            path: 'create',
            name: 'OrderCreate',
            component: () => import('@/views/Order/OrderCreate.vue'),
            meta: { title: '创建订单', roles: ['CUSTOMER'] }
          },
          {
            path: 'dispatch',
            name: 'OrderDispatch',
            component: () => import('@/views/Order/OrderDispatch.vue'),
            meta: { title: '订单调度', roles: ['ADMIN', 'OPERATOR'] }
          },
          {
            path: 'tracking/:id',
            name: 'OrderTracking',
            component: () => import('@/views/Order/OrderTracking.vue'),
            meta: { title: '订单跟踪', roles: ['ADMIN', 'CUSTOMER', 'OPERATOR'] }
          }
        ]
      },

      // 资源管理
      {
        path: 'resource',
        name: 'Resource',
        meta: { title: '资源管理', icon: 'Box', roles: ['ADMIN', 'OPERATOR'] },
        children: [
          {
            path: 'aircraft',
            name: 'AircraftManagement',
            component: () => import('@/views/Aircraft/List.vue'),
            meta: { title: '飞行器管理', roles: ['ADMIN', 'OPERATOR'] }
          },
          {
            path: 'station',
            name: 'StationManagement',
            component: () => import('@/views/Resource/StationManagement.vue'),
            meta: { title: '场站管理', roles: ['ADMIN'] }
          },
          {
            path: 'personnel',
            name: 'PersonnelManagement',
            component: () => import('@/views/Resource/PersonnelManagement.vue'),
            meta: { title: '人员管理', roles: ['ADMIN'] }
          }
        ]
      },

      // 空域合规
      {
        path: 'airspace',
        name: 'Airspace',
        meta: { title: '空域合规', icon: 'MapLocation', roles: ['ADMIN', 'AIR_CONTROL'] },
        children: [
          {
            path: 'map',
            name: 'AirspaceMap',
            component: () => import('@/views/Airspace/AirspaceMap.vue'),
            meta: { title: '空域地图', roles: ['ADMIN', 'AIR_CONTROL'] }
          },
          {
            path: 'list',
            name: 'AirspaceList',
            component: () => import('@/views/Airspace/List.vue'),
            meta: { title: '空域管理', roles: ['ADMIN', 'AIR_CONTROL'] }
          },
          {
            path: 'flight-plan/list',
            name: 'FlightPlanList',
            component: () => import('@/views/FlightPlan/List.vue'),
            meta: { title: '飞行计划', roles: ['ADMIN', 'AIR_CONTROL', 'PILOT', 'SUPERVISOR'] }
          },
          {
            path: 'flight-plan/create',
            name: 'FlightPlanCreate',
            component: () => import('@/views/FlightPlan/Create.vue'),
            meta: { title: '申报计划', roles: ['ADMIN', 'PILOT'] }
          },
          {
            path: 'flight-plan/approval',
            name: 'FlightPlanApproval',
            component: () => import('@/views/FlightPlan/List.vue'),
            meta: { title: '计划审批', roles: ['ADMIN', 'AIR_CONTROL', 'SUPERVISOR'] }
          },
          {
            path: 'violation',
            name: 'ViolationManagement',
            component: () => import('@/views/Airspace/ViolationManagement.vue'),
            meta: { title: '违规处理', roles: ['ADMIN', 'AIR_CONTROL'] }
          }
        ]
      },

      // 应急客服
      {
        path: 'emergency',
        name: 'Emergency',
        meta: { title: '应急客服', icon: 'Bell', roles: ['ADMIN', 'CUSTOMER', 'AIR_CONTROL'] },
        children: [
          {
            path: 'alert',
            name: 'EmergencyAlert',
            component: () => import('@/views/Emergency/EmergencyAlert.vue'),
            meta: { title: '应急告警', roles: ['ADMIN', 'AIR_CONTROL', 'OPERATOR'] }
          },
          {
            path: 'workorder',
            name: 'WorkorderManagement',
            component: () => import('@/views/Emergency/WorkorderManagement.vue'),
            meta: { title: '客服工单', roles: ['ADMIN', 'CUSTOMER'] }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 不需要认证的页面
  if (!to.meta.requiresAuth) {
    next()
    return
  }

  // 检查是否登录（注意：isLoggedIn 是函数，必须调用，否则此分支恒不成立）
  if (!userStore.isLoggedIn()) {
    next('/login')
    return
  }

  // 检查角色权限
  if (to.meta.roles && to.meta.roles.length > 0) {
    const userRole = userStore.userInfo?.role
    if (!to.meta.roles.includes(userRole)) {
      console.warn('无权限访问:', to.path, '需要角色:', to.meta.roles, '当前角色:', userRole)
      // 有角色但无权访问当前页 → 回工作台；
      // 若本页就是工作台、或本地没有角色信息，则退出登录回登录页，避免与 /dashboard 互相重定向形成死循环（白屏）
      if (userRole && to.path !== '/dashboard') {
        next('/dashboard')
      } else {
        userStore.logout()
        next('/login')
      }
      return
    }
  }

  next()
})

export default router
