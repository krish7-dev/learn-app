import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { dashboardApi } from '../api/dashboardApi'
import { revisionApi } from '../api/revisionApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import { formatDate, statusColor } from '../utils/formatters'

export default function DashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardApi.getToday,
  })

  const qc = useQueryClient()
  const markDone = useMutation({
    mutationFn: ({ id }) => revisionApi.update(id, 'DONE'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['dashboard'] }),
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  return (
    <div>
      {/* Stats row */}
      <div className="stats-row">
        {[
          { label: 'Lectures', value: `${data.completedLectures} / ${data.totalLectures}` },
          { label: 'Topics', value: data.totalTopics },
          { label: 'Mastered', value: data.masteredTopics },
          { label: 'Revision Due', value: data.revisionDue?.length ?? 0 },
        ].map(({ label, value }) => (
          <div key={label} className="card" style={{ flex: 1 }}>
            <div style={{ fontSize: 24, fontWeight: 700 }}>{value}</div>
            <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>{label}</div>
          </div>
        ))}
      </div>

      <div className="grid-2">
        {/* Continue Lecture */}
        <div>
          <div className="section-title">Continue Learning</div>
          {data.continueLecture ? (
            <Link to={`/lectures/${data.continueLecture.id}`}>
              <div className="card" style={{ cursor: 'pointer', transition: 'border-color 0.15s' }}
                   onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                   onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}>
                <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 4 }}>
                  {data.continueLecture.moduleName || 'Lecture'}
                </div>
                <div style={{ fontWeight: 600, marginBottom: 8 }}>{data.continueLecture.title}</div>
                <span className="badge" style={{ background: statusColor(data.continueLecture.status) + '30', color: statusColor(data.continueLecture.status) }}>
                  {data.continueLecture.status}
                </span>
              </div>
            </Link>
          ) : (
            <div className="card empty-state" style={{ padding: 24 }}>
              No lectures in progress. <Link to="/courses" style={{ color: 'var(--accent)' }}>Start one →</Link>
            </div>
          )}
        </div>

        {/* Revision Due */}
        <div>
          <div className="section-title">Revision Due</div>
          {data.revisionDue?.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {data.revisionDue.map((item) => (
                <div key={item.id} className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ fontWeight: 500, fontSize: 13 }}>{item.title}</div>
                    <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>
                      {item.revisionType} · Due {formatDate(item.dueAt)}
                    </div>
                  </div>
                  <button
                    className="btn btn-sm btn-primary"
                    disabled={markDone.isPending}
                    onClick={() => markDone.mutate({ id: item.id })}
                  >
                    Done
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="card empty-state" style={{ padding: 24 }}>All caught up! 🎉</div>
          )}
        </div>

        {/* Weak Topics */}
        <div style={{ gridColumn: '1 / -1' }}>
          <div className="section-title">Topics to Revisit</div>
          {data.weakTopics?.length > 0 ? (
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              {data.weakTopics.map((t) => (
                <Link key={t.id} to={`/topics/${t.id}`}>
                  <div className="card" style={{ minWidth: 140, cursor: 'pointer' }}
                       onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                       onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}>
                    <div style={{ fontWeight: 500, fontSize: 13 }}>{t.name}</div>
                    <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 4 }}>
                      Mastery: {t.masteryScore}%
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="card empty-state" style={{ padding: 24 }}>No weak topics yet.</div>
          )}
        </div>
      </div>
    </div>
  )
}
