<template>
  <view class="placeholder">
    <text class="title">{{ title }}</text>
    <text v-if="desc" class="desc">{{ desc }}</text>
    <view class="todo-list">
      <text v-for="(item, index) in todos" :key="index" class="todo-item">· {{ item }}</text>
    </view>
    <text class="hint">本页为工程骨架占位，待按上述清单实现</text>
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  desc: {
    type: String,
    default: ''
  },
  todos: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.placeholder {
  min-height: 100vh;
  padding: 64rpx 40rpx;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.title {
  font-size: 38rpx;
  font-weight: bold;
  color: #333333;
}

.desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #999999;
  line-height: 40rpx;
}

.todo-list {
  margin-top: 40rpx;
  display: flex;
  flex-direction: column;
}

.todo-item {
  font-size: 26rpx;
  color: #666666;
  line-height: 48rpx;
}

.hint {
  margin-top: 48rpx;
  font-size: 24rpx;
  color: #bbbbbb;
}
</style>
