import { useEffect, useState } from 'react'
import { api, setToken, setOnUnauthorized } from './api.js'
import { MODULES } from './modules.js'
import Dashboard from './Dashboard.jsx'
import CrudPage from './CrudPage.jsx'
import RecycleBin from './RecycleBin.jsx'

function Login({ onLogin }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      const data = await api('/auth/login', { method: 'POST', body: form })
      setToken(data.token)
      localStorage.setItem('ms_user', JSON.stringify(data.user))
      onLogin(data.user)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  return (
    <main className="login">
      <form onSubmit={submit} className="login-card">
        <h1>Zanzibar Fresh Market Authority</h1>
        <p>Mfumo wa leseni za wafanyabiashara na ugawaji wa stalls</p>
        {error && <div className="alert error" role="alert">{error}</div>}
        <label>Username
          <input value={form.username} autoFocus autoComplete="username"
            onChange={(e) => setForm({ ...form, username: e.target.value })} />
        </label>
        <label>Password
          <input type="password" value={form.password} autoComplete="current-password"
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </label>
        <button className="btn primary" disabled={busy}>{busy ? 'Inaingia…' : 'Ingia'}</button>
      </form>
    </main>
  )
}

export default function App() {
  const [user, setUser] = useState(() => {
    try { return localStorage.getItem('ms_token') ? JSON.parse(localStorage.getItem('ms_user')) : null } catch { return null }
  })
  const [page, setPage] = useState('dashboard')
  const [lookups, setLookups] = useState(null)

  const logout = () => {
    setToken(''); localStorage.removeItem('ms_user'); setUser(null); setLookups(null); setPage('dashboard')
  }
  useEffect(() => { setOnUnauthorized(logout) }, [])
  const loadLookups = () => api('/lookups').then(setLookups).catch(() => {})
  useEffect(() => { if (user) loadLookups() }, [user])

  if (!user) return <Login onLogin={setUser} />
  const isAdmin = user.roleId === 1
  const current = MODULES.find((m) => m.key === page)

  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">Zanzibar Fresh<br />Market Authority</div>
        <nav>
          <button className={page === 'dashboard' ? 'on' : ''} onClick={() => setPage('dashboard')}>Dashboard</button>
          {MODULES.map((m) => (
            <button key={m.key} className={page === m.key ? 'on' : ''} onClick={() => setPage(m.key)}>{m.label}</button>
          ))}
          {isAdmin && <button className={page === 'bin' ? 'on' : ''} onClick={() => setPage('bin')}>Recycle Bin</button>}
        </nav>
        <div className="me">
          <span>{user.name || user.username}</span>
          <button className="btn ghost" onClick={logout}>Toka</button>
        </div>
      </aside>
      <main className="main">
        {page === 'dashboard' && <Dashboard />}
        {current && <CrudPage key={current.key} mod={current} lookups={lookups} isAdmin={isAdmin} onChanged={loadLookups} />}
        {page === 'bin' && isAdmin && <RecycleBin onChanged={loadLookups} />}
      </main>
    </div>
  )
}
