import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { listAdminConversations, assignConversation } from '../../api/adminChat.js'
import './AdminChatPage.css'

export default function AdminChatPage({ userPermissions = [], isSuperAdmin = false }) {

    const canViewInquiry = isSuperAdmin || userPermissions.includes('CHAT_INQUIRY_VIEW_ASSIGNED')
    const canViewOrder   = isSuperAdmin || userPermissions.includes('CHAT_ORDER_VIEW_ASSIGNED')
    const canReplyInquiry = isSuperAdmin || userPermissions.includes('CHAT_INQUIRY_REPLY')
    const canReplyOrder   = isSuperAdmin || userPermissions.includes('CHAT_ORDER_REPLY')

    const canViewAny = canViewInquiry || canViewOrder

    // 在 useState 里：优先 INQUIRY，否则 ORDER
    const [chatType, setChatType] = useState(() =>
      canViewInquiry ? 'INQUIRY' : 'ORDER'
    )

    const navigate = useNavigate()
    const token = localStorage.getItem('token')

    const [queueTab, setQueueTab] = useState('waiting') // 'waiting' | 'mine'
    const [conversations, setConversations] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [assigningId, setAssigningId] = useState(null)

    const loadConversations = useCallback(async () => {
      if (!token) {
        setConversations([])
        setLoading(false)
        return
      }
      setLoading(true)
      setError(null)
      try {
        const opts = {
          type: chatType,
          page,
          size: 20,
          assignedToMe: queueTab === 'mine',
        }
        if (queueTab === 'waiting') {
          opts.status = 'WAITING_HUMAN'
        }
        const data = await listAdminConversations(token, opts)
        setConversations(data?.content ?? [])
        setTotalPages(data?.totalPages ?? 1)
      } catch (e) {
        setError(e.message || 'Failed to load conversations')
      } finally {
        setLoading(false)
      }
    }, [token, queueTab, chatType, page])

    useEffect(() => {
      loadConversations()
    }, [loadConversations])

    async function handleAssign(conversationId) {
      if (!token || assigningId != null) return
      setAssigningId(conversationId)
      setError(null)
      try {
        await assignConversation(token, conversationId)
        await loadConversations()
      } catch (e) {
        setError(e.message || 'Assign failed')
      } finally {
        setAssigningId(null)
      }
    }

    function switchQueueTab(tab) {
      setQueueTab(tab)
      setPage(0)
    }

    function switchChatType(type) {
      setChatType(type)
      setPage(0)
    }

    if (!canViewAny) {
      return (
        <div className="acp-page">
          <h1 className="acp-title">Chat</h1>
          <p>You do not have permission to view chat conversations.</p>
        </div>
      )
    }

    const canReplyCurrent =
      chatType === 'INQUIRY' ? canReplyInquiry : canReplyOrder

    return (
      <div className="acp-page">
        <h1 className="acp-title">Chat</h1>

        {/* 类型：INQUIRY / ORDER */}
        <div className="acp-tabs">
          {canViewInquiry && (
            <button
              type="button"
              className={`acp-tab${chatType === 'INQUIRY' ? ' acp-tab--active' : ''}`}
              onClick={() => switchChatType('INQUIRY')}
            >
              Inquiries
            </button>
          )}
          {canViewOrder && (
            <button
              type="button"
              className={`acp-tab${chatType === 'ORDER' ? ' acp-tab--active' : ''}`}
              onClick={() => switchChatType('ORDER')}
            >
              Order chats
            </button>
          )}
        </div>

        {/* 队列：Waiting / Mine */}
        <div className="acp-tabs acp-tabs--secondary">
          <button
            type="button"
            className={`acp-tab${queueTab === 'waiting' ? ' acp-tab--active' : ''}`}
            onClick={() => switchQueueTab('waiting')}
          >
            Waiting
          </button>
          <button
            type="button"
            className={`acp-tab${queueTab === 'mine' ? ' acp-tab--active' : ''}`}
            onClick={() => switchQueueTab('mine')}
          >
            Mine
          </button>
        </div>

        {error && <p className="acp-error" role="alert">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading && conversations.length === 0 && (
          <p className="acp-empty">No conversations in this queue.</p>
        )}

        {!loading && conversations.length > 0 && (
          <ul className="acp-list">
            {conversations.map((c) => (
              <li key={c.id} className="acp-item">
                <div className="acp-item__main">
                  <strong>#{c.id}</strong>
                  <span className="acp-item__type">{c.type}</span>
                  <span className="acp-item__status">{c.status}</span>
                  {c.productId != null && <span>Product {c.productId}</span>}
                  {c.orderId != null && <span>Order {c.orderId}</span>}
                  <span className="acp-item__date">
                    {c.createdAt ? new Date(c.createdAt).toLocaleString() : ''}
                  </span>
                </div>
                <div className="acp-item__actions">
                  {queueTab === 'waiting' && canReplyCurrent && (
                    <button
                      type="button"
                      className="acp-btn acp-btn--primary"
                      disabled={assigningId === c.id}
                      onClick={() => handleAssign(c.id)}
                    >
                      {assigningId === c.id ? 'Assigning...' : 'Assign'}
                    </button>
                  )}
                  {queueTab === 'mine' && c.status === 'ASSIGNED' && (
                    <button
                      type="button"
                      className="acp-btn"
                      onClick={() => navigate(`/admin/chat/${c.id}`, { state: { conversation: c } })}
                    >
                      Open
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}

        {totalPages > 1 && (
          <div className="acp-pagination">
            <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Prev
            </button>
            <span>Page {page + 1} / {totalPages}</span>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        )}
      </div>
    )
}