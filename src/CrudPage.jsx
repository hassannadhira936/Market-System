import { useCallback, useEffect, useState } from 'react'
import { api, money } from './api.js'

const fmtDate = (v) => (v ? String(v).slice(0, 10) : '')

function Form({ mod, lookups, row, onSaved, onCancel }) {
  const editing = !!row
  const [values, setValues] = useState(() => {
    const v = {}
    mod.fields.forEach((f) => { v[f.key] = editing ? (row[f.key] ?? '') : (f.initial ?? '') })
    mod.fields.filter((f) => f.type === 'date').forEach((f) => { if (editing) v[f.key] = fmtDate(row[f.key]) })
    return v
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await api(`/data/${mod.key}${editing ? '/' + row[mod.pk] : ''}`, { method: editing ? 'PUT' : 'POST', body: values })
      onSaved(editing ? 'Mabadiliko yamehifadhiwa.' : 'Imehifadhiwa kikamilifu.')
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const input = (f) => {
    const set = (val) => setValues({ ...values, [f.key]: val })
    const common = { value: values[f.key] ?? '', required: f.required, disabled: editing && f.lockOnEdit, onChange: (e) => set(e.target.value) }
    if (f.type === 'select') {
      const opts = f.options
        ? f.options
        : (lookups?.[f.lookup] || []).filter(f.filter || (() => true)).map((o) => ({ value: o.id, label: o.label }))
      return (
        <select {...common}>
          <option value="">— Chagua —</option>
          {opts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      )
    }
    if (f.type === 'textarea') return <textarea rows={3} {...common} />
    return <input type={f.type || 'text'} step={f.type === 'number' ? 'any' : undefined} {...common} />
  }

  return (
    <div className="overlay" onClick={onCancel}>
      <form className="modal" onSubmit={submit} onClick={(e) => e.stopPropagation()}>
        <h3>{editing ? `Hariri ${mod.single}` : `Ongeza ${mod.single}`}</h3>
        {error && <div className="alert error" role="alert">{error}</div>}
        <div className="grid">
          {mod.fields.map((f) => (
            <label key={f.key} className={f.type === 'textarea' ? 'wide' : ''}>{f.label}{f.required ? ' *' : ''}{input(f)}</label>
          ))}
        </div>
        <div className="actions">
          <button type="button" className="btn ghost" onClick={onCancel}>Ghairi</button>
          <button className="btn primary" disabled={busy}>{busy ? 'Inahifadhi…' : 'Hifadhi'}</button>
        </div>
      </form>
    </div>
  )
}

export default function CrudPage({ mod, lookups, isAdmin, onChanged }) {
  const [rows, setRows] = useState([])
  const [q, setQ] = useState('')
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(null) // null | {} (mpya) | row (hariri)
  const [notice, setNotice] = useState(null)

  const load = useCallback(async (search = '') => {
    setLoading(true)
    try { setRows(await api(`/data/${mod.key}?q=${encodeURIComponent(search)}`)) }
    catch (e) { setNotice({ type: 'error', text: e.message }) }
    finally { setLoading(false) }
  }, [mod.key])
  useEffect(() => { load() }, [load])

  async function remove(row) {
    const msg = mod.key === 'vendor-types' ? 'Futa kabisa aina hii?' : 'Hamisha kwenda Recycle Bin?'
    if (!window.confirm(msg)) return
    try {
      const r = await api(`/data/${mod.key}/${row[mod.pk]}`, { method: 'DELETE' })
      setNotice({ type: 'ok', text: r.message }); load(q); onChanged?.()
    } catch (e) { setNotice({ type: 'error', text: e.message }) }
  }

  const cell = (c, row) => {
    const v = row[c.key]
    if (c.money) return money(v)
    if (c.date) return fmtDate(v)
    if (c.badge) return <span className={`badge ${String(v || '').toLowerCase()}`}>{v}</span>
    if (c.clip) return <span className="clip">{v}</span>
    return v ?? ''
  }

  return (
    <>
      <div className="bar-top">
        <h2>{mod.label}</h2>
        <form className="search" onSubmit={(e) => { e.preventDefault(); load(q) }}>
          <input placeholder="Tafuta…" value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="btn ghost">Tafuta</button>
        </form>
        <button className="btn primary" onClick={() => setForm({})}>Ongeza {mod.single}</button>
      </div>
      {notice && <div className={`alert ${notice.type}`} role="status">{notice.text}</div>}
      <div className="table-wrap">
        <table>
          <thead><tr>{mod.columns.map((c) => <th key={c.key}>{c.label}</th>)}<th /></tr></thead>
          <tbody>
            {loading && <tr><td colSpan={mod.columns.length + 1} className="muted">Inapakia…</td></tr>}
            {!loading && rows.length === 0 && (
              <tr><td colSpan={mod.columns.length + 1} className="muted">Hakuna rekodi. Bonyeza “Ongeza {mod.single}” kuanza.</td></tr>
            )}
            {rows.map((r) => (
              <tr key={r[mod.pk]}>
                {mod.columns.map((c) => <td key={c.key}>{cell(c, r)}</td>)}
                <td className="row-actions">
                  <button className="btn ghost sm" onClick={() => setForm(r)}>Hariri</button>
                  {isAdmin && <button className="btn danger sm" onClick={() => remove(r)}>Futa</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {form && (
        <Form mod={mod} lookups={lookups} row={form[mod.pk] ? form : null}
          onCancel={() => setForm(null)}
          onSaved={(text) => { setForm(null); setNotice({ type: 'ok', text }); load(q); onChanged?.() }} />
      )}
    </>
  )
}
