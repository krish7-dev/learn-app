import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { courseApi } from '../api/courseApi'
import { lectureApi } from '../api/lectureApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import { formatDate, statusColor } from '../utils/formatters'
import ConfirmDeleteModal from '../components/common/ConfirmDeleteModal'

function CreateLectureModal({ courseId, onClose }) {
  const [form, setForm] = useState({ title: '', moduleName: '', sourceName: '', sourceOrder: '', rawContent: '' })
  const qc = useQueryClient()
  const create = useMutation({
    mutationFn: (data) => lectureApi.create(courseId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['lectures', courseId] }); onClose() },
  })

  const submit = () => create.mutate({
    ...form,
    sourceOrder: form.sourceOrder ? Number(form.sourceOrder) : null,
  })

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Add Lecture</h2>
        <div className="form-group">
          <label>Title *</label>
          <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="e.g. Arrays and Prefix Sum" />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div className="form-group">
            <label>Module</label>
            <input value={form.moduleName} onChange={(e) => setForm({ ...form, moduleName: e.target.value })} placeholder="e.g. Arrays" />
          </div>
          <div className="form-group">
            <label>Order</label>
            <input type="number" value={form.sourceOrder} onChange={(e) => setForm({ ...form, sourceOrder: e.target.value })} />
          </div>
        </div>
        <div className="form-group">
          <label>Source</label>
          <input value={form.sourceName} onChange={(e) => setForm({ ...form, sourceName: e.target.value })} placeholder="e.g. Scaler" />
        </div>
        <div className="form-group">
          <label>Raw Transcript (paste lecture content here)</label>
          <textarea rows={6} value={form.rawContent} onChange={(e) => setForm({ ...form, rawContent: e.target.value })} placeholder="Paste the lecture transcript…" />
          <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 4 }}>{form.rawContent.length}/50000</div>
        </div>
        {create.error && <div className="error-box" style={{ marginBottom: 12 }}>{create.error.message}</div>}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!form.title || create.isPending} onClick={submit}>
            {create.isPending ? 'Adding…' : 'Add Lecture'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function CourseDetailPage() {
  const { id } = useParams()
  const [showModal, setShowModal] = useState(false)
  const [deleting, setDeleting] = useState(null)
  const qc = useQueryClient()

  const { data: course, isLoading: cl } = useQuery({
    queryKey: ['course', id],
    queryFn: () => courseApi.getById(id),
  })

  const { data: lectureData, isLoading: ll, error: le } = useQuery({
    queryKey: ['lectures', id],
    queryFn: () => lectureApi.listByCourse(id),
  })

  const deleteLecture = useMutation({
    mutationFn: (lectureId) => lectureApi.delete(lectureId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['lectures', id] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setDeleting(null)
    },
  })

  if (cl || ll) return <LoadingSpinner />
  if (le) return <ErrorMessage message={le.message} />

  const lectures = lectureData?.content ?? []
  const byModule = lectures.reduce((acc, l) => {
    const key = l.moduleName || 'General'
    ;(acc[key] = acc[key] || []).push(l)
    return acc
  }, {})

  return (
    <div>
      <div className="page-header">
        <div>
          <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 4 }}>
            <Link to="/courses" style={{ color: 'var(--accent)' }}>Courses</Link> /
          </div>
          <h1>{course?.title}</h1>
          {course?.goal && <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>🎯 {course.goal}</p>}
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Add Lecture</button>
      </div>

      {Object.entries(byModule).map(([module, items]) => (
        <div key={module} style={{ marginBottom: 24 }}>
          <div className="section-title">{module}</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {items.map((l) => (
              <Link key={l.id} to={`/lectures/${l.id}`}>
                <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer', gap: 12 }}
                     onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                     onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{l.title}</div>
                    <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>
                      {l.sourceName && `${l.sourceName} · `}{formatDate(l.createdAt)}
                      {l.notesGenerated && <span style={{ color: 'var(--success)', marginLeft: 8 }}>✓ Notes</span>}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                    <span className="badge" style={{ background: statusColor(l.status) + '25', color: statusColor(l.status) }}>
                      {l.status?.replace('_', ' ')}
                    </span>
                    <button
                      className="btn btn-sm btn-danger"
                      onClick={(e) => { e.preventDefault(); e.stopPropagation(); setDeleting(l) }}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      ))}

      {lectures.length === 0 && (
        <div className="empty-state">No lectures yet. Add the first one!</div>
      )}

      {showModal && <CreateLectureModal courseId={id} onClose={() => setShowModal(false)} />}
      {deleting && (
        <ConfirmDeleteModal
          title="Delete Lecture"
          itemName={deleting.title}
          isPending={deleteLecture.isPending}
          onConfirm={() => deleteLecture.mutate(deleting.id)}
          onClose={() => setDeleting(null)}
        />
      )}
    </div>
  )
}
