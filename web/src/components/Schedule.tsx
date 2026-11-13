import { useState } from 'react'
import type { VideoJob } from '../types'

interface Props {
  state: { jobs: VideoJob[] }
  updateJob: (id: string, patch: Partial<VideoJob>) => void
}

export default function Schedule({ state, updateJob }: Props) {
  const approved = state.jobs.filter((j) => j.status === 'APPROVED')
  const scheduled = state.jobs
    .filter((j) => j.status === 'SCHEDULED')
    .sort((a, b) => (a.scheduledDateTime ?? '').localeCompare(b.scheduledDateTime ?? ''))
  const [when, setWhen] = useState<Record<string, string>>({})

  const schedule = (job: VideoJob) => {
    const value = when[job.id]
    if (!value) return
    // datetime-local gives "2026-11-13T19:40" — store as ISO local date-time.
    updateJob(job.id, { status: 'SCHEDULED', scheduledDateTime: value + ':00' })
  }

  return (
    <div>
      <h2>Scheduling</h2>
      <p className="muted">Approved videos get a publish date/time. The Android app publishes them at that time even if closed (WorkManager). The web dashboard queues them the same way.</p>
      <h3>Scheduled ({scheduled.length})</h3>
      {scheduled.length === 0 && <p className="muted">Nothing scheduled yet.</p>}
      {scheduled.map((j) => (
        <div key={j.id} className="card">
          <b>{j.title || j.prompt.slice(0, 60)}</b>
          <p className="muted">{j.targetPageName} • {j.scheduledDateTime} ({j.scheduledTimezone})</p>
          <button className="secondary" onClick={() => updateJob(j.id, { status: 'APPROVED', scheduledDateTime: null })}>Cancel schedule</button>
        </div>
      ))}
      <h3>Approved — ready to schedule ({approved.length})</h3>
      {approved.length === 0 && <p className="muted">No approved videos waiting.</p>}
      {approved.map((j) => (
        <div key={j.id} className="card">
          <b>{j.title || j.prompt.slice(0, 60)}</b>
          <p className="muted">{j.targetPageName}</p>
          <div className="row">
            <input
              type="datetime-local"
              value={when[j.id] ?? ''}
              onChange={(e) => setWhen({ ...when, [j.id]: e.target.value })}
              style={{ width: 240 }}
            />
            <button onClick={() => schedule(j)} disabled={!when[j.id]}>Schedule</button>
          </div>
        </div>
      ))}
    </div>
  )
}
