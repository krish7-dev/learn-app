import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { revisionApi } from '../api/revisionApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import { formatDate } from '../utils/formatters'

const TYPE_COLOR = {
  RECALL: '#6366f1',
  FLASHCARD: '#f59e0b',
  PRACTICE: '#10b981',
  TEACH_BACK: '#ec4899',
  MIXED_TEST: '#ef4444',
}

const PRIORITY_COLOR = {
  HIGH: '#ef4444',
  MEDIUM: '#f59e0b',
  LOW: 'var(--muted)',
}

export default function RevisionPage() {
  const qc = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['revision'],
    queryFn: revisionApi.getPending,
  })

  const update = useMutation({
    mutationFn: ({ id, status }) => revisionApi.update(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['revision'] }),
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const items = data?.content ?? (Array.isArray(data) ? data : [])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Revision Queue</h1>
          <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>
            {items.length > 0 ? `${items.length} item${items.length !== 1 ? 's' : ''} due` : 'All caught up!'}
          </p>
        </div>
      </div>

      {items.length === 0 ? (
        <div className="card empty-state" style={{ padding: 40 }}>
          <div style={{ fontSize: 32, marginBottom: 12 }}>🎉</div>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>All caught up!</div>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>
            No revisions due. Keep learning —{' '}
            <Link to="/courses" style={{ color: 'var(--accent)' }}>browse your courses →</Link>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {items.map((item) => (
            <div
              key={item.id}
              className="card"
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}
            >
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                  <span className="badge" style={{
                    background: (TYPE_COLOR[item.revisionType] ?? 'var(--muted)') + '20',
                    color: TYPE_COLOR[item.revisionType] ?? 'var(--muted)',
                    fontSize: 10,
                  }}>
                    {item.revisionType?.replace('_', ' ')}
                  </span>
                  {item.priority && (
                    <span style={{ fontSize: 11, color: PRIORITY_COLOR[item.priority] ?? 'var(--muted)' }}>
                      {item.priority}
                    </span>
                  )}
                </div>
                <div style={{ fontWeight: 500, fontSize: 14, marginBottom: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {item.title}
                </div>
                <div style={{ fontSize: 11, color: 'var(--muted)' }}>
                  Due {formatDate(item.dueAt)}
                  {item.lectureId && (
                    <>
                      {' · '}
                      <Link to={`/lectures/${item.lectureId}`} style={{ color: 'var(--accent)' }}>
                        View lecture →
                      </Link>
                    </>
                  )}
                  {item.topicId && (
                    <>
                      {' · '}
                      <Link to={`/topics/${item.topicId}`} style={{ color: 'var(--accent)' }}>
                        View topic →
                      </Link>
                    </>
                  )}
                </div>
              </div>

              <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                <button
                  className="btn btn-sm btn-secondary"
                  disabled={update.isPending}
                  onClick={() => update.mutate({ id: item.id, status: 'SKIPPED' })}
                >
                  Skip
                </button>
                <button
                  className="btn btn-sm btn-primary"
                  disabled={update.isPending}
                  onClick={() => update.mutate({ id: item.id, status: 'DONE' })}
                >
                  Done ✓
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
