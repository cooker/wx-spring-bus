import { request } from './request.js'

/**
 * 首页统计
 * @param {string} [date] yyyy-MM-dd，不传为当日
 * @returns {Promise<{ date, todayProcessingCount, totalEventCount, topicCount, consumerCount, hourlyCounts }>}
 */
export async function getHomeStats(date) {
  const path = date ? `/stats/home?date=${encodeURIComponent(date)}` : '/stats/home'
  const { ok, data } = await request(path)
  if (!ok) return null
  return data
}
