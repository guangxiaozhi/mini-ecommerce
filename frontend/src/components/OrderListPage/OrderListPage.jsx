// 对应 /orders（我的订单列表，显示当前用户的订单列表（JSON 里的 id、totalAmount、status、createdAt 等）；）

import { useState, useEffect } from 'react'
import {listOrders} from "../../api/orders.js";
import {Link} from "react-router-dom";

export default function OrderListPage({onNeedAuth, userName}){
    const [orders, setOrders] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const token = localStorage.getItem('token')

    async function loadOrders(){
        if(!token){
            setOrders([])
            setLoading(false)
            return
        }
        setLoading(true)
        setError(null)
        try{
            const ordersList = await listOrders(token)
            setOrders(ordersList)
        } catch (e) {
            setError(e.message ?? "Failed to load the orders")
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {loadOrders()}, [userName])

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
        <ul>
            {orders.map(o =>(
                <li key={o.id}>
                    <Link to={`/orders/${o.id}`}>
                        # {o.id} — {o.totalAmount} — {o.status}
                    </Link>

                </li>
            ))}
        </ul>
    )
}