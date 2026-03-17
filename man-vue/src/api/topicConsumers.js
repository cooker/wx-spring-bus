import { request } from './request.js'

/**
 * Topic–消费者配置列表
 * @param {{ topic?: string, consumerId?: string }} [filters]
 */
export async function getTopicConsumerList(filters = {}) {
  const params = new URLSearchParams()
  if (filters.topic) params.set('topic', filters.topic)
  if (filters.consumerId) params.set('consumerId', filters.consumerId)
  const qs = params.toString()
  const path = qs ? `/topic-consumers?${qs}` : '/topic-consumers'
  const { ok, data } = await request(path)
  if (!ok) throw new Error(data?.message || '加载失败')
  return Array.isArray(data) ? data : []
}

/**
 * 单条 Topic–消费者配置
 * @param {string} id
 */
export async function getTopicConsumer(id) {
  const { ok, data, status } = await request(`/topic-consumers/${id}`)
  if (!ok) {
    if (status === 404) return null
    throw new Error(data?.message || '加载失败')
  }
  return data
}

/**
 * 新增 Topic–消费者配置
 * @param {{ topic: string, consumerId: string, enabled: boolean, sortOrder?: number, description?: string }} body
 */
export async function createTopicConsumer(body) {
  const { ok, data } = await request('/topic-consumers', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!ok) throw new Error(data?.message || '保存失败')
  return data
}

/**
 * 更新 Topic–消费者配置
 * @param {string} id
 * @param {{ topic: string, consumerId: string, enabled: boolean, sortOrder?: number, description?: string }} body
 */
export async function updateTopicConsumer(id, body) {
  const { ok, data } = await request(`/topic-consumers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
  if (!ok) throw new Error(data?.message || '保存失败')
  return data
}

/**
 * 删除 Topic–消费者配置
 * @param {string} id
 */
export async function deleteTopicConsumer(id) {
  const { ok, data } = await request(`/topic-consumers/${id}`, { method: 'DELETE' })
  if (!ok) throw new Error(data?.message || '删除失败')
}
