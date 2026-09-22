<script setup>
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'

onLaunch(() => {
  // 启动时从本地存储恢复登录态
  useUserStore().restore()
})

onShow(() => {
  // 预留：切前台时刷新用户信息
})
</script>

<style>
page {
  background-color: #f5f5f5;
  color: #333;
  font-size: 28rpx;
}
</style>
