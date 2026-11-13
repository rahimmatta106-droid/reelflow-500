import type { VideoJob } from '../types'

interface Props {
  state: { jobs: VideoJob[] }
}

export default function Analytics({ state }: Props) {
  const jobs = state.jobs
  const published = jobs.filter((j) => j.status === 'PUBLISHED')
  const byPage = new Map<string, number>()
  published.forEach((j) => byPage.set(j.targetPageName, (byPage.get(j.targetPageName) ?? 0) + 1))

  return (
    <div>
      <h2>Analytics</h2>
      <p className="muted">Counts from videos on this device. Live reach/views come from the publish record.</p>
      <div className="stats">
        <div className="stat"><b>{jobs.length}</b><span>Total videos</span></div>
        <div className="stat"><b>{published.length}</b><span>Published</span></div>
        <div className="stat"><b>{jobs.filter((j) => j.status === 'SCHEDULED').length}</b><span>Scheduled</span></div>
        <div className="stat"><b>{jobs.filter((j) => j.status === 'READY_FOR_REVIEW').length}</b><span>Pending review</span></div>
        <div className="stat"><b>{jobs.filter((j) => j.status === 'REJECTED').length}</b><span>Rejected</span></div>
        <div className="stat"><b>{jobs.filter((j) => j.status === 'FAILED').length}</b><span>Failed</span></div>
      </div>
      <h3>Published by page</h3>
      {byPage.size === 0 && <p className="muted">Nothing published yet.</p>}
      {[...byPage.entries()].map(([page, count]) => (
        <div key={page} className="card">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <b>{page}</b><span>{count}</span>
          </div>
        </div>
      ))}
      <h3>Recent jobs</h3>
      {jobs.slice(0, 10).map((j) => (
        <div key={j.id} className="card">
          <b>{j.title || j.prompt.slice(0, 60)}</b>{' '}
          <span className={`chip ${j.status === 'PUBLISHED' ? 'green' : j.status === 'REJECTED' ? 'red' : 'grey'}`}>{j.status}</span>
          <p className="muted">{j.targetPageName}</p>
        </div>
      ))}
    </div>
  )
}
