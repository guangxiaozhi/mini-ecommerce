import { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { listAdminMessages, sendAdminMessage, listAdminConversations } from '../../api/adminChat.js'
import '../ChatRoomPage/ChatRoomPage.css'

export default function AdminChatRoomPage({ userPermissions = [], isSuperAdmin = false }) {
  const { conversationId } = useParams()
  const navigate = useNavigate()

  const location = useLocation()
  const [myAgentUserId, setMyAgentUserId] = useState(
    () => location.state?.conversation?.assignedAgentUserId ?? null
  )
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [sending, setSending] = useState(false)
  const bottomRef = useRef(null)
  const chatType = location.state?.conversation?.type ?? 'INQUIRY'
  const canReplyInquiry = isSuperAdmin || userPermissions.includes('CHAT_INQUIRY_REPLY')
  const canReplyOrder   = isSuperAdmin || userPermissions.includes('CHAT_ORDER_REPLY')
  const canReply = chatType === 'ORDER' ? canReplyOrder : canReplyInquiry

  async function loadMessages() {
    const token = localStorage.getItem('token')
    if (!token) {
      setError('Please sign in again.')
      setLoading(false)
      return
    }
    try {
      const page = await listAdminMessages(token, conversationId, 0, 50)
      setMessages(page?.content ?? [])
      setError(null)
    } catch (e) {
      setError(e.message || 'Failed to load messages')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadMessages()
    const timer = setInterval(loadMessages, 2000)
    return () => clearInterval(timer)
  }, [conversationId])

  useEffect(() => {
    if (myAgentUserId != null) return
    const token = localStorage.getItem('token')
    if (!token) return

    listAdminConversations(token, { assignedToMe: true, page: 0, size: 100 })
      .then((page) => {
        const conv = (page?.content ?? []).find(
          (c) => String(c.id) === String(conversationId)
        )
        if (conv?.assignedAgentUserId != null) {
          setMyAgentUserId(conv.assignedAgentUserId)
        }
      })
      .catch(() => {})
  }, [conversationId, myAgentUserId])

  async function handleSend(e) {
    e.preventDefault()
    const text = input.trim()
    if (!text || sending || !canReply) return

    const token = localStorage.getItem('token')
    if (!token) {
      setError('Please sign in again.')
      return
    }

    setSending(true)
    try {
      await sendAdminMessage(token, conversationId, text)
      setInput('')
      await loadMessages()
    } catch (e) {
      setError(e.message || 'Send failed')
    } finally {
      setSending(false)
    }
  }

  if (loading) return <p>Loading...</p>

  return (
    <div className="chat-room">
      <button type="button" onClick={() => navigate('/admin/chat')}>
        ← Back to queue
      </button>

      <h2 style={{ margin: '0 0 0.75rem', fontSize: '1.1rem' }}>
        Conversation #{conversationId}
        {location.state?.conversation?.status && (
          <span style={{ marginLeft: 8, fontWeight: 'normal', color: '#6b7280' }}>
            · {location.state.conversation.status}
          </span>
        )}
      </h2>

      {error && <p className="chat-room__error">{error}</p>}

      <div className="chat-room__messages">
        {messages.map((m) => {
          const isSystem = m.type === 'SYSTEM'
          const mine =
            !isSystem &&
            myAgentUserId != null &&
            Number(m.senderUserId) === Number(myAgentUserId)

          const bubbleClass = isSystem
            ? 'chat-bubble chat-bubble--system'
            : mine
              ? 'chat-bubble chat-bubble--mine'
              : 'chat-bubble chat-bubble--other'

          return (
            <div key={m.id} className={bubbleClass}>
              <div className="chat-bubble__text">{m.content}</div>
              <div className="chat-bubble__meta">
                {m.type} · {new Date(m.createdAt).toLocaleString()}
              </div>
            </div>
          )
        })}
        <div ref={bottomRef} />
      </div>

      {canReply ? (
        <form className="chat-room__form" onSubmit={handleSend}>
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Reply as agent..."
            maxLength={2000}
          />
          <button type="submit" disabled={sending}>
            {sending ? 'Sending...' : 'Send'}
          </button>
        </form>
      ) : (
        <p className="chat-room__status-hint">You do not have permission to reply.</p>
      )}
    </div>
  )
}