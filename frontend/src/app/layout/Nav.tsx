import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { apiFetch } from '../api/http'

function Icon({ name }: { name: 'dashboard' | 'tx' | 'dispute' }) {
  if (name === 'dashboard') {
    return (
      <svg className="nav-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M4 13h7V4H4v9Zm9 7h7V11h-7v9ZM4 20h7v-5H4v5Zm9-11h7V4h-7v5Z" fill="currentColor" opacity="0.92"/>
      </svg>
    )
  }
  if (name === 'tx') {
    return (
      <svg className="nav-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M7 7h10M7 12h10M7 17h6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
        <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v13A2.5 2.5 0 0 1 17.5 21h-11A2.5 2.5 0 0 1 4 18.5v-13Z" stroke="currentColor" strokeWidth="2" opacity=".65"/>
      </svg>
    )
  }
  return (
    <svg className="nav-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M12 2 2 7v6c0 5 3.5 9.5 10 9 6.5.5 10-4 10-9V7L12 2Z" stroke="currentColor" strokeWidth="2"/>
      <path d="M9 12h6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
    </svg>
  )
}

export default function Nav() {
  const { auth, logout } = useAuth()
  const nav = useNavigate()
  const loc = useLocation()
  const isSupport = auth?.roles?.includes('SUPPORT')

  async function onLogout() {
    // Best-effort: revoke token on backend, then always clear local session.
    try {
      if (auth?.accessToken) {
        await apiFetch<void>('/auth/logout', { method: 'POST', auth })
      }
    } catch {
      // ignore: even if backend is down, we still clear local session
    } finally {
      logout()
      nav('/login', { replace: true })
    }
  }

  const isActive = (path: string) =>
    (loc.pathname === path || loc.pathname.startsWith(path + '/')) ? 'nav-link active' : 'nav-link'

  return (
    <div>
      <div className="brand">
        <div className="brand-mark" aria-hidden="true">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M16.8 7.6c-1-1-2.4-1.6-4-1.6-3.1 0-5.6 2.5-5.6 5.6s2.5 5.6 5.6 5.6c1.6 0 3-.6 4-1.6" stroke="white" strokeWidth="2" strokeLinecap="round"/>
          </svg>
        </div>
        <div>
          <div className="brand-title">CAPITEC</div>
          <div className="brand-sub">Transaction Disputes</div>
        </div>
      </div>

<div className="nav-link nav-dashboard" style={{ cursor: 'default' }}>
  <Icon name="dashboard" />
  Dashboard
</div>
      <div className="nav-section">
        <div className="nav-section-title">Banking</div>

        {!isSupport && (
          <>
            <Link to="/transactions" className={isActive('/transactions')}>
              <Icon name="tx" />
              Transactions
            </Link>
            <Link to="/disputes" className={isActive('/disputes')}>
              <Icon name="dispute" />
              Disputes
            </Link>
          </>
        )}

        {isSupport && (
          <Link to="/support/disputes" className={isActive('/support/disputes')}>
            <Icon name="dispute" />
            Disputes
          </Link>
        )}
      </div>

      <div className="nav-section" style={{ marginTop: 18 }}>
        <div className="nav-section-title">Session</div>
        <button className="btn btn-ghost" onClick={onLogout} style={{ width: '100%' }}>
          Logout
        </button>
      </div>
    </div>
  )
}
