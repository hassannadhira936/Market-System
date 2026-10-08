import { useEffect, useState } from 'react'
import { api, money } from './api.js'

export default function Dashboard() {
  const [d, setD] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { api('/dashboard').then(setD).catch((e) => setError(e.message)) }, [])

  if (error) return <div className="alert error">{error}</div>
  if (!d) return <p className="muted">Inapakia…</p>

  const max = Math.max(1, ...d.monthlyRevenue.map((m) => Number(m.total)))
  const stats = [
    ['Wafanyabiashara', d.vendors], ['Aina za biashara', d.vendorTypes], ['Stalls zote', d.stalls],
    ['Zilizochukuliwa', d.occupied], ['Zilizo wazi', d.vacant], ['Leseni Active', d.activeLicenses],
  ]

  return (
    <>
      <h2>Dashboard</h2>
      <section className="hero">
        <div>
          <div className="hero-label">Mapato yaliyolipwa</div>
          <div className="hero-num">{money(d.revenue)}</div>
          <div className="muted">Malipo {d.payments} yamerekodiwa</div>
        </div>
        <div className="bars" aria-label="Mapato ya miezi 6 iliyopita">
          {d.monthlyRevenue.length === 0 && <span className="muted">Hakuna malipo bado.</span>}
          {d.monthlyRevenue.map((m) => (
            <div key={m.month} className="bar" title={`${m.month}: ${money(m.total)}`}>
              <div style={{ height: `${(Number(m.total) / max) * 100}%` }} />
              <span>{m.month.slice(5)}</span>
            </div>
          ))}
        </div>
      </section>
      {d.expiringLicenses > 0 && (
        <div className="alert warn">Leseni {d.expiringLicenses} zinaisha ndani ya siku 30. Fungua ukurasa wa Leseni kuzihakiki.</div>
      )}
      <section className="stats">
        {stats.map(([label, n]) => (
          <div key={label} className="stat"><b>{n}</b><span>{label}</span></div>
        ))}
      </section>
    </>
  )
}
