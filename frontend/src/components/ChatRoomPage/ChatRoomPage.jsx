import { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { listMessages, sendMessage, listConversations  } from '../../api/chat.js'
import './ChatRoomPage.css'

export default function ChatRoomPage({ userName, onNeedAuth }) {
    const { conversationId } = useParams()
    const navigate = useNavigate()
    const location = useLocation()
    const conversation = location.state?.conversation

    const [myUserId, setMyUserId] = useState(
      () => location.state?.conversation?.createdByUserId ?? null
    )
    const [messages, setMessages] = useState([])
    const [input, setInput] = useState('')
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [sending, setSending] = useState(false)
    const bottomRef = useRef(null)

    async function loadMessages() {
      const token = localStorage.getItem('token')
      if (!token) {
        onNeedAuth?.()
        return
      }
      try {
        const page = await listMessages(token, conversationId, 0, 50)
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
        onNeedAuth?.()
        setLoading(false)
        return
      }
      loadMessages()
      const timer = setInterval(loadMessages, 4000)
      return () => clearInterval(timer)
    }, [conversationId])

    useEffect(() => {
      if (myUserId != null) return

      const token = localStorage.getItem('token')
      if (!token) return

      listConversations(token, 0, 100)
        .then((page) => {
          const list = page?.content ?? []
          const conv = list.find((c) => String(c.id) === String(conversationId))
          if (conv?.createdByUserId != null) {
            setMyUserId(conv.createdByUserId)
          }
        })
        .catch(() => {})
    }, [conversationId, myUserId])

    async function handleSend(e) {
      e.preventDefault()
      const text = input.trim()
      if (!text || sending) return
      const token = localStorage.getItem('token')
      if (!token) { onNeedAuth?.(); return }
      setSending(true)
      try {
        await sendMessage(token, conversationId, text)
        setInput('')
        await loadMessages()
      } catch (e) {
        setError(e.message || 'Send failed')
      } finally {
        setSending(false)
      }
    }

    const token = localStorage.getItem('token')
    if (!token) return <p>Please sign in to use chat.</p>

    if (loading) return <p>Loading...</p>

    return (
      <div className="chat-room">
        <button type="button" onClick={() => navigate('/chat')}>← Back to conversations</button>
        {error && <p className="chat-room__error">{error}</p>}
        <div className="chat-room__messages">
          {messages.map((m) => {
            const mine =
              myUserId != null && Number(m.senderUserId) === Number(myUserId)
            return (
              <div key={m.id} className={mine ? 'chat-bubble chat-bubble--mine' : 'chat-bubble chat-bubble--other'}>
                <div className="chat-bubble__text">{m.content}</div>
                <div className="chat-bubble__meta">{m.type} · {new Date(m.createdAt).toLocaleString()}</div>
              </div>
            )
          })}
          <div ref={bottomRef} />
        </div>
        <form className="chat-room__form" onSubmit={handleSend}>
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type a message..."
            maxLength={2000}
          />
          <button type="submit" disabled={sending}>{sending ? 'Sending...' : 'Send'}</button>
        </form>
      </div>
    )

    }