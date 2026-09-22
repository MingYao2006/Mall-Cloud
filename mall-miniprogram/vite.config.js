import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app 已内置 @ 指向 src 的别名，无需重复配置
export default defineConfig({
  plugins: [uni()],
  server: {
    port: 3100,
    proxy: {
      // H5 端开发环境经此代理访问网关，避免跨域
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true
      },
      // 上传文件与商品图片等静态资源
      '/uploads': {
        target: 'http://localhost:9000',
        changeOrigin: true
      }
    }
  }
})
