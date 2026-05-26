export default function ConfirmModal({ title, message, confirmLabel = 'Yes, continue', onConfirm, onClose, isPending }) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 400 }} onClick={e => e.stopPropagation()}>
        <h2 style={{ fontSize: 16, marginBottom: 8 }}>{title}</h2>
        {message && <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 20 }}>{message}</p>}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose} disabled={isPending}>Cancel</button>
          <button className="btn btn-primary" onClick={onConfirm} disabled={isPending} autoFocus>
            {isPending ? '⏳ Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
