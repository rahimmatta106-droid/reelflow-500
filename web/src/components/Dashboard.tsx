import type { Tab } from '../types'

// Dashboard is rendered from App with the full state; keep the props loose
// so the file stays small.
export default function Dashboard({ state, go }: {
  state: {
    pages: unknown[]
    jobs: { id: string; status: string; targetPageName: string; title: string; prompt: string }[]
  }
  go: (tab: Tab) => void
}) {
  const jobs = state.jobs
  const published = jobs.filter((j) => j.status === 'PUBLISHED').length
  const scheduled = jobs.filter((j) => j.status === 'SCHEDULED').length
  const pending = jobs.filter((j) => j.status === 'READY_FOR_REVIEW').length
  const views = 0

  return (
    <div>
      <h2>Dashboard</h2>
      <p className="muted">{state.pages.length} pages • {jobs.length} videos • {published} published</p>
      <div className="stats">
        <div className="stat"><b>{jobs.length}</b><span>Videos</span></div>
        <div className="stat"><b>{state.pages.length}</b><span>Pages</span></div>
        <div className="stat"><b>{scheduled}</b><span>Scheduled</span></div>
        <div className="stat"><b>{published}</b><span>Published</span></div>
        <div className="stat"><b>{pending}</b><span>Pending review</span></div>
        <div className="stat"><b>{views}</b><span>Total views</span></div>
      </div>
      <div className="row">
        <button onClick={() => go('create')}>Create video</button>
        <button onClick={() => go('review')}>Review ({pending})</button>
        <button className="secondary" onClick={() => go('schedule')}>Schedule</button>
        <button className="secondary" onClick={() => go('pages')}>Pages</button>
        <button className="secondary" onClick={() => go('analytics')}>Analytics</button>
        <button className="secondary" onClick={() => go('settings')}>Settings</button>
      </div>
      <h3>Recent videos</h3>
      {jobs.length === 0 && <p className="muted">No videos yet — create one from the Create tab.</p>}
      {jobs.slice(0, 5).map((j) => (
        <div key={j.id} className="card">
          <b>{j.title || j.prompt.slice(0, 60)}</b>
          <p className="muted">{j.targetPageName} • {j.status}</p>
        </div>
      ))}
    </div>
  )
}
