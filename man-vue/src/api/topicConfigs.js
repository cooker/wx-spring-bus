import { request } from './request.js'

/**
 * Topic 配置列表
 * @param {string} [topic] 按 topic 关键字筛选
 */
export async function getTopicConfigList(topic) {
  const path = topic ? `/topic-configs?topic=${encodeURIComponent(topic)}` : '/topic-configs'
  const { ok, data } = await request(path)
  if (!ok) throw new Error(data?.message || '加载失败')
  return Array.isArray(data) ? data : []
}

/**
 * 单条 Topic 配置
 * @param {string} id
 */
export async function getTopicConfig(id) {
  const { ok, data, status } = await request(`/topic-configs/${id}`)
  if (!ok) {
    if (status === 404) return null
    throw new Error(data?.message || '加载失败')
  }
  return data
}

/**
 * 新增 Topic 配置
 * @param {{ topic: string, nameZh?: string, description?: string }} body
 */
export async function createTopicConfig(body) {
  const { ok, data } = await request('/topic-configs', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!ok) throw new Error(data?.message || '保存失败')
  return data
}

/**
 * 更新 Topic 配置
 * @param {string} id
 * @param {{ topic: string, nameZh?: string, description?: string }} body
 */
export async function updateTopicConfig(id, body) {
  const { ok, data } = await request(`/topic-configs/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
  if (!ok) throw new Error(data?.message || '保存失败')
  return data
}

/**
 * 删除 Topic 配置
 * @param {string} id
 */
export async function deleteTopicConfig(id) {
  const { ok, data } = await request(`/topic-configs/${id}`, { method: 'DELETE' })
  if (!ok) throw new Error(data?.message || '删除失败')
}
