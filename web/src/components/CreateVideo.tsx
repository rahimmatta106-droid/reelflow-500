import { useState } from 'react'
import type { FacebookPage, VideoJob } from '../types'

const MODELS = [
  { id: 'veo-3.0-generate-001', label: 'Veo 3.0 Standard', cost: '$6.00 / 8s' },
  { id: 'veo-3.0-fast-generate-001', label: 'Veo 3.0 Fast', cost: '$3.20 / 8s' },
  { id: 'veo-3.1-generate-001', label: 'Veo 3.1 Standard', cost: 'Veo 3.1 pricing' },
  { id: 'veo-3.1-fast-generate-001', label: 'Veo 3.1 Fast', cost: 'Veo 3.1 pricing' },
]

interface Props {
  state: { pages: FacebookPage[]; veoKeySet: boolean }
  addJob: (job: VideoJob) => void
}

export default function CreateVideo({ state, addJob }: Props) {
  const [pageId, setPageId] = useState('')
  const [model, setModel] = useState(MODELS[0].id)
  const [useMaster, setUseMaster] = useState(true)
  const [customPrompt, setCustomPrompt] = useState('')
  const [title, setTitle] = useState('')
  const [caption, setCaption] = useState('')
  const [hashtags, setHashtags] = useState('#Reels #Trending #Viral')

  const page = state.pages.find((p) => p.id === pageId)
  const masterPrompt = page?.masterPrompt ?? ''
  const prompt = useMaster ? masterPrompt : customPrompt
  const canCreate = !!page && state.veoKeySet && prompt.trim().length > 0

  const create = () => {
    if (!page || !canCreate) return
    addJob({
      id: `j${Date.now()}`,
      targetPageId: page.id,
      targetPageName: page.name,
      prompt,
      veoModel: model,
      status: 'AI_GENERATING',
      title: title.trim(),
      caption: caption.trim(),
      hashtags: hashtags.split(/\s+/).filter(Boolean),
      thumbnailUrl: null,
      scheduledDateTime: null,
      scheduledTimezone: 'America/New_York',
      postUrl: null,
      createdAt: Date.now(),
    })
    // Demo: generation is simulated in the web dashboard. The Android app
    // calls the real Veo API with the prompt 100% verbatim.
    setTimeout(() => {
      // left as AI_GENERATING in this demo build; real status comes from Veo.
    }, 0)
    alert('Video job created. In the Android app this calls Veo with the prompt 100% verbatim; here it is queued as a demo job.')
  }

  return (
    <div>
      <h2>Create video</h2>
      {!state.veoKeySet && <p style={{ color: '#ef4444' }}>Veo API key not set — add it in Settings first.</p>}
      <label>Target page (one video → one page)</label>
      <select value={pageId} onChange={(e) => setPageId(e.target.value)}>
        <option value="">Select a page</option>
        {state.pages.filter((p) => p.isEnabled).map((p) => (
          <option key={p.id} value={p.id}>{p.name}</option>
        ))}
      </select>
      <label>Veo model</label>
      <select value={model} onChange={(e) => setModel(e.target.value)}>
        {MODELS.map((m) => <option key={m.id} value={m.id}>{m.label} — {m.cost}</option>)}
      </select>
      <p className="muted">Veo generates 8-second clips.</p>
      <label>Prompt (sent 100% verbatim to Veo)</label>
      <div className="row">
        <button className={useMaster ? '' : 'secondary'} onClick={() => setUseMaster(true)}>Page master prompt</button>
        <button className={!useMaster ? '' : 'secondary'} onClick={() => setUseMaster(false)}>Custom prompt</button>
      </div>
      {useMaster ? (
        <p className="muted">{masterPrompt || 'This page has no master prompt saved — write one in Pages.'}</p>
      ) : (
        <textarea rows={5} value={customPrompt} onChange={(e) => setCustomPrompt(e.target.value)} placeholder="Type the prompt…" />
      )}
      <label>Title</label>
      <input value={title} onChange={(e) => setTitle(e.target.value)} />
      <label>Caption</label>
      <textarea rows={3} value={caption} onChange={(e) => setCaption(e.target.value)} />
      <label>Hashtags</label>
      <input value={hashtags} onChange={(e) => setHashtags(e.target.value)} />
      <div style={{ marginTop: 16 }}>
        <button onClick={create} disabled={!canCreate}>Generate video</button>
      </div>
    </div>
  )
}
