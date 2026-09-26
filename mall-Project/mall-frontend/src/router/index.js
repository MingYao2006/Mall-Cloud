import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/category',
    name: 'Category',
    component: () => import('@/views/Category.vue'),
    meta: { title: '全部分类' }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/ProductDetail.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/Cart.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/views/Orders.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人信息', requiresAuth: true }
  },
  {
    path: '/coupon',
    name: 'Coupon',
    component: () => import('@/views/Coupon.vue'),
    meta: { title: '优惠券秒杀', requiresAuth: true }
  },
  {
    path: '/seckill',
    name: 'Seckill',
    component: () => import('@/views/Seckill.vue'),
    meta: { title: '限时秒杀' }
  },
  {
    path: '/pay-result',
    name: 'PayResult',
    component: () => import('@/views/PayResult.vue'),
    meta: { title: '支付结果' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 切页回到顶部；带 hash 则滚到对应锚点（避开固定顶栏）
  scrollBehavior(to, from, savedPosition) {
    if (to.hash) return { el: to.hash, behavior: 'smooth', top: 88 }
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '商城'
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
