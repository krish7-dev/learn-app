import { useState, useMemo, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { targetApi } from '../api/targetApi'
import { timelineApi } from '../api/timelineApi'
import { lectureApi } from '../api/lectureApi'
import { inferLectureType, inferPriority, inferEstimatedMinutes, makeLectureId, makeSourceCode } from '../utils/lectureInference'
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


const FOUNDATION_TOPIC_KEYWORDS = [
  ['introduction to problem solving', 'intro to problem'],        // 0
  ['time complexity', 'asymptotic'],                              // 1
  ['intermediate arrays', 'arrays', 'array'],                    // 2
  ['prefix sum'],                                                 // 3
  ['carry forward'],                                              // 4
  ['subarray', 'subarrays'],                                      // 5
  ['2d matri', 'matrix', 'matrices'],                             // 6
  ['sliding window'],                                             // 7
  ['bit manipulation', 'bitwise'],                                // 8
  ['maths', 'math'],                                              // 9
  ['recursion'],                                                  // 10
  ['sorting', 'bubble sort', 'merge sort', 'quick sort'],        // 11
  ['binary search'],                                              // 12
  ['two pointer', 'two-pointer'],                                 // 13
  ['hashing', 'hash map', 'hashmap'],                             // 14
  ['linked list'],                                                // 15
  ['stack', 'queue', 'deque'],                                    // 16
  ['tree', 'bst', 'binary tree'],                                 // 17
  ['heap', 'priority queue'],                                     // 18
  ['greedy'],                                                     // 19
  ['backtracking', 'backtrack'],                                  // 20
  ['dynamic programming', 'memoization', 'tabulation'],          // 21
  ['graph', 'bfs', 'dfs', 'shortest path', 'topological'],       // 22
]

function getFoundationTopicOrder(lecture) {
  const text = ((lecture.moduleName ?? '') + ' ' + lecture.title).toLowerCase()
  for (let i = 0; i < FOUNDATION_TOPIC_KEYWORDS.length; i++) {
    if (FOUNDATION_TOPIC_KEYWORDS[i].some(kw => text.includes(kw))) return i
  }
  return FOUNDATION_TOPIC_KEYWORDS.length
}

function getFoundationLevelOrder(lecture) {
  const mod = (lecture.moduleName ?? '').toLowerCase()
  if (mod.includes('intermediate')) return 0
  if (mod.includes('advanced')) return 2
  return 1
}

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

function DayColumn({ day, onDone, onSkip, marking, isToday }) {
  const items = day.items ?? []

  const dateObj = new Date(day.date + 'T00:00:00')
  const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' })
  const dayNum  = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })

  return (
    <div style={{
      minWidth: 160, flex: '1 1 160px',
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

function groupByWeek(days) {
  const weeks = []
  let current = []
  days.forEach((day, i) => {
    current.push(day)
    const date = new Date(day.date + 'T00:00:00')
    if (date.getDay() === 0 || i === days.length - 1) {
      weeks.push(current)
      current = []
    }
  })
  if (current.length) weeks.push(current)
  return weeks
}

function FullPlanView({ days, onDone, onSkip, marking }) {
  const todayStr = new Date().toLocaleDateString('en-CA')
  const [selectedDay, setSelectedDay] = useState(todayStr)

  const getItems = (day) => day.items ?? []

  const dayMap = useMemo(() => {
    const m = {}
    days.forEach(d => { m[d.date] = d })
    return m
  }, [days])

  const months = useMemo(() => {
    if (!days.length) return []
    const start = new Date(days[0].date + 'T00:00:00')
    const end   = new Date(days[days.length - 1].date + 'T00:00:00')
    const result = []
    let cur = new Date(start.getFullYear(), start.getMonth(), 1)
    const endMonth = new Date(end.getFullYear(), end.getMonth(), 1)
    while (cur <= endMonth) {
      result.push({ year: cur.getFullYear(), month: cur.getMonth() })
      cur = new Date(cur.getFullYear(), cur.getMonth() + 1, 1)
    }
    return result
  }, [days])

  const selectedData  = selectedDay ? dayMap[selectedDay] : null
  const selectedItems = selectedData ? getItems(selectedData) : []

  return (
    <div>
      {months.map(({ year, month }) => {
        const monthLabel  = new Date(year, month, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
        const firstDow    = new Date(year, month, 1).getDay()
        const daysInMonth = new Date(year, month + 1, 0).getDate()
        const cells = [
          ...Array(firstDow).fill(null),
          ...Array.from({ length: daysInMonth }, (_, i) => {
            const d = i + 1
            return `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
          }),
        ]

        return (
          <div key={`${year}-${month}`} style={{ marginBottom: 28 }}>
            <div style={{ fontSize: 14, fontWeight: 700, marginBottom: 8, color: 'var(--text)' }}>
              {monthLabel}
            </div>

            {/* Weekday headers */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 3, marginBottom: 3 }}>
              {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(d => (
                <div key={d} style={{ fontSize: 10, fontWeight: 600, color: 'var(--muted)', textAlign: 'center', paddingBottom: 2 }}>
                  {d}
                </div>
              ))}
            </div>

            {/* Day cells */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 3 }}>
              {cells.map((dateStr, i) => {
                if (!dateStr) return <div key={`e${i}`} />

                const dayData  = dayMap[dateStr]
                const items    = dayData ? getItems(dayData) : []
                const hasItems = items.length > 0
                const isToday  = dateStr === todayStr
                const isPast   = dateStr < todayStr
                const isSel    = selectedDay === dateStr
                const doneCount = items.filter(it => it.status === 'DONE').length
                const allDone  = hasItems && doneCount === items.length
                const types    = [...new Set(items.map(it => it.itemType))]
                const totalMins = items.reduce((s, it) => s + (it.estimatedMinutes ?? 0), 0)

                return (
                  <div
                    key={dateStr}
                    onClick={() => hasItems && setSelectedDay(isSel ? null : dateStr)}
                    style={{
                      minWidth: 0,
                      overflow: 'hidden',
                      border: isSel
                        ? '2px solid var(--accent)'
                        : isToday
                          ? '1.5px solid var(--accent)'
                          : '1px solid var(--border)',
                      borderRadius: 6,
                      padding: '4px 5px',
                      background: isSel
                        ? 'var(--accent)18'
                        : isToday
                          ? 'var(--accent)08'
                          : 'var(--card)',
                      cursor: hasItems ? 'pointer' : 'default',
                      opacity: isPast && !isToday ? 0.6 : 1,
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 2,
                    }}
                  >
                    {/* Date number */}
                    <span style={{
                      fontSize: 11, fontWeight: isToday ? 700 : 400, lineHeight: 1,
                      color: isToday ? 'var(--accent)' : hasItems ? 'var(--text)' : 'var(--muted)',
                    }}>
                      {parseInt(dateStr.slice(8))}
                    </span>

                    {/* Colored type dots */}
                    {hasItems && (
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                        {types.map(type => {
                          const cfg = TYPE_COLORS[type] ?? { color: '#6b7280' }
                          return (
                            <span key={type} style={{
                              width: 5, height: 5, borderRadius: '50%',
                              background: cfg.color, flexShrink: 0,
                            }} />
                          )
                        })}
                      </div>
                    )}

                    {/* Task title summary — wraps within cell width */}
                    {hasItems && (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                        {items.slice(0, 2).map(it => (
                          <span key={it.id} style={{
                            fontSize: 9, lineHeight: 1.3,
                            color: it.status === 'DONE' ? '#16a34a' : 'var(--muted)',
                            textDecoration: it.status === 'DONE' ? 'line-through' : 'none',
                            wordBreak: 'break-word',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}>
                            {it.title}
                          </span>
                        ))}
                        {items.length > 2 && (
                          <span style={{ fontSize: 9, color: 'var(--muted)', lineHeight: 1.3 }}>
                            +{items.length - 2}
                          </span>
                        )}
                      </div>
                    )}

                    {/* Done ratio + minutes */}
                    {hasItems && (
                      <div style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                        marginTop: 'auto',
                      }}>
                        <span style={{
                          fontSize: 9, lineHeight: 1,
                          color: allDone ? '#16a34a' : 'var(--muted)',
                        }}>
                          {doneCount}/{items.length}
                        </span>
                        <span style={{ fontSize: 9, color: 'var(--muted)', lineHeight: 1 }}>
                          {totalMins}m
                        </span>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        )
      })}

      {/* Selected day detail panel */}
      {selectedDay && selectedItems.length > 0 && (
        <div style={{
          border: '1px solid var(--border)', borderRadius: 8,
          background: 'var(--surface2)', overflow: 'hidden', marginTop: 4,
        }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: '10px 16px', borderBottom: '1px solid var(--border)',
          }}>
            <span style={{ fontSize: 14, fontWeight: 700 }}>
              {new Date(selectedDay + 'T00:00:00').toLocaleDateString('en-US', {
                weekday: 'long', month: 'long', day: 'numeric',
              })}
            </span>
            <button
              onClick={() => setSelectedDay(null)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)', fontSize: 16, lineHeight: 1 }}
            >✕</button>
          </div>
          <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
            {selectedItems.map(item => (
              <ItemCard key={item.id} item={item} onDone={onDone} onSkip={onSkip} marking={marking} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

const EXPORT_EXPECTED_COLS = 10
const EXPORT_COL_ESTIMATED_MINUTES = 9 // 0-indexed, last column

function ExportSettingsModal({ settings, onChange, onClose }) {
  const [local, setLocal] = useState(settings)
  const set = (key, val) => setLocal(s => ({ ...s, [key]: val }))

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 440 }} onClick={e => e.stopPropagation()}>
        <h2 style={{ marginBottom: 16 }}>Export Settings</h2>
        <p style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 20 }}>
          These values are sent to the AI with every Export / Send to AI request. Change them rarely.
        </p>

        <div className="form-group">
          <label>Plan Mode</label>
          <select value={local.planMode} onChange={e => set('planMode', e.target.value)}>
            <option value="CRASH_INTERVIEW_PREP">Crash Interview Prep</option>
            <option value="STRUCTURED_COURSE_COMPLETION">Structured Course Completion</option>
            <option value="REVISION_ONLY">Revision Only</option>
            <option value="BALANCED_LEARNING">Balanced Learning</option>
            <option value="FOUNDATION_REBUILD">Foundation Rebuild</option>
          </select>
          <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 6 }}>
            {{
              CRASH_INTERVIEW_PREP:        'Tight deadlines -- prioritize interview-critical topics first, skip optional, compress basics.',
              STRUCTURED_COURSE_COMPLETION:'Complete the course in order -- follow module sequence, add regular revision.',
              REVISION_ONLY:               'Already studied -- focus on revision and practice, avoid new lectures.',
              BALANCED_LEARNING:           'Middle ground -- study important lectures, add revision, less aggressive than crash mode.',
              FOUNDATION_REBUILD:          'Forgotten fundamentals -- rebuild Arrays, Hashing, Linked Lists, Stacks before moving to advanced topics.',
            }[local.planMode]}
          </div>
        </div>

        <div className="form-group">
          <label>Revision Style</label>
          <select value={local.revisionStyle} onChange={e => set('revisionStyle', e.target.value)}>
            <option value="spaced_revision">spaced_revision</option>
            <option value="none">none</option>
          </select>
        </div>

        <div style={{ display: 'flex', gap: 24, marginBottom: 16 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, cursor: 'pointer' }}>
            <input type="checkbox" checked={local.includeOptional}
              onChange={e => set('includeOptional', e.target.checked)} />
            Include Optional lectures
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, cursor: 'pointer' }}>
            <input type="checkbox" checked={local.includeContests}
              onChange={e => set('includeContests', e.target.checked)} />
            Include Contests
          </label>
        </div>

        <div className="form-group">
          <label>Known Weak Topics <span style={{ fontSize: 11, color: 'var(--muted)' }}>(comma-separated)</span></label>
          <input type="text" value={local.knownWeakTopics}
            onChange={e => set('knownWeakTopics', e.target.value)}
            placeholder="e.g. DP, Graphs" />
        </div>

        <div className="form-group">
          <label>Already Confident Topics <span style={{ fontSize: 11, color: 'var(--muted)' }}>(comma-separated)</span></label>
          <input type="text" value={local.alreadyConfidentTopics}
            onChange={e => set('alreadyConfidentTopics', e.target.value)}
            placeholder="e.g. Arrays, Hashing" />
        </div>

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={() => { onChange(local); onClose() }}>Save</button>
        </div>
      </div>
    </div>
  )
}

function validateExportRows(rows) {
  const errors = []
  rows.forEach((row, i) => {
    const cols = row.split(' | ')
    if (cols.length !== EXPORT_EXPECTED_COLS) {
      errors.push(`Row ${i + 1}: expected ${EXPORT_EXPECTED_COLS} cols, got ${cols.length}: "${row.slice(0, 120)}"`)
      return
    }
    const minsVal = cols[EXPORT_COL_ESTIMATED_MINUTES].trim()
    if (!/^\d+$/.test(minsVal)) {
      errors.push(`Row ${i + 1}: estimated_minutes is not digits-only: "${minsVal}"`)
    }
  })
  return errors
}

function ImportPlanModal({ initialJson = '', onClose, onImport, isPending, error }) {
  const [json, setJson] = useState(initialJson)
  const [parseError, setParseError] = useState(null)

  function handleImport() {
    setParseError(null)
    let parsed
    try {
      parsed = JSON.parse(json.trim())
    } catch {
      setParseError('Invalid JSON — paste the raw JSON output from the AI.')
      return
    }
    if (!parsed.days || !Array.isArray(parsed.days)) {
      setParseError('JSON must have a "days" array. Check the format in the exported file.')
      return
    }
    onImport(parsed)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 560 }} onClick={(e) => e.stopPropagation()}>
        <h2>Import AI Plan</h2>
        <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 16 }}>
          Paste the JSON schedule generated by your AI agent. Use <strong>Export for AI</strong> to get the lecture list and instructions to give the AI.
        </p>
        <div className="form-group">
          <label>JSON Plan</label>
          <textarea
            rows={12}
            value={json}
            onChange={(e) => setJson(e.target.value)}
            placeholder={'{\n  "days": [\n    {\n      "date": "2026-05-12",\n      "items": [...]\n    }\n  ]\n}'}
            style={{ fontFamily: 'monospace', fontSize: 12 }}
          />
        </div>
        {(parseError || error) && (
          <div className="error-box" style={{ marginBottom: 12 }}>
            {parseError || error?.message}
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!json.trim() || isPending} onClick={handleImport}>
            {isPending ? 'Importing…' : 'Import Plan'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function StudyTimelinePage() {
  const { id } = useParams()
  const [view, setView] = useState('full')
  const [marking, setMarking] = useState(null)
  const [confirmGenerate, setConfirmGenerate] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [isGenerating, setIsGenerating] = useState(false)
  const [showImport, setShowImport] = useState(false)
  const [aiDraft, setAiDraft] = useState('')
  const [showExportSettings, setShowExportSettings] = useState(false)
  const [exportSettings, setExportSettings] = useState({
    planMode: 'CRASH_INTERVIEW_PREP',
    includeOptional: false,
    includeContests: true,
    revisionStyle: 'spaced_revision',
    knownWeakTopics: '',
    alreadyConfidentTopics: '',
  })
  const qc = useQueryClient()

  const { data: weekData, isLoading, error } = useQuery({
    queryKey: ['target', id, 'week'],
    queryFn: () => targetApi.getWeek(id),
  })

  const { data: fullData, isLoading: fullLoading } = useQuery({
    queryKey: ['target', id, 'full'],
    queryFn: () => targetApi.getFullTimeline(id),
    enabled: view === 'full',
  })

  const courseId = (fullData?.target ?? weekData?.target)?.courseId
  const { data: lectureData } = useQuery({
    queryKey: ['lectures', String(courseId)],
    queryFn: () => lectureApi.listByCourse(courseId, 0, 500),
    enabled: !!courseId,
  })

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ['target', id, 'week'] })
    qc.invalidateQueries({ queryKey: ['target', id, 'full'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const [genError, setGenError] = useState(null)
  const [exportError, setExportError] = useState(null)

  useEffect(() => {
    if (!isGenerating) return
    const interval = setInterval(async () => {
      try {
        const data = await targetApi.getGenerationStatus(id)
        if (data.status === 'DONE') {
          clearInterval(interval)
          invalidateAll()
          setIsGenerating(false)
          setConfirmGenerate(false)
        } else if (data.status === 'ERROR') {
          clearInterval(interval)
          setGenError(data.message || 'Generation failed')
          setIsGenerating(false)
        }
      } catch (e) {
        clearInterval(interval)
        setGenError(e.message)
        setIsGenerating(false)
      }
    }, 3000)
    return () => clearInterval(interval)
  }, [isGenerating])

  const generateTimeline = useMutation({
    mutationFn: () => targetApi.generateTimeline(id),
    onSuccess: () => setIsGenerating(true),
    onError: () => setIsGenerating(false),
  })

  const deleteTimeline = useMutation({
    mutationFn: () => targetApi.deleteTimeline(id),
    onSuccess: () => { invalidateAll(); setConfirmDelete(false) },
  })

  const importTimeline = useMutation({
    mutationFn: (data) => targetApi.importTimeline(id, data),
    onSuccess: () => { invalidateAll(); setShowImport(false) },
  })

  const sendToAI = useMutation({
    mutationFn: () => {
      setExportError(null)
      return targetApi.askAi(id, buildExportText())
    },
    onSuccess: (data) => {
      setShowImport(true)
      setAiDraft(data.content)
    },
    onError: (err) => setExportError(err.message),
  })

  const markItem = useMutation({
    mutationFn: ({ itemId, status }) => timelineApi.markItem(itemId, { status }),
    onMutate: ({ itemId }) => setMarking(itemId),
    onSuccess: () => { invalidateAll(); setMarking(null) },
    onError: () => setMarking(null),
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const activeData = view === 'full' ? fullData : weekData
  const { target, days = [] } = activeData ?? (weekData ?? {})
  const priorityColor = { HIGH: '#ef4444', MEDIUM: '#f59e0b', LOW: '#6b7280' }[target?.priority] ?? '#6b7280'
  const todayStr = new Date().toLocaleDateString('en-CA')
  const hasMissedInAnyDay = (weekData?.days ?? []).some((d) => d.hasMissedItems)
  const onDone  = (itemId) => markItem.mutate({ itemId, status: 'DONE' })
  const onSkip  = (itemId) => markItem.mutate({ itemId, status: 'SKIPPED' })

  function buildExportText() {
    const { planMode, includeOptional, includeContests, revisionStyle,
            knownWeakTopics, alreadyConfidentTopics } = exportSettings
    const sanitizeField = (val) => String(val ?? '').replace(/[\r\n\t]/g, ' ').trim()
    const dailyBudget = target?.dailyMinutes ?? 60
    const lectures = lectureData?.content ?? []

    const annotated = lectures.map(l => {
      const type     = inferLectureType(l.title)
      const priority = inferPriority(type, l.title)
      return {
        l, type, priority,
        mins:       inferEstimatedMinutes(type, priority, l.estimatedMinutes),
        lectureId:  makeLectureId(l.moduleName, l.sourceOrder, l.id),
        sourceCode: makeSourceCode(l.sourceOrder, l.id),
      }
    })

    const filtered = includeOptional ? annotated : annotated.filter(m => m.type !== 'OPTIONAL')

    const TYPE_ORDER     = { CORE: 0, PRACTICE: 2, CONTEST: 2, REVISION: 3, DISCUSSION: 4, OPTIONAL: 5 }
    const PRIORITY_ORDER = { HIGH: 0, MEDIUM: 1, LOW: 2 }
    const sorted = planMode === 'FOUNDATION_REBUILD'
      ? [...filtered].sort((a, b) => {
          const oa = getFoundationTopicOrder(a.l), ob = getFoundationTopicOrder(b.l)
          if (oa !== ob) return oa - ob
          const la = getFoundationLevelOrder(a.l), lb = getFoundationLevelOrder(b.l)
          return la - lb
        })
      : [...filtered].sort((a, b) => {
          const ta = TYPE_ORDER[a.type] ?? 3, tb = TYPE_ORDER[b.type] ?? 3
          if (ta !== tb) return ta - tb
          const pa = PRIORITY_ORDER[a.priority] ?? 2, pb = PRIORITY_ORDER[b.priority] ?? 2
          return pa - pb
        })

    // Build each row as an explicit array of strings — one entry per lecture
    // sanitizeField strips \n/\r which would break row boundaries
    const lectureRows = sorted.map(({ l, type, priority, mins, lectureId, sourceCode }) => [
      sanitizeField(l.moduleName ?? 'General'),
      sanitizeField(lectureId),
      sanitizeField(l.id),
      sanitizeField(sourceCode),
      sanitizeField(l.title),
      sanitizeField(l.status),
      sanitizeField(l.contentStatus ?? 'NOT_ADDED'),
      sanitizeField(type),
      sanitizeField(priority),
      sanitizeField(mins),
    ].join(' | '))

    // Validate before proceeding
    const rowErrors = validateExportRows(lectureRows)
    if (rowErrors.length > 0) {
      rowErrors.forEach(e => console.error('[Export validation]', e))
      throw new Error(`Export validation failed: ${rowErrors[0]}`)
    }

    // Join lecture rows into their own block — explicit join, independent of any other array
    const lectureBlock = lectureRows.join('\n')

    const weakTopicsArr  = knownWeakTopics.trim()  ? knownWeakTopics.split(',').map(s => s.trim()).filter(Boolean)  : []
    const confTopicsArr  = alreadyConfidentTopics.trim() ? alreadyConfidentTopics.split(',').map(s => s.trim()).filter(Boolean) : []

    const header = [
      `TARGET: ${target?.title ?? ''}`,
      `DEADLINE: ${target?.targetDate ?? ''}`,
      `TODAY: ${new Date().toLocaleDateString('en-CA')}`,
      `DAILY_BUDGET_MINUTES: ${dailyBudget}`,
      `PLAN_MODE: ${planMode}`,
      `INCLUDE_OPTIONAL: ${includeOptional}`,
      `INCLUDE_CONTESTS: ${includeContests}`,
      `REVISION_STYLE: ${revisionStyle}`,
      `KNOWN_WEAK_TOPICS: [${weakTopicsArr.join(', ')}]`,
      `ALREADY_CONFIDENT_TOPICS: [${confTopicsArr.join(', ')}]`,
    ].join('\n')

    const lectureSection = [
      'LECTURES:',
      'module | lecture_id | db_id | source_code | title | progress | content | type | priority | estimated_minutes',
      lectureBlock,
      '',
      '---',
    ].join('\n')

    const fieldKey = [
      'FIELD KEY:',
      '  progress:   NOT_STARTED=not studied | IN_PROGRESS=currently studying | COMPLETED=done',
      '  content:    NOT_ADDED=no material | TRANSCRIPT_ADDED=transcript pasted | NOTES_GENERATED=AI notes ready',
      '  type:       CORE=must study | PRACTICE=problem solving | CONTEST=timed contest | REVISION=review | DISCUSSION=post-contest | OPTIONAL=skip',
      '  priority:   HIGH=schedule early | MEDIUM=normal | LOW=schedule last',
      '  db_id:      numeric database ID -- copy this exact number into lectureId in the JSON output',
    ].join('\n')

    const PLAN_MODE_INSTRUCTIONS = {
      CRASH_INTERVIEW_PREP: [
        'PLAN_MODE = CRASH_INTERVIEW_PREP',
        'Goal: maximize interview readiness before the deadline.',
        '- Do NOT follow course order. Prioritize HIGH priority CORE topics first.',
        '- Prioritize: Binary Search, DP, Graphs, Trees, Hashing, Two Pointers, Sliding Window,',
        '    Recursion, Backtracking, Greedy, Stack, Queue, Heap, Linked Lists, Arrays.',
        '- Skip OPTIONAL lectures.',
        '- Compress MEDIUM priority basics -- include only if time allows.',
        '- Add REVISION only if time allows after covering critical topics.',
        '- Schedule DISCUSSION and CONTEST last -- only if time remains.',
      ].join('\n'),

      STRUCTURED_COURSE_COMPLETION: [
        'PLAN_MODE = STRUCTURED_COURSE_COMPLETION',
        'Goal: complete the course properly in module/lecture order.',
        '- Follow the export row order. Do not skip ahead.',
        '- Include all CORE lectures.',
        '- Add regular REVISION after each topic group.',
        '- Include CONTEST lectures if INCLUDE_CONTESTS = true.',
        '- Avoid aggressive skipping -- only skip OPTIONAL if INCLUDE_OPTIONAL = false.',
      ].join('\n'),

      REVISION_ONLY: [
        'PLAN_MODE = REVISION_ONLY',
        'Goal: revision and practice plan for someone who has already studied.',
        '- Avoid scheduling new STUDY_LECTURE items unless there are no completed lectures in the topic.',
        '- Focus on REVISION and PRACTICE items.',
        '- Prioritize lectures in KNOWN_WEAK_TOPICS first.',
        '- Use COMPLETED and IN_PROGRESS lectures as the primary source.',
        '- Skip NOT_STARTED lectures unless they are in KNOWN_WEAK_TOPICS.',
      ].join('\n'),

      BALANCED_LEARNING: [
        'PLAN_MODE = BALANCED_LEARNING',
        'Goal: steady progress -- study new lectures and review regularly without rushing.',
        '- Follow a mostly ordered approach but prioritize HIGH CORE topics.',
        '- Add REVISION after each major topic group.',
        '- Add PRACTICE after completing 3+ lectures in a topic group.',
        '- Skip OPTIONAL unless INCLUDE_OPTIONAL = true.',
        '- Do not rush as aggressively as CRASH_INTERVIEW_PREP.',
      ].join('\n'),

      FOUNDATION_REBUILD: [
        'PLAN_MODE = FOUNDATION_REBUILD',
        'Goal: rebuild core DSA fundamentals first, then move into interview-heavy topics.',
        '- Rows are pre-sorted in prerequisite order. Follow the row order strictly.',
        '- Start with the earliest rows: Intro to Problem Solving, Time Complexity, Arrays, then progress.',
        '- Do NOT schedule advanced topics (DP, Graphs, Segment Trees) until basics are covered.',
        '- Prefer Intermediate-level lectures before Advanced-level lectures within the same topic.',
        '- Add REVISION after each foundational topic group before moving to the next.',
        '- Add PRACTICE after completing 3+ lectures of the same foundational topic.',
        '- MEDIUM priority lectures are included if they reinforce fundamentals.',
        '- Skip OPTIONAL and contest lectures until all foundation topics are scheduled.',
        '- Generate only from TODAY to DEADLINE inclusive.',
        '- If there is not enough time, create the best foundation plan possible within the deadline.',
        '- Do not create dates after DEADLINE.',
        '- It is okay to leave unscheduled lectures -- do not exceed the deadline to fit everything.',
        '- Prefer prerequisite basics over advanced/high-priority rows.',
      ].join('\n'),
    }

    const planModeBlock = PLAN_MODE_INSTRUCTIONS[planMode]
      ?? `PLAN_MODE = ${planMode} -- follow the export row order for scheduling.`

    const instructions = [
      'INSTRUCTIONS FOR AI:',
      '',
      planModeBlock,
      '',
      'DEADLINE HARD RULE:',
      `  Generate days ONLY from TODAY (${new Date().toLocaleDateString('en-CA')}) to DEADLINE (${target?.targetDate ?? ''}) inclusive.`,
      '  Never output dates after DEADLINE.',
      '  If time is tight, schedule only what fits. It is okay to leave lectures unscheduled.',
      '',
      'GENERATE ONLY THESE ITEM TYPES:',
      '  STUDY_LECTURE  30-60 min  -- direct study of a lecture',
      '  REVISION       20-30 min  -- revisit a studied lecture 2-3 days later',
      '  PRACTICE       20-60 min  -- problem-solving session after a topic group',
      'Do NOT create ADD_TRANSCRIPT or GENERATE_NOTES items.',
      'Assume all material is already prepared. Start directly with STUDY_LECTURE.',
      '',
      'DAILY BUDGET:',
      `  Total estimatedMinutes per day MUST be <= ${dailyBudget}.`,
      '  If REVISION or PRACTICE would push a day over budget, skip it.',
      '  Never exceed the daily budget. Prefer STUDY_LECTURE over REVISION when time is tight.',
      '',
      'SCHEDULING:',
      '  Skip lectures where progress = COMPLETED.',
      includeOptional
        ? '  OPTIONAL lectures are included in the plan.'
        : '  Skip all lectures where type = OPTIONAL (INCLUDE_OPTIONAL = false).',
      includeContests
        ? '  CONTEST lectures are included in the plan.'
        : '  Skip all lectures where type = CONTEST (INCLUDE_CONTESTS = false).',
      '',
      'REVISION: after studying a lecture, schedule REVISION 2-3 days later (20-30 min).',
      'PRACTICE: after 3+ lectures from same topic group, add one PRACTICE block.',
      '',
      'lectureId:',
      '  STUDY_LECTURE and lecture-specific PRACTICE -- copy db_id as integer (e.g. 42).',
      '  REVISION and standalone PRACTICE -- lectureId = null.',
      '  NEVER use lecture_id (text slug) or source_code. Only db_id.',
      '',
      'Return ONLY valid JSON. No markdown. No explanation. No comments inside JSON.',
      '',
      '{',
      '  "days": [',
      '    {',
      '      "date": "YYYY-MM-DD",',
      '      "items": [',
      '        {',
      '          "itemType": "STUDY_LECTURE | REVISION | PRACTICE",',
      '          "title": "short title max 6 words",',
      '          "estimatedMinutes": 20-60,',
      '          "lectureId": copy exact db_id number or null',
      '        }',
      '      ]',
      '    }',
      '  ]',
      '}',
    ].join('\n')

    return [header, '', lectureSection, '', fieldKey, '', instructions].join('\n')
  }

  function exportForAI() {
    setExportError(null)
    let text
    try {
      text = buildExportText()
    } catch (err) {
      setExportError(err.message)
      return
    }
    const blob = new Blob([text], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${(target?.title ?? 'target').replace(/[^a-z0-9]/gi, '_')}-lectures.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  function downloadCsv() {
    const exportDays = (fullData?.days ?? weekData?.days ?? [])
    const rows = [['Date', 'Day', 'Type', 'Title', 'Est. Minutes', 'Status']]
    exportDays.forEach(day => {
      const dayName = new Date(day.date + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'short' })
      ;(day.items ?? []).forEach(item => {
        rows.push([day.date, dayName, item.itemType, `"${item.title.replace(/"/g, '""')}"`, item.estimatedMinutes, item.status])
      })
      if ((day.items ?? []).length === 0) rows.push([day.date, dayName, '', '', '', ''])
    })
    const csv = rows.map(r => r.join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${target?.title ?? 'timeline'}-plan.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

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

        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          <button className="btn btn-secondary" style={{ fontSize: 13 }}
            onClick={() => setShowImport(true)}>
            Import Plan
          </button>
          <button className="btn btn-primary" style={{ fontSize: 13 }}
            disabled={!lectureData?.content?.length || sendToAI.isPending}
            onClick={() => sendToAI.mutate()}>
            {sendToAI.isPending ? 'Asking AI…' : 'Send to AI'}
          </button>
          <button className="btn btn-secondary" style={{ fontSize: 13 }}
            disabled={!lectureData?.content?.length}
            onClick={exportForAI}>
            Export for AI
          </button>
          <button className="btn btn-secondary" style={{ fontSize: 13 }}
            disabled={!(fullData?.days ?? weekData?.days ?? []).length}
            onClick={downloadCsv}>
            Download CSV
          </button>
          <button className="btn btn-secondary" style={{ fontSize: 13, color: 'var(--muted)' }}
            onClick={() => setShowExportSettings(true)}
            title="Edit export settings (plan mode, topics, revision style)">
            ⚙ Settings
          </button>
          {!confirmGenerate ? (
            <button className="btn btn-secondary"
              onClick={() => setConfirmGenerate(true)}>
              Regenerate Timeline
            </button>
          ) : (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: 'var(--muted)' }}>Replace all pending items?</span>
              <button className="btn btn-primary"
                disabled={isGenerating}
                onClick={() => generateTimeline.mutate()}>
                {isGenerating ? 'Generating…' : 'Yes, regenerate'}
              </button>
              <button className="btn btn-secondary" onClick={() => setConfirmGenerate(false)}>Cancel</button>
            </div>
          )}
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
            onClick={() => setConfirmGenerate(true)}>
            Replan from today
          </button>
        </div>
      )}

      {exportError && (
        <div className="error-box" style={{ marginBottom: 16 }}>
          Export error: {exportError}
        </div>
      )}

      {genError && (
        <div className="error-box" style={{ marginBottom: 16 }}>
          {genError}
        </div>
      )}

      <div style={{ display: 'flex', gap: 4, marginBottom: 20 }}>
        <button onClick={() => setView('week')}
          className={view === 'week' ? 'btn btn-primary' : 'btn btn-secondary'}
          style={{ fontSize: 13 }}>This Week</button>
        <button onClick={() => setView('full')}
          className={view === 'full' ? 'btn btn-primary' : 'btn btn-secondary'}
          style={{ fontSize: 13 }}>Full Plan</button>
      </div>

      {(weekData?.days ?? []).length === 0 ? (
        <div className="empty-state">
          <p>No timeline generated yet.</p>
          {!confirmGenerate ? (
            <button className="btn btn-primary" style={{ marginTop: 16 }}
              onClick={() => setConfirmGenerate(true)}>
              Generate Timeline
            </button>
          ) : (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginTop: 16, justifyContent: 'center' }}>
              <span style={{ fontSize: 13, color: 'var(--muted)' }}>Generate your plan now?</span>
              <button className="btn btn-primary"
                disabled={isGenerating}
                onClick={() => generateTimeline.mutate()}>
                {isGenerating ? 'Generating…' : 'Yes, generate'}
              </button>
              <button className="btn btn-secondary" onClick={() => setConfirmGenerate(false)}>Cancel</button>
            </div>
          )}
        </div>
      ) : view === 'full' ? (
        fullLoading ? <LoadingSpinner /> : (
          <FullPlanView
            days={fullData?.days ?? []}
            onDone={onDone}
            onSkip={onSkip}
            marking={marking}
          />
        )
      ) : (
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          {days.map((day) => (
            <DayColumn
              key={day.date}
              day={day}
              marking={marking}
              isToday={day.date === todayStr}
              onDone={onDone}
              onSkip={onSkip}
            />
          ))}
        </div>
      )}

      {showImport && (
        <ImportPlanModal
          initialJson={aiDraft}
          onClose={() => { setShowImport(false); setAiDraft('') }}
          onImport={(data) => importTimeline.mutate(data)}
          isPending={importTimeline.isPending}
          error={importTimeline.error}
        />
      )}

      {showExportSettings && (
        <ExportSettingsModal
          settings={exportSettings}
          onChange={setExportSettings}
          onClose={() => setShowExportSettings(false)}
        />
      )}
    </div>
  )
}
