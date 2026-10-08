import { useEffect, useState } from 'react'
import { api } from './api.js'

const NAMES = { vendors: 'Mfanyabiashara', stalls: 'Stall', licenses: 'Leseni', allocations: 'Ugawaji', announcements: 'Tangazo' }

export default function RecycleBin({ onChanged }) {
  const [rows, setRows] = useState([])
  const [notice, setNotice] = useState(null)
  const load = () => api('/recycle-bin').then(setRows).catch((e) => setNotice({ type: 'error', text: e.message }))
  useEffect(() => { load() }, [])

  async function act(row, kind) {
    if (kind === 'purge' && !window.confirm('Ondoa kabisa? Hatua hii haiwezi kurudishwa.')) return
    try {
      const path = `/recycle-bin/${row.module}/${row.id}`
      const r = await api(kind === 'restore' ? path + '/restore' : path, { method: kind === 'restore' ? 'POST' : 'DELETE' })
      setNotice({ type: 'ok', text: r.message }); load(); onChanged?.()
    } catch (e) { setNotice({ type: 'error', text: e.message }) }
  }

  return (
    <>
      <h2>Recycle Bin</h2>
      {notice && <div className={`alert ${notice.type}`}>{notice.text}</div>}
      <div className="table-wrap">
        <table>
          <thead><tr><th>Aina</th><th>Jina / Namba</th><th>Ilifutwa</th><th /></tr></thead>
          <tbody>
            {rows.length === 0 && <tr><td colSpan="4" className="muted">Recycle Bin ni tupu.</td></tr>}
            {rows.map((r) => (
              <tr key={r.module + r.id}>
                <td>{NAMES[r.module]}</td><td>{r.label}</td><td>{String(r.deleted_at || '').replace('T', ' ').slice(0, 16)}</td>
                <td className="row-actions">
                  <button className="btn ghost sm" onClick={() => act(r, 'restore')}>Rejesha</button>
                  <button className="btn danger sm" onClick={() => act(r, 'purge')}>Ondoa kabisa</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
