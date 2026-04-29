  import { useEffect, useState, useMemo } from 'react'
  import { adminListProducts } from '../../api/adminProducts'
  import {adminListUsers} from "../../api/adminUsers.js";
  import {adminGetAnalytics, adminListOrders} from "../../api/adminOrders.js";
  import {adminListInventory} from "../../api/adminInventory.js";
  import {PieChart, Pie, Cell, Tooltip, BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Legend} from "recharts";
  import './AdminDashboard.css'
  import  {useNavigate} from "react-router-dom";

  export default function AdminDashboard() {
      const [productCount, setProductCount] = useState('...')
      const [userCount, setUserCount] = useState('...')
      const [orderCount, setOrderCount] = useState('...')
      const [inventoryCount, setInventoryCount] = useState('...')
      const [productChartData, setProductChartData] = useState([])
      const [inventoryChartData, setInventoryChartData] = useState([])
      const [userChartData, setUserChartData] = useState([])
      const [orderChartData, serOrderChartData] = useState([])
      const token = useMemo(() => localStorage.getItem('token'), [])
      const navigate = useNavigate()

      useEffect(() => {
          if (!token) return
          adminListProducts(token)
              .then(data => {
                  if (!Array.isArray(data)) return
                  setProductCount(data.length)
                  const active = data.filter(p=> p.active).length
                  const inactive = data.length - active
                  setProductChartData([
                      {name: 'Active', value: active},
                      {name: 'Inactive', value: inactive},
                  ])
              })
              .catch(() => setProductCount('?'))

          adminListUsers(token, {})
              .then(data=>{
                  if(!Array.isArray(data)) return
                  setUserCount(data.length)
                  const counts = {}
                  const today = new Date()
                  for (let i = 6; i >= 0; i--) {
                      const d = new Date(today)
                      d.setDate(d.getDate() - i)
                      const key = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
                      counts[key] = 0
                  }
                  data.forEach(u => {
                      if (!u.createdAt) return
                      const d = new Date(u.createdAt)
                      const key = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
                      if (key in counts) counts[key]++
                  })
                  setUserChartData(Object.entries(counts).map(([date, count]) => ({ date, count })))
              })
              .catch(() => setUserCount('?'))

          adminListOrders(token, {page:0, size:1})
              .then(data => setOrderCount(data ?.totalElements ?? 0))
              .catch(() => setOrderCount('?'))
          const now = new Date()
          const from = new Date(now)
          from.setFullYear(from.getFullYear() - 1)
          adminGetAnalytics(token, from.toISOString().slice(0, 10), now.toISOString().slice(0, 10))
              .then(data =>{
                  if(!data?.ordersByStatus) return
                  serOrderChartData(
                      Object.entries(data.ordersByStatus).map(([status, count]) =>({name:status, value: Number(count)}))
                  )
              })
              .catch(() =>{})

          adminListInventory(token)
              .then(data =>{
                  if (!Array.isArray(data)) return
                  const total = data.reduce((sum, item) => sum + item.onHandQty, 0)
                  setInventoryCount(total)
                  const chartData = [...data]
                      .sort((a, b) => b.onHandQty - a.onHandQty)
                      .slice(0, 10)
                      .map(item =>({name: item.productName, qty: item.onHandQty
                      }))
                  setInventoryChartData(chartData)
              })
              .catch(() => setInventoryCount('?'))
      }, [token])

      const cards = [
          { label: 'Products', value: productCount, icon: '🛍️', color: '#5c6bc0', link: '/admin/products'},
          { label: 'Units in Stock',value: inventoryCount,icon: '📦',color: '#8e24aa',link: '/admin/inventory'},
          { label: 'Users',    value: userCount,    icon: '👥', color: '#26a69a', link: '/admin/users'},
          { label: 'Orders',   value: orderCount,   icon: '🧾', color: '#ef6c00', link: '/admin/orders' }
      ]

      return (
          <div>
              <h1 className="ad-title">Dashboard</h1>

              <div className="ad-cards">
                  {cards.map(card => (
                      <div key={card.label} className="ad-card"
                           onClick={() => navigate(card.link)}
                           style={{cursor: 'pointer'}}
                      >
                          <div className="ad-card__icon" style={{ color: card.color }}>
                              {card.icon}
                          </div>
                          <div className="ad-card__info">
                              <div className="ad-card__value">{card.value}</div>
                              <div className="ad-card__label">{card.label}</div>
                          </div>
                      </div>
                  ))}
              </div>

              <div className="ad-charts">
                  <div className="ad-chart-box">
                      <h3 className="ad-chart-title">Products</h3>
                      <ResponsiveContainer width="100%" height={220}>
                          <PieChart>
                              <Pie data={productChartData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                                  <Cell fill="#5c6bc0" />
                                  <Cell fill="#b0bec5" />
                              </Pie>
                              <Tooltip />
                              <Legend />
                          </PieChart>
                      </ResponsiveContainer>
                  </div>

                  <div className="ad-chart-box">
                      <h3 className="ad-chart-title">New Users (last 7 days)</h3>
                      <ResponsiveContainer width="100%" height={220}>
                          <BarChart data={userChartData}>
                              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                              <YAxis allowDecimals={false} />
                              <Tooltip />
                              <Bar dataKey="count" fill="#26a69a" radius={[4, 4, 0, 0]} />
                          </BarChart>
                      </ResponsiveContainer>
                  </div>

                  <div className="ad-chart-box">
                      <h3 className="ad-chart-title">Orders by Status</h3>
                      <ResponsiveContainer width="100%" height={220}>
                          <PieChart>
                              <Pie data={orderChartData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                                  {orderChartData.map((_, i) => (
                                      <Cell key={i} fill={['#ef6c00','#5c6bc0','#26a69a','#e53935','#8e24aa','#00897b','#f9a825'][i % 7]}
                                      />
                                  ))}
                              </Pie>
                              <Tooltip />
                              <Legend />
                          </PieChart>
                      </ResponsiveContainer>
                  </div>

                  <div className="ad-chart-box">
                      <h3 className="ad-chart-title">Inventory Stock Levels</h3>
                      <ResponsiveContainer width="100%" height={220}>
                          <BarChart data={inventoryChartData}>
                              <XAxis dataKey="name" tick={{fontSize:10}} interval={0}/>
                              <YAxis allowDecimals={false}/>
                              <Tooltip/>
                              <Bar dataKey="qty" fill="#ef6c00" radius={[4,4,0,0]}/>
                          </BarChart>
                      </ResponsiveContainer>
                  </div>
              </div>
          </div>
      )
  }
