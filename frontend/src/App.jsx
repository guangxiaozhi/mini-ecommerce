import { useState } from 'react'
import { Routes, Route } from 'react-router-dom'
import './App.css'
import UserProfile from './components/UserProfile/UserProfile.jsx'
import Header from './components/Header/Header.jsx'
import AuthModal from './components/AuthModal/AuthModal.jsx'

function App() {
  const [msg, setMsg] = useState('')
  const [authOpen, setAuthOpen] = useState(false)
  const [userName, setUserName] = useState(
    () => localStorage.getItem('username') || null
  )

  function handleLoggedIn(name) {
    if (name) {
      localStorage.setItem('username', name)
      setUserName(name)
    }
    setAuthOpen(false)
  }

  function handleLogout(){
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      setUserName(null)
  }

  return (
    <>
      <Header
        onOpenAuth={() => setAuthOpen(true)}
        userName={userName}
        onLogout={handleLogout}
      />
      <AuthModal
        open={authOpen}
        onClose={() => setAuthOpen(false)}
        onMessage={setMsg}
        onLoggedIn={handleLoggedIn}
      />
        <Routes>
            <Route
                path='/'
                element={
                    <main>
                        <p>Products Coming Soon...</p>
                        <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
              {msg}
            </pre>
                    </main>
                }
            />
            <Route
                path="/profile"
                element={<UserProfile onMessage={setMsg} />}
            />
        </Routes>

    </>
  )
}

export default App
