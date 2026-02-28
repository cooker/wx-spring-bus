import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '../layouts/BasicLayout.vue'

const routes = [
  {
    path: '/',
    component: BasicLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/HomeView.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'events',
        name: 'EventList',
        component: () => import('../views/EventListView.vue'),
        meta: { title: '事件列表' },
      },
      {
        path: 'events/:eventId/view',
        name: 'EventView',
        component: () => import('../views/EventViewView.vue'),
        meta: { title: '事件视图' },
      },
      {
        path: 'events/:eventId',
        name: 'EventDetail',
        component: () => import('../views/EventDetailView.vue'),
        meta: { title: '事件详情' },
      },
      {
        path: 'topic-configs',
        name: 'TopicConfigList',
        component: () => import('../views/TopicConfigListView.vue'),
        meta: { title: 'Topic 配置' },
      },
      {
        path: 'topic-consumers',
        name: 'TopicConsumerList',
        component: () => import('../views/TopicConsumerListView.vue'),
        meta: { title: '消费者配置' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
