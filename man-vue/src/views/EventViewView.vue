<template>
  <div class="event-view">
    <a-button type="link" class="back" @click="router.push('/events')">← 返回列表</a-button>
    <a-spin :spinning="loading">
      <a-alert v-if="error" type="error" :message="error" show-icon class="mb-2" />
      <template v-else-if="event">
        <a-card title="事件调用链路" class="mb-2">
          <a-tree
            :tree-data="callChainTreeData"
            :field-names="{ title: 'title', key: 'key', children: 'children' }"
            default-expand-all
            block-node
            selectable
            @select="onCallChainSelect"
          >
            <template #title="{ title, dataRef }">
              <template v-if="dataRef">
                <span v-if="dataRef.link" class="tree-node-link" @click.stop="go(dataRef.link)">{{ title }}</span>
                <span v-else :class="{ 'current-event': dataRef.isCurrent }">{{ title }}</span>
                <span v-if="dataRef.desc" class="muted"> {{ dataRef.desc }}</span>
                <span v-if="dataRef.topic" class="muted"> {{ dataRef.topic }}</span>
                <span v-if="dataRef.time" class="muted"> {{ dataRef.time }}</span>
              </template>
              <span v-else>{{ title }}</span>
            </template>
          </a-tree>
        </a-card>

        <a-card class="mb-2 consumption-card">
          <template #title>消费链路</template>
          <template #extra>
            <span v-if="event.topic" class="topic-zh-extra">
              Topic：<span class="topic-zh-text">{{ topicDisplay }}</span>
            </span>
          </template>
          <a-timeline v-if="consumptionByAttempt.length" mode="left" class="consumption-timeline">
            <a-timeline-item
              v-for="group in consumptionByAttempt"
              :key="group.attemptNo"
            >
              <template #dot>
                <span class="timeline-dot">{{ group.attemptNo }}</span>
              </template>
              <div class="timeline-group">
                <div class="timeline-group-head">
                  <span class="timeline-label">{{ group.attemptNo === 0 ? '第 1 次尝试' : `第 ${group.attemptNo + 1} 次重试` }}</span>
                  <span v-if="group.timeText" class="muted timeline-time">{{ group.timeText }}</span>
                </div>
                <div class="consumption-list">
                  <div
                    v-for="c in group.items"
                    :key="c.key"
                    class="consumption-item"
                  >
                    <span class="consumer-id">{{ c.consumerId }}</span>
                    <a-tag v-if="c.statusTag === 'success'" color="success" size="small">成功</a-tag>
                    <a-tag v-else-if="c.statusTag === 'fail'" color="error" size="small">失败</a-tag>
                    <a-tag v-else-if="c.statusTag === 'pending'" color="default" size="small">待消费</a-tag>
                    <span v-if="c.createdDesc" class="muted"> {{ c.createdDesc }}</span>
                    <span v-if="c.desc" class="muted"> {{ c.desc }}</span>
                    <div v-if="c.errorMsg" class="error-msg">{{ c.errorMsg }}</div>
                  </div>
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else description="暂无消费记录" />
        </a-card>

        <a-button type="link" @click="router.push(`/events/${event.eventId}`)">查看事件详情 →</a-button>
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
const traceEvents = ref([])
const topicNameZhMap = ref({})

/** 当前事件 topic 的中英对照展示（消费链路等用） */
const topicDisplay = computed(() => {
  const t = event.value?.topic
  if (!t) return ''
  const zh = topicNameZhMap.value[t]
  return zh ? `${t} · ${zh}` : t
})

function formatTime(iso) {
  if (!iso) return '-'
  try {
    return new Date(iso).toLocaleString('zh-CN')
  } catch {
    return iso
  }
}

/** 事件调用链路树：按调用顺序 发起方 → 父事件 → 当前事件 → 同链路事件（同链路按 occurredAt 排序）；topic 展示中英说明 */
const callChainTreeData = computed(() => {
  const ev = event.value
  if (!ev) return []
  const nameMap = topicNameZhMap.value
  const formatTopic = (t) => {
    if (!t) return '-'
    const zh = nameMap[t]
    return zh ? `${t} · ${zh}` : t
  }

  const initiatorDesc = ev.initiator
    ? `${ev.initiator.service || '-'} / ${ev.initiator.operation || '-'}${ev.initiator.userId ? ` · userId: ${ev.initiator.userId}` : ''}`
    : '-'

  const currentNode = {
    key: 'current',
    title: '当前事件',
    isCurrent: true,
    eventId: ev.eventId,
    topic: formatTopic(ev.topic),
    time: formatTime(ev.occurredAt),
    children: [],
  }

  const others = (traceEvents.value || [])
    .filter((e) => e.eventId !== ev.eventId)
    .sort((a, b) => {
      const at = a.occurredAt ? new Date(a.occurredAt).getTime() : 0
      const bt = b.occurredAt ? new Date(b.occurredAt).getTime() : 0
      return at - bt
    })
  currentNode.children = others.map((e) => ({
    key: e.eventId,
    title: e.eventId,
    link: `/events/${e.eventId}`,
    topic: `${formatTopic(e.topic)} · ${formatTime(e.occurredAt)}`,
    children: [],
  }))

  const rootChildren = []
  if (ev.parentEventId) {
    rootChildren.push({
      key: 'parent',
      title: `父事件 · ${ev.parentEventId}`,
      link: `/events/${ev.parentEventId}`,
      eventId: ev.parentEventId,
      children: [currentNode],
    })
  } else {
    rootChildren.push(currentNode)
  }

  return [
    {
      key: 'initiator',
      title: '发起方',
      desc: initiatorDesc,
      children: rootChildren,
    },
  ]
})

async function loadTopicConfigs() {
  try {
    const res = await fetch('/api/topic-configs')
    if (!res.ok) return
    const data = await res.json()
    const map = {}
    if (Array.isArray(data)) data.forEach((item) => { if (item.topic && item.nameZh) map[item.topic] = item.nameZh })
    topicNameZhMap.value = map
  } catch {
    // ignore
  }
}

function onCallChainSelect(selectedKeys, e) {
  const node = e?.node
  const data = node?.dataRef || node
  if (data?.link) router.push(data.link)
}

/** 消费链路按 attemptNo 分组，用于时间轴展示；每组带时间文案 */
const consumptionByAttempt = computed(() => {
  const list = event.value?.consumptions || []
  const byAttempt = new Map()
  for (const c of list) {
    const no = c.attemptNo ?? 0
    if (!byAttempt.has(no)) byAttempt.set(no, [])
    byAttempt.get(no).push({
      key: c.id || `${c.consumerId}-${no}`,
      consumerId: c.consumerId,
      statusTag: c.success === true ? 'success' : c.success === false ? 'fail' : 'pending',
      createdDesc: c.createdAt ? `创建：${formatTime(c.createdAt)}` : null,
      desc: c.consumedAt ? `消费：${formatTime(c.consumedAt)}` : '未消费',
      errorMsg: c.errorMessage || null,
      errorCode: c.errorCode,
      attemptNo: c.attemptNo,
      createdAt: c.createdAt,
      consumedAt: c.consumedAt,
    })
  }
  const sortedNos = [...byAttempt.keys()].sort((a, b) => a - b)
  return sortedNos.map((attemptNo) => {
    const items = byAttempt.get(attemptNo)
    const times = items.flatMap((i) => [i.consumedAt, i.createdAt].filter(Boolean))
    const timeText = times.length
      ? formatTime(times.reduce((a, b) => (new Date(a).getTime() > new Date(b).getTime() ? a : b)))
      : null
    return { attemptNo, items, timeText }
  })
})

function go(path) {
  router.push(path)
}

async function loadEvent() {
  loading.value = true
  error.value = ''
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

async function loadTraceEvents() {
  if (!event.value?.traceId) return
  try {
    const res = await fetch(`/api/events?traceId=${encodeURIComponent(event.value.traceId)}&size=100`)
    if (!res.ok) return
    const data = await res.json()
    traceEvents.value = data.content || []
  } catch {
    traceEvents.value = []
  }
}

onMounted(async () => {
  await loadTopicConfigs()
  await loadEvent()
  if (event.value) await loadTraceEvents()
})
</script>

<style scoped>
.event-view { padding: 0; }
.back { padding-left: 0; margin-bottom: 8px; }
.mb-2 { margin-bottom: 16px; }
.muted { color: rgba(0,0,0,.45); font-size: 12px; margin-left: 6px; }
.current-event { font-weight: 600; }
.tree-node-link { color: #1677ff; cursor: pointer; }
.tree-node-link:hover { text-decoration: underline; }
.error-msg { color: #ff4d4f; font-size: 12px; margin-top: 4px; }
:deep(.ant-tree .ant-tree-node-content-wrapper) { align-items: baseline; }
.topic-zh-extra { font-size: 12px; color: rgba(0,0,0,.65); }
.topic-zh-extra .topic-zh-text { color: rgba(0,0,0,.85); }
.consumption-timeline { padding-left: 8px; }
.timeline-dot { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 50%; background: #1677ff; color: #fff; font-size: 12px; font-weight: 500; }
.timeline-group { margin-top: 2px; }
.timeline-group-head { margin-bottom: 8px; }
.timeline-label { font-weight: 500; margin-right: 8px; }
.timeline-time { font-size: 12px; }
.consumption-list { display: flex; flex-direction: column; gap: 6px; }
.consumption-item { padding: 6px 10px; background: rgba(0,0,0,.02); border-radius: 6px; font-size: 13px; }
.consumption-item .consumer-id { font-weight: 500; margin-right: 6px; }
.consumption-item .muted { margin-left: 4px; }
</style>
