import { useState, useRef, useEffect, useContext, useCallback, createContext } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { lectureApi } from '../api/lectureApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import ConfirmDeleteModal from '../components/common/ConfirmDeleteModal'
import ConfirmModal from '../components/common/ConfirmModal'
import { formatDate } from '../utils/formatters'

const CONFUSION_TYPES = [
  { key: 'THEORY_UNCLEAR', label: '🤔 Theory unclear' },
  { key: 'EXAMPLE_UNCLEAR', label: '📝 Example unclear' },
  { key: 'WHEN_TO_USE_UNCLEAR', label: '❓ When to use?' },
  { key: 'CODE_CONFUSING', label: '💻 Code confusing' },
  { key: 'EDGE_CASE_CONFUSING', label: '⚠️ Edge case' },
  { key: 'LOST_FOCUS', label: '😵 Lost focus' },
]

function CollapsibleCode({ children }) {
  const [expanded, setExpanded] = useState(false)
  const child = Array.isArray(children) ? children[0] : children
  const rawCode = String(child?.props?.children ?? '').trimEnd()
  const lineCount = rawCode.split('\n').length
  const PREVIEW = 6
  const needsCollapse = lineCount > PREVIEW

  return (
    <div style={{ marginBottom: 8, border: '1px solid var(--border)', borderRadius: 6, overflow: 'hidden' }}>
      <div style={{ position: 'relative' }}>
        <pre style={{
          margin: 0,
          maxHeight: needsCollapse && !expanded ? 130 : 'none',
          overflow: needsCollapse && !expanded ? 'hidden' : 'auto',
        }}>
          {children}
        </pre>
        {needsCollapse && !expanded && (
          <div style={{
            position: 'absolute', bottom: 0, left: 0, right: 0, height: 40,
            background: 'linear-gradient(to bottom, transparent, var(--surface))',
            pointerEvents: 'none',
          }} />
        )}
      </div>
      {needsCollapse && (
        <button
          onClick={() => setExpanded(e => !e)}
          style={{
            display: 'block', width: '100%', padding: '4px 0',
            fontSize: 11, color: 'var(--accent)', background: 'var(--surface2)',
            border: 'none', borderTop: '1px solid var(--border)',
            cursor: 'pointer', textAlign: 'center',
          }}
        >
          {expanded ? '▲ Collapse' : `▼ Show all ${lineCount} lines`}
        </button>
      )}
    </div>
  )
}

function slugify(text) {
  return text.toLowerCase().replace(/[^\w\s-]/g, '').replace(/\s+/g, '-').replace(/-+/g, '-').trim()
}

function nodeText(node) {
  if (typeof node === 'string') return node
  if (Array.isArray(node)) return node.map(nodeText).join('')
  if (node?.props?.children) return nodeText(node.props.children)
  return ''
}

function extractHeadings(markdown) {
  if (!markdown) return []
  const headings = []
  for (const line of markdown.split('\n')) {
    const m = line.match(/^(#{1,4})\s+(.+)$/)
    if (m) headings.push({ level: m[1].length, text: m[2].trim(), id: slugify(m[2].trim()) })
  }
  return headings
}

const CompletionCtx = createContext({ completed: new Set(), toggle: () => {} })

function mkHeading(Tag) {
  return function HeadingWithId({ children, ...props }) {
    const id = slugify(nodeText(children))
    const { completed, toggle } = useContext(CompletionCtx)
    const done = completed.has(id)
    return (
      <Tag id={id} {...props}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ flex: 1 }}>{children}</span>
          <button
            onClick={e => { e.stopPropagation(); toggle(id) }}
            style={{
              fontSize: 10, padding: '1px 8px', borderRadius: 10, whiteSpace: 'nowrap',
              border: `1px solid ${done ? '#10b981' : 'var(--border)'}`,
              background: done ? '#10b981' : 'transparent',
              color: done ? '#fff' : 'var(--muted)',
              cursor: 'pointer', fontWeight: 500, flexShrink: 0,
            }}
          >
            {done ? '✓ Done' : 'Mark done'}
          </button>
        </span>
      </Tag>
    )
  }
}

const mdComponents = {
  pre: CollapsibleCode,
  h1: mkHeading('h1'), h2: mkHeading('h2'), h3: mkHeading('h3'), h4: mkHeading('h4'),
}

function NotesIndex({ headings, completed, onToggle, className }) {
  if (!headings.length) return null
  const doneCount = headings.filter(h => completed.has(h.id)).length
  const allDone = doneCount === headings.length
  return (
    <div className={className} style={{
      width: 188, flexShrink: 0,
      position: 'sticky', top: 16,
      maxHeight: 'calc(100vh - 100px)',
      overflowY: 'auto',
      alignSelf: 'flex-start',
    }}>
      {/* Header + progress */}
      <div style={{
        fontSize: 10, fontWeight: 700, color: 'var(--muted)',
        textTransform: 'uppercase', letterSpacing: '0.08em',
        marginBottom: 6, paddingBottom: 6,
        borderBottom: '1px solid var(--border)',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <span>Contents</span>
        <span style={{ color: allDone ? '#10b981' : 'var(--muted)', fontVariantNumeric: 'tabular-nums' }}>
          {doneCount}/{headings.length}
        </span>
      </div>
      <div style={{ height: 3, background: 'var(--border)', borderRadius: 2, marginBottom: 10 }}>
        <div style={{
          height: '100%', borderRadius: 2, background: '#10b981',
          width: `${headings.length ? (doneCount / headings.length) * 100 : 0}%`,
          transition: 'width 0.3s ease',
        }} />
      </div>

      {/* Items */}
      {headings.map((h, i) => {
        const done = completed.has(h.id)
        return (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 5,
            paddingTop: 3, paddingBottom: 3,
            paddingLeft: 4 + (h.level - 1) * 10,
            borderLeft: `2px solid ${done ? '#10b981' : 'var(--border)'}`,
            marginBottom: 1,
          }}>
            <span
              onClick={() => document.getElementById(h.id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
              style={{
                flex: 1, fontSize: 11, lineHeight: 1.4, cursor: 'pointer',
                color: done ? '#10b981' : 'var(--muted)',
              }}
              onMouseEnter={e => { if (!done) e.currentTarget.style.color = 'var(--accent)' }}
              onMouseLeave={e => { if (!done) e.currentTarget.style.color = done ? '#10b981' : 'var(--muted)' }}
            >
              {h.text}
            </span>
            <button
              onClick={() => onToggle(h.id)}
              title={done ? 'Mark undone' : 'Mark done'}
              style={{
                width: 14, height: 14, borderRadius: '50%', flexShrink: 0,
                border: `1px solid ${done ? '#10b981' : 'var(--border)'}`,
                background: done ? '#10b981' : 'transparent',
                color: done ? '#fff' : 'transparent',
                cursor: 'pointer', fontSize: 8, lineHeight: 1,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}
            >✓</button>
          </div>
        )
      })}
    </div>
  )
}

function Md({ children }) {
  if (!children) return null
  return (
    <div className="md">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={mdComponents}>{children}</ReactMarkdown>
    </div>
  )
}

function FlashCard({ question, answer, front, back }) {
  const [flipped, setFlipped] = useState(false)
  const q = question ?? front
  const a = answer ?? back
  return (
    <div
      className="card"
      onClick={() => setFlipped(f => !f)}
      style={{ cursor: 'pointer', borderLeft: '3px solid var(--accent)', minHeight: 80, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}
    >
      <div style={{ fontSize: 10, color: 'var(--muted)', marginBottom: 6, textTransform: 'uppercase', letterSpacing: 0.5 }}>
        {flipped ? 'Answer' : 'Question — tap to reveal'}
      </div>
      <div style={{ fontSize: 13 }}>{flipped ? a : q}</div>
    </div>
  )
}

function useCompletedHeadings(lectureId) {
  const key = `lecture-done-${lectureId}`
  const [completed, setCompleted] = useState(() => {
    try { return new Set(JSON.parse(localStorage.getItem(key) ?? '[]')) } catch { return new Set() }
  })
  const toggle = useCallback((id) => {
    setCompleted(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      localStorage.setItem(key, JSON.stringify([...next]))
      return next
    })
  }, [key])
  return { completed, toggle }
}

function LearnTab({ notes }) {
  const { id: lectureId } = useParams()
  const { completed, toggle } = useCompletedHeadings(lectureId)
  if (!notes?.fullCleanNotes) return <div className="empty-state">No notes yet. Generate them above.</div>
  const headings = extractHeadings(notes.fullCleanNotes)
  return (
    <CompletionCtx.Provider value={{ completed, toggle }}>
      <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
        <div className="note-content" style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div className="card" style={{ padding: '28px 32px' }}>
            <Md>{notes.fullCleanNotes}</Md>
          </div>
          {notes.chatAdditions && (
            <div className="card" style={{ padding: '28px 32px', borderLeft: '3px solid #8b5cf6' }}>
              <div className="section-title" style={{ marginBottom: 12 }}>Notes from Chat</div>
              <Md>{notes.chatAdditions}</Md>
            </div>
          )}
        </div>
        <NotesIndex className="notes-index" headings={headings} completed={completed} onToggle={toggle} />
      </div>
    </CompletionCtx.Provider>
  )
}

function SimpleTab({ notes }) {
  if (!notes?.simpleExplanation && !notes?.practicalUsage) return <div className="empty-state">No notes yet.</div>
  return (
    <div className="note-content" style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {notes.simpleExplanation && (
        <div className="card" style={{ padding: '28px 32px', borderLeft: '3px solid #10b981' }}>
          <div className="section-title" style={{ marginBottom: 12 }}>Simple Explanation</div>
          <Md>{notes.simpleExplanation}</Md>
        </div>
      )}
      {notes.practicalUsage && (
        <div className="card" style={{ padding: '28px 32px', borderLeft: '3px solid #f59e0b' }}>
          <div className="section-title" style={{ marginBottom: 12 }}>Practical Usage</div>
          <Md>{notes.practicalUsage}</Md>
        </div>
      )}
    </div>
  )
}

function ExamplesTab({ notes }) {
  const examples = Array.isArray(notes?.examples) ? notes.examples : []
  if (!examples.length) return <div className="empty-state">No examples yet.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {examples.map((ex, i) => {
        const approachContent = ex.approach
          ?? (ex.code ? `\`\`\`${ex.language || ''}\n${ex.code}\n\`\`\`` : null)
        return (
          <div key={i} className="card" style={{ padding: '20px 24px' }}>
            {ex.title && <div style={{ fontWeight: 600, marginBottom: 12, fontSize: 15 }}>{ex.title}</div>}
            {ex.problem && <div style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 8 }}>{ex.problem}</div>}
            {ex.input && (
              <div style={{ marginBottom: 8 }}>
                <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Input</div>
                <Md>{`\`\`\`\n${ex.input}\n\`\`\``}</Md>
              </div>
            )}
            {approachContent && (
              <div style={{ marginBottom: 8 }}>
                <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Approach</div>
                <Md>{approachContent}</Md>
              </div>
            )}
            {ex.output && (
              <div style={{ marginBottom: 8 }}>
                <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Output</div>
                <Md>{`\`\`\`\n${ex.output}\n\`\`\``}</Md>
              </div>
            )}
            {ex.explanation && (
              <div style={{ marginTop: 4, fontSize: 13, color: 'var(--muted)', lineHeight: 1.7 }}>{ex.explanation}</div>
            )}
          </div>
        )
      })}
    </div>
  )
}

function MistakeCard({ m }) {
  if (typeof m === 'string') {
    return (
      <div className="card" style={{ borderLeft: '3px solid var(--danger)' }}>
        <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
          <span style={{ color: 'var(--danger)', flexShrink: 0 }}>✗</span>
          <span style={{ fontSize: 13 }}>{m}</span>
        </div>
      </div>
    )
  }
  const why = m.why_it_is_wrong ?? m.explanation
  const wrong = m.wrong
  const correct = m.correct_approach ?? m.right
  return (
    <div className="card" style={{ borderLeft: '3px solid var(--danger)', display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <span style={{ color: 'var(--danger)', flexShrink: 0, fontWeight: 700 }}>✗</span>
        <span style={{ fontSize: 14, fontWeight: 600 }}>{m.mistake}</span>
      </div>
      {why && (
        <div style={{ fontSize: 13, color: 'var(--muted)' }}>{why}</div>
      )}
      {wrong && (
        <div>
          <div style={{ fontSize: 11, color: 'var(--danger)', marginBottom: 4, fontWeight: 600 }}>✗ WRONG</div>
          <Md>{`\`\`\`\n${wrong}\n\`\`\``}</Md>
        </div>
      )}
      {correct && (
        <div>
          <div style={{ fontSize: 11, color: 'var(--success)', marginBottom: 4, fontWeight: 600 }}>✓ CORRECT</div>
          <Md>{`\`\`\`\n${correct}\n\`\`\``}</Md>
        </div>
      )}
    </div>
  )
}

function EdgeCard({ e }) {
  if (typeof e === 'string') {
    return (
      <div className="card" style={{ borderLeft: '3px solid var(--warn)', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <span style={{ color: 'var(--warn)', flexShrink: 0 }}>⚠</span>
        <span style={{ fontSize: 13 }}>{e}</span>
      </div>
    )
  }
  const handling = e.handling ?? e.how_to_handle
  return (
    <div className="card" style={{ borderLeft: '3px solid var(--warn)', display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <span style={{ color: 'var(--warn)', flexShrink: 0 }}>⚠</span>
        <span style={{ fontSize: 14, fontWeight: 600 }}>{e.case}</span>
      </div>
      {handling && (
        <div style={{ fontSize: 13, color: 'var(--success)' }}>→ {handling}</div>
      )}
    </div>
  )
}

function MistakesTab({ notes }) {
  const mistakes = Array.isArray(notes?.mistakesToAvoid) ? notes.mistakesToAvoid
    : Array.isArray(notes?.mistakes_to_avoid) ? notes.mistakes_to_avoid : []
  const edges = Array.isArray(notes?.edgeCases) ? notes.edgeCases
    : Array.isArray(notes?.edge_cases) ? notes.edge_cases : []
  if (!mistakes.length && !edges.length) return <div className="empty-state">No mistakes or edge cases noted. Regenerate notes to populate this tab.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {mistakes.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Mistakes to Avoid</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {mistakes.map((m, i) => <MistakeCard key={i} m={m} />)}
          </div>
        </div>
      )}
      {edges.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Edge Cases</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {edges.map((e, i) => <EdgeCard key={i} e={e} />)}
          </div>
        </div>
      )}
    </div>
  )
}

function ReviseTab({ notes }) {
  const flashcards = Array.isArray(notes?.flashcards) ? notes.flashcards : []
  const revisionNotes = notes?.revisionNotes ?? notes?.revision_notes
  if (!revisionNotes && !flashcards.length) return <div className="empty-state">No revision material yet.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {revisionNotes && (
        <div className="card" style={{ padding: '20px 24px', borderLeft: '3px solid var(--accent)' }}>
          <div className="section-title" style={{ marginBottom: 12 }}>Quick Revision Notes</div>
          <Md>{revisionNotes}</Md>
        </div>
      )}
      {flashcards.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Flashcards — tap to flip</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 10 }}>
            {flashcards.map((fc, i) => (
              <FlashCard key={i} question={fc.question} answer={fc.answer} front={fc.front} back={fc.back} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function PracticeTab({ notes }) {
  const practice = Array.isArray(notes?.practiceQuestions) ? notes.practiceQuestions
    : Array.isArray(notes?.practice_questions) ? notes.practice_questions : []
  const interview = Array.isArray(notes?.interviewQuestions) ? notes.interviewQuestions
    : Array.isArray(notes?.interview_questions) ? notes.interview_questions : []
  const weakChecks = Array.isArray(notes?.weakAreaChecks) ? notes.weakAreaChecks
    : Array.isArray(notes?.weak_area_checks) ? notes.weak_area_checks : []
  if (!practice.length && !interview.length && !weakChecks.length) return <div className="empty-state">No practice questions yet.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {interview.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Interview Questions</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {interview.map((q, i) => (
              <div key={i} className="card" style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                <span style={{ color: 'var(--accent)', fontWeight: 700, flexShrink: 0 }}>Q{i + 1}</span>
                <span style={{ fontSize: 13 }}>{typeof q === 'string' ? q : JSON.stringify(q)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
      {practice.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Practice Problems</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {practice.map((q, i) => {
              const isObj = typeof q === 'object' && q !== null
              return (
                <div key={i} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                    <span style={{ color: 'var(--success)', fontWeight: 700, flexShrink: 0 }}>{i + 1}.</span>
                    <span style={{ fontSize: 13, fontWeight: 600 }}>{isObj ? q.title : q}</span>
                  </div>
                  {isObj && q.task && <div style={{ fontSize: 13, color: 'var(--muted)', paddingLeft: 20 }}>{q.task}</div>}
                  {isObj && (q.difficulty || q.related_concept) && (
                    <div style={{ paddingLeft: 20, fontSize: 11, color: 'var(--accent)' }}>
                      {[q.difficulty, q.related_concept].filter(Boolean).join(' · ')}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}
      {weakChecks.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Weak Area Checks</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {weakChecks.map((w, i) => {
              const isObj = typeof w === 'object' && w !== null
              return (
                <div key={i} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{isObj ? w.check : w}</div>
                  {isObj && w.expected_understanding && (
                    <div style={{ fontSize: 12, color: 'var(--muted)' }}>{w.expected_understanding}</div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}

function NotesSection({ notes }) {
  const [sub, setSub] = useState('learn')
  const subTabs = [
    { key: 'learn', label: '📖 Learn' },
    { key: 'simple', label: '💡 Simple' },
    { key: 'examples', label: '🔍 Examples' },
    { key: 'mistakes', label: '⚠️ Mistakes' },
    { key: 'revise', label: '🔁 Revise' },
    { key: 'practice', label: '🏋️ Practice' },
  ]

  if (!notes) return <div className="empty-state">No notes yet. Click Generate Notes above.</div>

  return (
    <div>
      <div className="tabs" style={{ marginBottom: 20 }}>
        {subTabs.map(t => (
          <button key={t.key} className={`tab ${sub === t.key ? 'active' : ''}`} onClick={() => setSub(t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      {sub === 'learn' && <LearnTab notes={notes} />}
      {sub === 'simple' && <SimpleTab notes={notes} />}
      {sub === 'examples' && <ExamplesTab notes={notes} />}
      {sub === 'mistakes' && <MistakesTab notes={notes} />}
      {sub === 'revise' && <ReviseTab notes={notes} />}
      {sub === 'practice' && <PracticeTab notes={notes} />}
    </div>
  )
}

function ChatDrawer({ lectureId }) {
  const [open, setOpen] = useState(false)
  const [msg, setMsg] = useState('')
  const [messages, setMessages] = useState([])
  const [savedIdx, setSavedIdx] = useState(new Set())
  const bottomRef = useRef()
  const inputRef = useRef()
  const qc = useQueryClient()

  const chat = useMutation({
    mutationFn: (message) => lectureApi.chat(lectureId, message),
    onSuccess: (data) => {
      setMessages((prev) => [...prev, { role: 'ASSISTANT', text: data.message }])
    },
  })

  const saveToNotes = useMutation({
    mutationFn: ({ content }) => lectureApi.addToNotes(lectureId, content),
    onSuccess: (data, variables) => {
      qc.setQueryData(['lecture', String(lectureId)], data)
      setSavedIdx((prev) => new Set([...prev, variables.index]))
    },
  })

  const send = () => {
    if (!msg.trim()) return
    const text = msg
    setMessages((prev) => [...prev, { role: 'USER', text }])
    setMsg('')
    chat.mutate(text)
  }

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    if (open) setTimeout(() => inputRef.current?.focus(), 100)
  }, [open])

  return (
    <>
      {/* Floating button */}
      <button
        onClick={() => setOpen(o => !o)}
        style={{
          position: 'fixed',
          bottom: 24,
          right: 24,
          width: 52,
          height: 52,
          borderRadius: '50%',
          background: open ? 'var(--surface2)' : 'var(--accent)',
          border: '1px solid var(--border)',
          boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
          fontSize: 22,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 300,
          transition: 'all 0.2s',
          cursor: 'pointer',
        }}
        title="Ask Tutor"
      >
        {open ? '✕' : '💬'}
      </button>

      {/* Chat panel */}
      {open && (
        <div style={{
          position: 'fixed',
          bottom: 88,
          right: 24,
          width: 'min(420px, calc(100vw - 48px))',
          height: 'min(520px, calc(100vh - 120px))',
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 12,
          boxShadow: '0 8px 40px rgba(0,0,0,0.5)',
          display: 'flex',
          flexDirection: 'column',
          zIndex: 299,
          overflow: 'hidden',
        }}>
          {/* Header */}
          <div style={{
            padding: '12px 16px',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            flexShrink: 0,
          }}>
            <span style={{ fontSize: 16 }}>💬</span>
            <span style={{ fontWeight: 600, fontSize: 13 }}>Ask Tutor</span>
            {messages.length > 0 && (
              <button
                onClick={() => setMessages([])}
                style={{ marginLeft: 'auto', fontSize: 11, color: 'var(--muted)' }}
              >
                Clear
              </button>
            )}
          </div>

          {/* Messages */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
            {messages.length === 0 && (
              <div style={{ textAlign: 'center', color: 'var(--muted)', fontSize: 13, marginTop: 20 }}>
                Ask anything about this lecture.
              </div>
            )}
            {messages.map((m, i) => (
              <div key={i} style={{
                alignSelf: m.role === 'USER' ? 'flex-end' : 'flex-start',
                maxWidth: '90%',
                display: 'flex',
                flexDirection: 'column',
                gap: 4,
              }}>
                <div style={{
                  background: m.role === 'USER' ? 'var(--accent)' : 'var(--surface2)',
                  padding: '8px 12px',
                  borderRadius: 10,
                  fontSize: 13,
                }}>
                  {m.role === 'USER'
                    ? <span style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{m.text}</span>
                    : <Md>{m.text}</Md>
                  }
                </div>
                {m.role === 'ASSISTANT' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <button
                      onClick={() => saveToNotes.mutate({ content: m.text, index: i })}
                      disabled={savedIdx.has(i) || saveToNotes.isPending}
                      style={{
                        alignSelf: 'flex-start',
                        fontSize: 11,
                        color: savedIdx.has(i) ? 'var(--success)' : 'var(--muted)',
                        padding: '2px 6px',
                        borderRadius: 4,
                        border: '1px solid var(--border)',
                        background: 'transparent',
                        cursor: savedIdx.has(i) ? 'default' : 'pointer',
                      }}
                    >
                      {saveToNotes.isPending && !savedIdx.has(i)
                        ? 'Saving…'
                        : savedIdx.has(i)
                        ? '✓ Saved to notes'
                        : '＋ Save to notes'}
                    </button>
                    {saveToNotes.error && (
                      <span style={{ fontSize: 11, color: 'var(--danger)' }}>
                        {saveToNotes.error.message}
                      </span>
                    )}
                  </div>
                )}
              </div>
            ))}
            {chat.isPending && (
              <div style={{ alignSelf: 'flex-start', color: 'var(--muted)', fontSize: 13 }}>Thinking…</div>
            )}
            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div style={{ padding: '10px 12px', borderTop: '1px solid var(--border)', display: 'flex', gap: 8, flexShrink: 0 }}>
            <textarea
              ref={inputRef}
              rows={2}
              value={msg}
              onChange={(e) => setMsg(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }}
              placeholder="Ask… (Enter to send)"
              style={{ flex: 1, fontSize: 13, resize: 'none' }}
            />
            <button
              className="btn btn-primary btn-sm"
              onClick={send}
              disabled={chat.isPending || !msg.trim()}
              style={{ alignSelf: 'flex-end' }}
            >
              Send
            </button>
          </div>
          {chat.error && (
            <div className="error-box" style={{ margin: '0 12px 10px', fontSize: 12 }}>{chat.error.message}</div>
          )}
        </div>
      )}
    </>
  )
}

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
          position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 200,
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

const STATUS_OPTIONS = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED']

function EditLectureModal({ lecture, onClose }) {
  const qc = useQueryClient()
  const [form, setForm] = useState({
    title: lecture.title ?? '',
    moduleName: lecture.moduleName ?? '',
    sourceName: lecture.sourceName ?? '',
    sourceOrder: lecture.sourceOrder != null ? String(lecture.sourceOrder) : '',
    status: lecture.status ?? 'NOT_STARTED',
    rawContent: lecture.rawContent ?? '',
  })

  const { data: siblingData } = useQuery({
    queryKey: ['lecture-siblings', String(lecture.courseId)],
    queryFn: () => lectureApi.listByCourse(lecture.courseId, 0, 500),
    enabled: !!lecture.courseId,
  })
  const siblings = siblingData?.content ?? []
  const existingSources = [...new Set(siblings.map(l => l.sourceName).filter(Boolean))]
  const existingModules = [...new Set(siblings.map(l => l.moduleName).filter(Boolean))]

  const update = useMutation({
    mutationFn: (data) => lectureApi.update(lecture.id, data),
    onSuccess: (data) => {
      qc.setQueryData(['lecture', String(lecture.id)], data)
      qc.invalidateQueries({ queryKey: ['lectures', String(lecture.courseId)] })
      onClose()
    },
  })

  const submit = () => update.mutate({
    title: form.title || null,
    moduleName: form.moduleName || null,
    sourceName: form.sourceName || null,
    sourceOrder: form.sourceOrder ? Number(form.sourceOrder) : null,
    status: form.status || null,
    rawContent: form.rawContent || null,
  })

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" style={{ maxWidth: 520 }} onClick={e => e.stopPropagation()}>
        <h2>Edit Lecture</h2>

        <div className="form-group">
          <label>Title *</label>
          <input value={form.title} onChange={e => set('title', e.target.value)} />
        </div>

        <div className="form-grid-2" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div className="form-group">
            <label>Module <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 400 }}>(auto-set by AI if blank)</span></label>
            <Combobox value={form.moduleName} onChange={v => set('moduleName', v)}
              options={existingModules} placeholder="e.g. Arrays" />
          </div>
          <div className="form-group">
            <label>Order</label>
            <input type="number" value={form.sourceOrder} onChange={e => set('sourceOrder', e.target.value)} />
          </div>
        </div>

        <div className="form-group">
          <label>Source</label>
          <Combobox value={form.sourceName} onChange={v => set('sourceName', v)}
            options={existingSources} placeholder="e.g. Scaler, Striver A2Z…" />
        </div>

        <div className="form-group">
          <label>Status</label>
          <select className="input" value={form.status} onChange={e => set('status', e.target.value)}
            style={{ cursor: 'pointer' }}>
            {STATUS_OPTIONS.map(s => (
              <option key={s} value={s}>{s.replace('_', ' ')}</option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Raw Transcript</label>
          <textarea rows={5} value={form.rawContent} onChange={e => set('rawContent', e.target.value)}
            placeholder="Paste updated transcript…" />
          <div style={{ fontSize: 11, color: 'var(--muted)', marginTop: 4 }}>{form.rawContent.length.toLocaleString()} / 200,000 chars</div>
        </div>

        {update.error && <div className="error-box" style={{ marginBottom: 12 }}>{update.error.message}</div>}

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!form.title || update.isPending} onClick={submit}>
            {update.isPending ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  )
}

function ConfusionPanel({ lectureId }) {
  const [note, setNote] = useState('')
  const log = useMutation({ mutationFn: (data) => lectureApi.logConfusion(lectureId, data) })

  return (
    <div>
      <div className="section-title" style={{ marginBottom: 12 }}>What's confusing?</div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
        {CONFUSION_TYPES.map(({ key, label }) => (
          <button key={key} className="btn btn-secondary btn-sm"
            disabled={log.isPending}
            onClick={() => log.mutate({ confusionType: key, note: note || undefined })}>
            {label}
          </button>
        ))}
      </div>
      <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional note about what's confusing…" />
      {log.isSuccess && <div style={{ color: 'var(--success)', fontSize: 13, marginTop: 8 }}>✓ Logged</div>}
      {log.error && <div className="error-box" style={{ marginTop: 8 }}>{log.error.message}</div>}
    </div>
  )
}

export default function LecturePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [tab, setTab] = useState('notes')
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showImportModal, setShowImportModal] = useState(false)
  const [importContent, setImportContent] = useState('')
  const [showRegenerateConfirm, setShowRegenerateConfirm] = useState(false)
  const [showImportConfirm, setShowImportConfirm] = useState(false)
  const [showRetryParseConfirm, setShowRetryParseConfirm] = useState(false)
  const qc = useQueryClient()

  const { data: lecture, isLoading, error } = useQuery({
    queryKey: ['lecture', id],
    queryFn: () => lectureApi.getById(id),
  })

  const generateNotes = useMutation({
    mutationFn: () => lectureApi.generateNotes(id),
    onSuccess: (data) => qc.setQueryData(['lecture', id], data),
  })

  const retryParse = useMutation({
    mutationFn: () => lectureApi.retryParseNotes(id),
    onSuccess: (data) => qc.setQueryData(['lecture', id], data),
  })

  const importNotes = useMutation({
    mutationFn: () => lectureApi.importNotes(id, importContent),
    onSuccess: (data) => {
      qc.setQueryData(['lecture', id], data)
      setShowImportModal(false)
      setImportContent('')
      setTab('notes')
    },
  })

  const cleanTranscript = useMutation({
    mutationFn: () => lectureApi.cleanTranscript(id),
    onSuccess: (data) => qc.setQueryData(['lecture', id], data),
  })

  const deleteLecture = useMutation({
    mutationFn: () => lectureApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      navigate(lecture?.courseId ? `/courses/${lecture.courseId}` : '/courses')
    },
  })

  function downloadTxt() {
    const n = lecture.notes
    const lines = []

    lines.push(`LECTURE: ${lecture.title}`)
    if (lecture.moduleName) lines.push(`MODULE: ${lecture.moduleName}`)
    lines.push(`STATUS: ${lecture.status}`)
    lines.push('')

    if (n?.fullCleanNotes) {
      lines.push('=== NOTES ===')
      lines.push(n.fullCleanNotes)
      lines.push('')
    }
    if (n?.simpleExplanation) {
      lines.push('=== SIMPLE EXPLANATION ===')
      lines.push(n.simpleExplanation)
      lines.push('')
    }
    if (n?.practicalUsage) {
      lines.push('=== PRACTICAL USAGE ===')
      lines.push(n.practicalUsage)
      lines.push('')
    }
    const examples = Array.isArray(n?.examples) ? n.examples : []
    const mistakes = Array.isArray(n?.mistakesToAvoid) ? n.mistakesToAvoid
      : Array.isArray(n?.mistakes_to_avoid) ? n.mistakes_to_avoid : []
    const edges = Array.isArray(n?.edgeCases) ? n.edgeCases
      : Array.isArray(n?.edge_cases) ? n.edge_cases : []
    const flashcards = Array.isArray(n?.flashcards) ? n.flashcards : []
    const interview = Array.isArray(n?.interviewQuestions) ? n.interviewQuestions
      : Array.isArray(n?.interview_questions) ? n.interview_questions : []
    const practice = Array.isArray(n?.practiceQuestions) ? n.practiceQuestions
      : Array.isArray(n?.practice_questions) ? n.practice_questions : []

    if (examples.length) {
      lines.push('=== EXAMPLES ===')
      examples.forEach((ex, i) => {
        if (ex.title) lines.push(`${i + 1}. ${ex.title}`)
        if (ex.problem) lines.push(ex.problem)
        const approach = ex.approach ?? ex.code
        if (approach) lines.push(approach)
        if (ex.explanation) lines.push(ex.explanation)
        lines.push('')
      })
    }
    if (mistakes.length) {
      lines.push('=== MISTAKES TO AVOID ===')
      mistakes.forEach((m) => {
        if (typeof m === 'string') {
          lines.push(`- ${m}`)
        } else {
          const why = m.why_it_is_wrong ?? m.explanation
          lines.push(`- ${m.mistake}${why ? ': ' + why : ''}`)
        }
      })
      lines.push('')
    }
    if (edges.length) {
      lines.push('=== EDGE CASES ===')
      edges.forEach((e) => {
        if (typeof e === 'string') {
          lines.push(`- ${e}`)
        } else {
          const handling = e.handling ?? e.how_to_handle
          lines.push(`- ${e.case}${handling ? ' → ' + handling : ''}`)
        }
      })
      lines.push('')
    }
    const revisionNotes = n?.revisionNotes ?? n?.revision_notes
    if (revisionNotes) {
      lines.push('=== REVISION NOTES ===')
      lines.push(revisionNotes)
      lines.push('')
    }
    if (flashcards.length) {
      lines.push('=== FLASHCARDS ===')
      flashcards.forEach((fc, i) => {
        lines.push(`Q${i + 1}: ${fc.question ?? fc.front}`)
        lines.push(`A${i + 1}: ${fc.answer ?? fc.back}`)
      })
      lines.push('')
    }
    if (interview.length) {
      lines.push('=== INTERVIEW QUESTIONS ===')
      interview.forEach((q, i) => lines.push(`${i + 1}. ${typeof q === 'string' ? q : JSON.stringify(q)}`))
      lines.push('')
    }
    if (practice.length) {
      lines.push('=== PRACTICE QUESTIONS ===')
      practice.forEach((q, i) => {
        const isObj = typeof q === 'object' && q !== null
        const text = isObj
          ? `${q.title || `Question ${i + 1}`}${q.task ? ': ' + q.task : ''}${q.difficulty ? ' [' + q.difficulty + ']' : ''}${q.related_concept ? ' (' + q.related_concept + ')' : ''}`
          : q
        lines.push(`${i + 1}. ${text}`)
      })
      lines.push('')
    }
    if (lecture.rawContent) {
      lines.push('=== RAW TRANSCRIPT ===')
      lines.push(lecture.rawContent)
    }

    const blob = new Blob([lines.join('\n')], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${lecture.title.replace(/[^a-z0-9]/gi, '_')}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const tabs = ['notes', 'confusions', 'raw']

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 4 }}>
          <Link to={`/courses/${lecture.courseId}`} style={{ color: 'var(--accent)' }}>← Lectures</Link>
          {lecture.moduleName && ` · ${lecture.moduleName}`}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <h1 style={{ fontSize: 20, fontWeight: 600 }}>{lecture.title}</h1>
          <div className="actions-row" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            {lecture.notes?.model && (
              <span style={{ fontSize: 10, color: 'var(--muted)', background: 'var(--surface2)', padding: '2px 7px', borderRadius: 4, fontFamily: 'monospace' }}>
                {lecture.notes.model}
              </span>
            )}
            <button
              className="btn btn-primary"
              onClick={() => lecture.notes ? setShowRegenerateConfirm(true) : generateNotes.mutate()}
              disabled={generateNotes.isPending || retryParse.isPending}
            >
              {generateNotes.isPending ? '⏳ Generating…' : lecture.notes ? '↻ Regenerate Notes' : '✨ Generate Notes'}
            </button>
            {lecture.contentStatus === 'PARSE_FAILED' && (
              <button
                className="btn btn-secondary"
                onClick={() => lecture.notes ? setShowRetryParseConfirm(true) : retryParse.mutate()}
                disabled={retryParse.isPending || generateNotes.isPending}
                title="Re-parse the last saved AI response without calling OpenAI again"
              >
                {retryParse.isPending ? '⏳ Retrying…' : '↻ Retry Parse'}
              </button>
            )}
            <button className="btn btn-secondary" onClick={() => setShowImportModal(true)}>Import Notes</button>
            <button className="btn btn-secondary" onClick={downloadTxt} disabled={!lecture.notes && !lecture.rawContent}>Download</button>
            <button className="btn btn-secondary" onClick={() => setShowEditModal(true)}>Edit</button>
            <button className="btn btn-danger btn-sm" onClick={() => setShowDeleteModal(true)}>Delete</button>
          </div>
        </div>
        {generateNotes.error && <div className="error-box" style={{ marginTop: 8 }}>{generateNotes.error.message}</div>}
        {retryParse.error && <div className="error-box" style={{ marginTop: 8 }}>{retryParse.error.message}</div>}
      </div>

      <div className="tabs">
        {tabs.map((t) => (
          <button key={t} className={`tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
            {t === 'notes' ? '📝 Notes' : t === 'confusions' ? '😵 Confusions' : '📄 Raw'}
          </button>
        ))}
      </div>

      {tab === 'notes' && <NotesSection notes={lecture.notes} />}
      {tab === 'confusions' && <ConfusionPanel lectureId={id} />}
      {tab === 'raw' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {lecture.rawContent && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <span style={{ fontSize: 12, color: 'var(--muted)' }}>
                ~{Math.round(lecture.rawContent.length / 4).toLocaleString()} tokens
                ({lecture.rawContent.length.toLocaleString()} chars)
              </span>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => cleanTranscript.mutate()}
                disabled={cleanTranscript.isPending}
                title="Strip filler words and non-technical chatter using a cheaper model (gpt-4.1-mini)"
              >
                {cleanTranscript.isPending ? '⏳ Cleaning…' : '🧹 Clean Transcript'}
              </button>
              {cleanTranscript.error && (
                <span style={{ fontSize: 12, color: 'var(--error)' }}>{cleanTranscript.error.message}</span>
              )}
            </div>
          )}
          <div className="card prose" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
            {lecture.rawContent || <span style={{ color: 'var(--muted)' }}>No raw content yet.</span>}
          </div>
        </div>
      )}

      <ChatDrawer lectureId={id} />

      {showEditModal && (
        <EditLectureModal lecture={lecture} onClose={() => setShowEditModal(false)} />
      )}

      {showDeleteModal && (
        <ConfirmDeleteModal
          title="Delete Lecture"
          itemName={lecture.title}
          isPending={deleteLecture.isPending}
          onConfirm={() => deleteLecture.mutate()}
          onClose={() => setShowDeleteModal(false)}
        />
      )}

      {showImportModal && (
        <div className="modal-overlay" onClick={() => setShowImportModal(false)}>
          <div className="modal" style={{ maxWidth: 700, width: '95vw' }} onClick={e => e.stopPropagation()}>
            <h3 style={{ marginBottom: 12 }}>Import Notes</h3>
            <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 12 }}>
              Paste the JSON from your AI tool (ChatGPT, Claude, etc.). Markdown code fences are fine.
              Make sure <code>full_clean_notes</code> uses markdown (headers, bold, code blocks) for proper formatting.
            </p>
            <textarea
              rows={16}
              style={{ width: '100%', fontFamily: 'monospace', fontSize: 12, resize: 'vertical', boxSizing: 'border-box' }}
              placeholder={'{\n  "title": "...",\n  "full_clean_notes": "...",\n  ...\n}'}
              value={importContent}
              onChange={e => setImportContent(e.target.value)}
            />
            {importNotes.error && (
              <div className="error-box" style={{ marginTop: 8 }}>{importNotes.error.message}</div>
            )}
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 12 }}>
              <button className="btn btn-secondary" onClick={() => { setShowImportModal(false); setImportContent('') }}>Cancel</button>
              <button
                className="btn btn-primary"
                onClick={() => setShowImportConfirm(true)}
                disabled={importNotes.isPending || !importContent.trim()}
              >
                {importNotes.isPending ? '⏳ Importing…' : 'Import'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showRegenerateConfirm && (
        <ConfirmModal
          title="Regenerate Notes"
          message="This will overwrite your existing notes. Continue?"
          confirmLabel="Yes, regenerate"
          isPending={generateNotes.isPending}
          onConfirm={() => { generateNotes.mutate(); setShowRegenerateConfirm(false) }}
          onClose={() => setShowRegenerateConfirm(false)}
        />
      )}

      {showImportConfirm && (
        <ConfirmModal
          title="Import Notes"
          message="This will overwrite your existing notes. Continue?"
          confirmLabel="Yes, import"
          isPending={importNotes.isPending}
          onConfirm={() => { importNotes.mutate(); setShowImportConfirm(false) }}
          onClose={() => setShowImportConfirm(false)}
        />
      )}

      {showRetryParseConfirm && (
        <ConfirmModal
          title="Retry Parse"
          message="This will overwrite your existing notes with the last cached AI response. Continue?"
          confirmLabel="Yes, retry"
          isPending={retryParse.isPending}
          onConfirm={() => { retryParse.mutate(); setShowRetryParseConfirm(false) }}
          onClose={() => setShowRetryParseConfirm(false)}
        />
      )}
    </div>
  )
}
