// 对应 /orders/:orderId（单条订单详情，显示单条订单及 items。）

import {Link, useParams} from 'react-router-dom'
import { useState, useEffect} from "react";
import { getOrder} from "../../api/orders.js";

export default function OrderDetailPage({onNeedAuth, userName}){
    // 上面两行也可以替代第三行
    // const params = useParams()
    // const orderId = params.orderId
    const {orderId} = useParams()
    const [order, setOrder] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const token = localStorage.getItem('token')

    async function loadOrder(orderId){
        if(!token){
            setOrder([])
            setLoading(false)
            return
        }
        if(!orderId){
            setLoading(false)
            return
        }
        setLoading(true)
        setError(null)
        try{
            const currentOrder = await getOrder(token, orderId)
            setOrder(currentOrder)
        } catch(e){
            setError(e.message ?? 'Failed to load the order')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadOrder(orderId)
    }, [orderId, userName]);


    // ── No token ──
    if (!token){
        return (
            <div>
                <p>Please sign in to view your orders.</p>
                <button onClick={onNeedAuth}>Sign In</button>
                <Link to="/">← Continue Shopping</Link>
            </div>

        )
    }

    if(loading) return <div>loading</div>
    if (error) return <div>{error}</div>

    return (
        <div className="order-detail">
            <h1>订单详情</h1>
            <dl>
                <dt>订单号</dt>
                <dd>{order.id}</dd>
                <dt>总金额</dt>
                <dd>{order.totalAmount}</dd>
                <dt>状态</dt>
                <dd>{order.status}</dd>
                <dt>下单时间</dt>
                <dd>{order.createdAt}</dd>
            </dl>
            <h2>商品明细</h2>
            <ul>
                {(order.items ?? []).map((line) => (
                    <li key={line.productId}>
                        {line.productName} × {line.quantity} — 单价 {line.unitPrice} — 小计 {line.subtotal}
                    </li>
                ))}
            </ul>
        </div>
    )
}