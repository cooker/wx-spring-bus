<template>
  <div class="topic-consumer-list">
    <a-card title="消费者配置">
      <a-alert
        type="info"
        show-icon
        class="config-desc mb-2"
      >
        <template #message>配置说明</template>
        <template #description>
          消费者配置用于维护 topic 与消费者的关联；仅「启用」的配置会在事件创建时参与消费初始化（预期消费者列表）。下表「说明」为每条消费者配置的用途说明，悬停可查看完整内容。
        </template>
      </a-alert>
      <a-form layout="inline" class="filters" :model="filters">
        <a-form-item label="主题 (topic)">
          <a-input
            v-model:value="filters.topic"
            placeholder="如 order.purchased"
            allow-clear
            style="width: 180px"
            @press-enter="load"
          />
        </a-form-item>
        <a-form-item label="消费者 ID (consumerId)">
          <a-input
            v-model:value="filters.consumerId"
            placeholder="如 member-service"
            allow-clear
            style="width: 160px"
            @press-enter="load"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="loading" @click="load">查询</a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="openCreate">新增</a-button>
        </a-form-item>
      </a-form>
      <a-alert v-if="error" type="error" :message="error" show-icon class="mb-2" />
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'topic'">
            <a-tooltip v-if="topicNameZh(record.topic)" :title="topicNameZh(record.topic)">
              <span>{{ record.topic }}</span>
            </a-tooltip>
            <span v-else>{{ record.topic }}</span>
          </template>
          <template v-else-if="column.key === 'description'">
            <a-tooltip v-if="record.description" :title="record.description">
              <span class="desc-cell">{{ record.description }}</span>
            </a-tooltip>
            <span v-else class="muted">—</span>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'success' : 'default'">
              {{ record.enabled ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'updatedAt'">
            {{ formatTime(record.updatedAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-popconfirm
              title="确定删除该配置？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <div class="total-wrap">共 {{ list.length }} 条</div>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑配置' : '新增配置'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitForm"
      @cancel="closeModal"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        layout="vertical"
        class="form-in-modal"
      >
        <a-form-item label="主题 (topic)" name="topic">
          <a-input v-model:value="form.topic" placeholder="如 order.purchased" :disabled="!!editingId" />
        </a-form-item>
        <a-form-item label="消费者 ID (consumerId)" name="consumerId">
          <a-input v-model:value="form.consumerId" placeholder="如 member-service" :disabled="!!editingId" />
        </a-form-item>
        <a-form-item label="是否启用 (enabled)" name="enabled">
          <a-switch v-model:checked="form.enabled" />
        </a-form-item>
        <a-form-item label="排序 (sortOrder)" name="sortOrder">
          <a-input-number v-model:value="form.sortOrder" :min="0" placeholder="可选，数字越小越靠前" style="width: 100%" />
        </a-form-item>
        <a-form-item label="说明 (description)" name="description">
          <a-textarea v-model:value="form.description" placeholder="可选" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'

const loading = ref(false)
const error = ref('')
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const list = ref([])
const topicNameZhMap = ref({})

const filters = reactive({ topic: '', consumerId: '' })

const form = reactive({
  topic: '',
  consumerId: '',
  enabled: true,
  sortOrder: null,
  description: '',
})

const formRules = {
  topic: [{ required: true, message: '请输入 topic' }],
  consumerId: [{ required: true, message: '请输入 consumerId' }],
}

const columns = [
  { title: 'ID (id)', dataIndex: 'id', key: 'id', ellipsis: true, width: 120 },
  { title: '主题 (topic)', dataIndex: 'topic', key: 'topic', width: 160 },
  { title: '消费者 ID (consumerId)', dataIndex: 'consumerId', key: 'consumerId', width: 140 },
  { title: '是否启用 (enabled)', key: 'enabled', width: 100 },
  { title: '排序 (sortOrder)', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '说明 (description)', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '创建时间 (createdAt)', key: 'createdAt', width: 170 },
  { title: '更新时间 (updatedAt)', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

function topicNameZh(topic) {
  if (!topic) return ''
  return topicNameZhMap.value[topic] || ''
}

function formatTime(iso) {
  if (!iso) return '-'
  try {
    return new Date(iso).toLocaleString('zh-CN')
  } catch {
    return iso
  }
}

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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (filters.topic) params.set('topic', filters.topic)
    if (filters.consumerId) params.set('consumerId', filters.consumerId)
    const qs = params.toString()
    const res = await fetch(`/api/topic-consumers${qs ? `?${qs}` : ''}`)
    if (!res.ok) throw new Error(res.statusText)
    const data = await res.json()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.topic = ''
  form.consumerId = ''
  form.enabled = true
  form.sortOrder = null
  form.description = ''
  modalVisible.value = true
}

function openEdit(record) {
  editingId.value = record.id
  form.topic = record.topic
  form.consumerId = record.consumerId
  form.enabled = record.enabled
  form.sortOrder = record.sortOrder ?? null
  form.description = record.description ?? ''
  modalVisible.value = true
}

function closeModal() {
  modalVisible.value = false
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  error.value = ''
  try {
    const body = {
      topic: form.topic.trim(),
      consumerId: form.consumerId.trim(),
      enabled: form.enabled,
      sortOrder: form.sortOrder,
      description: form.description ? form.description.trim() : null,
    }
    const url = editingId.value
      ? `/api/topic-consumers/${editingId.value}`
      : '/api/topic-consumers'
    const method = editingId.value ? 'PUT' : 'POST'
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      const msg = data.message || res.statusText || '保存失败'
      error.value = msg
      message.error(msg)
      return
    }
    message.success(editingId.value ? '保存成功' : '新增成功')
    closeModal()
    load()
  } catch (e) {
    const msg = e.message || '保存失败'
    error.value = msg
    message.error(msg)
  } finally {
    saving.value = false
  }
}

async function doDelete(id) {
  error.value = ''
  try {
    const res = await fetch(`/api/topic-consumers/${id}`, { method: 'DELETE' })
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      const msg = data.message || res.statusText || '删除失败'
      error.value = msg
      message.error(msg)
      return
    }
    message.success('已删除')
    load()
  } catch (e) {
    error.value = e.message || '删除失败'
    message.error(e.message || '删除失败')
  }
}

onMounted(async () => {
  await loadTopicConfigs()
  load()
})
</script>

<style scoped>
.topic-consumer-list { padding: 0; }
.filters { margin-bottom: 16px; }
.filters :deep(.ant-form-item) { margin-right: 16px; margin-bottom: 8px; }
.mb-2 { margin-bottom: 16px; }
.total-wrap { margin-top: 16px; text-align: right; color: rgba(0,0,0,.45); font-size: 14px; }
.form-in-modal :deep(.ant-form-item) { margin-bottom: 16px; }
.desc-cell { display: inline-block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.muted { color: rgba(0,0,0,.45); }
</style>
