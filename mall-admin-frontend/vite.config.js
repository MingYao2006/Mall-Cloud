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
        port: 3001,
        proxy: {
            // B 端请求统一走网关
            '/api/admin': {
                target: 'http://localhost:9000',
                changeOrigin: true
            },
            // C 端登录接口（B 端复用 C 端登录，需代理到网关）
            '/api/c': {
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
