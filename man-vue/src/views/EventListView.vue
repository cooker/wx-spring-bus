<template>
  <div class="event-list">
    <a-card title="事件列表">
      <a-form layout="inline" class="filters" :model="filters">
        <a-form-item label="状态">
          <a-select
            v-model:value="filters.status"
            placeholder="全部"
            allow-clear
            style="width: 140px"
            @change="() => load(0)"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option v-for="(label, code) in STATUS_OPTIONS" :key="code" :value="code">
              {{ label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Topic">
          <a-input
            v-model:value="filters.topic"
            placeholder="如 order.purchased"
            allow-clear
            style="width: 160px"
            @press-enter="() => load(0)"
          />
        </a-form-item>
        <a-form-item label="userId">
          <a-input
            v-model:value="filters.userId"
            placeholder="发起方用户 ID"
            allow-clear
            style="width: 140px"
            @press-enter="() => load(0)"
          />
        </a-form-item>
        <a-form-item label="发生时间">
          <a-range-picker
            v-model:value="occurredAtRange"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss.SSSZ"
            :placeholder="['开始', '结束']"
            allow-clear
            style="width: 360px"
            @change="onOccurredAtChange"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="loading" @click="() => load(0)">查询</a-button>
        </a-form-item>
      </a-form>
      <a-alert v-if="error" type="error" :message="error" show-icon class="mb-2" />
      <a-table
        :columns="columns"
        :data-source="page.content"
        :loading="loading"
        :pagination="false"
        row-key="eventId"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'eventId'">
            <router-link :to="`/events/${record.eventId}`">{{ record.eventId }}</router-link>
          </template>
          <template v-else-if="column.key === 'parentEventId'">
            <router-link v-if="record.parentEventId" :to="`/events/${record.parentEventId}`">{{ record.parentEventId }}</router-link>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'topic'">
            <a-tooltip v-if="topicNameZh(record.topic)" :title="topicNameZh(record.topic)">
              <span>{{ record.topic }}</span>
            </a-tooltip>
            <span v-else>{{ record.topic }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)" :title="record.status">
              {{ statusLabel(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'occurredAt'">
            {{ formatTime(record.occurredAt) }}
          </template>
          <template v-else-if="column.key === 'initiatorUserId'">
            {{ (record.initiator && record.initiator.userId) || '-' }}
          </template>
          <template v-else-if="column.key === 'initiator'">
            {{ (record.initiator && record.initiator.service) || '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <router-link :to="`/events/${record.eventId}/view`">视图</router-link>
            <a-divider type="vertical" />
            <router-link :to="`/events/${record.eventId}`">详情</router-link>
          </template>
        </template>
      </a-table>
      <div class="pagination-wrap">
        <a-pagination
          v-model:current="currentPage"
          :total="page.totalElements"
          :page-size="20"
          show-size-changer="false"
          :show-total="(total) => `共 ${total} 条`"
          @change="onPageChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'

const STATUS_OPTIONS = {
  SENT: '已发送',
  CONSUMED: '已消费',
  FAILED: '失败',
  PARTIAL: '部分成功',
  RETRYING: '重试中',
  PENDING: '待发送',
  EXPIRED: '已过期',
}

const loading = ref(false)
const error = ref('')
const occurredAtRange = ref(null)
const topicNameZhMap = ref({})
const page = reactive({ content: [], number: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const filters = reactive({
  status: '',
  topic: '',
  userId: '',
  occurredAtFrom: '',
  occurredAtTo: '',
})

const currentPage = computed({
  get: () => page.number + 1,
  set: (v) => { page.number = (v || 1) - 1 },
})

const columns = [
  { title: 'eventId', dataIndex: 'eventId', key: 'eventId', ellipsis: true },
  { title: 'parentEventId', key: 'parentEventId', ellipsis: true, width: 140 },
  { title: 'topic', dataIndex: 'topic', key: 'topic', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: 'occurredAt', key: 'occurredAt', width: 180 },
  { title: 'retryCount', dataIndex: 'retryCount', key: 'retryCount', width: 90 },
  { title: 'userId', key: 'initiatorUserId', width: 140 },
  { title: 'initiator', key: 'initiator', width: 120 },
  { title: '', key: 'action', width: 80 },
]

function topicNameZh(topic) {
  if (!topic) return ''
  return topicNameZhMap.value[topic] || ''
}

function statusLabel(s) {
  return STATUS_OPTIONS[s] || s || '-'
}

function statusColor(s) {
  const map = { FAILED: 'error', CONSUMED: 'success', SENT: 'processing', RETRYING: 'processing', PARTIAL: 'warning' }
  return map[s] || 'default'
}

function toISOString(v) {
  if (!v) return ''
  if (typeof v === 'string') return v
  if (v.toDate && typeof v.toDate === 'function') return v.toDate().toISOString()
  if (v instanceof Date) return v.toISOString()
  return String(v)
}

function onOccurredAtChange(dates) {
  if (dates && dates.length === 2 && dates[0] && dates[1]) {
    filters.occurredAtFrom = toISOString(dates[0])
    filters.occurredAtTo = toISOString(dates[1])
    load(0)
  } else {
    filters.occurredAtFrom = ''
    filters.occurredAtTo = ''
  }
}

function formatTime(iso) {
  if (!iso) return '-'
  try {
    return new Date(iso).toLocaleString('zh-CN')
  } catch {
    return iso
  }
}

async function load(pageNum = 0) {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ page: pageNum, size: 20 })
    if (filters.status) params.set('status', filters.status)
    if (filters.topic) params.set('topic', filters.topic)
    if (filters.userId) params.set('userId', filters.userId)
    if (filters.occurredAtFrom) params.set('occurredAtFrom', filters.occurredAtFrom)
    if (filters.occurredAtTo) params.set('occurredAtTo', filters.occurredAtTo)
    const res = await fetch(`/api/events?${params}`)
    if (!res.ok) throw new Error(res.statusText)
    const data = await res.json()
    page.content = data.content || []
    page.number = data.number ?? 0
    page.totalPages = data.totalPages ?? 0
    page.totalElements = data.totalElements ?? 0
    page.first = data.first ?? true
    page.last = data.last ?? true
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function onPageChange(pageNum) {
  load(pageNum - 1)
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
  load(0)
})
</script>

<style scoped>
.event-list { padding: 0; }
.filters { margin-bottom: 16px; }
.filters :deep(.ant-form-item) { margin-right: 16px; margin-bottom: 8px; }
.mb-2 { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; text-align: right; }
</style>
