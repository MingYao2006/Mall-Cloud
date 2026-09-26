<template>
  <div class="ai-guide">
    <div class="chat-card">
      <div class="chat-header">
        <el-icon :size="20" class="logo"><ChatDotRound /></el-icon>
        <div class="head-text">
          <div class="title">AI 智能导购</div>
          <div class="sub">DeepSeek + 工具调用 + RAG 语义检索</div>
        </div>
        <el-button class="clear-btn" text :disabled="sending" @click="clearChat">清空对话</el-button>
      </div>

      <div ref="listRef" class="chat-body">
        <div v-if="!messages.length" class="welcome">
          <p class="welcome-title">你好，我是你的专属导购助手</p>
          <p class="welcome-desc">我可以帮你找商品、查优惠券、加购物车、看订单状态，直接说出来就行。</p>
          <div class="chips">
            <el-tag
              v-for="q in quickPrompts"
              :key="q"
              class="chip"
              effect="plain"
              @click="send(q)"
            >{{ q }}</el-tag>
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="avatar">
            <el-icon v-if="m.role === 'user'"><User /></el-icon>
            <el-icon v-else><ChatDotRound /></el-icon>
          </div>
          <div class="bubble">
            <span class="text">{{ m.content }}</span>
            <span v-if="m.streaming" class="caret" />
          </div>
        </div>
      </div>

      <div class="chat-footer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          :disabled="sending"
          placeholder="例如：推荐一款 200 元以内的耳机（Enter 发送，Shift + Enter 换行）"
          @keydown.enter.exact.prevent="send()"
        />
        <el-button type="primary" class="send-btn" :loading="sending" @click="send()">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, User } from '@element-plus/icons-vue'
import { chatStream } from '@/api'

const router = useRouter()
const messages = ref([])
const input = ref('')
const sending = ref(false)
const listRef = ref(null)

const quickPrompts = [
  '推荐一款 200 元以内的耳机',
  '现在有什么可以领的优惠券',
  '帮我看看我的订单到哪了',
  '有什么适合送女朋友的礼物'
]

// 会话 ID：同一浏览器保持上下文，清空对话时重置
function newSessionId() {
  return 'web-' + Date.now() + '-' + Math.random().toString(16).slice(2, 10)
}
const sessionId = ref(newSessionId())

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function send(text) {
  const content = (text !== undefined ? text : input.value).trim()
  if (!content || sending.value) return

  input.value = ''
  messages.value.push({ role: 'user', content })
  messages.value.push({ role: 'assistant', content: '', streaming: true })
  const idx = messages.value.length - 1
  sending.value = true
  await scrollToBottom()

  try {
    await chatStream({
      message: content,
      sessionId: sessionId.value,
      onToken: (t) => {
        messages.value[idx].content += t
        scrollToBottom()
      },
      onError: (msg) => {
        messages.value[idx].content += (messages.value[idx].content ? '\n' : '') + '（' + msg + '）'
      }
    })
    if (!messages.value[idx].content) {
      messages.value[idx].content = '抱歉，我没有得到有效回复，请稍后再试。'
    }
  } catch (e) {
    messages.value.splice(idx, 1)
    if (e && e.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else if (e && e.status === 403) {
      ElMessage.error('无权限访问 AI 导购')
    } else {
      ElMessage.error('AI 导购服务暂时不可用，请确认已启动 mall-ai-agent（端口 8095）')
    }
  } finally {
    messages.value[idx] && (messages.value[idx].streaming = false)
    sending.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  sessionId.value = newSessionId()
}
</script>

<style scoped>
.ai-guide {
  max-width: 900px;
  margin: 24px auto;
  padding: 0 16px;
}
.chat-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  min-height: 460px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid #f0f0f0;
}
.chat-header .logo {
  color: #409eff;
}
.head-text .title {
  font-size: 16px;
  font-weight: 600;
}
.head-text .sub {
  font-size: 12px;
  color: #909399;
}
.clear-btn {
  margin-left: auto;
}
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  background: #fafafa;
}
.welcome {
  text-align: center;
  color: #606266;
  padding-top: 40px;
}
.welcome-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}
.welcome-desc {
  font-size: 13px;
  color: #909399;
  margin: 8px 0 18px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.chip {
  cursor: pointer;
}
.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.msg.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ecf5ff;
  color: #409eff;
  flex-shrink: 0;
}
.msg.user .avatar {
  background: #f4f4f5;
  color: #606266;
}
.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.user .bubble {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.caret {
  display: inline-block;
  width: 6px;
  height: 14px;
  margin-left: 2px;
  background: #409eff;
  vertical-align: -2px;
  animation: blink 1s steps(2, start) infinite;
}
@keyframes blink {
  to {
    visibility: hidden;
  }
}
.chat-footer {
  display: flex;
  gap: 10px;
  padding: 12px 18px;
  border-top: 1px solid #f0f0f0;
  align-items: flex-end;
}
.send-btn {
  height: 54px;
  padding: 0 22px;
}
</style>