import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { topicApi } from '../api/topicApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import ConfirmDeleteModal from '../components/common/ConfirmDeleteModal'
import { formatDate } from '../utils/formatters'

function NotesSection({ topic }) {
  const hasNotes = topic.combinedNotes || topic.revisionNotes || topic.mistakesToAvoid || topic.patterns
  if (!hasNotes) return <div className="empty-state">No combined notes yet. Generate notes for lectures covering this topic.</div>

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {topic.combinedNotes && (
        <div>
          <div className="section-title">Combined Notes</div>
          <div className="card prose">{topic.combinedNotes}</div>
        </div>
      )}
      {topic.revisionNotes && (
        <div>
          <div className="section-title">Revision Notes</div>
          <div className="card prose">{topic.revisionNotes}</div>
        </div>
      )}
      {topic.mistakesToAvoid && (
        <div>
          <div className="section-title">Mistakes to Avoid</div>
          <div className="card">
            <ul className="bulleted">
              {(Array.isArray(topic.mistakesToAvoid) ? topic.mistakesToAvoid : Object.values(topic.mistakesToAvoid)).map((m, i) => (
                <li key={i} style={{ fontSize: 13, marginBottom: 4 }}>{typeof m === 'string' ? m : JSON.stringify(m)}</li>
              ))}
            </ul>
          </div>
        </div>
      )}
      {topic.patterns && (
        <div>
          <div className="section-title">Patterns</div>
          <div className="card">
            <ul className="bulleted">
              {(Array.isArray(topic.patterns) ? topic.patterns : Object.values(topic.patterns)).map((p, i) => (
                <li key={i} style={{ fontSize: 13, marginBottom: 4 }}>{typeof p === 'string' ? p : JSON.stringify(p)}</li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  )
}

function TeachBackTab({ topicId }) {
  const [answer, setAnswer] = useState('')
  const [result, setResult] = useState(null)
  const teach = useMutation({
    mutationFn: (text) => topicApi.teachBack(topicId, text),
    onSuccess: (data) => { setResult(data); setAnswer('') },
  })

  return (
    <div>
      <div style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 16 }}>
        Explain this topic in your own words — the AI will evaluate your understanding.
      </div>
      <textarea
        rows={6}
        value={answer}
        onChange={(e) => setAnswer(e.target.value)}
        placeholder="Explain this topic as if you're teaching it to someone…"
        style={{ marginBottom: 12 }}
      />
      <button
        className="btn btn-primary"
        onClick={() => teach.mutate(answer)}
        disabled={teach.isPending || !answer.trim()}
      >
        {teach.isPending ? '⏳ Evaluating…' : 'Submit Explanation'}
      </button>
      {teach.error && <div className="error-box" style={{ marginTop: 12 }}>{teach.error.message}</div>}

      {result && (
        <div style={{ marginTop: 20 }}>
          <div className="section-title">Feedback</div>
          <div className="card">
            {result.score != null && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
                <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--accent)' }}>{result.score}/10</div>
                <div style={{ fontSize: 13, color: 'var(--muted)' }}>Understanding score</div>
              </div>
            )}
            {result.feedback && <div style={{ fontSize: 13, whiteSpace: 'pre-wrap' }}>{result.feedback}</div>}
            {result.gaps?.length > 0 && (
              <div style={{ marginTop: 12 }}>
                <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 6 }}>Gaps detected:</div>
                <ul className="bulleted">
                  {result.gaps.map((g, i) => (
                    <li key={i} style={{ fontSize: 13, color: '#f59e0b' }}>{g}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default function TopicDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [tab, setTab] = useState('notes')
  const [showDeleteModal, setShowDeleteModal] = useState(false)

  const deleteTopic = useMutation({
    mutationFn: () => topicApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      navigate('/topics')
    },
  })

  const { data: topic, isLoading, error } = useQuery({
    queryKey: ['topic', id],
    queryFn: () => topicApi.getById(id),
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const STATUS_COLOR = {
    NOT_STARTED: 'var(--muted)',
    LEARNING: '#f59e0b',
    REVISING: '#6366f1',
    MASTERED: 'var(--success)',
  }

  const tabs = ['notes', 'teach-back', 'lectures']

  return (
    <div>
      <div style={{ marginBottom: 20 }}>
        <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 4 }}>
          <Link to="/topics" style={{ color: 'var(--accent)' }}>← Topics</Link>
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
          <div>
            <h1 style={{ fontSize: 22, fontWeight: 700, marginBottom: 4 }}>{topic.name}</h1>
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <span className="badge" style={{
                background: (STATUS_COLOR[topic.status] ?? 'var(--muted)') + '25',
                color: STATUS_COLOR[topic.status] ?? 'var(--muted)',
              }}>
                {topic.status?.replace('_', ' ')}
              </span>
              {topic.category && (
                <span style={{ fontSize: 12, color: 'var(--muted)' }}>{topic.category}</span>
              )}
              {topic.difficulty && (
                <span style={{ fontSize: 12, color: 'var(--muted)' }}>{topic.difficulty}</span>
              )}
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
          <button className="btn btn-sm btn-danger" onClick={() => setShowDeleteModal(true)}>Delete</button>
          <div className="card" style={{ minWidth: 100, textAlign: 'center', padding: '12px 16px' }}>
            <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--accent)' }}>{topic.masteryScore ?? 0}%</div>
            <div style={{ fontSize: 11, color: 'var(--muted)' }}>Mastery</div>
          </div>
          </div>
        </div>

        {topic.description && (
          <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 8 }}>{topic.description}</p>
        )}
      </div>

      <div className="tabs">
        {tabs.map((t) => (
          <button key={t} className={`tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
            {t === 'notes' ? 'Notes' : t === 'teach-back' ? 'Teach Back' : 'Source Lectures'}
          </button>
        ))}
      </div>

      {tab === 'notes' && <NotesSection topic={topic} />}
      {tab === 'teach-back' && <TeachBackTab topicId={id} />}
      {showDeleteModal && (
        <ConfirmDeleteModal
          title="Delete Topic"
          itemName={topic.name}
          isPending={deleteTopic.isPending}
          onConfirm={() => deleteTopic.mutate()}
          onClose={() => setShowDeleteModal(false)}
        />
      )}
      {tab === 'lectures' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {topic.linkedLectures?.length > 0 ? topic.linkedLectures.map((l) => (
            <Link key={l.id} to={`/lectures/${l.id}`}>
              <div
                className="card"
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}
                onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
              >
                <div>
                  <div style={{ fontWeight: 500 }}>{l.title}</div>
                  <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>
                    {l.moduleName && `${l.moduleName} · `}{formatDate(l.createdAt)}
                  </div>
                </div>
                <span style={{ fontSize: 12, color: 'var(--accent)' }}>→</span>
              </div>
            </Link>
          )) : (
            <div className="empty-state">No linked lectures.</div>
          )}
        </div>
      )}
    </div>
  )
}
