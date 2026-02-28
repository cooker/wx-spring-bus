<template>
  <a-layout class="basic-layout">
    <a-layout-sider v-model:collapsed="collapsed" :width="220" theme="dark">
      <div class="logo">Wx Bus</div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        :items="menuItems"
        @click="onMenuClick"
      />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <span class="title">事件调度 - 管理端</span>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const menuItems = [
  { key: '/', label: '首页' },
  { key: '/events', label: '事件列表' },
  { key: '/topic-configs', label: 'Topic 配置' },
  { key: '/topic-consumers', label: '消费者配置' },
]

const selectedKeys = computed(() => {
  const path = route.path
  if (path.startsWith('/events')) return ['/events']
  if (path.startsWith('/topic-configs')) return ['/topic-configs']
  if (path.startsWith('/topic-consumers')) return ['/topic-consumers']
  return [route.path || '/']
})

function onMenuClick({ key }) {
  if (key && key !== route.path) router.push(key)
}
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}
.logo {
  height: 48px;
  line-height: 48px;
  text-align: center;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 600;
  font-size: 16px;
}
.header {
  background: #fff;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.title {
  font-size: 16px;
  font-weight: 500;
}
.content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  min-height: 280px;
  border-radius: 4px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
