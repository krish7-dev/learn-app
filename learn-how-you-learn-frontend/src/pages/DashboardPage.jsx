import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { dashboardApi } from '../api/dashboardApi'
import { revisionApi } from '../api/revisionApi'
import { timelineApi } from '../api/timelineApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import { formatDate, statusColor } from '../utils/formatters'

const PRIORITY_COLORS = { HIGH: '#ef4444', MEDIUM: '#f59e0b', LOW: '#6b7280' }
const TYPE_COLORS = {
  ADD_TRANSCRIPT: '#7c3aed', GENERATE_NOTES: '#2563eb', STUDY_LECTURE: '#16a34a',
  REVISION: '#d97706', WEAK_AREA: '#dc2626', PRACTICE: '#0d9484', TEACH_BACK: '#9333ea', BUFFER: '#6b7280',
}

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
  const markTimelineItem = useMutation({
    mutationFn: ({ id }) => timelineApi.markItem(id, { status: 'DONE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['dashboard'] }),
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  return (
    <div>
      {/* Active Targets row */}
      {data.activeTargets?.length > 0 && (
        <div style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
            <div className="section-title" style={{ margin: 0 }}>Your Targets</div>
            <Link to="/targets" style={{ fontSize: 12, color: 'var(--accent)' }}>Manage →</Link>
          </div>
          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            {data.activeTargets.map((t) => {
              const pc = PRIORITY_COLORS[t.priority] ?? '#6b7280'
              return (
                <Link key={t.id} to={`/targets/${t.id}/timeline`}>
                  <div className="card" style={{ minWidth: 180, cursor: 'pointer', padding: '12px 16px' }}
                       onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--accent)')}
                       onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--border)')}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                      <span style={{ fontSize: 13, fontWeight: 600 }}>{t.title}</span>
                      <span className="badge" style={{ background: pc + '20', color: pc, fontSize: 10 }}>{t.priority}</span>
                    </div>
                    {t.totalLectures > 0 ? (
                      <div>
                        <div style={{ height: 4, background: 'var(--border)', borderRadius: 2, overflow: 'hidden', marginBottom: 4 }}>
                          <div style={{ height: '100%', width: `${Math.min(100, t.progressPercent)}%`, background: 'var(--accent)', borderRadius: 2 }} />
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--muted)' }}>{Math.round(t.progressPercent)}% · {t.daysRemaining}d left</div>
                      </div>
                    ) : (
                      <div style={{ fontSize: 11, color: 'var(--muted)' }}>{t.daysRemaining}d remaining</div>
                    )}
                  </div>
                </Link>
              )
            })}
          </div>
        </div>
      )}
      {!data.activeTargets?.length && (
        <div style={{ marginBottom: 20 }}>
          <Link to="/targets" style={{ fontSize: 13, color: 'var(--accent)' }}>Set a learning target →</Link>
        </div>
      )}

      {/* Today's Study Plan */}
      {data.todayMinimumPlan?.length > 0 && (
        <div style={{ marginBottom: 24 }}>
          <div className="section-title">Today's Study Plan</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {data.todayMinimumPlan.map((item) => {
              const color = TYPE_COLORS[item.itemType] ?? '#6b7280'
              return (
                <div key={item.id} className="card"
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 16px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span className="badge" style={{ background: color + '20', color, fontSize: 11 }}>
                      {item.itemType.replace('_', ' ')}
                    </span>
                    <div>
                      <div style={{ fontSize: 13, fontWeight: 500 }}>{item.title}</div>
                      <div style={{ fontSize: 11, color: 'var(--muted)' }}>{item.estimatedMinutes} min</div>
                    </div>
                  </div>
                  {item.status === 'DONE' ? (
                    <span style={{ fontSize: 12, color: '#22c55e', fontWeight: 600 }}>✓ Done</span>
                  ) : (
                    <button className="btn btn-sm btn-primary"
                      disabled={markTimelineItem.isPending}
                      onClick={() => markTimelineItem.mutate({ id: item.id })}>
                      Done
                    </button>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

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
