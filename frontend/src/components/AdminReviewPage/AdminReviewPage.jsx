import { useEffect, useState } from 'react'
import { listAdminReviews, adminHideReview, adminUnhideReview } from '../../api/reviews'
import Stars from '../Stars/Stars'
import './AdminReviewPage.css'

export default function AdminReviewsPage() {
    const token = localStorage.getItem('token')
    const [productName, setProductName] = useState('')
    const [username, setUsername] = useState('')
    const [hiddenOnly, setHiddenOnly] = useState(false)
    const [page, setPage] = useState(0)
    const [data, setData] = useState({ content: [], totalPages: 0 })
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)

    async function load() {
        setLoading(true); setError(null)
        try {
            const d = await listAdminReviews(token, {
                productName: productName.trim() || undefined,
                username: username.trim() || undefined,
                hiddenOnly, page, size: 10, sort: 'newest',
            })
            setData(d)
        } catch (e) { setError(e.message) }
        finally { setLoading(false) }
    }

    useEffect(() => { load() }, [page, hiddenOnly])

    function applyFilters(e) {
        e.preventDefault()
        setPage(0)
        load()
    }

    async function hide(id) {
        try { await adminHideReview(token, id); await load() }
        catch (e) { setError(e.message) }
    }

    async function unhide(id) {
        try { await adminUnhideReview(token, id); await load() }
        catch (e) { setError(e.message) }
    }

    return (
        <div className="arp-page">
            <div className="arp-card">
                <h2 className="arp-card__title">Reviews</h2>

                <form className="arp-filter-bar" onSubmit={applyFilters}>
                    <input
                        className="arp-input"
                        type="text"
                        placeholder="Product Name"
                        value={productName}
                        onChange={e => setProductName(e.target.value)}
                    />
                    <input
                        className="arp-input"
                        type="text"
                        placeholder="User name"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                    />
                    <label className="arp-label-inline">
                        <input
                            type="checkbox"
                            checked={hiddenOnly}
                            onChange={e => setHiddenOnly(e.target.checked)}
                        />
                        Hidden only
                    </label>
                    <button type="submit" className="arp-btn arp-btn--primary">Apply</button>
                </form>

                {error && <div className="arp-error">{error}</div>}
                {loading && <div className="arp-state">Loading…</div>}

                {!loading && (
                    <div className="arp-table-wrap">
                        <table className="arp-table arp-table--compact">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Product</th>
                                    <th>User</th>
                                    <th>Rating</th>
                                    <th>Comment</th>
                                    <th>Status</th>
                                    <th>Created</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {data.content.map(r => (
                                    <tr key={r.id} className={r.status !== 'ACTIVE' ? 'arp-row--inactive' : ''}>
                                        <td>{r.id}</td>
                                        <td>{r.productName} ({r.productId})</td>
                                        <td>{r.username} ({r.userId})</td>
                                        <td><Stars rating={r.rating} /></td>
                                        <td className="arp-comment-cell">{r.comment}</td>
                                        <td><StatusBadge status={r.status} /></td>
                                        <td>{new Date(r.createdAt).toLocaleDateString()}</td>
                                        <td>
                                            {r.status === 'ACTIVE' && (
                                                <button className="arp-btn arp-btn--sm" onClick={() => hide(r.id)}>Hide</button>
                                            )}
                                            {r.status === 'HIDDEN_BY_ADMIN' && (
                                                <button className="arp-btn arp-btn--sm" onClick={() => unhide(r.id)}>Unhide</button>
                                            )}
                                            {r.status === 'DELETED_BY_USER' && <span>—</span>}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {data.totalPages > 1 && (
                    <div className="arp-pagination">
                        <button
                            className="arp-btn arp-btn--sm"
                            disabled={page === 0}
                            onClick={() => setPage(p => p - 1)}
                        >Prev</button>
                        <span className="arp-pagination__info">Page {page + 1} of {data.totalPages}</span>
                        <button
                            className="arp-btn arp-btn--sm"
                            disabled={page + 1 >= data.totalPages}
                            onClick={() => setPage(p => p + 1)}
                        >Next</button>
                    </div>
                )}
            </div>
        </div>
    )
}

function StatusBadge({ status }) {
    if (status === 'ACTIVE') return <span className="arp-badge arp-badge--active">Active</span>
    if (status === 'HIDDEN_BY_ADMIN') return <span className="arp-badge arp-badge--hidden">Hidden by admin</span>
    if (status === 'DELETED_BY_USER') return <span className="arp-badge arp-badge--deleted">Deleted by user</span>
    return <span className="arp-badge">{status}</span>
}
