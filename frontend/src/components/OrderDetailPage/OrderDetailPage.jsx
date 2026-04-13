// 对应 /orders/:orderId（单条订单详情，显示单条订单及 items。）

import { Link, useParams } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { getOrder } from '../../api/orders.js'
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
    if (s === 'PAID') return 'order-detail__badge order-detail__badge--paid'
    if (s === 'CANCELLED') return 'order-detail__badge order-detail__badge--cancelled'
    return 'order-detail__badge order-detail__badge--pending'
}

export default function OrderDetailPage({ onNeedAuth, userName }) {
    const { orderId } = useParams()
    const [order, setOrder] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const token = localStorage.getItem('token')

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
                <span className={statusClass(order.status)}>{order.status}</span>
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
                                    <tr key={`${line.productId}-${line.productName}`}>
                                        <td className="order-detail-table__name">{line.productName}</td>
                                        <td className="order-detail-table__num">{formatMoney(line.unitPrice)}</td>
                                        <td className="order-detail-table__num">{line.quantity}</td>
                                        <td className="order-detail-table__num order-detail-table__subtotal">
                                            {formatMoney(line.subtotal)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    )
}
