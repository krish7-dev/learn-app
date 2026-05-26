import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { targetApi } from '../api/targetApi'
import { courseApi } from '../api/courseApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'

const PRIORITY_COLORS = { HIGH: '#ef4444', MEDIUM: '#f59e0b', LOW: '#6b7280' }
const SCOPE_OPTS = ['COURSE', 'MODULE', 'TOPIC', 'CUSTOM']

function TargetForm({ onClose }) {
  const qc = useQueryClient()
  const [form, setForm] = useState({
    title: '', description: '', targetScope: 'COURSE',
    courseId: '', moduleName: '', targetDate: '',
    dailyMinutes: 60, priority: 'MEDIUM',
  })

  const { data: coursesData } = useQuery({
    queryKey: ['courses'],
    queryFn: () => courseApi.list(0, 50),
  })
  const courses = coursesData?.content ?? []

  const create = useMutation({
    mutationFn: (data) => targetApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['targets'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
  })

  function set(k, v) { setForm((f) => ({ ...f, [k]: v })) }

  const valid = form.title.trim() && form.targetDate && form.dailyMinutes

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 500 }} onClick={(e) => e.stopPropagation()}>
        <h2 style={{ marginBottom: 20 }}>New Learning Target</h2>

        <div className="form-group">
          <label>Title *</label>
          <input value={form.title} onChange={(e) => set('title', e.target.value)} placeholder="e.g. Master DSA for interviews" />
        </div>

        <div className="form-group">
          <label>Scope</label>
          <select value={form.targetScope} onChange={(e) => set('targetScope', e.target.value)}>
            {SCOPE_OPTS.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>

        {(form.targetScope === 'COURSE' || form.targetScope === 'MODULE') && (
          <div className="form-group">
            <label>Course</label>
            <select value={form.courseId} onChange={(e) => set('courseId', e.target.value)}>
              <option value="">— select course —</option>
              {courses.map((c) => <option key={c.id} value={c.id}>{c.title}</option>)}
            </select>
          </div>
        )}

        {form.targetScope === 'MODULE' && (
          <div className="form-group">
            <label>Module name</label>
            <input value={form.moduleName} onChange={(e) => set('moduleName', e.target.value)} placeholder="e.g. Arrays & Hashing" />
          </div>
        )}

        <div style={{ display: 'flex', gap: 12 }}>
          <div className="form-group" style={{ flex: 1 }}>
            <label>Target date *</label>
            <input type="date" value={form.targetDate} min={new Date().toISOString().slice(0, 10)}
              onChange={(e) => set('targetDate', e.target.value)} />
          </div>
          <div className="form-group" style={{ flex: 1 }}>
            <label>Priority</label>
            <select value={form.priority} onChange={(e) => set('priority', e.target.value)}>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>

        <div className="form-group">
          <label>Daily budget: {form.dailyMinutes} min</label>
          <input type="range" min={15} max={180} step={15} value={form.dailyMinutes}
            onChange={(e) => set('dailyMinutes', Number(e.target.value))}
            style={{ width: '100%', accentColor: 'var(--accent)' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>
            <span>15 min</span><span>3 hrs</span>
          </div>
        </div>

        <div className="form-group">
          <label>Description</label>
          <textarea rows={2} value={form.description} onChange={(e) => set('description', e.target.value)}
            placeholder="Optional notes about this target" />
        </div>

        {create.error && <div className="error-box" style={{ marginBottom: 12 }}>{create.error.message}</div>}

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!valid || create.isPending}
            onClick={() => create.mutate({
              ...form,
              courseId: form.courseId ? Number(form.courseId) : undefined,
              moduleName: form.moduleName || undefined,
            })}>
            {create.isPending ? 'Creating…' : 'Create Target'}
          </button>
        </div>
      </div>
    </div>
  )
}

function ProgressBar({ percent, total }) {
  if (total === 0) return <p style={{ fontSize: 12, color: 'var(--muted)', marginTop: 4 }}>Add lectures to see progress</p>
  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--muted)', marginBottom: 4 }}>
        <span>Progress</span><span>{Math.round(percent)}%</span>
      </div>
      <div style={{ height: 6, background: 'var(--border)', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${Math.min(100, percent)}%`, background: 'var(--accent)', borderRadius: 3, transition: 'width 0.3s' }} />
      </div>
    </div>
  )
}

function TargetCard({ target, onGenerate, generating }) {
  const priorityColor = PRIORITY_COLORS[target.priority] ?? '#6b7280'
  const daysLeft = target.daysRemaining
  const [confirmGen, setConfirmGen] = useState(false)
  const isGenerating = generating === target.id

  return (
    <div className="card" style={{ padding: '20px 24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
        <h3 style={{ fontSize: 15, fontWeight: 600, marginRight: 12 }}>{target.title}</h3>
        <span className="badge" style={{ background: priorityColor + '20', color: priorityColor, flexShrink: 0 }}>
          {target.priority}
        </span>
      </div>

      {target.description && <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 8 }}>{target.description}</p>}

      <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 4 }}>
        Scope: {target.targetScope}{target.moduleName ? ` · ${target.moduleName}` : ''}
        &nbsp;·&nbsp;
        {daysLeft > 0 ? `${daysLeft} days left` : 'Overdue'}
        {target.isOnTrack !== null && (
          <span style={{ marginLeft: 8, color: target.isOnTrack ? '#22c55e' : '#ef4444' }}>
            {target.isOnTrack ? '✓ On track' : '⚠ Behind'}
          </span>
        )}
      </div>

      <ProgressBar percent={target.progressPercent} total={target.totalLectures} />

      <div style={{ display: 'flex', gap: 8, marginTop: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        {!confirmGen ? (
          <button className="btn btn-primary" style={{ fontSize: 13 }}
            disabled={isGenerating}
            onClick={() => setConfirmGen(true)}>
            {isGenerating ? 'Generating…' : 'Generate Timeline'}
          </button>
        ) : (
          <>
            <span style={{ fontSize: 13, color: 'var(--muted)' }}>Generate plan now?</span>
            <button className="btn btn-primary" style={{ fontSize: 13 }}
              disabled={isGenerating}
              onClick={() => { onGenerate(target.id); setConfirmGen(false) }}>
              {isGenerating ? 'Generating…' : 'Yes'}
            </button>
            <button className="btn btn-secondary" style={{ fontSize: 13 }}
              onClick={() => setConfirmGen(false)}>
              Cancel
            </button>
          </>
        )}
        <Link to={`/targets/${target.id}/timeline`}>
          <button className="btn btn-secondary" style={{ fontSize: 13 }}>View Week →</button>
        </Link>
      </div>
    </div>
  )
}

export default function LearningTargetPage() {
  const [showForm, setShowForm] = useState(false)
  const [generating, setGenerating] = useState(null)
  const [genError, setGenError] = useState(null)
  const qc = useQueryClient()

  const { data: targets = [], isLoading, error } = useQuery({
    queryKey: ['targets'],
    queryFn: targetApi.list,
  })

  useEffect(() => {
    if (!generating) return
    const interval = setInterval(async () => {
      try {
        const data = await targetApi.getGenerationStatus(generating)
        if (data.status === 'DONE') {
          clearInterval(interval)
          qc.invalidateQueries({ queryKey: ['targets'] })
          qc.invalidateQueries({ queryKey: ['dashboard'] })
          setGenerating(null)
        } else if (data.status === 'ERROR') {
          clearInterval(interval)
          setGenError(data.message || 'Generation failed')
          setGenerating(null)
        }
      } catch (e) {
        clearInterval(interval)
        setGenError(e.message)
        setGenerating(null)
      }
    }, 3000)
    return () => clearInterval(interval)
  }, [generating])

  const generateTimeline = useMutation({
    mutationFn: (id) => targetApi.generateTimeline(id),
    onMutate: (id) => { setGenerating(id); setGenError(null) },
    onError: (err) => { setGenerating(null); setGenError(err.message) },
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  return (
    <div>
      <div className="page-header">
        <h1>Learning Targets</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(true)}>+ New Target</button>
      </div>

      {targets.length === 0 ? (
        <div className="empty-state">
          <p>No active targets yet. Set a target to get an AI-generated study timeline.</p>
          <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => setShowForm(true)}>
            Create your first target
          </button>
        </div>
      ) : (
        <div className="grid-2">
          {targets.map((t) => (
            <TargetCard key={t.id} target={t}
              onGenerate={(id) => generateTimeline.mutate(id)}
              generating={generating} />
          ))}
        </div>
      )}

      {genError && (
        <div className="error-box" style={{ marginTop: 16 }}>
          Timeline generation failed: {genError}
        </div>
      )}

      {showForm && <TargetForm onClose={() => setShowForm(false)} />}
    </div>
  )
}
