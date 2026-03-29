import { useState } from 'react'
import './App.css'
import RegisterForm from "./components/RegisterForm/RegisterForm.jsx";
import LoginForm from "./components/LoginForm/LoginForm.jsx";
import UserProfile from "./components/UserProfile/UserProfile.jsx";

function App() {
  const [msg, setMsg] = useState('')

  return (
    <>

      <RegisterForm onMessage={setMsg} />

      <LoginForm onMessage={setMsg} />

      <UserProfile onMessage={setMsg} />
      <pre
        style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}
      >
        {msg}
      </pre>
    </>
  )
}

export default App
