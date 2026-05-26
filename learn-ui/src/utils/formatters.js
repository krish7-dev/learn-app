export function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

export function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
  })
}

export function statusColor(status) {
  return {
    NOT_STARTED: '#6b7280',
    IN_PROGRESS: '#f59e0b',
    COMPLETED: '#10b981',
    ARCHIVED: '#4b5563',
    LEARNING: '#3b82f6',
    REVISING: '#8b5cf6',
    MASTERED: '#10b981',
  }[status] ?? '#6b7280'
}

export function difficultyColor(d) {
  return { EASY: '#10b981', MEDIUM: '#f59e0b', HARD: '#ef4444' }[d] ?? '#6b7280'
}

export function masteryLabel(score) {
  if (score >= 80) return 'Mastered'
  if (score >= 50) return 'Good'
  if (score >= 20) return 'Learning'
  return 'Weak'
}
