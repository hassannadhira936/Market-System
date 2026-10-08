const BASE = import.meta.env.VITE_API_URL || '/api'
let token = localStorage.getItem('ms_token') || ''
let onUnauthorized = () => {}

export const setToken = (t) => {
  token = t || ''
  t ? localStorage.setItem('ms_token', t) : localStorage.removeItem('ms_token')
}
export const setOnUnauthorized = (fn) => { onUnauthorized = fn }

export async function api(path, { method = 'GET', body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  let data = null
  try { data = await res.json() } catch { /* jibu tupu */ }
  if (res.status === 401 && token) onUnauthorized()
  if (!res.ok) throw new Error(data?.error || `Hitilafu (${res.status})`)
  return data
}

export const money = (n) => `${Number(n || 0).toLocaleString('en-US')} TZS`
