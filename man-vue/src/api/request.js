/**
 * 管理端 API 请求基址与封装，统一使用 /api/v1。
 */
const BASE = '/api/v1'

/**
 * @param {string} path 相对路径，如 '/events'、'/stats/home'
 * @param {RequestInit} [options]
 * @returns {Promise<{ ok: boolean, data: any, status: number }>}
 */
export async function request(path, options = {}) {
  const url = path.startsWith('http') ? path : `${BASE}${path.startsWith('/') ? path : '/' + path}`
  const hasBody = options.body != null
  const res = await fetch(url, {
    ...options,
    headers: {
      ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })
  const data = await res.json().catch(() => ({}))
  return { ok: res.ok, data, status: res.status }
}

export { BASE }
