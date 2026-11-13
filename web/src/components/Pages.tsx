import { useState } from 'react'
import type { FacebookPage } from '../types'

interface Props {
  state: { pages: FacebookPage[] }
  updatePage: (id: string, patch: Partial<FacebookPage>) => void
  addPage: (page: FacebookPage) => void
}

export default function Pages({ state, updatePage, addPage }: Props) {
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<FacebookPage | null>(null)
  const [promptText, setPromptText] = useState('')
  const [newName, setNewName] = useState('')
  const [newId, setNewId] = useState('')

  const shown = state.pages.filter((p) =>
    !query.trim() ||
    p.name.toLowerCase().includes(query.toLowerCase()) ||
    p.pageId.includes(query) ||
    p.niche.toLowerCase().includes(query.toLowerCase())
  )

  return (
    <div>
      <h2>Facebook Pages ({state.pages.length} / 500)</h2>
      <p className="muted">Pages come from connected accounts. Each page has its own master prompt, sent verbatim to Veo.</p>
      <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search pages" />
      {shown.length === 0 && <p className="muted">No pages yet — connect an account in Settings.</p>}
      {shown.map((p) => (
        <div key={p.id} className="card">
          <div className="row">
            <div style={{ flex: 1 }}>
              <b>{p.name}</b>
              <p className="muted">ID: {p.pageId} • {p.niche} • {p.followers.toLocaleString()} followers</p>
              <p className="muted">
                Token: {p.tokenHealth}{p.hasToken ? ' • saved on device' : ' • no token saved'}
              </p>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <input
                type="checkbox"
                checked={p.isEnabled}
                onChange={() => updatePage(p.id, { isEnabled: !p.isEnabled })}
                style={{ width: 'auto' }}
              />
              Enabled
            </label>
          </div>
          <div className="row">
            <button className="secondary" onClick={() => { setEditing(p); setPromptText(p.masterPrompt) }}>
              Master prompt
            </button>
          </div>
          {editing?.id === p.id && (
            <div style={{ marginTop: 10 }}>
              <textarea rows={5} value={promptText} onChange={(e) => setPromptText(e.target.value)} placeholder="Master prompt (verbatim)" />
              <div className="row" style={{ marginTop: 8 }}>
                <button onClick={() => { updatePage(p.id, { masterPrompt: promptText }); setEditing(null) }}>Save prompt</button>
                <button className="secondary" onClick={() => setEditing(null)}>Cancel</button>
              </div>
            </div>
          )}
        </div>
      ))}
      <h3>Add a page manually</h3>
      <div className="card">
        <label>Page name</label>
        <input value={newName} onChange={(e) => setNewName(e.target.value)} />
        <label>Graph Page ID (numeric)</label>
        <input value={newId} onChange={(e) => setNewId(e.target.value)} />
        <div style={{ marginTop: 10 }}>
          <button
            disabled={!newName.trim() || !newId.trim()}
            onClick={() => {
              addPage({
                id: `p${Date.now()}`, name: newName.trim(), pageId: newId.trim(),
                niche: '', followers: 0, isEnabled: true, tokenHealth: 'ACTIVE',
                hasToken: false, masterPrompt: '',
              })
              setNewName('')
              setNewId('')
            }}
          >
            Add page
          </button>
        </div>
      </div>
    </div>
  )
}
