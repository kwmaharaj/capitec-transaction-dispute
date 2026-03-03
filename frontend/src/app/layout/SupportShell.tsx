import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import Nav from './Nav'

export default function SupportShell() {
  const { auth } = useAuth()
  const isSupport = auth?.roles?.includes('SUPPORT')
  if (!isSupport) return <Navigate to="/transactions" replace />

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Nav />
      </aside>

      <div className="main">
        <div className="topbar">
          <div className="topbar-title">Support</div>
          <div className="topbar-spacer" />
          <div className="topbar-meta">
            <span>Role: SUPPORT</span>
          </div>
        </div>

        <div className="content">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
