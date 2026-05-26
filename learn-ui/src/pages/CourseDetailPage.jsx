import { useState, useMemo } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { courseApi } from '../api/courseApi'
import { lectureApi } from '../api/lectureApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import { formatDate, statusColor } from '../utils/formatters'
import ConfirmDeleteModal from '../components/common/ConfirmDeleteModal'

function Combobox({ value, onChange, options = [], placeholder = '' }) {
  const [open, setOpen] = useState(false)
  const filtered = options.filter(s => s.toLowerCase().includes(value.toLowerCase()))
  return (
    <div style={{ position: 'relative' }}>
      <input
        className="input"
        value={value}
        onChange={(e) => { onChange(e.target.value); setOpen(true) }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(() => setOpen(false), 150)}
        placeholder={placeholder}
      />
      {open && filtered.length > 0 && (
        <div style={{
          position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 100,
          background: 'var(--card)', border: '1px solid var(--border)',
          borderRadius: 6, boxShadow: '0 4px 16px rgba(0,0,0,0.15)', marginTop: 2,
        }}>
          {filtered.map(s => (
            <div key={s} onMouseDown={() => { onChange(s); setOpen(false) }}
              style={{ padding: '8px 12px', fontSize: 13, cursor: 'pointer', borderBottom: '1px solid var(--border)' }}
              onMouseEnter={e => e.currentTarget.style.background = 'var(--surface2)'}
              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
              {s}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function parsePaste(text, defaultModule) {
  return text
    .split('\n')
    .map(line => line.trim())
    .filter(line => line && !line.startsWith('#') && !line.startsWith('//'))
    .map((line, i) => {
      const colonIdx = line.indexOf(':')
      if (colonIdx > 0 && colonIdx < 40) {
        const left = line.slice(0, colonIdx).trim()
        const right = line.slice(colonIdx + 1).trim()
        if (right) return { moduleName: left, title: right, sourceOrder: i + 1 }
      }
      return { moduleName: defaultModule || '', title: line, sourceOrder: i + 1 }
    })
    .filter(item => item.title)
}

function ImportJsonBatchModal({ courseId, existingSources, existingModules, onClose }) {
  const [moduleName, setModuleName] = useState('')
  const [sourceName, setSourceName] = useState('')
  const [files, setFiles] = useState([])
  const [error, setError] = useState('')
  const qc = useQueryClient()

  const importBatch = useMutation({
    mutationFn: async () => {
      const contents = await Promise.all(
        files.map(f => f.text())
      )
      return lectureApi.importNotesBatch(courseId, { moduleName: moduleName || null, sourceName: sourceName || null, contents })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['lectures', courseId] })
      qc.invalidateQueries({ queryKey: ['learning-tree'] })
      onClose()
    },
    onError: (e) => setError(e.message),
  })

  const handleFiles = (e) => {
    const picked = Array.from(e.target.files)
      .filter(f => f.name.endsWith('.json'))
      .sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' }))
    setFiles(picked)
    setError('')
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 560, width: '90vw' }} onClick={e => e.stopPropagation()}>
        <h2>Import JSON Notes (Batch)</h2>
        <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 16 }}>
          Select multiple <code>.json</code> files. Each file should follow the same structure as Import Notes.
          A lecture will be created for each file using the <code>title</code> field inside the JSON.
        </p>

        <div className="form-grid-2" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 12 }}>
          <div className="form-group" style={{ margin: 0 }}>
            <label>Module <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>(optional)</span></label>
            <Combobox value={moduleName} onChange={setModuleName} options={existingModules} placeholder="e.g. Arrays" />
          </div>
          <div className="form-group" style={{ margin: 0 }}>
            <label>Source <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>(optional)</span></label>
            <Combobox value={sourceName} onChange={setSourceName} options={existingSources} placeholder="e.g. Scaler" />
          </div>
        </div>

        <div className="form-group">
          <label>JSON Files</label>
          <input
            type="file"
            multiple
            accept=".json"
            onChange={handleFiles}
            style={{ fontSize: 13 }}
          />
        </div>

        {files.length > 0 && (
          <div style={{ marginBottom: 12, border: '1px solid var(--border)', borderRadius: 6, maxHeight: 200, overflowY: 'auto', fontSize: 12 }}>
            {files.map((f, i) => (
              <div key={i} style={{ padding: '6px 12px', borderBottom: '1px solid var(--border)', display: 'flex', gap: 8 }}>
                <span style={{ color: 'var(--muted)', flexShrink: 0 }}>{i + 1}.</span>
                <span>{f.name}</span>
              </div>
            ))}
          </div>
        )}

        {error && <div className="error-box" style={{ marginBottom: 12 }}>{error}</div>}

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button
            className="btn btn-primary"
            disabled={files.length === 0 || importBatch.isPending}
            onClick={() => importBatch.mutate()}
          >
            {importBatch.isPending ? 'Importing…' : `Import ${files.length} File${files.length !== 1 ? 's' : ''}`}
          </button>
        </div>
      </div>
    </div>
  )
}

function BulkImportModal({ courseId, existingSources, onClose }) {
  const [text, setText] = useState('')
  const [defaultModule, setDefaultModule] = useState('')
  const [sourceName, setSourceName] = useState('')
  const qc = useQueryClient()

  const parsed = useMemo(() => parsePaste(text, defaultModule), [text, defaultModule])
  const moduleCount = new Set(parsed.map(p => p.moduleName).filter(Boolean)).size

  const parseWithAi = useMutation({
    mutationFn: () => lectureApi.parseList(courseId, text),
    onSuccess: (data) => setText(data.formatted),
  })

  const bulkCreate = useMutation({
    mutationFn: () => lectureApi.bulkCreate(courseId, {
      sourceName: sourceName || null,
      lectures: parsed.map(item => ({
        title: item.title,
        moduleName: item.moduleName || null,
        sourceOrder: item.sourceOrder,
      })),
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['lectures', courseId] })
      qc.invalidateQueries({ queryKey: ['learning-tree'] })
      onClose()
    },
  })

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 640, width: '90vw' }} onClick={e => e.stopPropagation()}>
        <h2>Import Lecture List</h2>
        <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 16 }}>
          Paste your lecture names — messy or structured. Use <strong>Parse with AI</strong> to auto-assign modules,
          or format manually as <code>Module: Title</code>.
        </p>

        <div className="form-grid-2" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 12 }}>
          <div className="form-group" style={{ margin: 0 }}>
            <label>Source <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>(optional)</span></label>
            <Combobox value={sourceName} onChange={setSourceName}
              options={existingSources} placeholder="e.g. Striver A2Z, Scaler…" />
          </div>
          <div className="form-group" style={{ margin: 0 }}>
            <label>Default Module <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>(if no prefix &amp; no AI parse)</span></label>
            <input className="input" value={defaultModule} onChange={e => setDefaultModule(e.target.value)}
              placeholder="e.g. General" />
          </div>
        </div>

        <div className="form-group">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <label style={{ margin: 0 }}>Lecture List</label>
            <button
              className="btn btn-secondary"
              style={{ fontSize: 11, padding: '3px 10px' }}
              disabled={!text.trim() || parseWithAi.isPending}
              onClick={() => parseWithAi.mutate()}>
              {parseWithAi.isPending ? '✦ Parsing…' : '✦ Parse with AI'}
            </button>
          </div>
          <textarea
            rows={10}
            value={text}
            onChange={e => setText(e.target.value)}
            placeholder={`Paste anything:\n\n1. Introduction to Arrays\n2. Two Pointers\nSliding Window Basics\nBinary Search\n\n...AI will group them into modules`}
            style={{ fontFamily: 'monospace', fontSize: 13 }}
          />
          {parseWithAi.error && (
            <p style={{ fontSize: 12, color: 'var(--danger)', marginTop: 4 }}>{parseWithAi.error.message}</p>
          )}
        </div>

        {parsed.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 8 }}>
              Preview — {parsed.length} lecture{parsed.length !== 1 ? 's' : ''} across {moduleCount} module{moduleCount !== 1 ? 's' : ''}
            </div>
            <div style={{
              maxHeight: 200, overflowY: 'auto', border: '1px solid var(--border)',
              borderRadius: 6, fontSize: 12,
            }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ background: 'var(--surface2)' }}>
                    <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, width: '35%', borderBottom: '1px solid var(--border)' }}>Module</th>
                    <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, borderBottom: '1px solid var(--border)' }}>Title</th>
                  </tr>
                </thead>
                <tbody>
                  {parsed.map((item, i) => (
                    <tr key={i} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '5px 10px', color: item.moduleName ? 'var(--text)' : 'var(--muted)' }}>
                        {item.moduleName || <em>none</em>}
                      </td>
                      <td style={{ padding: '5px 10px' }}>{item.title}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {bulkCreate.error && (
          <div className="error-box" style={{ marginBottom: 12 }}>{bulkCreate.error.message}</div>
        )}

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button
            className="btn btn-primary"
            disabled={parsed.length === 0 || bulkCreate.isPending}
            onClick={() => bulkCreate.mutate()}>
            {bulkCreate.isPending ? 'Importing…' : `Import ${parsed.length} Lecture${parsed.length !== 1 ? 's' : ''}`}
          </button>
        </div>
      </div>
    </div>
  )
}

function CreateLectureModal({ courseId, existingSources, existingModules, onClose }) {
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
        <div className="form-grid-2" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div className="form-group">
            <label>Module <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>(auto-set by AI if blank)</span></label>
            <Combobox value={form.moduleName} onChange={(v) => setForm({ ...form, moduleName: v })}
              options={existingModules} placeholder="e.g. Arrays" />
          </div>
          <div className="form-group">
            <label>Order</label>
            <input type="number" value={form.sourceOrder} onChange={(e) => setForm({ ...form, sourceOrder: e.target.value })} />
          </div>
        </div>
        <div className="form-group">
          <label>Source</label>
          <Combobox value={form.sourceName} onChange={(v) => setForm({ ...form, sourceName: v })}
            options={existingSources} placeholder="e.g. Scaler, Striver A2Z…" />
        </div>
        <div className="form-group">
          <label>Raw Transcript <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>(optional — paste later)</span></label>
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

function ProgressBar({ completed, withNotes, withTranscript, noContent, total }) {
  if (total === 0) return null
  const pctComplete = Math.round((completed / total) * 100)
  const pctNotes    = Math.round((withNotes / total) * 100)
  const pctTranscript = Math.round((withTranscript / total) * 100)

  return (
    <div style={{ marginBottom: 20 }}>
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 8 }}>
        <Stat label="Total" value={total} color="var(--muted)" />
        <Stat label="Completed" value={completed} color="var(--success)" />
        <Stat label="Notes ready" value={withNotes} color="#2563eb" />
        <Stat label="Transcript added" value={withTranscript} color="#7c3aed" />
        <Stat label="No content yet" value={noContent} color="var(--muted)" />
      </div>
      <div style={{ height: 6, background: 'var(--border)', borderRadius: 3, overflow: 'hidden', display: 'flex' }}>
        <div style={{ width: `${pctComplete}%`, background: 'var(--success)', transition: 'width 0.4s' }} />
        <div style={{ width: `${pctNotes}%`, background: '#2563eb', transition: 'width 0.4s' }} />
        <div style={{ width: `${pctTranscript}%`, background: '#7c3aed', transition: 'width 0.4s' }} />
      </div>
    </div>
  )
}

function Stat({ label, value, color }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
      <span style={{ width: 8, height: 8, borderRadius: '50%', background: color, flexShrink: 0 }} />
      <span style={{ fontSize: 12, color: 'var(--muted)' }}>{label}</span>
      <span style={{ fontSize: 13, fontWeight: 600 }}>{value}</span>
    </div>
  )
}

export default function CourseDetailPage() {
  const { id } = useParams()
  const [showModal, setShowModal] = useState(false)
  const [showImport, setShowImport] = useState(false)
  const [showJsonBatch, setShowJsonBatch] = useState(false)
  const [deleting, setDeleting] = useState(null)
  const [deletingModule, setDeletingModule] = useState(null)
  const [expanded, setExpanded] = useState(new Set())
  const [renamingModule, setRenamingModule] = useState(null)
  const [renameValue, setRenameValue] = useState('')

  const toggleModule = (module) =>
    setExpanded(prev => {
      const next = new Set(prev)
      next.has(module) ? next.delete(module) : next.add(module)
      return next
    })
  const qc = useQueryClient()

  const { data: course, isLoading: cl } = useQuery({
    queryKey: ['course', id],
    queryFn: () => courseApi.getById(id),
  })

  const { data: lectureData, isLoading: ll, error: le } = useQuery({
    queryKey: ['lectures', id],
    queryFn: () => lectureApi.listByCourse(id, 0, 500),
  })

  const deleteLecture = useMutation({
    mutationFn: (lectureId) => lectureApi.delete(lectureId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['lectures', id] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setDeleting(null)
    },
  })

  const reorderModule = useMutation({
    mutationFn: (newOrder) => courseApi.updateModuleOrder(id, newOrder),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['course', id] }),
  })

  const deleteModule = useMutation({
    mutationFn: (moduleName) => {
      const ids = (byModule[moduleName] ?? []).map(l => l.id)
      return Promise.all(ids.map(lid => lectureApi.delete(lid)))
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['lectures', id] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setDeletingModule(null)
    },
  })

  const renameModule = useMutation({
    mutationFn: ({ oldName, newName }) => courseApi.renameModule(id, oldName, newName),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['lectures', id] })
      setRenamingModule(null)
      setRenameValue('')
    },
  })

  if (cl || ll) return <LoadingSpinner />
  if (le) return <ErrorMessage message={le.message} />

  const lectures = lectureData?.content ?? []
  const existingSources = [...new Set(lectures.map(l => l.sourceName).filter(Boolean))]
  const existingModules = [...new Set(lectures.map(l => l.moduleName).filter(Boolean))]
  const byModule = lectures.reduce((acc, l) => {
    const key = l.moduleName || 'General'
    ;(acc[key] = acc[key] || []).push(l)
    return acc
  }, {})

  const savedOrder = course?.moduleOrder ?? []
  const moduleKeys = Object.keys(byModule).sort((a, b) => {
    const ai = savedOrder.indexOf(a)
    const bi = savedOrder.indexOf(b)
    if (ai === -1 && bi === -1) return 0
    if (ai === -1) return 1
    if (bi === -1) return -1
    return ai - bi
  })

  const moveModule = (index, dir) => {
    const next = [...moduleKeys]
    const target = index + dir
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    reorderModule.mutate(next)
  }

  const stats = {
    total: lectures.length,
    completed: lectures.filter(l => l.status === 'COMPLETED').length,
    withNotes: lectures.filter(l => l.contentStatus === 'NOTES_GENERATED').length,
    withTranscript: lectures.filter(l => l.contentStatus === 'TRANSCRIPT_ADDED').length,
    noContent: lectures.filter(l => !l.contentStatus || l.contentStatus === 'NOT_ADDED').length,
  }

  function downloadTxt() {
    const lines = []
    lines.push(`Course: ${course?.title ?? ''}`)
    lines.push(`Total: ${lectures.length} lectures`)
    lines.push('')
    moduleKeys.forEach(module => {
      lines.push(`=== ${module} ===`)
      byModule[module].forEach((l, i) => {
        lines.push(`${i + 1}. ${l.title} [${l.status}] [${l.contentStatus ?? 'NOT_ADDED'}]`)
      })
      lines.push('')
    })
    const blob = new Blob([lines.join('\n')], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${(course?.title ?? 'lectures').replace(/[^a-z0-9]/gi, '_')}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

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
        <div className="actions-row" style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-secondary" onClick={downloadTxt} disabled={lectures.length === 0}>Download</button>
          <button className="btn btn-secondary" onClick={() => setShowImport(true)}>Import List</button>
          <button className="btn btn-secondary" onClick={() => setShowJsonBatch(true)}>Import JSON Files</button>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Add Lecture</button>
        </div>
      </div>

      <ProgressBar {...stats} />

      {moduleKeys.map((module, idx) => {
        const items = byModule[module]
        const isCollapsed = !expanded.has(module)
        const notesReady = items.filter(l => l.contentStatus === 'NOTES_GENERATED').length
        return (
        <div key={module} style={{ marginBottom: 24 }}>
          <div
            className="section-title"
            style={{ display: 'flex', alignItems: 'center', gap: 8, userSelect: 'none' }}>
            {renamingModule === module ? (
              <form
                style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1 }}
                onSubmit={e => { e.preventDefault(); renameModule.mutate({ oldName: module, newName: renameValue }) }}>
                <input
                  autoFocus
                  className="input"
                  style={{ fontSize: 12, padding: '2px 8px', height: 28, flex: 1 }}
                  value={renameValue}
                  onChange={e => setRenameValue(e.target.value)}
                  onKeyDown={e => e.key === 'Escape' && (setRenamingModule(null), setRenameValue(''))}
                />
                <button className="btn btn-primary" style={{ fontSize: 11, padding: '2px 10px' }}
                  disabled={!renameValue.trim() || renameModule.isPending} type="submit">
                  {renameModule.isPending ? '…' : 'Save'}
                </button>
                <button className="btn btn-secondary" style={{ fontSize: 11, padding: '2px 8px' }} type="button"
                  onClick={() => { setRenamingModule(null); setRenameValue('') }}>Cancel</button>
              </form>
            ) : (
              <span
                onClick={() => toggleModule(module)}
                style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8, flex: 1 }}>
                <span style={{ fontSize: 11, color: 'var(--muted)', width: 12 }}>
                  {isCollapsed ? '▸' : '▾'}
                </span>
                {module}
                <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>
                  {notesReady}/{items.length} notes ready
                </span>
              </span>
            )}
            {renamingModule !== module && (
              <div style={{ display: 'flex', gap: 2, marginLeft: 'auto' }}>
                <button
                  className="btn btn-secondary"
                  style={{ fontSize: 11, padding: '1px 6px', lineHeight: 1.4 }}
                  onClick={() => { setRenamingModule(module); setRenameValue(module) }}
                  title="Rename module">✎</button>
                <button
                  className="btn btn-secondary"
                  style={{ fontSize: 11, padding: '1px 6px', lineHeight: 1.4 }}
                  disabled={idx === 0}
                  onClick={() => moveModule(idx, -1)}
                  title="Move up">▲</button>
                <button
                  className="btn btn-secondary"
                  style={{ fontSize: 11, padding: '1px 6px', lineHeight: 1.4 }}
                  disabled={idx === moduleKeys.length - 1}
                  onClick={() => moveModule(idx, 1)}
                  title="Move down">▼</button>
                <button
                  className="btn btn-sm btn-danger"
                  style={{ fontSize: 11, padding: '1px 8px', lineHeight: 1.4, marginLeft: 4 }}
                  onClick={() => setDeletingModule(module)}
                  title="Delete module">✕</button>
              </div>
            )}
          </div>
          {!isCollapsed && <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {items.map((l) => (
              <Link key={l.id} to={`/lectures/${l.id}`}>
                <div className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer', gap: 12 }}
                     onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--accent)'}
                     onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {l.title}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>
                      {l.sourceName && `${l.sourceName} · `}{formatDate(l.createdAt)}
                      {l.contentStatus === 'NOTES_GENERATED' && <span style={{ color: '#2563eb', marginLeft: 8 }}>✓ Notes</span>}
                      {l.contentStatus === 'TRANSCRIPT_ADDED' && <span style={{ color: '#7c3aed', marginLeft: 8 }}>✓ Transcript</span>}
                      {(!l.contentStatus || l.contentStatus === 'NOT_ADDED') && <span style={{ color: 'var(--muted)', marginLeft: 8 }}>No content</span>}
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
          </div>}
        </div>
        )
      })}

      {lectures.length === 0 && (
        <div className="empty-state">
          <p style={{ marginBottom: 12 }}>No lectures yet.</p>
          <button className="btn btn-primary" onClick={() => setShowImport(true)}>Import a lecture list</button>
        </div>
      )}

      {showImport && (
        <BulkImportModal
          courseId={id}
          existingSources={existingSources}
          onClose={() => setShowImport(false)}
        />
      )}
      {showJsonBatch && (
        <ImportJsonBatchModal
          courseId={id}
          existingSources={existingSources}
          existingModules={existingModules}
          onClose={() => setShowJsonBatch(false)}
        />
      )}
      {showModal && (
        <CreateLectureModal
          courseId={id}
          existingSources={existingSources}
          existingModules={existingModules}
          onClose={() => setShowModal(false)}
        />
      )}
      {deleting && (
        <ConfirmDeleteModal
          title="Delete Lecture"
          itemName={deleting.title}
          isPending={deleteLecture.isPending}
          onConfirm={() => deleteLecture.mutate(deleting.id)}
          onClose={() => setDeleting(null)}
        />
      )}
      {deletingModule && (
        <ConfirmDeleteModal
          title="Delete Module"
          itemName={`${deletingModule} (${byModule[deletingModule]?.length ?? 0} lectures)`}
          isPending={deleteModule.isPending}
          onConfirm={() => deleteModule.mutate(deletingModule)}
          onClose={() => setDeletingModule(null)}
        />
      )}
    </div>
  )
}
