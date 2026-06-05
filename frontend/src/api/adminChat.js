const ADMIN_CHAT_BASE = '/api/admin/chat'

//读响应 JSON；失败时用后端 message 或状态码抛 Error，页面 catch 显示
async function handleResponse(res) {
  if (res.status === 204) return null
  let data
  try { data = await res.json() } catch { data = null }
  if (!res.ok) throw new Error(data?.message || `${res.status} ${res.statusText}`)
  return data
}

//带 JWT：Authorization: Bearer <token> + JSON Content-Type
function authHeaders(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
}

/**
 * GET Admin 会话列表
 * opts: { status, type, assignedToMe, page, size }
 * 返回 Spring Page: { content, totalElements, ... }
 */
export function listAdminConversations(token, opts = {}) {
  const params = new URLSearchParams()
  if (opts.status != null && opts.status !== '') {
    params.set('status', opts.status)
  }
  if (opts.type != null && opts.type !== '') {
    params.set('type', opts.type)
  }
  if (opts.assignedToMe != null) {
    params.set('assignedToMe', String(opts.assignedToMe))
  }
  params.set('page', String(opts.page ?? 0))
  params.set('size', String(opts.size ?? 20))

  return fetch(`${ADMIN_CHAT_BASE}/conversations?${params}`, {
    headers: authHeaders(token),
  }).then(handleResponse)
}

/** POST 接单 */
export function assignConversation(token, conversationId) {
  return fetch(`${ADMIN_CHAT_BASE}/conversations/${conversationId}/assign`, {
    method: 'POST',
    headers: authHeaders(token),
  }).then(handleResponse)
}

/** GET 某会话消息（Admin） */
export function listAdminMessages(token, conversationId, page = 0, size = 50) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  })
  return fetch(`${ADMIN_CHAT_BASE}/conversations/${conversationId}/messages?${params}`, {
    headers: authHeaders(token),
  }).then(handleResponse)
}

/** POST 客服发消息 body: { content } */
export function sendAdminMessage(token, conversationId, content) {
  return fetch(`${ADMIN_CHAT_BASE}/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({ content }),
  }).then(handleResponse)
}

/** POST 关闭会话 */
export function closeConversation(token, conversationId) {
  return fetch(`${ADMIN_CHAT_BASE}/conversations/${conversationId}/close`, {
    method: 'POST',
    headers: authHeaders(token),
  }).then(handleResponse)
}
