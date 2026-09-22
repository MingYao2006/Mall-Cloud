import { createRouter, createWebHistory } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '管理员登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '控制台', icon: 'Odometer' }
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/Products.vue'),
        meta: { title: '商品管理', icon: 'Goods' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/Categories.vue'),
        meta: { title: '分类管理', icon: 'Menu' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue'),
        meta: { title: '订单管理', icon: 'List' }
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('@/views/Comments.vue'),
        meta: { title: '评论管理', icon: 'ChatDotSquare' }
      },
      {
        path: 'coupons',
        name: 'Coupons',
        component: () => import('@/views/Coupons.vue'),
        meta: { title: '优惠券管理', icon: 'Ticket' }
      },
      {
        path: 'search',
        name: 'SearchManage',
        component: () => import('@/views/SearchManage.vue'),
        meta: { title: '搜索管理', icon: 'Search' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理', icon: 'User' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 商城管理后台` : '商城管理后台'
  const store = useAdminStore()
  if (to.meta.public) {
    if (store.token && to.path === '/login') {
      next('/')
    } else {
      next()
    }
  } else {
    if (!store.token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router
