import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// uni-app Vue3 使用 createSSRApp 创建应用实例
export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  return { app }
}
