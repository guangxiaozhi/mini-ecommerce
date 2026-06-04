import { useEffect, useState, useCallback, useRef } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
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
    const location = useLocation()
    const token = localStorage.getItem('token')

    const [queueTab, setQueueTab] = useState('waiting') // 'waiting' | 'mine'| 'closed'
    const [conversations, setConversations] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [assigningId, setAssigningId] = useState(null)
    const loadSeqRef = useRef(0)

    const loadConversations = useCallback(async () => {
      if (!token) {
        setConversations([])
        setLoading(false)
        return
      }

      const seq = ++loadSeqRef.current
      setLoading(true)
      setError(null)
      setConversations([])

      try {
        const opts = {
          type: chatType,
          page,
          size: 20,
          assignedToMe: queueTab === 'mine' || queueTab === 'closed',
        }
        if (queueTab === 'waiting') {
          opts.status = 'WAITING_HUMAN'
        }
        if (queueTab === 'closed') {
          opts.status = 'CLOSED'
        }

        const data = await listAdminConversations(token, opts)

        if (seq !== loadSeqRef.current) return

        setConversations(data?.content ?? [])
        setTotalPages(data?.totalPages ?? 1)
      } catch (e) {
        if (seq !== loadSeqRef.current) return
        setError(e.message || 'Failed to load conversations')
        setConversations([])
      } finally {
        if (seq === loadSeqRef.current) {
          setLoading(false)
        }
      }
    }, [token, queueTab, chatType, page])

    useEffect(() => {
      loadConversations()
    }, [loadConversations])

    useEffect(() => {
      const tab = location.state?.queueTab
      if (tab === 'mine' || tab === 'closed' || tab === 'waiting') {
        setQueueTab(tab)
        setPage(0)
      }
    }, [location.state?.queueTab])
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
      setConversations([])
      setError(null)
    }

    function switchChatType(type) {
      setChatType(type)
      setPage(0)
      setConversations([])
      setError(null)
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
          <button
            type="button"
            className={`acp-tab${queueTab === 'closed' ? ' acp-tab--active' : ''}`}
            onClick={() => switchQueueTab('closed')}
          >
            Closed
          </button>
        </div>

        {error && <p className="acp-error" role="alert">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading && conversations.length === 0 && (
          <p className="acp-empty">
            {queueTab === 'closed'
              ? 'No closed conversations assigned to you.'
              : 'No conversations in this queue.'}
          </p>
        )}

        {!loading && conversations.length > 0 && (
          <ul className="acp-list" key={queueTab}>
            {conversations.map((c) => (
              <li key={c.id} className="acp-item">
                <div className="acp-item__main">
                  <strong>#{c.id}</strong>
                  <span className="acp-item__type">{c.type}</span>
                  <span className="acp-item__status">{queueTab === 'closed' ? 'CLOSED' : c.status}</span>
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
                    {queueTab === 'closed' && (
                      <button
                        type="button"
                        className="acp-btn"
                        onClick={() =>
                          navigate(`/admin/chat/${c.id}`, { state: { conversation: { ...c, status: 'CLOSED' }, } })
                        }
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