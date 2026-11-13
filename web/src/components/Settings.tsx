import { useState } from 'react'
import type { FacebookPage, VideoJob } from '../types'

interface SavedState {
  pages: FacebookPage[]
  jobs: VideoJob[]
  veoKeySet: boolean
  mode: 'MOCK' | 'REAL'
}

interface Props {
  state: SavedState
  setState: (fn: (s: SavedState) => SavedState) => void
  addPage: (page: FacebookPage) => void
}

export default function Settings({ state, setState, addPage }: Props) {
  const [token, setToken] = useState('')
  const [veoKey, setVeoKey] = useState('')
  const [message, setMessage] = useState('')

  const connectWithToken = () => {
    if (!token.trim()) {
      setMessage('Token khaali hai. Explorer se token copy kar ke paste karo.')
      return
    }
    // Demo: the web dashboard records the connection; the Android app
    // validates the token against /me and /me/accounts and saves page tokens.
    addPage({
      id: `p${Date.now()}`, name: 'Connected Account Page', pageId: `${Date.now()}`,
      niche: '', followers: 0, isEnabled: true, tokenHealth: 'ACTIVE',
      hasToken: true, masterPrompt: '',
    })
    setMessage('Token saved (demo). In the Android app this calls /me + /me/accounts and saves every page token encrypted.')
    setToken('')
  }

  return (
    <div>
      <h2>Settings</h2>
      {message && <p>{message}</p>}
      <h3>Connect Facebook</h3>
      <div className="card">
        <p className="muted">
          Two ways to connect — Facebook Login button, or paste a User Token from Graph API Explorer.
          There is deliberately NO email+password login: Facebook forbids it in third-party apps.
        </p>
        <div className="row">
          <button onClick={() => setMessage('Facebook Login opens the official Facebook login page (Meta App ID 1138116601998610). Use the Android app for the real login flow.')}>
            Login with Facebook
          </button>
        </div>
        <label>User Token (Graph API Explorer)</label>
        <input
          type="password"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          placeholder="User Token paste karo..."
        />
        <div style={{ marginTop: 10 }}>
          <button onClick={connectWithToken}>Token se Connect</button>
        </div>
      </div>
      <h3>Veo API key</h3>
      <div className="card">
        <p className="muted">{state.veoKeySet ? 'Key saved.' : 'Key not set — video generation needs it.'}</p>
        <label>Google AI Studio API key</label>
        <input type="password" value={veoKey} onChange={(e) => setVeoKey(e.target.value)} />
        <div style={{ marginTop: 10 }}>
          <button
            onClick={() => {
              setState((s) => ({ ...s, veoKeySet: veoKey.trim().length > 0 }))
              setVeoKey('')
              setMessage('Veo key saved.')
            }}
          >
            Save Veo key
          </button>
        </div>
      </div>
      <h3>Publishing mode</h3>
      <div className="card">
        <div className="row">
          <button className={state.mode === 'MOCK' ? '' : 'secondary'} onClick={() => setState((s) => ({ ...s, mode: 'MOCK' }))}>Demo (mock)</button>
          <button className={state.mode === 'REAL' ? '' : 'secondary'} onClick={() => setState((s) => ({ ...s, mode: 'REAL' }))}>Real (Meta API)</button>
        </div>
        <p className="muted">Demo mode simulates publishing. Real mode uploads to Facebook with saved page tokens.</p>
      </div>
    </div>
  )
}
