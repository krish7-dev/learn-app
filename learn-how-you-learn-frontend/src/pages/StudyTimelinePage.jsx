import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { targetApi } from '../api/targetApi'
import { timelineApi } from '../api/timelineApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'

const TYPE_COLORS = {
  ADD_TRANSCRIPT:  { bg: '#7c3aed20', color: '#7c3aed', label: 'Add Transcript' },
  GENERATE_NOTES:  { bg: '#2563eb20', color: '#2563eb', label: 'Generate Notes' },
  STUDY_LECTURE:   { bg: '#16a34a20', color: '#16a34a', label: 'Study' },
  REVISION:        { bg: '#d9770620', color: '#d97706', label: 'Revision' },
  WEAK_AREA:       { bg: '#dc262620', color: '#dc2626', label: 'Weak Area' },
  PRACTICE:        { bg: '#0d948420', color: '#0d9484', label: 'Practice' },
  TEACH_BACK:      { bg: '#7c3aed20', color: '#9333ea', label: 'Teach Back' },
  BUFFER:          { bg: '#6b728020', color: '#6b7280', label: 'Buffer' },
}

const TIERS = ['MINIMUM', 'MEDIUM', 'FULL']

function TypeBadge({ type }) {
  const cfg = TYPE_COLORS[type] ?? { bg: '#6b728020', color: '#6b7280', label: type }
  return (
    <span className="badge" style={{ background: cfg.bg, color: cfg.color, fontSize: 11, fontWeight: 600, letterSpacing: '0.02em' }}>
      {cfg.label}
    </span>
  )
}

function ItemCard({ item, onDone, onSkip, marking }) {
  const busy = marking === item.id
  const isDone = item.status === 'DONE'
  const isSkipped = item.status === 'SKIPPED'

  return (
    <div className="card" style={{
      padding: '14px 16px',
      opacity: isDone || isSkipped ? 0.6 : 1,
      borderLeft: isDone ? '3px solid #22c55e' : isSkipped ? '3px solid #6b7280' : '3px solid transparent',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 6 }}>
        <TypeBadge type={item.itemType} />
        <span style={{ fontSize: 11, color: 'var(--muted)' }}>{item.estimatedMinutes} min</span>
      </div>
      <p style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>
        {isDone && '✓ '}{item.title}
      </p>
      {item.description && (
        <p style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 8 }}>{item.description}</p>
      )}
      {!isDone && !isSkipped && (
        <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
          <button className="btn btn-primary" style={{ fontSize: 12, padding: '4px 10px' }}
            disabled={busy} onClick={() => onDone(item.id)}>
            {busy ? '…' : 'Done'}
          </button>
          <button className="btn btn-secondary" style={{ fontSize: 12, padding: '4px 10px' }}
            disabled={busy} onClick={() => onSkip(item.id)}>
            Skip
          </button>
        </div>
      )}
    </div>
  )
}

function DayColumn({ day, tier, onDone, onSkip, marking, isToday }) {
  const items = tier === 'FULL' ? day.fullPlan
    : tier === 'MEDIUM' ? day.mediumPlan
    : day.minimumPlan

  const dateObj = new Date(day.date + 'T00:00:00')
  const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' })
  const dayNum  = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })

  return (
    <div style={{
      minWidth: 200, flex: '1 1 200px',
      border: isToday ? '2px solid var(--accent)' : '1px solid var(--border)',
      borderRadius: 8, padding: '12px 14px',
      background: isToday ? 'var(--accent)08' : 'var(--card)',
    }}>
      <div style={{ marginBottom: 12, textAlign: 'center' }}>
        <div style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>{dayName}</div>
        <div style={{ fontSize: 14, fontWeight: 700, color: isToday ? 'var(--accent)' : 'var(--text)' }}>{dayNum}</div>
        {isToday && <div style={{ fontSize: 10, color: 'var(--accent)', fontWeight: 600 }}>TODAY</div>}
      </div>

      {items && items.length > 0 ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {items.map((item) => (
            <ItemCard key={item.id} item={item} onDone={onDone} onSkip={onSkip} marking={marking} />
          ))}
        </div>
      ) : (
        <p style={{ fontSize: 12, color: 'var(--muted)', textAlign: 'center', padding: '16px 0' }}>Rest day</p>
      )}
    </div>
  )
}

export default function StudyTimelinePage() {
  const { id } = useParams()
  const [tier, setTier] = useState('MEDIUM')
  const [marking, setMarking] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const qc = useQueryClient()

  const { data: weekData, isLoading, error, refetch } = useQuery({
    queryKey: ['target', id, 'week'],
    queryFn: () => targetApi.getWeek(id),
  })

  const generateTimeline = useMutation({
    mutationFn: () => targetApi.generateTimeline(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['target', id, 'week'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  const deleteTimeline = useMutation({
    mutationFn: () => targetApi.deleteTimeline(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['target', id, 'week'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setConfirmDelete(false)
    },
  })

  const markItem = useMutation({
    mutationFn: ({ itemId, status }) => timelineApi.markItem(itemId, { status }),
    onMutate: ({ itemId }) => setMarking(itemId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['target', id, 'week'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setMarking(null)
    },
    onError: () => setMarking(null),
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const { target, days = [] } = weekData ?? {}
  const priorityColor = { HIGH: '#ef4444', MEDIUM: '#f59e0b', LOW: '#6b7280' }[target?.priority] ?? '#6b7280'
  const todayStr = new Date().toISOString().slice(0, 10)
  const hasMissedInAnyDay = days.some((d) => d.hasMissedItems)

  return (
    <div>
      <div className="page-header" style={{ flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <h1 style={{ fontSize: 22 }}>{target?.title}</h1>
            <span className="badge" style={{ background: priorityColor + '20', color: priorityColor }}>
              {target?.priority}
            </span>
            {target?.isOnTrack !== null && (
              <span style={{ fontSize: 13, color: target?.isOnTrack ? '#22c55e' : '#ef4444', fontWeight: 600 }}>
                {target?.isOnTrack ? '✓ On track' : '⚠ Behind'}
              </span>
            )}
          </div>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>
            {target?.daysRemaining > 0 ? `${target.daysRemaining} days remaining` : 'Deadline passed'}
            &nbsp;·&nbsp;{Math.round(target?.progressPercent ?? 0)}% complete
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-secondary"
            disabled={generateTimeline.isPending}
            onClick={() => generateTimeline.mutate()}>
            {generateTimeline.isPending ? 'Generating…' : 'Regenerate Timeline'}
          </button>
          {!confirmDelete ? (
            <button className="btn btn-secondary" style={{ color: 'var(--danger)' }}
              onClick={() => setConfirmDelete(true)}>
              Delete Timeline
            </button>
          ) : (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: 'var(--danger)' }}>Clear all pending items?</span>
              <button className="btn btn-danger"
                disabled={deleteTimeline.isPending}
                onClick={() => deleteTimeline.mutate()}>
                {deleteTimeline.isPending ? 'Deleting…' : 'Yes, delete'}
              </button>
              <button className="btn btn-secondary" onClick={() => setConfirmDelete(false)}>Cancel</button>
            </div>
          )}
        </div>
      </div>

      {hasMissedInAnyDay && (
        <div style={{
          background: '#f59e0b18', border: '1px solid #f59e0b40',
          borderRadius: 8, padding: '12px 16px', marginBottom: 20,
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <p style={{ fontSize: 13, color: '#92400e' }}>
            Some tasks from earlier days are still pending. Regenerating will carry them forward.
          </p>
          <button className="btn btn-secondary" style={{ fontSize: 12, flexShrink: 0, marginLeft: 12 }}
            disabled={generateTimeline.isPending}
            onClick={() => generateTimeline.mutate()}>
            Replan from today
          </button>
        </div>
      )}

      {generateTimeline.error && (
        <div className="error-box" style={{ marginBottom: 16 }}>
          {generateTimeline.error.message}
        </div>
      )}

      <div style={{ display: 'flex', gap: 6, marginBottom: 20 }}>
        {TIERS.map((t) => (
          <button key={t} onClick={() => setTier(t)}
            className={tier === t ? 'btn btn-primary' : 'btn btn-secondary'}
            style={{ fontSize: 13 }}>
            {t.charAt(0) + t.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {days.length === 0 ? (
        <div className="empty-state">
          <p>No timeline generated yet.</p>
          <button className="btn btn-primary" style={{ marginTop: 16 }}
            disabled={generateTimeline.isPending}
            onClick={() => generateTimeline.mutate()}>
            {generateTimeline.isPending ? 'Generating…' : 'Generate Timeline'}
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', gap: 12, overflowX: 'auto', paddingBottom: 8, flexWrap: 'wrap' }}>
          {days.map((day) => (
            <DayColumn
              key={day.date}
              day={day}
              tier={tier}
              marking={marking}
              isToday={day.date === todayStr}
              onDone={(itemId) => markItem.mutate({ itemId, status: 'DONE' })}
              onSkip={(itemId) => markItem.mutate({ itemId, status: 'SKIPPED' })}
            />
          ))}
        </div>
      )}
    </div>
  )
}
