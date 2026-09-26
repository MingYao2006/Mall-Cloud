import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import './assets/styles/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import reveal from './directives/reveal'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.directive('reveal', reveal)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
