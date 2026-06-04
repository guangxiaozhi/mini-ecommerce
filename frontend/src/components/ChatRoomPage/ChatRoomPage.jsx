import { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { listMessages, sendMessage, listConversations, transferToHuman, reopenConversation } from '../../api/chat.js'
import './ChatRoomPage.css'

export default function ChatRoomPage({ onNeedAuth }) {
    const { conversationId } = useParams()
    const navigate = useNavigate()
    const location = useLocation()

    const [myUserId, setMyUserId] = useState(
      () => location.state?.conversation?.createdByUserId ?? null
    )
    const [messages, setMessages] = useState([])
    const [input, setInput] = useState('')
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [sending, setSending] = useState(false)
    const [conversationStatus, setConversationStatus] = useState(
      () => location.state?.conversation?.status ?? 'BOT'
    )
    const [transferring, setTransferring] = useState(false)
    const bottomRef = useRef(null)
    const [reopening, setReopening] = useState(false)

    async function syncConversationMeta() {
      const token = localStorage.getItem('token')
      if (!token) return

      try {
        const page = await listConversations(token, 0, 100)
        const conv = (page?.content ?? []).find(
          (c) => String(c.id) === String(conversationId)
        )
        if (!conv) return

        if (myUserId == null && conv.createdByUserId != null) {
          setMyUserId(conv.createdByUserId)
        }
        if (conv.status != null) {
          setConversationStatus(conv.status)
        }
      } catch {
        // 同步失败不打断聊天；下次轮询会再试
      }
    }

    async function loadMessages() {
      const token = localStorage.getItem('token')
      if (!token) {
        onNeedAuth?.()
        return
      }
      try {
        const page = await listMessages(token, conversationId, 0, 50)
        setMessages(page?.content ?? [])
        await syncConversationMeta()
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



    async function handleTransfer() {
      const token = localStorage.getItem('token')
      if (!token) {
        onNeedAuth?.()
        return
      }
      setTransferring(true)
      setError(null)
      try {
        const updated = await transferToHuman(token, conversationId)
        if (updated?.status) {
          setConversationStatus(updated.status)
        }
        await loadMessages()
      } catch (e) {
        setError(e.message || 'Transfer failed')
      } finally {
        setTransferring(false)
      }
    }

    async function handleReopen() {
      const token = localStorage.getItem('token')
      if (!token) {
        onNeedAuth?.()
        return
      }
      setReopening(true)
      setError(null)
      try {
        const updated = await reopenConversation(token, conversationId)
        if (updated?.status) {
          setConversationStatus(updated.status)
        }
        await loadMessages()
      } catch (e) {
        setError(e.message || 'Reopen failed')
      } finally {
        setReopening(false)
      }
    }

    function isTransferPhrase(text) {
      const t = text.trim()
      return t.toLowerCase() === 'speak to an agent' || t === '转人工'
    }

    async function handleSend(e) {
      e.preventDefault()
      if (conversationStatus === 'CLOSED') return
      const text = input.trim()
      if (!text || sending) return
      const token = localStorage.getItem('token')
      if (!token) { onNeedAuth?.(); return }
      setSending(true)
      try {
        await sendMessage(token, conversationId, text)

        if (isTransferPhrase(text) && conversationStatus === 'BOT') {
          setConversationStatus('WAITING_HUMAN')
        }
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
        {conversationStatus === 'BOT' && (
          <button
            type="button"
            className="chat-room__transfer-btn"
            disabled={transferring}
            onClick={handleTransfer}
          >
            {transferring ? 'Transferring...' : 'Speak to an agent'}
          </button>
        )}

        {conversationStatus === 'BOT' && (
          <p className="chat-room__status-hint">
            You are chatting with our virtual assistant. Need a human? Tap &quot;Speak to an agent&quot; above
            or type &quot;Speak to an Agent&quot; or &quot;转人工&quot; in the box below.
          </p>
        )}

        {(conversationStatus === 'WAITING_HUMAN' || conversationStatus === 'ASSIGNED') && (
          <p className="chat-room__status-hint">
            {conversationStatus === 'WAITING_HUMAN'
              ? 'Waiting for a human agent...'
              : 'An agent is handling this chat.'}
          </p>
        )}
        {conversationStatus === 'CLOSED' && (
          <>
              <p className="chat-room__status-hint">
                This conversation has been closed. You can still read the history below.
              </p>
              <button
                type="button"
                className="chat-room__transfer-btn"
                disabled={reopening}
                onClick={handleReopen}
              >
                {reopening ? 'Reopening...' : 'Continue conversation'}
              </button>
            </>
        )}
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
        {conversationStatus !== 'CLOSED' && (
          <form className="chat-room__form" onSubmit={handleSend}>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Type a message..."
              maxLength={2000}
            />
            <button type="submit" disabled={sending}>
              {sending ? 'Sending...' : 'Send'}
            </button>
          </form>
        )}
      </div>
    )
}