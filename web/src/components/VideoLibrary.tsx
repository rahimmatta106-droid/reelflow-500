import { useState } from 'react'
import type { FacebookPage, VideoJob } from '../types'

const STATUS_COLOR: Record<string, string> = {
  DRAFT: 'grey', AI_GENERATING: 'blue', READY_FOR_REVIEW: 'amber',
  APPROVED: 'green', REJECTED: 'red', SCHEDULED: 'blue',
  PUBLISHING: 'blue', PUBLISHED: 'green', FAILED: 'red',
}

interface Props {
  state: { pages: FacebookPage[]; jobs: VideoJob[] }
  updateJob: (id: string, patch: Partial<VideoJob>) => void
  deleteJob: (id: string) => void
  updatePage: (id: string, patch: Partial<FacebookPage>) => void
}

export default function VideoLibrary({ state, updateJob, deleteJob }: Props) {
  const [filter, setFilter] = useState<string>('ALL')
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [assignPageId, setAssignPageId] = useState('')

  const shown = state.jobs.filter((j) => filter === 'ALL' || j.status === filter)
  const enabledPages = state.pages.filter((p) => p.isEnabled)

  const toggleSelect = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const bulkAssign = () => {
    const page = state.pages.find((p) => p.id === assignPageId)
    if (!page) return
    selected.forEach((id) => updateJob(id, { targetPageId: page.id, targetPageName: page.name }))
    setSelected(new Set())
  }

  return (
    <div>
      <h2>Video Library</h2>
      <div className="row">
        <select value={filter} onChange={(e) => setFilter(e.target.value)} style={{ width: 180 }}>
          <option value="ALL">All ({state.jobs.length})</option>
          {['DRAFT', 'AI_GENERATING', 'READY_FOR_REVIEW', 'APPROVED', 'REJECTED', 'SCHEDULED', 'PUBLISHED', 'FAILED'].map((s) => (
            <option key={s} value={s}>{s} ({state.jobs.filter((j) => j.status === s).length})</option>
          ))}
        </select>
        {selected.size > 0 && (
          <>
            <select value={assignPageId} onChange={(e) => setAssignPageId(e.target.value)} style={{ width: 200 }}>
              <option value="">Assign to page…</option>
              {enabledPages.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
            <button onClick={bulkAssign} disabled={!assignPageId}>Assign ({selected.size})</button>
          </>
        )}
      </div>
      {shown.length === 0 && <p className="muted">No videos here yet.</p>}
      {shown.map((j) => (
        <div key={j.id} className="card">
          <div className="row">
            <input type="checkbox" checked={selected.has(j.id)} onChange={() => toggleSelect(j.id)} style={{ width: 'auto' }} />
            <div style={{ flex: 1 }}>
              <b>{j.title || j.prompt.slice(0, 60)}</b>{' '}
              <span className={`chip ${STATUS_COLOR[j.status]}`}>{j.status}</span>
              <p className="muted">{j.targetPageName} • {j.veoModel} • {j.hashtags.join(' ')}</p>
              {j.scheduledDateTime && <p className="muted">Scheduled: {j.scheduledDateTime}</p>}
              {j.postUrl && <p><a href={j.postUrl} target="_blank" rel="noreferrer">{j.postUrl}</a></p>}
            </div>
            <button className="danger" onClick={() => { if (confirm('Delete this video?')) deleteJob(j.id) }}>Delete</button>
          </div>
        </div>
      ))}
    </div>
  )
}
