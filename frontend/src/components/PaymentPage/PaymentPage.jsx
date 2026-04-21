import { useState, useEffect } from 'react'
  import { useParams, useNavigate, Link, useLocation } from 'react-router-dom'
  import { getOrder, payOrder } from '../../api/orders.js'

  function formatMoney(n) {
      if (n == null) return '—'
      return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(n))
  }

  export default function PaymentPage({ onNeedAuth }) {
      const { orderId } = useParams()
      const navigate = useNavigate()
      const token = localStorage.getItem('token')
      const [order, setOrder] = useState(null)
      const [loading, setLoading] = useState(true)
      const [error, setError] = useState(null)
      const [paying, setPaying] = useState(false)
      const location = useLocation()
      const justPaid = location.state?.paid === true

      useEffect(() => {
          if (!token) return
          setLoading(true)
          getOrder(token, orderId)
              .then(o => {
                  if (o.status !== 'PENDING') {
                      navigate(`/orders/${orderId}`, { replace: true })
                      return
                  }
                  setOrder(o)
              })
              .catch(e => setError(e.message))
              .finally(() => setLoading(false))
      }, [orderId])

      async function handlePay() {
          setPaying(true)
          setError(null)
          try {
              await payOrder(token, orderId)
              navigate(`/orders/${orderId}`, { state: { paid: true }, replace: true })
          } catch (e) {
              setError(e.message)
          } finally {
              setPaying(false)
          }
      }

      if (!token) {
          return (
              <div style={{ padding: '2rem', textAlign: 'center' }}>
                  <p>Please sign in to complete payment.</p>
                  <button onClick={onNeedAuth}>Sign In</button>
              </div>
          )
      }

      if (loading) return <div style={{ padding: '2rem' }}>Loading order…</div>
      if (error)   return <div style={{ padding: '2rem', color: 'red' }}>{error}</div>
      if (!order)  return null

      const items = order.items ?? []

      return (
          <div style={{ maxWidth: 600, margin: '2rem auto', padding: '0 1rem' }}>
              <h1>Complete Payment</h1>
              <p style={{ color: '#666' }}>Order #{order.id}</p>

              <table style={{ width: '100%', borderCollapse: 'collapse', margin: '1rem 0' }}>
                  <thead>
                      <tr style={{ borderBottom: '2px solid #eee', textAlign: 'left' }}>
                          <th style={{ padding: '0.5rem' }}>Product</th>
                          <th style={{ padding: '0.5rem', textAlign: 'right' }}>Unit Price</th>
                          <th style={{ padding: '0.5rem', textAlign: 'right' }}>Qty</th>
                          <th style={{ padding: '0.5rem', textAlign: 'right' }}>Subtotal</th>
                      </tr>
                  </thead>
                  <tbody>
                      {items.map(line => (
                          <tr key={line.orderItemId} style={{ borderBottom: '1px solid #f0f0f0' }}>
                              <td style={{ padding: '0.5rem' }}>{line.productName}</td>
                              <td style={{ padding: '0.5rem', textAlign: 'right' }}>{formatMoney(line.unitPrice)}</td>
                              <td style={{ padding: '0.5rem', textAlign: 'right' }}>{line.quantity}</td>
                              <td style={{ padding: '0.5rem', textAlign: 'right' }}>{formatMoney(line.subtotal)}</td>
                          </tr>
                      ))}
                  </tbody>
              </table>

              <div style={{ textAlign: 'right', fontSize: '1.2rem', fontWeight: 'bold', marginBottom: '1.5rem' }}>
                  Total: {formatMoney(order.totalAmount)}
              </div>

              {error && <div style={{ color: 'red', marginBottom: '1rem' }}>{error}</div>}

              <button
                  onClick={handlePay}
                  disabled={paying}
                  style={{
                      width: '100%', padding: '0.85rem',
                      background: '#2563eb', color: '#fff',
                      border: 'none', borderRadius: 8,
                      fontSize: '1rem', cursor: paying ? 'not-allowed' : 'pointer',
                  }}
              >
                  {paying ? 'Processing…' : 'Confirm Payment'}
              </button>

              <Link to="/orders" style={{ display: 'block', marginTop: '1rem', textAlign: 'center', color: '#666' }}>
                  {justPaid && (
                      <div style={{ background: '#dcfce7', color: '#166534', padding: '0.75rem 1rem', borderRadius: 8, marginBottom:
                              '1rem' }}>
                          Payment successful! Your order is now being processed.
                      </div>
                  )}
                  ← Back to orders
              </Link>
          </div>
      )
  }