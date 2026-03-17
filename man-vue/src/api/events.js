import { request } from './request.js'

/**
 * 事件分页列表
 * @param {Record<string, string|number|undefined>} params page, size, status, topic, userId, occurredAtFrom, occurredAtTo, traceId
 */
export async function getEventList(params = {}) {
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.set(k, String(v))
  })
  const path = `/events?${qs.toString()}`
  const { ok, data } = await request(path)
  if (!ok) throw new Error(data?.message || '加载失败')
  return data
}

/**
 * 事件详情（含消费记录）
 * @param {string} eventId
 */
export async function getEventDetail(eventId) {
  const { ok, data, status } = await request(`/events/${eventId}`)
  if (!ok) {
    if (status === 404) return { notFound: true }
    throw new Error(data?.message || '加载失败')
  }
  return data
}

/**
 * 重推事件
 * @param {string} eventId
 * @returns {Promise<{ success: boolean, message?: string }>}
 */
export async function retryEvent(eventId) {
  const { ok, data } = await request(`/events/${eventId}/retry`, { method: 'POST' })
  return { success: ok && data?.success, message: data?.message }
}
