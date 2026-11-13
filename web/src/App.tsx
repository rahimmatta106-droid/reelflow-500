import { useEffect, useState } from 'react'
import type { FacebookPage, Tab, VideoJob } from './types'
import Dashboard from './components/Dashboard'
import VideoLibrary from './components/VideoLibrary'
import CreateVideo from './components/CreateVideo'
import Approval from './components/Approval'
import Schedule from './components/Schedule'
import Pages from './components/Pages'
import Analytics from './components/Analytics'
import Settings from './components/Settings'

const LS_KEY = 'reelflow500_state_v1'

function seedPages(): FacebookPage[] {
  return [
    { id: 'p1', name: 'Tech Pulse', pageId: '100000000000001', niche: 'Tech & AI', followers: 12400, isEnabled: true, tokenHealth: 'ACTIVE', hasToken: false, masterPrompt: '' },
    { id: 'p2', name: 'Fitness Daily', pageId: '100000000000002', niche: 'Fitness', followers: 8300, isEnabled: true, tokenHealth: 'ACTIVE', hasToken: false, masterPrompt: '' },
    { id: 'p3', name: 'Travel Diaries', pageId: '100000000000003', niche: 'Travel', followers: 31200, isEnabled: true, tokenHealth: 'EXPIRING_SOON', hasToken: false, masterPrompt: '' },
  ]
}

function seedJobs(pages: FacebookPage[]): VideoJob[] {
  return [
    {
      id: 'j1', targetPageId: pages[0].id, targetPageName: pages[0].name,
      prompt: 'Futuristic quantum computing lab, cinematic neon light, vertical reel',
      veoModel: 'veo-3.0-generate-001', status: 'READY_FOR_REVIEW',
      title: 'Quantum AI Just Shattered Computing Limits!',
      caption: 'Would you trust a quantum AI assistant?',
      hashtags: ['#QuantumAI', '#FutureTech', '#Viral'],
      thumbnailUrl: null, scheduledDateTime: null, scheduledTimezone: 'America/New_York',
      postUrl: null, createdAt: Date.now() - 3600_000,
    },
    {
      id: 'j2', targetPageId: pages[1].id, targetPageName: pages[1].name,
      prompt: 'Athlete kettlebell snatch in a sunlit gym, slow motion',
      veoModel: 'veo-3.0-fast-generate-001', status: 'PUBLISHED',
      title: '3 Kettlebell Moves That Melt Core Fat',
      caption: 'Stop doing endless crunches!',
      hashtags: ['#Fitness', '#Workout'],
      thumbnailUrl: null, scheduledDateTime: null, scheduledTimezone: 'America/New_York',
      postUrl: 'https://facebook.com/reel/891048291048', createdAt: Date.now() - 86400_000,
    },
  ]
}

interface SavedState {
  pages: FacebookPage[]
  jobs: VideoJob[]
  veoKeySet: boolean
  mode: 'MOCK' | 'REAL'
}

function loadState(): SavedState {
  try {
    const raw = localStorage.getItem(LS_KEY)
    if (raw) return JSON.parse(raw) as SavedState
  } catch { /* ignore */ }
  const pages = seedPages()
  return { pages, jobs: seedJobs(pages), veoKeySet: false, mode: 'MOCK' }
}

const TABS: { id: Tab; label: string }[] = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'library', label: 'Video Library' },
  { id: 'create', label: 'Create Video' },
  { id: 'review', label: 'Approval' },
  { id: 'schedule', label: 'Schedule' },
  { id: 'pages', label: 'Pages' },
  { id: 'analytics', label: 'Analytics' },
  { id: 'settings', label: 'Settings' },
]

export default function App() {
  const [tab, setTab] = useState<Tab>('dashboard')
  const [state, setState] = useState<SavedState>(loadState)

  useEffect(() => {
    localStorage.setItem(LS_KEY, JSON.stringify(state))
  }, [state])

  const updateJob = (id: string, patch: Partial<VideoJob>) =>
    setState((s) => ({ ...s, jobs: s.jobs.map((j) => (j.id === id ? { ...j, ...patch } : j)) }))

  const addJob = (job: VideoJob) =>
    setState((s) => ({ ...s, jobs: [job, ...s.jobs] }))

  const deleteJob = (id: string) =>
    setState((s) => ({ ...s, jobs: s.jobs.filter((j) => j.id !== id) }))

  const updatePage = (id: string, patch: Partial<FacebookPage>) =>
    setState((s) => ({ ...s, pages: s.pages.map((p) => (p.id === id ? { ...p, ...patch } : p)) }))

  const addPage = (page: FacebookPage) =>
    setState((s) => ({ ...s, pages: [...s.pages, page] }))

  const pending = state.jobs.filter((j) => j.status === 'READY_FOR_REVIEW').length

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>ReelFlow 500</h1>
        <p>Facebook Automation — up to 500 pages. Local-first, no server.</p>
        <nav>
          {TABS.map((t) => (
            <button key={t.id} className={tab === t.id ? 'active' : ''} onClick={() => setTab(t.id)}>
              {t.label}{t.id === 'review' && pending > 0 ? ` (${pending})` : ''}
            </button>
          ))}
        </nav>
        <p>Mode: {state.mode} • Veo key: {state.veoKeySet ? 'saved' : 'not set'}</p>
      </aside>
      <main className="main">
        {tab === 'dashboard' && <Dashboard state={state} go={setTab} />}
        {tab === 'library' && (
          <VideoLibrary state={state} updateJob={updateJob} deleteJob={deleteJob} updatePage={updatePage} />
        )}
        {tab === 'create' && <CreateVideo state={state} addJob={addJob} />}
        {tab === 'review' && <Approval state={state} updateJob={updateJob} />}
        {tab === 'schedule' && <Schedule state={state} updateJob={updateJob} />}
        {tab === 'pages' && <Pages state={state} updatePage={updatePage} addPage={addPage} />}
        {tab === 'analytics' && <Analytics state={state} />}
        {tab === 'settings' && (
          <Settings
            state={state}
            setState={setState}
            addPage={addPage}
          />
        )}
      </main>
    </div>
  )
}
