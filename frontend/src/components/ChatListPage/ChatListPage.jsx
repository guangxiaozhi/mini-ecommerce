import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listConversations } from '../../api/chat.js'
import './ChatListPage.css'

export default function ChatListPage({ userName, onNeedAuth }) {
    const [conversations, setConversations] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const token = localStorage.getItem('token')
    const navigate = useNavigate()

    async function loadConversations() {
      if (!token) {
        setConversations([])
        setLoading(false)
        return
      }
      setLoading(true)
      setError(null)
      try {
        const data = await listConversations(token, page, 20)
        setConversations(data?.content ?? [])
        setTotalPages(data?.totalPages ?? 1)
      } catch (e) {
        setError(e.message ?? 'Failed to load conversations')
      } finally {
        setLoading(false)
      }
    }

    useEffect(() => {
      loadConversations()
    }, [userName, page, token])

    if (!token) {
      return (
        <div className="chat-list-page">
          <h1 className="chat-list-page__title">Messages</h1>
          <div className="chat-list-auth">
            <p>Please sign in to view your messages.</p>
            <button type="button" onClick={onNeedAuth}>Sign In</button>
            <Link to="/">← Continue Shopping</Link>
          </div>
        </div>
      )
    }
    if (loading) {
      return (
        <div className="chat-list-page">
          <h1 className="chat-list-page__title">Messages</h1>
          <p>Loading...</p>
        </div>
      )
    }
    if (error) {
      return (
        <div className="chat-list-page">
          <h1 className="chat-list-page__title">Messages</h1>
          <p role="alert">{error}</p>
        </div>
      )
    }
    return (
      <div className="chat-list-page">
        <h1 className="chat-list-page__title">Messages</h1>
        {conversations.length === 0 ? (
          <p className="chat-list-empty">
            No conversations yet. Start from a product or order page.
          </p>
        ) : (
          <ul className="chat-list">
            {conversations.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  className="chat-list__item"
                  onClick={() =>
                    navigate(`/chat/${c.id}`, { state: { conversation: c } })
                  }
                >
                  <div>
                    {c.type === 'ORDER'
                      ? `Order chat #${c.orderId}`
                      : `Product inquiry #${c.productId}`}
                  </div>
                  <div className="chat-list__meta">
                    {c.type} · {new Date(c.createdAt).toLocaleString()}
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
        {totalPages > 1 && (
          <div className="chat-list-pagination">
            <button
              type="button"
              disabled={page <= 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </button>
            <span>Page {page + 1} of {totalPages}</span>
            <button
              type="button"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        )}
      </div>
    )
}
