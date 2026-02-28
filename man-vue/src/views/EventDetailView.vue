<template>
  <div class="event-detail">
    <a-button type="link" class="back" @click="router.push('/events')">← 返回列表</a-button>
    <a-spin :spinning="loading">
      <a-alert v-if="error" type="error" :message="error" show-icon class="mb-2" />
      <template v-else-if="event">
        <a-card title="事件详情">
          <a-descriptions title="基本信息" :column="1" bordered size="small" class="desc-block">
            <a-descriptions-item label="事件 ID (eventId)">{{ event.eventId || '-' }}</a-descriptions-item>
            <a-descriptions-item label="主题 (topic)">
              <a-tooltip v-if="topicNameZh(event.topic)" :title="topicNameZh(event.topic)">
                <span>{{ event.topic || '-' }}</span>
              </a-tooltip>
              <span v-else>{{ event.topic || '-' }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="状态 (status)">
              <a-tag :color="statusColor(event.status)" :title="event.status">{{ statusLabel(event.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态更新时间 (statusAt)">{{ formatTime(event.statusAt) }}</a-descriptions-item>
            <a-descriptions-item label="发生时间 (occurredAt)">{{ formatTime(event.occurredAt) }}</a-descriptions-item>
            <a-descriptions-item label="发送时间 (sentAt)">{{ formatTime(event.sentAt) }}</a-descriptions-item>
            <a-descriptions-item label="最近发送时间 (lastSentAt)">{{ formatTime(event.lastSentAt) }}</a-descriptions-item>
            <a-descriptions-item label="过期时间 (expireAt)">{{ formatTime(event.expireAt) }}</a-descriptions-item>
            <a-descriptions-item label="重试次数 (retryCount)">{{ event.retryCount ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间 (createdAt)">{{ formatTime(event.createdAt) }}</a-descriptions-item>
            <a-descriptions-item label="更新时间 (updatedAt)">{{ formatTime(event.updatedAt) }}</a-descriptions-item>
            <a-descriptions-item label="链路 ID (traceId)">{{ event.traceId || '-' }}</a-descriptions-item>
            <a-descriptions-item label="跨度 ID (spanId)">{{ event.spanId || '-' }}</a-descriptions-item>
            <a-descriptions-item label="父事件 ID (parentEventId)">{{ event.parentEventId || '-' }}</a-descriptions-item>
            <a-descriptions-item v-if="canRetry" label="操作 (action)">
              <a-button type="primary" size="small" :loading="retrying" @click="doRetry">重推</a-button>
              <span v-if="retryMessage" :class="retryOk ? 'text-success' : 'text-danger'">{{ retryMessage }}</span>
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions title="发起方 (initiator)" :column="1" bordered size="small" class="desc-block mt-2">
            <a-descriptions-item label="服务 (service)">{{ (event.initiator && event.initiator.service) || '-' }}</a-descriptions-item>
            <a-descriptions-item label="操作 (operation)">{{ (event.initiator && event.initiator.operation) || '-' }}</a-descriptions-item>
            <a-descriptions-item label="用户 ID (userId)">{{ (event.initiator && event.initiator.userId) || '-' }}</a-descriptions-item>
            <a-descriptions-item label="客户端请求 ID (clientRequestId)">{{ (event.initiator && event.initiator.clientRequestId) || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card title="载荷 (payload)" class="mt-2">
          <a-descriptions :column="1" bordered size="small" class="desc-block">
            <a-descriptions-item label="载荷类型 (payloadType)">{{ event.payloadType || '-' }}</a-descriptions-item>
          </a-descriptions>
          <div class="payload-wrap">
            <pre class="payload">{{ typeof event.payload === 'object' ? JSON.stringify(event.payload, null, 2) : (event.payload ?? '-') }}</pre>
          </div>
        </a-card>

        <a-card title="消费记录 (consumptions)" class="mt-2">
          <a-table
            :columns="consumptionColumns"
            :data-source="event.consumptions || []"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'success'">
                {{ record.success === null ? '待消费' : record.success ? '成功' : '失败' }}
              </template>
              <template v-else-if="column.key === 'consumedAt'">
                {{ formatTime(record.consumedAt) }}
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ formatTime(record.createdAt) }}
              </template>
            </template>
          </a-table>
          <a-empty v-if="!event.consumptions || event.consumptions.length === 0" description="暂无消费记录" />
        </a-card>
      </template>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const event = ref(null)
const retrying = ref(false)
const retryMessage = ref('')
const retryOk = ref(false)
const topicNameZhMap = ref({})

const STATUS_LABELS = {
  SENT: '已发送',
  CONSUMED: '已消费',
  FAILED: '失败',
  PARTIAL: '部分成功',
  RETRYING: '重试中',
  PENDING: '待发送',
  EXPIRED: '已过期',
}

const canRetry = computed(() => {
  const s = event.value && event.value.status
  return s === 'FAILED' || s === 'RETRYING' || s === 'SENT'
})

const consumptionColumns = [
  { title: '记录 ID (id)', dataIndex: 'id', key: 'id', ellipsis: true, width: 140 },
  { title: '消费者 ID (consumerId)', dataIndex: 'consumerId', key: 'consumerId', ellipsis: true },
  { title: '尝试序号 (attemptNo)', dataIndex: 'attemptNo', key: 'attemptNo', width: 110 },
  { title: '是否成功 (success)', key: 'success', width: 110 },
  { title: '消费时间 (consumedAt)', key: 'consumedAt', width: 200 },
  { title: '错误信息 (errorMessage)', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  { title: '错误码 (errorCode)', dataIndex: 'errorCode', key: 'errorCode', width: 140 },
  { title: '创建时间 (createdAt)', key: 'createdAt', width: 200 },
]

function topicNameZh(topic) {
  if (!topic) return ''
  return topicNameZhMap.value[topic] || ''
}

function statusLabel(s) {
  return STATUS_LABELS[s] || s || '-'
}

function statusColor(s) {
  const map = { FAILED: 'error', CONSUMED: 'success', SENT: 'processing', RETRYING: 'processing', PARTIAL: 'warning' }
  return map[s] || 'default'
}

function formatTime(iso) {
  if (!iso) return '-'
  try {
    return new Date(iso).toLocaleString('zh-CN')
  } catch {
    return iso
  }
}

async function load() {
  loading.value = true
  error.value = ''
  retryMessage.value = ''
  try {
    const res = await fetch(`/api/events/${route.params.eventId}`)
    if (!res.ok) {
      if (res.status === 404) error.value = '事件不存在'
      else throw new Error(res.statusText)
      return
    }
    event.value = await res.json()
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function doRetry() {
  if (!event.value || retrying.value) return
  retrying.value = true
  retryMessage.value = ''
  try {
    const res = await fetch(`/api/events/${event.value.eventId}/retry`, { method: 'POST' })
    const data = await res.json().catch(() => ({}))
    retryOk.value = res.ok && data.success
    retryMessage.value = data.message || (res.ok ? '重推成功' : '重推失败')
    if (retryOk.value) await load()
  } catch (e) {
    retryMessage.value = e.message || '请求失败'
    retryOk.value = false
  } finally {
    retrying.value = false
  }
}

async function loadTopicConfigs() {
  try {
    const res = await fetch('/api/topic-configs')
    if (!res.ok) return
    const list = await res.json()
    const map = {}
    if (Array.isArray(list)) list.forEach((item) => { if (item.topic && item.nameZh) map[item.topic] = item.nameZh })
    topicNameZhMap.value = map
  } catch {
    // ignore
  }
}

onMounted(async () => {
  await loadTopicConfigs()
  load()
})
</script>

<style scoped>
.event-detail { padding: 0; }
.back { padding-left: 0; margin-bottom: 8px; }
.mb-2 { margin-bottom: 16px; }
.mt-2 { margin-top: 16px; }
.desc-block { margin-bottom: 0; }
.payload-wrap { margin-top: 12px; }
.payload { background: #f5f5f5; padding: 12px; overflow: auto; font-size: 12px; margin: 0; border-radius: 4px; }
.text-success { color: #52c41a; margin-left: 8px; }
.text-danger { color: #ff4d4f; margin-left: 8px; }
</style>
