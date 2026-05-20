const CHAT_BASE = '/api/chat'

async function handleResponse(res) {
  if (res.status === 204) return null
  let data
  try { data = await res.json() } catch { data = null }
  if (!res.ok) throw new Error(data?.message || `${res.status} ${res.statusText}`)
  return data
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
}

/** POST 建会话 body: { type: 'INQUIRY'|'ORDER', productId?, orderId? } */
export function createConversation(token, body) {
  return fetch(`${CHAT_BASE}/conversations`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(body),
  }).then(handleResponse)
}

/** GET 我的会话列表，返回 Spring Page：{ content, totalElements, ... } */
export function listConversations(token, page = 0, size = 20) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  return fetch(`${CHAT_BASE}/conversations?${params}`, {
    headers: authHeaders(token),
  }).then(handleResponse)
}

/** GET 某会话消息列表 */
export function listMessages(token, conversationId, page = 0, size = 50) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  return fetch(`${CHAT_BASE}/conversations/${conversationId}/messages?${params}`, {
    headers: authHeaders(token),
  }).then(handleResponse)
}

/** POST 发消息 body 在后端是 { content } */
export function sendMessage(token, conversationId, content) {
  return fetch(`${CHAT_BASE}/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({ content }),
  }).then(handleResponse)
}

export function transferToHuman(token, conversationId) {
  return fetch(`${CHAT_BASE}/conversations/${conversationId}/transfer-to-human`, {
    method: 'POST',
    headers: authHeaders(token),
  }).then(handleResponse)
}