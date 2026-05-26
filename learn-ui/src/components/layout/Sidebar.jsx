import { NavLink } from 'react-router-dom'
import { useAppStore } from '../../store/appStore'

const links = [
  { to: '/', label: 'Dashboard', icon: '⊞' },
  { to: '/courses', label: 'Courses', icon: '📚' },
  { to: '/targets', label: 'Targets', icon: '🎯' },
  { to: '/tree', label: 'Knowledge Tree', icon: '🌳' },
  { to: '/topics', label: 'Topics', icon: '🧠' },
  { to: '/revision', label: 'Revision', icon: '🔁' },
]

export default function Sidebar() {
  const open = useAppStore((s) => s.sidebarOpen)
  const toggle = useAppStore((s) => s.toggleSidebar)

  return (
    <>
      <div className={`sidebar-backdrop ${open ? 'open' : ''}`} onClick={toggle} />
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div style={{ padding: '20px 16px', width: 'var(--sidebar-w)' }}>
          <div style={{ fontWeight: 700, fontSize: 13, letterSpacing: 1, color: 'var(--accent)', marginBottom: 24 }}>
            LEARN HOW YOU LEARN
          </div>
          <nav style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {links.map(({ to, label, icon }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/'}
                onClick={() => { if (window.innerWidth <= 768) toggle() }}
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '8px 12px',
                  borderRadius: 'var(--radius)',
                  color: isActive ? 'var(--accent)' : 'var(--muted)',
                  background: isActive ? 'var(--surface2)' : 'transparent',
                  fontSize: 13,
                  fontWeight: isActive ? 500 : 400,
                  transition: 'all 0.15s',
                })}
              >
                <span>{icon}</span>
                {label}
              </NavLink>
            ))}
          </nav>
        </div>
      </aside>
    </>
  )
}
