import { useState } from 'react'

export default function ConfirmDeleteModal({ title, itemName, onConfirm, onClose, isPending }) {
  const [input, setInput] = useState('')
  const ready = input.trim().toLowerCase() === 'yes'

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 400 }} onClick={(e) => e.stopPropagation()}>
        <h2 style={{ fontSize: 16, marginBottom: 8 }}>{title ?? 'Delete?'}</h2>
        <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 16 }}>
          <strong style={{ color: 'var(--text)' }}>{itemName}</strong> will be permanently deleted. Type{' '}
          <strong style={{ color: 'var(--danger)' }}>yes</strong> to confirm.
        </p>
        <input
          autoFocus
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && ready) onConfirm() }}
          placeholder="Type yes to confirm"
          style={{ marginBottom: 16 }}
        />
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose} disabled={isPending}>Cancel</button>
          <button className="btn btn-danger" onClick={onConfirm} disabled={!ready || isPending}>
            {isPending ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  )
}
