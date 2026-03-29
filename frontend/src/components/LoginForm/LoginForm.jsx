import { useState } from 'react'
import { login } from '../../api/auth.js'

export default function LoginForm({onMessage}){
    const [username,setUsername] = useState('')
    const [password, setPassword] = useState('')

    async function handleLogin(e) {
        e.preventDefault()
        try {
            const data = await login({ username, password })
            onMessage(JSON.stringify(data, null, 2))
            if (data?.token) {
                localStorage.setItem('token', data.token)
            }
        } catch (err) {
            onMessage(err.message)
        }
    }

    return (
        <section>
            <h2>登录</h2>
            <form onSubmit={handleLogin}>
                <div>
                    <input
                        type="text"
                        placeholder="username"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />
                </div>
                <div>
                    <input
                        type="password"
                        placeholder="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                    />
                </div>
                <button type="submit">登录</button>
            </form>
        </section>
    )
}