import type { VideoJob } from '../types'

interface Props {
  state: { jobs: VideoJob[] }
  updateJob: (id: string, patch: Partial<VideoJob>) => void
}

export default function Approval({ state, updateJob }: Props) {
  const pending = state.jobs.filter((j) => j.status === 'READY_FOR_REVIEW')
  const decided = state.jobs
    .filter((j) => j.status === 'APPROVED' || j.status === 'REJECTED')
    .slice(0, 10)

  return (
    <div>
      <h2>Review &amp; Approval</h2>
      <p className="muted">{pending.length} video(s) waiting for approval. Nothing publishes without your approval.</p>
      {pending.length === 0 && <p className="muted">Nothing waiting — new videos appear here when ready.</p>}
      {pending.map((j) => (
        <div key={j.id} className="card">
          <b>{j.title || j.prompt.slice(0, 60)}</b>
          <p className="muted">{j.targetPageName} • {j.veoModel}</p>
          {j.caption && <p>Caption: {j.caption}</p>}
          {j.hashtags.length > 0 && <p>{j.hashtags.join(' ')}</p>}
          <div className="row">
            <button onClick={() => updateJob(j.id, { status: 'APPROVED' })}>Approve</button>
            <button className="danger" onClick={() => updateJob(j.id, { status: 'REJECTED' })}>Reject</button>
          </div>
        </div>
      ))}
      {decided.length > 0 && (
        <>
          <h3>Recently decided</h3>
          {decided.map((j) => (
            <div key={j.id} className="card">
              <b>{j.title || j.prompt.slice(0, 60)}</b>{' '}
              <span className={`chip ${j.status === 'APPROVED' ? 'green' : 'red'}`}>{j.status}</span>
              <p className="muted">{j.targetPageName}</p>
            </div>
          ))}
        </>
      )}
    </div>
  )
}
