import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import Nav from './Nav'

export default function UserShell() {
  const { auth } = useAuth()
  const isSupport = auth?.roles?.includes('SUPPORT')
  if (isSupport) return <Navigate to="/support/disputes" replace />

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Nav />
      </aside>

      <div className="main">
        <div className="topbar">
          <div className="topbar-title">Dashboard</div>
          <div className="topbar-spacer" />
          <div className="topbar-meta">
            <span>Role: USER</span>
          </div>
        </div>

        <div className="content">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
