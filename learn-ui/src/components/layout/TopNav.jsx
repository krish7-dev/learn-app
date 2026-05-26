import { useAppStore } from '../../store/appStore'

export default function TopNav({ title }) {
  const toggle = useAppStore((s) => s.toggleSidebar)

  return (
    <header style={{
      height: 52,
      borderBottom: '1px solid var(--border)',
      display: 'flex',
      alignItems: 'center',
      padding: '0 20px',
      gap: 12,
      background: 'var(--surface)',
      flexShrink: 0,
    }}>
      <button onClick={toggle} style={{ fontSize: 16, padding: '4px 8px', color: 'var(--muted)' }}>☰</button>
      {title && <span style={{ fontWeight: 500, fontSize: 14 }}>{title}</span>}
    </header>
  )
}
