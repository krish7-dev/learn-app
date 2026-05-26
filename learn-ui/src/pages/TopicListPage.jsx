import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { topicApi } from '../api/topicApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import ConfirmDeleteModal from '../components/common/ConfirmDeleteModal'

const STATUS_COLORS = {
  NOT_STARTED: 'var(--muted)',
  LEARNING: '#f59e0b',
  REVISING: '#6366f1',
  MASTERED: 'var(--success)',
}

export default function TopicListPage() {
  const [page, setPage] = useState(0)
  const [deleting, setDeleting] = useState(null)
  const qc = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['topics', page],
    queryFn: () => topicApi.list(page, 20),
  })

  const deleteTopic = useMutation({
    mutationFn: (id) => topicApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['topics'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setDeleting(null)
    },
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const topics = data?.content ?? []
  const totalPages = data?.totalPages ?? 1

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Topics</h1>
          <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>
            Concepts extracted across all your lectures
          </p>
        </div>
      </div>

      {topics.length === 0 ? (
        <div className="empty-state">
          No topics yet. Generate notes for a lecture to extract topics automatically.
        </div>
      ) : (
        <div className="grid-2">
          {topics.map((t) => (
            <Link key={t.id} to={`/topics/${t.id}`}>
              <div
                className="card"
                style={{ cursor: 'pointer', height: '100%' }}
                onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                  <h3 style={{ fontSize: 15, fontWeight: 600 }}>{t.name}</h3>
                  <span className="badge" style={{
                    background: (STATUS_COLORS[t.status] ?? 'var(--muted)') + '25',
                    color: STATUS_COLORS[t.status] ?? 'var(--muted)',
                    flexShrink: 0,
                    marginLeft: 8,
                  }}>
                    {t.status?.replace('_', ' ')}
                  </span>
                </div>

                {t.category && (
                  <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 8 }}>
                    {t.category}{t.difficulty && ` · ${t.difficulty}`}
                  </div>
                )}

                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 8 }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ height: 4, background: 'var(--border)', borderRadius: 2, overflow: 'hidden' }}>
                      <div style={{
                        height: '100%',
                        width: `${t.masteryScore ?? 0}%`,
                        background: 'var(--accent)',
                        borderRadius: 2,
                        transition: 'width 0.3s',
                      }} />
                    </div>
                  </div>
                  <div style={{ fontSize: 12, color: 'var(--muted)', whiteSpace: 'nowrap' }}>
                    {t.masteryScore ?? 0}% mastery
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
                  {t.lectureCount != null && (
                    <div style={{ fontSize: 11, color: 'var(--muted)' }}>
                      {t.lectureCount} lecture{t.lectureCount !== 1 ? 's' : ''}
                    </div>
                  )}
                  <button
                    className="btn btn-sm btn-danger"
                    style={{ marginLeft: 'auto' }}
                    onClick={(e) => { e.preventDefault(); e.stopPropagation(); setDeleting(t) }}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {deleting && (
        <ConfirmDeleteModal
          title="Delete Topic"
          itemName={deleting.name}
          isPending={deleteTopic.isPending}
          onConfirm={() => deleteTopic.mutate(deleting.id)}
          onClose={() => setDeleting(null)}
        />
      )}

      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 24 }}>
          <button
            className="btn btn-secondary btn-sm"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >
            Previous
          </button>
          <span style={{ fontSize: 13, color: 'var(--muted)', alignSelf: 'center' }}>
            {page + 1} / {totalPages}
          </span>
          <button
            className="btn btn-secondary btn-sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
