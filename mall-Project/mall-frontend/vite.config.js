import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src')
        }
    },
    server: {
        port: 3000,
        proxy: {
            // C 端请求统一走网关
            '/api/c': {
                target: 'http://localhost:9000',
                changeOrigin: true
            },
            // 支付宝回调路径（网关专用路由）
            '/api/payment': {
                target: 'http://localhost:9000',
                changeOrigin: true
            },
            // 静态资源（上传文件访问）
            '/uploads': {
                target: 'http://localhost:9000',
                changeOrigin: true
            }
        }
    }
})
