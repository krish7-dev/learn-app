import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { courseApi } from '../api/courseApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import { formatDate, statusColor } from '../utils/formatters'
import ConfirmDeleteModal from '../components/common/ConfirmDeleteModal'

function CreateCourseModal({ onClose }) {
  const [form, setForm] = useState({ title: '', description: '', goal: '' })
  const qc = useQueryClient()
  const create = useMutation({
    mutationFn: courseApi.create,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['courses'] }); onClose() },
  })

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>New Course</h2>
        <div className="form-group">
          <label>Title *</label>
          <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="e.g. Scaler DSA" />
        </div>
        <div className="form-group">
          <label>Description</label>
          <textarea rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </div>
        <div className="form-group">
          <label>Goal</label>
          <input value={form.goal} onChange={(e) => setForm({ ...form, goal: e.target.value })} placeholder="e.g. Crack FAANG interviews" />
        </div>
        {create.error && <div className="error-box" style={{ marginBottom: 12 }}>{create.error.message}</div>}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!form.title || create.isPending}
            onClick={() => create.mutate(form)}>
            {create.isPending ? 'Creating…' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function CoursesPage() {
  const [showModal, setShowModal] = useState(false)
  const [deleting, setDeleting] = useState(null)
  const qc = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['courses'],
    queryFn: () => courseApi.list(),
  })

  const deleteCourse = useMutation({
    mutationFn: (id) => courseApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['courses'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setDeleting(null)
    },
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const courses = data?.content ?? []

  return (
    <div>
      <div className="page-header">
        <h1>Courses</h1>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ New Course</button>
      </div>

      {courses.length === 0 ? (
        <div className="empty-state">
          <p>No courses yet.</p>
          <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => setShowModal(true)}>Create your first course</button>
        </div>
      ) : (
        <div className="grid-2">
          {courses.map((c) => (
            <Link key={c.id} to={`/courses/${c.id}`}>
              <div className="card" style={{ cursor: 'pointer', height: '100%', position: 'relative' }}
                   onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                   onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                  <h3 style={{ fontSize: 15, fontWeight: 600 }}>{c.title}</h3>
                  <span className="badge" style={{ background: statusColor(c.status) + '25', color: statusColor(c.status), flexShrink: 0, marginLeft: 8 }}>
                    {c.status?.replace('_', ' ')}
                  </span>
                </div>
                {c.description && <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 8 }}>{c.description}</p>}
                {c.goal && <p style={{ fontSize: 12, color: 'var(--accent)' }}>🎯 {c.goal}</p>}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 12 }}>
                  <div style={{ fontSize: 11, color: 'var(--muted)' }}>Created {formatDate(c.createdAt)}</div>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={(e) => { e.preventDefault(); e.stopPropagation(); setDeleting(c) }}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {showModal && <CreateCourseModal onClose={() => setShowModal(false)} />}
      {deleting && (
        <ConfirmDeleteModal
          title="Delete Course"
          itemName={deleting.title}
          isPending={deleteCourse.isPending}
          onConfirm={() => deleteCourse.mutate(deleting.id)}
          onClose={() => setDeleting(null)}
        />
      )}
    </div>
  )
}
