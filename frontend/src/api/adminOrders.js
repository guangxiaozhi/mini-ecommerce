const BASE = '/api/admin/orders'

async function parseBody(res) {
    const text = await res.text()
    if (!text) return null
    try {
        return JSON.parse(text)
    } catch {
        return text
    }
}

async function handleResponse(res) {
    const data = await parseBody(res)
    if (!res.ok) {
        const msg =
            data && typeof data === 'object' && data.message != null
                ? String(data.message)
                : typeof data === 'string'
                    ? data
                    : `${res.status} ${res.statusText}`
        throw new Error(msg)
    }
    return data
}

function authHeader(token) {
    return {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
    }
}

export async function adminListOrders(token, { status, userId, page, size } = {}) {
    const params = new URLSearchParams()
    if (status != null) params.append('status', status)
    if (userId != null) params.append('userId', userId)
    if (page != null) params.append('page', page)
    if (size != null) params.append('size', size)
    const url = params.toString() ? `${BASE}?${params.toString()}` : BASE
    const res = await fetch(url, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
    })
    return handleResponse(res)
}

export async function adminGetOrder(token, orderId) {
    const res = await fetch(`${BASE}/${orderId}`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
    })
    return handleResponse(res)
}

export async function adminUpdateOrderStatus(token, orderId, status) {
    const res = await fetch(`${BASE}/${orderId}/status`, {
        method: 'PUT',
        headers: authHeader(token),
        body: JSON.stringify({ status }),
    })
    return handleResponse(res)
}

export async function adminGetAnalytics(token, from, to) {
    const params = new URLSearchParams()
    params.append('from', from)
    params.append('to', to)
    const res = await fetch(`${BASE}/analytics?${params.toString()}`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
    })
    return handleResponse(res)
}

export async function adminListReturns(token, { status, page, size } = {}) {
    const params = new URLSearchParams()
    if (status != null) params.append('status', status)
    if (page != null) params.append('page', page)
    if (size != null) params.append('size', size)
    const url = params.toString() ? `${BASE}/returns?${params.toString()}` : `${BASE}/returns`
    const res = await fetch(url, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
    })
    return handleResponse(res)
}

export async function adminGetReturn(token, returnId) {
    const res = await fetch(`${BASE}/returns/${returnId}`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` },
    })
    return handleResponse(res)
}

export async function adminApproveReturn(token, returnId) {
    const res = await fetch(`${BASE}/returns/${returnId}/approve`, {
        method: 'PUT',
        headers: authHeader(token),
        body: JSON.stringify({}),
    })
    return handleResponse(res)
}

export async function adminRejectReturn(token, returnId, reason) {
    const res = await fetch(`${BASE}/returns/${returnId}/reject`, {
        method: 'PUT',
        headers: authHeader(token),
        body: JSON.stringify({ reason }),
    })
    return handleResponse(res)
}

export async function adminConfirmRefund(token, returnId, refundAmount) {
    const res = await fetch(`${BASE}/returns/${returnId}/refund`, {
        method: 'PUT',
        headers: authHeader(token),
        body: JSON.stringify({ refundAmount }),
    })
    return handleResponse(res)
}
