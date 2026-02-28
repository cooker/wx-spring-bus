<template>
  <div class="home">
    <div class="date-row">
      <span class="date-label">查看日期：</span>
      <a-date-picker
        v-model:value="selectedDate"
        value-format="YYYY-MM-DD"
        @change="onDateChange"
      />
    </div>

    <a-row :gutter="[16, 16]" class="stats-row">
      <a-col :xs="24" :sm="12" :md="6">
        <a-card size="small" class="stat-card">
          <a-statistic :title="dateTitle + '事件总数'" :value="stats.totalEventCount" />
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card size="small" class="stat-card">
          <a-statistic :title="dateTitle + '处理中的事件'" :value="stats.todayProcessingCount" />
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card size="small" class="stat-card">
          <a-statistic title="Topic 总数" :value="stats.topicCount" />
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card size="small" class="stat-card">
          <a-statistic title="消费者总数" :value="stats.consumerCount" />
        </a-card>
      </a-col>
    </a-row>

    <a-card size="small" class="hourly-card" :title="dateTitle + '各小时事件数量（0–23 时）'">
      <div class="hourly-chart">
        <div
          v-for="(count, hour) in stats.hourlyCounts"
          :key="hour"
          class="hour-bar-wrap"
          :title="`${hour} 时: ${count} 条`"
        >
          <div
            class="hour-bar"
            :style="{ height: barHeight(count) + 'px' }"
          />
          <span class="hour-label">{{ hour }}</span>
          <span class="hour-count">{{ count }}</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'

const selectedDate = ref(dayjs().format('YYYY-MM-DD'))

const stats = ref({
  date: '',
  todayProcessingCount: 0,
  totalEventCount: 0,
  topicCount: 0,
  consumerCount: 0,
  hourlyCounts: Array.from({ length: 24 }, () => 0),
})

const dateTitle = computed(() => {
  const d = stats.value.date
  if (!d) return '当日'
  const today = dayjs().format('YYYY-MM-DD')
  return d === today ? '今日' : d + ' '
})

const maxHourly = () => {
  const arr = stats.value.hourlyCounts
  if (!arr?.length) return 1
  const m = Math.max(...arr)
  return m > 0 ? m : 1
}

function barHeight(count) {
  const max = maxHourly()
  const minH = 4
  const maxH = 120
  return Math.max(minH, Math.round((count / max) * maxH))
}

function onDateChange() {
  loadStats()
}

async function loadStats() {
  try {
    const raw = selectedDate.value
    const date = raw ? dayjs(raw).format('YYYY-MM-DD') : ''
    const url = date ? `/api/stats/home?date=${encodeURIComponent(date)}` : '/api/stats/home'
    const res = await fetch(url)
    if (!res.ok) return
    const data = await res.json()
    stats.value = {
      date: data.date ?? '',
      todayProcessingCount: data.todayProcessingCount ?? 0,
      totalEventCount: data.totalEventCount ?? 0,
      topicCount: data.topicCount ?? 0,
      consumerCount: data.consumerCount ?? 0,
      hourlyCounts: Array.isArray(data.hourlyCounts) && data.hourlyCounts.length === 24
        ? data.hourlyCounts
        : Array.from({ length: 24 }, () => 0),
    }
  } catch {
    // ignore
  }
}

onMounted(() => loadStats())
</script>

<style scoped>
.home { padding: 0; }
.date-row {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.date-label { font-size: 14px; color: #666; }
.stats-row { margin-bottom: 0; }
.stat-card { text-align: center; }
.stat-card :deep(.ant-statistic-title) { font-size: 14px; }
.stat-card :deep(.ant-statistic-content) { font-size: 24px; }
.mt-2 { margin-top: 16px; }

.hourly-card { margin-top: 16px; }
.hourly-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  min-height: 140px;
  padding: 8px 0;
}
.hour-bar-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.hour-bar {
  width: 100%;
  max-width: 24px;
  min-height: 4px;
  background: linear-gradient(to top, #1890ff, #69c0ff);
  border-radius: 4px 4px 0 0;
  transition: height 0.2s ease;
}
.hour-label {
  font-size: 10px;
  color: #666;
}
.hour-count {
  font-size: 11px;
  font-weight: 500;
  color: #1890ff;
}
</style>
