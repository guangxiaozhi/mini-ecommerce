import { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { listAdminMessages, sendAdminMessage, listAdminConversations, closeConversation } from '../../api/adminChat.js'
import { connectChatSocket, subscribeConversation, disconnectChatSocket } from '../../api/chatSocket.js'
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

  const canClose = isSuperAdmin || userPermissions.includes('CHAT_CONVERSATION_CLOSE')

  const [conversationStatus, setConversationStatus] = useState(
    () => location.state?.conversation?.status ?? 'ASSIGNED'
  )
  const [closing, setClosing] = useState(false)

  const isClosed = conversationStatus === 'CLOSED'

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
      const token = localStorage.getItem('token')
      if (!token) {
        setError('Please sign in again.')
        setLoading(false)
        return
      }

      loadMessages()

      let unsubscribe = () => {}

      connectChatSocket(token, {
        onConnect: () => {
          unsubscribe = subscribeConversation(conversationId, (newMsg) => {
            setMessages((prev) => {
              if (prev.some((m) => m.id === newMsg.id)) return prev
              return [...prev, newMsg]
            })
            if (newMsg.type === 'SYSTEM') {
              if (newMsg.content?.toLowerCase().includes('closed')) {
                setConversationStatus('CLOSED')
              }
            }
          })
        },
        onError: (msg) => {
          console.warn('WebSocket:', msg)
        },
      })

//       const timer = setInterval(loadMessages, 15000) // 15 秒兜底即可

      return () => {
        unsubscribe()
        disconnectChatSocket()
//         clearInterval(timer)
      }
    }, [conversationId])

    useEffect(() => {
      if (myAgentUserId != null && conversationStatus === 'CLOSED') return
      if (myAgentUserId != null && conversationStatus === 'ASSIGNED') return
      const token = localStorage.getItem('token')
      if (!token) return

      async function fetchMeta() {
        const statusesToTry =
          conversationStatus === 'CLOSED' ? ['CLOSED', 'ASSIGNED'] : ['ASSIGNED', 'CLOSED']

        for (const st of statusesToTry) {
          try {
            const page = await listAdminConversations(token, {
              assignedToMe: true,
              status: st,
              page: 0,
              size: 100,
            })
            const conv = (page?.content ?? []).find(
              (c) => String(c.id) === String(conversationId)
            )
            if (!conv) continue
            if (conv.status != null) setConversationStatus(conv.status)
            if (conv.assignedAgentUserId != null) setMyAgentUserId(conv.assignedAgentUserId)
            break
          } catch {
            // try next
          }
        }
      }

      fetchMeta()
    }, [conversationId, myAgentUserId, conversationStatus])

  async function handleSend(e) {
    e.preventDefault()
    const text = input.trim()
    if (!text || sending || !canReply || isClosed) return

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

  async function handleClose() {
    if (!canClose || closing || isClosed) return
    if (!window.confirm('Close this conversation? The customer will not be able to send new messages.')) {
      return
    }

    const token = localStorage.getItem('token')
    if (!token) {
      setError('Please sign in again.')
      return
    }

    setClosing(true)
    setError(null)
    try {
      const updated = await closeConversation(token, conversationId)
      if (updated?.status) {
        setConversationStatus(updated.status)
      }
      await loadMessages()
      navigate('/admin/chat', { state: { queueTab: 'closed' } })
    } catch (e) {
      setError(e.message || 'Close failed')
    } finally {
      setClosing(false)
    }
  }

  if (loading) return <p>Loading...</p>

  return (
    <div className="chat-room">
      <button type="button" onClick={() => navigate('/admin/chat')}>
        ← Back to queue
      </button>

      {canClose && !isClosed && (
        <button
          type="button"
          className="chat-room__transfer-btn"
          disabled={closing}
          onClick={handleClose}
        >
          {closing ? 'Closing...' : 'Close conversation'}
        </button>
      )}

      <h2 style={{ margin: '0 0 0.75rem', fontSize: '1.1rem' }}>
        Conversation #{conversationId}
        {conversationStatus && (
          <span style={{ marginLeft: 8, fontWeight: 'normal', color: '#6b7280' }}>
            · {conversationStatus}
          </span>
        )}
      </h2>

      {error && <p className="chat-room__error">{error}</p>}
      {isClosed && (
        <p className="chat-room__status-hint">This conversation is closed.</p>
      )}
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

        {canReply && !isClosed ? (
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
        ) : isClosed ? null : (
          <p className="chat-room__status-hint">You do not have permission to reply.</p>
        )}
    </div>
  )
}