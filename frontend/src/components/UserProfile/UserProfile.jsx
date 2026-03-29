import { useState, useEffect } from 'react'
import { getMe } from '../../api/auth'

export default function UserProfile({ onMessage }) {
    const [profile, setProfile] = useState(null)
    const [loading, setLoading] = useState(false)

    async function loadProfile() {
        const token = localStorage.getItem('token')
        if (!token) {
            const text = '请先登录（localStorage 里没有 token）'
            setProfile(null)
            onMessage?.(text)
            return
        }
        setLoading(true)
        try {
            const data = await getMe(token)
            setProfile(data)
            // onMessage?.(JSON.stringify(data, null, 2))
        } catch (err) {
            setProfile(null)
            onMessage?.(err.message)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadProfile()
    }, [])

    return (
        <section>
            <h2>用户资料</h2>
            <button type="button" onClick={loadProfile} disabled={loading}>
                {loading ? '加载中…' : '刷新资料'}
            </button>

            {profile && (
                <dl>
                    <dt>用户名</dt>
                    <dd>{profile.username}</dd>
                    <dt>权限</dt>
                    <dd>{profile.authorities}</dd>
                </dl>
            )}
        </section>
    )
}