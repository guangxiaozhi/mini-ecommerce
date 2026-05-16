import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Stars from '../Stars/Stars'
import { listMyReviews, updateReview, deleteReview } from '../../api/reviews'
import './MyReviewsPage.css'

export default function MyReviewsPage({ onNeedAuth }) {
        const token = localStorage.getItem('token')
         const [data, setData] = useState({ content: [], totalPages: 0 })
         const [page, setPage] = useState(0)
         const [loading, setLoading] = useState(true)
         const [error, setError] = useState(null)
         const [editingId, setEditingId] = useState(null)
         const [editRating, setEditRating] = useState(5)
         const [editComment, setEditComment] = useState('')
         const [editError, setEditError] = useState(null)

         async function load() {
                 if (!token) { setLoading(false); return }
                 setLoading(true)
                 setError(null)
                 try {
                     const d = await listMyReviews(token, { page, size: 10, sort: 'newest' })
                     setData(d)
                 } catch (e) {
                     setError(e.message)
                 } finally {
                     setLoading(false)
                 }
         }

         useEffect(() => { load() }, [page])

         function startEdit(r) {
            setEditingId(r.id)
             setEditRating(r.rating)
             setEditComment(r.comment ?? '')
             setEditError(null)
        }

        async function saveEdit() {
            const trimmed = (editComment ?? '').trim()
            if (trimmed.length < 10) { setEditError('Comment must be at least 10 characters.'); return }
            try {
                await updateReview(token, editingId, { rating: editRating, comment: trimmed })
                setEditingId(null)
                await load()
            } catch (e) { setEditError(e.message) }
        }

        async function handleDelete(id) {
            if (!window.confirm('Delete this review?')) return
            try {
                await deleteReview(token, id)
                await load()
            } catch (e) { setError(e.message) }
        }

        if (!token) {
            return (
                <div className="my-reviews-page">
                    <p>Please sign in to see your reviews.</p>
                    <button onClick={onNeedAuth}>Sign in</button>
                </div>
            )
        }

        return (
            <div className="my-reviews-page">
                <h1 className="my-reviews-page__title">My Reviews</h1>
                {error && <div className="my-reviews-page__error">{error}</div >}
                {loading && <div className="my-reviews-page__loading">Loading…</div>}
                {!loading && data.content.length === 0 && (
                    <p className="my-reviews-page__empty">You haven't reviewed anything yet.</p>
                )}
                <ul className="my-reviews-list">
                    {data.content.map(r => (
                        <li key={r.id} className={`my-review${r.hiddenByAdmin ? ' my-review--hidden' : ''}`}>
                            <div className="my-review__head">
                                <Link to={`/products/${r.productId}`} className="my-review__product">{r.productName}</Link>
                                {r.hiddenByAdmin && <span className="my-review__hidden-badge">Hidden by admin</span>}
                            </div>
                            {editingId === r.id ? (
                                <>
                                    <div className="my-review__rating-edit">
                                        {[1,2,3,4,5].map(n => (
                                            <button
                                                key={n}
                                                className={`my-review__star${n <= editRating ? ' my-review__star--on' : ''}`}
                                                onClick={() => setEditRating(n)}
                                                type="button"
                                                aria-label={`${n} star`}
                                            >★</button>
                                        ))}
                                    </div>
                                    <textarea value={editComment} onChange={e=> setEditComment(e.target.value)} maxLength={1000} rows={3} className="my-review__textarea" />
                                    {editError && <div className="my-review__error">{editError}</div>}
                                    <div className="my-review__actions">
                                        <button onClick={() => setEditingId(null)}>Cancel</button>
                                        <button onClick={saveEdit}>Save</button>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div className="my-review__rating"><Stars rating={r.rating} /></div>
                                    <p className="my-review__comment">{r.comment}</p>
                                    <p className="my-review__date">
                                        {new Date(r.createdAt).toLocaleDateString()}
                                        {r.edited && <span className="my-review__edited"> (edited)</span>}
                                    </p>
                                    {!r.hiddenByAdmin && (
                                        <div className="my-review__actions">
                                            <button onClick={() => startEdit(r)}>Edit</button>
                                            <button onClick={() => handleDelete(r.id)}>Delete</button>
                                        </div>
                                    )}
                                </>
                            )}
                        </li>
                    ))}
                </ul>
                {data.totalPages > 1 && (
                    <div className="my-reviews-page__pager">
                        <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
                        <span>Page {page + 1} of {data.totalPages}</span>
                        <button disabled={page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Next</button>
                    </div>
                )}
            </div>
        )
}