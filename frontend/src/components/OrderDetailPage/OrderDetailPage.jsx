// 对应 /orders/:orderId（单条订单详情，显示单条订单及 items。）

import { Link, useParams, useNavigate} from 'react-router-dom'
import { useState, useEffect } from 'react'
import { getOrder, createReturn } from '../../api/orders.js'
import { createConversation } from '../../api/chat.js'
import { getEligibility, createReview, updateReview, deleteReview} from "../../api/reviews.js";
import Stars from "../Stars/Stars.jsx";
import './OrderDetailPage.css'

function formatMoney(n) {
    if (n == null) return '—'
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(n))
}

function formatDate(iso) {
    if (!iso) return '—'
    try {
        return new Date(iso).toLocaleString()
    } catch {
        return String(iso)
    }
}

function statusClass(status) {
    const s = String(status || '').toUpperCase()
    const map = {
        PENDING:    'order-detail__badge--pending',
        PAID:       'order-detail__badge--paid',
        PROCESSING: 'order-detail__badge--processing',
        SHIPPED:    'order-detail__badge--shipped',
        DELIVERED:  'order-detail__badge--delivered',
        CLOSED:     'order-detail__badge--closed',
        CANCELLED:  'order-detail__badge--cancelled',
    }
    return `order-detail__badge ${map[s] ?? 'order-detail__badge--pending'}`
}

function returnedQtyByOrderItem(order) {
    const map = new Map()
    for (const rr of order.returnRequests ?? []) {
        if (rr.status === 'REJECTED') continue
        for (const ri of rr.items ?? []) {
            const id = ri.orderItemId
            map.set(id, (map.get(id) || 0) + Number(ri.quantity || 0))
        }
    }
    return map
}

function hasReturnableItems(order) {
    const used = returnedQtyByOrderItem(order)
    return (order.items ?? []).some((line) => {
        const bought = Number(line.quantity || 0)
        const usedQty = used.get(line.orderItemId) || 0
        return bought > usedQty
    })
}

function ReturnModal({ order, token, onSuccess, onClose }) {
    const [reason, setReason] = useState('')
    const [selections, setSelections] = useState(() =>
        Object.fromEntries(
            (order.items ?? []).map(item => [
                item.orderItemId,
                { checked: false, qty: item.quantity },
            ])
        )
    )
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState(null)

    function toggleItem(id) {
        setSelections(prev => ({
            ...prev,
            [id]: { ...prev[id], checked: !prev[id].checked },
        }))
    }

    function setQty(id, val) {
        setSelections(prev => ({
            ...prev,
            [id]: { ...prev[id], qty: val },
        }))
    }

    async function handleSubmit() {
        setError(null)
        if (!reason.trim()) {
            setError('Please describe the reason for your return.')
            return
        }
        const items = (order.items ?? [])
            .filter(item => selections[item.orderItemId]?.checked)
            .map(item => ({
                orderItemId: item.orderItemId,
                quantity: Number(selections[item.orderItemId].qty),
            }))
        if (items.length === 0) {
            setError('Please select at least one item to return.')
            return
        }
        const invalidQty = items.find(i => {
            const orig = (order.items ?? []).find(oi => oi.orderItemId === i.orderItemId)
            return isNaN(i.quantity) || i.quantity < 1 || (orig && i.quantity > orig.quantity)
        })
        if (invalidQty) {
            setError('Quantity must be between 1 and the original ordered amount.')
            return
        }
        setSubmitting(true)
        try {
            await createReturn(token, order.id, { reason: reason.trim(), items })
            onSuccess()
        } catch (e) {
            setError(e.message)
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="order-detail__modal-overlay" onClick={onClose}>
            <div className="order-detail__modal" onClick={e => e.stopPropagation()}>
                <div className="order-detail__modal-header">
                    <h2 className="order-detail__modal-title">Request a Return</h2>
                    <button
                        className="order-detail__modal-close"
                        onClick={onClose}
                        aria-label="Close"
                    >✕</button>
                </div>

                {error && <div className="order-detail__modal-error">{error}</div>}

                <div className="order-detail__modal-body">
                    <label className="order-detail__modal-label">
                        Reason <span aria-hidden="true">*</span>
                    </label>
                    <textarea
                        className="order-detail__modal-textarea"
                        placeholder="Describe why you're returning…"
                        value={reason}
                        onChange={e => setReason(e.target.value)}
                        rows={3}
                    />

                    <div className="order-detail__modal-label order-detail__modal-label--mt">
                        Items to return <span aria-hidden="true">*</span>
                    </div>
                    <ul className="order-detail__return-items">
                        {(order.items ?? []).map(item => {
                            const sel = selections[item.orderItemId] ?? { checked: false, qty: item.quantity }
                            return (
                                <li key={item.orderItemId} className="order-detail__return-item">
                                    <label className="order-detail__return-check">
                                        <input
                                            type="checkbox"
                                            checked={sel.checked}
                                            onChange={() => toggleItem(item.orderItemId)}
                                        />
                                        <span>{item.productName}</span>
                                    </label>
                                    <div className="order-detail__return-qty">
                                        <span className="order-detail__return-qty-label">Qty:</span>
                                        <input
                                            type="number"
                                            className="order-detail__return-qty-input"
                                            min={1}
                                            max={item.quantity}
                                            value={sel.qty}
                                            onChange={e => setQty(item.orderItemId, e.target.value)}
                                            disabled={!sel.checked}
                                        />
                                        <span className="order-detail__return-qty-max">/ {item.quantity}</span>
                                    </div>
                                </li>
                            )
                        })}
                    </ul>
                </div>

                <div className="order-detail__modal-footer">
                    <button
                        className="order-detail__modal-cancel"
                        onClick={onClose}
                        disabled={submitting}
                    >Cancel</button>
                    <button
                        className="order-detail__modal-submit"
                        onClick={handleSubmit}
                        disabled={submitting}
                    >{submitting ? 'Submitting…' : 'Submit Return'}</button>
                </div>
            </div>
        </div>
    )
}

function  ReviewControls({orderItemId, token, productName, onChange}){
    const [state, setState] = useState({loading:true, eligible:false,
        reason:null, existing:null, error:null })
    const [editing, setEditing] = useState(false)
    const [rating, setRating] = useState(5)
    const [comment,setComment] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [submitError, setSubmitError] = useState(null)

    async function refresh(){
        setState(s=>({...s, loading: true, error: null}))
        try{
            const r = await getEligibility(token, orderItemId)
            setState({loading: false, eligible: r.eligible, reason: r.reason, existing: r.existingReview, error: null})
        }catch (e){
            setState({loading: false, eligible: false, reason: null, existing: null, error: e.message})
        }
    }

    useEffect(()=>{refresh()}, [orderItemId])

    function startEdit(){
        const ex = state.existing
        if(ex){
            setRating(ex.rating)
            setComment(ex.comment ?? '')
        }else{
            setRating(5)
            setComment('')
        }
        setSubmitError(null)
        setEditing(true)
    }

    async function submit(){
        setSubmitError(null)
        const trimmed = (comment ?? '').trim()
        if(trimmed.length < 10) {setSubmitError('Comment must be at least 10 characters.'); return}
        setSubmitting(true)
        try {
            const ex = state.existing
            if(ex && !ex.deletedAt){
                await  updateReview(token, ex.id, {rating, comment: trimmed})
            }else{
                //create OR revive(server decides)
                await createReview(token, orderItemId, {rating, comment:trimmed})
            }
            setEditing(false)
            await refresh()
            if(onChange) onChange()
        }catch (e){
            setSubmitError(e.message)
        }finally {
            setSubmitting(false)
        }
    }

    async function handleDelete(){
        if (!state.existing || state.existing.deletedAt) return
        if(!window.confirm("Delete your review?")) return
        try {
            await deleteReview(token, state.existing.id)
            await refresh()
            if(onChange) onChange()
        } catch (e){
            setState(s=>({...s, error: e.message}))
        }
    }

    if(state.loading) return <div className="order-detail__review-loading">Loading...</div>
    if(state.error) return  <div className="order-detail__review-error">{state.error}</div>

    const ex = state.existing
    if(ex && ex.deletedByAdmin){
        return <div className="order-detail__review-hidden">Your review for {productName} was hidden by an admin.</div>
    }
    if(editing){
        return (
            <div className="order-detail__review-form">
                <div className="order-detail__review-rating">
                    {[1,2,3,4,5].map(n=>(
                        <button
                            key={n}
                            type="button"
                            className={`order-detail__review-star${n<= rating ? ' order-detail__review-star--on': ''}`}
                            onClick={() => setRating(n)}
                            aria-label={`${n} star`}
                        >★</button>
                    ))}
                </div>
                <textarea
                    className="order-detail__review-textarea"
                    value={comment}
                    onChange={e=>setComment(e.target.value)}
                    maxLength={1000}
                    rows={3}
                    placeholder="Share your thoughts (10-1000 characters)..."
                />
                <div className="order-detail__review-counter">{(comment?? '').length}/1000</div>
                {submitError && <div className="order-detail__review-error">{submitError}</div> }
                <div className="order-detail__review-actions">
                    <button onClick={()=> setEditing(false)} disabled={submitting}>Cancel</button>
                    <button onClick={submit} disabled={submitting}>{submitting ? 'Saving...' : 'Submit'}</button>
                </div>
            </div>
        )
    }

    if(ex && !ex.deletedAt){
        return (
            <div className="order-detail__review-existing">
                <div className="order-detail__review-existing-head">
                    <Stars rating={ex.rating}/>
                    <span className="order-detail__review-existing-label">Your review</span>
                </div>
                <p className="order-detail__review-existing-comment">{ex.comment}</p>
                <div className="order-detail__review-actions">
                    <button onClick={startEdit}>Edit your review</button>
                    <button onClick={handleDelete}>Delete</button>
                </div>
            </div>
        )
    }

    // No existing visible review (none, or user-deleted that we'll revive)
    return (
        <div className="order-detail__review-actions">
            <button onClick={startEdit}>Write a review</button>
        </div>
    )
}

export default function OrderDetailPage({ onNeedAuth, userName }) {
    const { orderId } = useParams()
    const [order, setOrder] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const token = localStorage.getItem('token')
    const [modalOpen, setModalOpen] = useState(false)
    const [chatLoading, setChatLoading] = useState(false)
    const navigate = useNavigate()

    async function loadOrder(id) {
        if (!token) {
            setOrder(null)
            setLoading(false)
            return
        }
        if (!id) {
            setLoading(false)
            return
        }
        setLoading(true)
        setError(null)
        try {
            const currentOrder = await getOrder(token, id)
            setOrder(currentOrder)
        } catch (e) {
            setError(e.message ?? 'Failed to load the order')
        } finally {
            setLoading(false)
        }
    }

    async function handleContactSupportAboutOrder() {
        if (!token) {
            onNeedAuth?.()
            return
        }
        setChatLoading(true)
        try {
            const conv = await createConversation(token, {
                type: 'ORDER',
                orderId: Number(orderId),
            })
            navigate(`/chat/${conv.id}`, { state: { conversation: conv } })
        } catch (e) {
            setError(e.message ?? 'Could not start chat about this order.')
        } finally {
            setChatLoading(false)
        }
    }

    useEffect(() => {
        loadOrder(orderId)
    }, [orderId, userName])

    if (!token) {
        return (
            <div className="order-detail-page">
                <div className="order-detail-auth">
                    <p className="order-detail-auth__msg">Please sign in to view this order.</p>
                    <button type="button" className="order-detail-auth__btn" onClick={onNeedAuth}>
                        Sign In
                    </button>
                    <Link to="/" className="order-detail-auth__link">
                        ← Continue Shopping
                    </Link>
                </div>
            </div>
        )
    }

    if (loading) {
        return (
            <div className="order-detail-page">
                <div className="order-detail-skeleton">
                    <div className="order-detail-skeleton__shimmer order-detail-skeleton__title" />
                    <div className="order-detail-skeleton__grid">
                        <div className="order-detail-skeleton__shimmer order-detail-skeleton__line" />
                        <div className="order-detail-skeleton__shimmer order-detail-skeleton__line" />
                    </div>
                    <div className="order-detail-skeleton__shimmer order-detail-skeleton__block" />
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="order-detail-page">
                <Link to="/orders" className="order-detail__back">
                    ← Your orders
                </Link>
                <div className="order-detail-error" role="alert">
                    {error}
                </div>
            </div>
        )
    }

    if (!order) {
        return (
            <div className="order-detail-page">
                <Link to="/orders" className="order-detail__back">
                    ← Your orders
                </Link>
                <p className="order-detail-notfound">Order not found.</p>
            </div>
        )
    }

    const items = order.items ?? []
    const returnRequests = order.returnRequests ?? []

    let returnBadgeText = null
    let returnBadgeClass = null

    if (returnRequests.some((rr) => rr.status === 'REQUESTED')) {
        returnBadgeText = 'Return Requested'
        returnBadgeClass = 'order-detail__badge--return-requested'
    } else if (returnRequests.some((rr) => rr.status === 'APPROVED')) {
        returnBadgeText = 'Return Approved'
        returnBadgeClass = 'order-detail__badge--return-approved'
    } else if (returnRequests.some((rr) => rr.status === 'REFUNDED')) {
        returnBadgeText = 'Refunded'
        returnBadgeClass = 'order-detail__badge--return-refunded'
    } else if (returnRequests.some((rr) => rr.status === 'REJECTED')) {
        returnBadgeText = 'Return Rejected'
        returnBadgeClass = 'order-detail__badge--return-rejected'
    }

    const returnRows = returnRequests.flatMap((rr) => {
        let badgeMod, statusLabel, details
        if (rr.status === 'REQUESTED') { badgeMod = 'return-requested';  statusLabel = 'Requested' }
        if (rr.status === 'APPROVED')  { badgeMod = 'return-approved'; statusLabel = 'Approved' }
        if (rr.status === 'REFUNDED')  { badgeMod = 'return-refunded'; statusLabel = 'Refunded'}
        if (rr.status === 'REJECTED')  { badgeMod = 'return-rejected'; statusLabel = 'Rejected' }

        details = rr.status === 'REFUNDED'
            ? `Refunded on ${formatDate(rr.resolvedAt)}`
            : rr.status === 'APPROVED'
            ? `Approved on ${formatDate(rr.resolvedAt)}`
            : rr.status === 'REJECTED'
            ? (rr.reason ?? '—')
            : `Reason: ${rr.reason ?? '—'}`

        return (rr.items ?? []).map((ri, j) => (
            <tr key={`${rr.id}-${ri.orderItemId}`}>
                <td className="order-detail-table__name">{ri.productName}</td>
                <td className="order-detail-table__num">{ri.quantity}</td>
                <td className="order-detail-table__num">
                  <span className={`order-detail__badge order-detail__badge--${badgeMod}`}>
                      {statusLabel}
                  </span>
                </td>
                <td className="order-detail-table__num">{j === 0 ? details : ''}</td>
            </tr>
        ))
    })
    return (
        <div className="order-detail-page">
            <Link to="/orders" className="order-detail__back">
                ← Your orders
            </Link>

            <header className="order-detail__header">
                <div>
                    <h1 className="order-detail__title">Order #{order.id}</h1>
                    <p className="order-detail__placed">Placed on {formatDate(order.createdAt)}</p>
                </div>
                <div className="order-detail__header-right">
                    <div className="order-detail__header-actions">
                        {returnBadgeText !== 'Refunded' && (
                            <span className={statusClass(order.status)}>{order.status}</span>
                        )}
                                            {returnBadgeText && (
                                                <span className={`order-detail__badge ${returnBadgeClass}`}>
                                                    {returnBadgeText}
                                                </span>
                                            )}
                                            {order.status === 'PENDING' && (
                                                <button
                                                    className="order-detail__return-btn"
                                                    onClick={() => navigate(`/payment/${order.id}`)}
                                                >
                                                    Pay Now
                                                </button>
                                            )}
                                            {['PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED'].includes(order.status) &&
                                                !(order.returnRequests ?? []).some((rr) => rr.status === 'REQUESTED') &&
                                                hasReturnableItems(order) && (
                                                <button
                                                    className="order-detail__return-btn"
                                                    onClick={() => setModalOpen(true)}
                                                >
                                                    ↩ Return
                                                </button>
                                            )}
                                            <button
                                                type="button"
                                                className="order-detail__return-btn order-detail__chat-btn"
                                                disabled={chatLoading}
                                                onClick={handleContactSupportAboutOrder}
                                            >
                                                {chatLoading ? 'Starting chat...' : 'Contact support'}
                                            </button>
                    </div>
                    {(order.status === 'CLOSED' || order.status === 'CANCELLED') && (
                        <p className="order-detail__status-hint" role="status">
                            {order.status === 'CLOSED'
                                ? 'This order is closed.'
                                : 'This order was cancelled.'}
                        </p>
                    )}
                </div>
            </header>

            <section className="order-detail-summary" aria-labelledby="order-summary-heading">
                <h2 id="order-summary-heading" className="order-detail-summary__title">
                    Order summary
                </h2>
                <dl className="order-detail-summary__dl">
                    <div className="order-detail-summary__row">
                        <dt>Order total</dt>
                        <dd>{formatMoney(order.totalAmount)}</dd>
                    </div>
                </dl>
            </section>

            <section className="order-detail-items" aria-labelledby="order-items-heading">
                <h2 id="order-items-heading" className="order-detail-items__title">
                    Items ({items.length})
                </h2>
                {items.length === 0 ? (
                    <p className="order-detail-items__empty">No line items for this order.</p>
                ) : (
                    <div className="order-detail-table-wrap">
                        <table className="order-detail-table">
                            <thead>
                                <tr>
                                    <th scope="col">Product</th>
                                    <th scope="col" className="order-detail-table__num">
                                        Unit price
                                    </th>
                                    <th scope="col" className="order-detail-table__num">
                                        Qty
                                    </th>
                                    <th scope="col" className="order-detail-table__num">
                                        Subtotal
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {items.map((line) => (
                                    <>
                                        <tr key={`item-${line.orderItemId}`}>
                                            <td className="order-detail-table__name">{line.productName}</td>
                                            <td className="order-detail-table__num">{formatMoney(line.unitPrice)}</td>
                                            <td className="order-detail-table__num">{line.quantity}</td>
                                            <td className="order-detail-table__num order-detail-table__subtotal">
                                                {formatMoney(line.subtotal)}
                                            </td>
                                        </tr>
                                        {(order.status === 'DELIVERED' || order.status === 'CLOSED') && (
                                            <tr key={`review-${line.orderItemId}`} className="order-detail__review-row">
                                                <td colSpan={4}>
                                                    <ReviewControls
                                                        orderItemId={line.orderItemId}
                                                        token={token}
                                                        productName={line.productName}
                                                        onChange={() => {}}
                                                    />
                                                </td>
                                            </tr>
                                        )}
                                    </>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
            {(order.returnRequests ?? []).length > 0 && (
                <section className="order-detail-items" aria-labelledby="returns-heading">
                    <h2 id="returns-heading" className="order-detail-items__title">
                        Returns ({order.returnRequests.length})
                    </h2>
                    <div className="order-detail-table-wrap">
                        <table className="order-detail-table">
                            <thead>
                            <tr>
                                <th scope="col">Product</th>
                                <th scope="col" className="order-detail-table__num">Returned Qty</th>
                                <th scope="col" className="order-detail-table__num">Status</th>
                                <th scope="col" className="order-detail-table__num">Details</th>
                            </tr>
                            </thead>
                            <tbody>{returnRows}</tbody>
                        </table>
                    </div>
                </section>
            )}
{/*         提交 return 成功后刷新当前订单数据 */}
            {modalOpen && (
                <ReturnModal
                  order={order}
                  token={token}
                  onSuccess={async () => {
                    setModalOpen(false)
                    await loadOrder(orderId)
                  }}
                  onClose={() => setModalOpen(false)}
                />
            )}
        </div>
    )
}
